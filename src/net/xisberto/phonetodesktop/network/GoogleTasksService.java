/*******************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop.network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.xisberto.phonetodesktop.PhoneToDesktopActivity;
import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.WaitListActivity;
import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.model.LocalTask;
import net.xisberto.phonetodesktop.model.LocalTask.Options;
import net.xisberto.phonetodesktop.model.LocalTask.PersistCallback;
import net.xisberto.phonetodesktop.model.LocalTask.Status;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.model.Task;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class GoogleTasksService extends IntentService {
	private static final int NOTIFICATION_SEND = 42,
			NOTIFICATION_SEND_LATER = 3, NOTIFICATION_ERROR = 1,
			NOTIFICATION_NEED_AUTHORIZE = 2;

	protected com.google.api.services.tasks.Tasks client;
	private GoogleAccountCredential credential;
	private HttpTransport transport;
	private JsonFactory jsonFactory;

	private Preferences preferences;
	private String list_id;

	private String[] cache_unshorten, cache_titles;

	public GoogleTasksService() {
		super("GoogleTasksService");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		preferences = new Preferences(this);
		list_id = preferences.loadListId();

		credential = GoogleAccountCredential.usingOAuth2(this, Utils.scopes);
		credential.setSelectedAccountName(preferences.loadAccountName());

		transport = AndroidHttp.newCompatibleTransport();
		jsonFactory = new GsonFactory();

		client = new com.google.api.services.tasks.Tasks.Builder(transport,
				jsonFactory, credential).setApplicationName("PhoneToDesktop")
				.build();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Utils.log("onStartCommand "+ intent.getAction());
		if (Utils.ACTION_SEND_TASKS.equals(intent.getAction())) {
			// We start foregroud as soon as we receive an ACTION_SEND_TASKS
			// action. This will make the service foreground when
			// SendTaskActivity goes away
			startForeground(NOTIFICATION_SEND,
					buildNotification(NOTIFICATION_SEND).build());
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			Utils.log("onHandleIntent "+ intent.getAction());
			long[] tasks_ids = intent.getLongArrayExtra(Utils.EXTRA_TASKS_IDS);
			try {
				if (action.equals(Utils.ACTION_PROCESS_TASK)) {
					long task_id = intent.getLongExtra(Utils.EXTRA_TASK_ID, -1);
					LocalTask task = DatabaseHelper.getInstance(this).getTask(
							task_id);
					final Intent result = new Intent(
							Utils.ACTION_RESULT_PROCESS_TASK);
					result.putExtra(Utils.EXTRA_TASK_ID, task.getLocalId());
					if (isOnline()) {
						processOptions(task);
						task.persistBlocking(new PersistCallback() {
							@Override
							public void run() {
								if (cache_unshorten != null) {
									result.putExtra(
											Utils.EXTRA_CACHE_UNSHORTEN,
											cache_unshorten);
								}
								if (cache_unshorten != null) {
									result.putExtra(Utils.EXTRA_CACHE_TITLES,
											cache_titles);
								}
								LocalBroadcastManager.getInstance(
										GoogleTasksService.this).sendBroadcast(
										result);
							}
						});
					} else {
						revertTaskToReady(tasks_ids);
						LocalBroadcastManager.getInstance(this).sendBroadcast(
								result);
					}
				} else if (action.equals(Utils.ACTION_SEND_TASKS)) {
					if (isOnline()) {

						if (tasks_ids.length == 1) {
							DatabaseHelper databaseHelper = DatabaseHelper
									.getInstance(this);
							LocalTask task = databaseHelper
									.getTask(tasks_ids[0]);
							handleActionSend(task);
						} else {
							handleActionSendMultiple(tasks_ids);
						}

						stopForeground(true);
					} else {
						revertTaskToReady(tasks_ids);
						((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
								.notify(NOTIFICATION_SEND_LATER,
										buildNotification(
												NOTIFICATION_SEND_LATER)
												.build());
					}
				} else if (action.equals(Utils.ACTION_LIST_TASKS)) {
					handleActionList();
				} else if (action.equals(Utils.ACTION_REMOVE_TASK)) {
					handleActionRemove(intent
							.getStringExtra(Utils.EXTRA_TASK_ID));
				}
			} catch (UserRecoverableAuthIOException userRecoverableException) {
				Utils.log(Log.getStackTraceString(userRecoverableException));
				stopForeground(true);
				revertTaskToReady(tasks_ids);
				((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
						.notify(NOTIFICATION_NEED_AUTHORIZE,
								buildNotification(NOTIFICATION_NEED_AUTHORIZE)
										.build());
			} catch (IOException ioException) {
				Utils.log(Log.getStackTraceString(ioException));
				if (action.equals(Utils.ACTION_SEND_TASKS)) {
					stopForeground(true);
					revertTaskToReady(tasks_ids);
					((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
							.notify(NOTIFICATION_ERROR,
									buildNotification(NOTIFICATION_ERROR)
											.build());
				} else {
					Intent broadcast = new Intent(Utils.ACTION_LIST_TASKS);
					broadcast.putExtra(Utils.EXTRA_ERROR_TEXT,
							getString(R.string.txt_error_list));
					LocalBroadcastManager.getInstance(this).sendBroadcast(
							broadcast);
				}
			} catch (NullPointerException npe) {
				Utils.log(Log.getStackTraceString(npe));
				stopForeground(true);
				revertTaskToReady(tasks_ids);
				((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
						.notify(NOTIFICATION_NEED_AUTHORIZE,
								buildNotification(NOTIFICATION_NEED_AUTHORIZE)
										.build());
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Utils.log("Destroying service");
	}

	private NotificationCompat.Builder buildNotification(int notif_id) {
		Builder builder = new NotificationCompat.Builder(this).setWhen(System
				.currentTimeMillis());
		// The default notification send the user to the waiting list
		Intent intentContent = new Intent(this, WaitListActivity.class);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(WaitListActivity.class);
		stackBuilder.addNextIntent(intentContent);
		PendingIntent pendingContent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingContent);

		switch (notif_id) {
		case NOTIFICATION_SEND:
			builder.setSmallIcon(android.R.drawable.stat_sys_upload)
					.setTicker(getString(R.string.txt_sending))
					.setContentTitle(getString(R.string.txt_sending));
			return builder;
		case NOTIFICATION_SEND_LATER:
			builder.setAutoCancel(true)
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.setTicker(getString(R.string.txt_error_no_connection))
					.setContentTitle(
							getString(R.string.txt_error_no_connection))
					.setContentText(getString(R.string.txt_error_try_again));
			return builder;
		case NOTIFICATION_ERROR:
			builder.setAutoCancel(true)
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.setTicker(getString(R.string.txt_error_sending))
					.setContentTitle(getString(R.string.txt_error_sending))
					.setContentText(getString(R.string.txt_error_try_again));
			return builder;
		case NOTIFICATION_NEED_AUTHORIZE:
			// When authorization is need, send the user to authorization
			// process
			intentContent.setClass(this, PhoneToDesktopActivity.class);
			intentContent.setAction(Utils.ACTION_AUTHENTICATE);
			intentContent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TOP);
			PendingIntent pendingAuthorize = PendingIntent.getActivity(this, 0,
					intentContent, PendingIntent.FLAG_CANCEL_CURRENT);
			builder.setContentIntent(pendingAuthorize).setAutoCancel(true)
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.setTicker(getString(R.string.txt_error_sending))
					.setContentTitle(getString(R.string.txt_error_sending))
					.setContentText(getString(R.string.txt_need_authorize));
			return builder;

		default:
			return null;
		}
	}

	private void handleActionSend(LocalTask task)
			throws UserRecoverableAuthIOException, IOException {

		PersistCallback callback = new PersistCallback() {
			@Override
			public void run() {
				LocalBroadcastManager.getInstance(GoogleTasksService.this)
						.sendBroadcast(
								new Intent(Utils.ACTION_LIST_LOCAL_TASKS));
			}
		};

		task.setStatus(Status.SENDING).persist(callback);
		
		Utils.log("Sending task " + task.getTitle());

		Task new_task = new Task().setTitle(task.getTitle());
		client.tasks().insert(list_id, new_task).execute();

		task.setStatus(Status.SENT).persist(callback);

	}

	private void handleActionSendMultiple(long... tasks_ids)
			throws UserRecoverableAuthIOException, IOException {
		Builder builder = buildNotification(NOTIFICATION_SEND);

		int i = 0;
		for (long task_id : tasks_ids) {
			String contentText = getString(R.string.txt_sending_multiple);
			contentText = String.format(contentText, i + 1, tasks_ids.length);
			builder.setContentText(contentText);
			((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
					.notify(NOTIFICATION_SEND, builder.build());

			DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
			LocalTask task = databaseHelper.getTask(task_id);

			handleActionSend(task);
		}
	}

	private void handleActionList() throws UserRecoverableAuthIOException,
			IOException {
		List<Task> list = client.tasks().list(list_id).execute().getItems();

		if (list != null) {
			ArrayList<String> ids = new ArrayList<String>();
			ArrayList<String> titles = new ArrayList<String>();
			for (Task task : list) {
				ids.add(task.getId());
				titles.add(task.getTitle());
			}

			Intent broadcast = new Intent(Utils.ACTION_LIST_TASKS);
			broadcast.putStringArrayListExtra(Utils.EXTRA_IDS, ids);
			broadcast.putStringArrayListExtra(Utils.EXTRA_TITLES, titles);
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
		}
	}

	private void handleActionRemove(String task_id)
			throws UserRecoverableAuthIOException, IOException {
		client.tasks().delete(list_id, task_id).execute();
		handleActionList();
	}

	private void revertTaskToReady(long... local_ids) {
		if (local_ids != null) {
			DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
			databaseHelper.setStatus(Status.READY, local_ids);
		}
	}

	private void processOptions(LocalTask task) throws IOException {
		URLOptions urlOptions = new URLOptions();
		String[] parts;
		cache_unshorten = null;
		cache_titles = null;
		switch (task.getStatus()) {
		case ADDED:
		case READY:
			if (task.hasOption(Options.OPTION_UNSHORTEN)) {
				task.setStatus(Status.PROCESSING_UNSHORTEN).persist();
				String links = Utils.filterLinks(task.getTitle()).trim();
				parts = urlOptions.unshorten(links.split(" "));
				cache_unshorten = parts.clone();
				task.setTitle(Utils.replace(task.getTitle(), parts))
						.removeOption(Options.OPTION_UNSHORTEN);
			}
			if (task.hasOption(Options.OPTION_GETTITLES)) {
				task.setStatus(Status.PROCESSING_TITLE).persist();
				String links = Utils.filterLinks(task.getTitle()).trim();
				parts = urlOptions.getTitles(links.split(" "));
				cache_titles = parts.clone();
				task.setTitle(Utils.appendInBrackets(task.getTitle(), parts))
						.removeOption(Options.OPTION_GETTITLES);
			}
			task.setStatus(Status.READY);
			break;
		case PROCESSING_UNSHORTEN:
			parts = Utils.filterLinks(task.getTitle()).split(" ");
			parts = urlOptions.unshorten(parts);
			cache_unshorten = parts.clone();
			task.setTitle(Utils.replace(task.getTitle(), parts)).removeOption(
					Options.OPTION_UNSHORTEN);
			if (!task.hasOption(Options.OPTION_GETTITLES)) {
				task.setStatus(Status.READY);
				break;
			}
		case PROCESSING_TITLE:
			parts = Utils.filterLinks(task.getTitle()).split(" ");
			parts = urlOptions.getTitles(parts);
			cache_titles = parts.clone();
			task.setTitle(Utils.appendInBrackets(task.getTitle(), parts))
					.removeOption(Options.OPTION_GETTITLES)
					.setStatus(Status.READY);
			break;
		}
	}

	private boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnectedOrConnecting()) {
			return true;
		}
		return false;
	}

}
