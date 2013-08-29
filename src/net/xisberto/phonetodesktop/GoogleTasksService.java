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
package net.xisberto.phonetodesktop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.model.LocalTask;
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
	private static final int
			NOTIFICATION_SEND = 42,
			NOTIFICATION_ERROR = 1,
			NOTIFICATION_NEED_AUTHORIZE = 2;
	
	protected com.google.api.services.tasks.Tasks client;
	private GoogleAccountCredential credential;
	private HttpTransport transport;
	private JsonFactory jsonFactory;

	private Preferences preferences;
	private String list_id;

	private Builder mBuilder;

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
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			final String action = intent.getAction();
			long extra_id = intent.getLongExtra(Utils.EXTRA_TASK_ID, -1);
			long[] tasks_ids = intent.getLongArrayExtra(Utils.EXTRA_TASKS_IDS);
			try {
				if (action.equals(Utils.ACTION_SEND_SINGLE_TASK)) {
					if (isOnline()) {
						showNotification(NOTIFICATION_SEND);
						handleActionSend(extra_id);
						stopForeground(true);
					} else {
						revertTaskToReady(extra_id);
					}
				} else if (action.equals(Utils.ACTION_SEND_MULTIPLE_TASKS)) {
					if (isOnline()) {
						showNotification(NOTIFICATION_SEND);
						handleActionSendMultiple(tasks_ids);
						stopForeground(true);
					} else {
						revertTaskToReady(tasks_ids);
					}
				} else if (action.equals(Utils.ACTION_LIST_TASKS)) {
					handleActionList();
				} else if (action.equals(Utils.ACTION_REMOVE_TASK)) {
					handleActionRemove(intent.getStringExtra(Utils.EXTRA_TASK_ID));
				}
			} catch (UserRecoverableAuthIOException userRecoverableException) {
				Utils.log(Log.getStackTraceString(userRecoverableException));
				stopForeground(true);
				revertTaskToReady(extra_id);
				showNotification(NOTIFICATION_NEED_AUTHORIZE);
			} catch (IOException ioException) {
				Utils.log(Log.getStackTraceString(ioException));
				if (action.equals(Utils.ACTION_SEND_SINGLE_TASK)) {
					stopForeground(true);
					revertTaskToReady(extra_id);
					showNotification(NOTIFICATION_ERROR);
				} else {
					Intent broadcast = new Intent(Utils.ACTION_LIST_TASKS);
					broadcast.putExtra(Utils.EXTRA_ERROR_TEXT, getString(R.string.txt_error_list));
					LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
				}
			} catch (NullPointerException npe) {
				Utils.log(Log.getStackTraceString(npe));
				stopForeground(true);
				revertTaskToReady(extra_id);
				showNotification(NOTIFICATION_NEED_AUTHORIZE);
			}
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Utils.log("Destroying service");
		stopForeground(true);
	}

	private void showNotification(int notif_id) {
		mBuilder = new NotificationCompat.Builder(this)
				.setWhen(System.currentTimeMillis());
		
		Intent intentContent = new Intent();
		PendingIntent pendingContent;
		
		switch (notif_id) {
		case NOTIFICATION_SEND:
			//Set an empty Intent for the notification
			pendingContent = PendingIntent.getActivity(
					this, 0, intentContent, PendingIntent.FLAG_CANCEL_CURRENT);
			mBuilder.setContentIntent(pendingContent)
					.setSmallIcon(android.R.drawable.stat_sys_upload)
					.setTicker(getString(R.string.txt_sending))
					.setContentTitle(getString(R.string.txt_sending));
			startForeground(NOTIFICATION_SEND, mBuilder.build());
			return;
		case NOTIFICATION_ERROR:
			//On error, we create an intent to retry the send
			intentContent.setClass(this, SendTasksActivity.class);
			intentContent.setAction(Intent.ACTION_SEND);
			intentContent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intentContent.putExtra(Intent.EXTRA_TEXT, preferences.loadLastSentText());
			PendingIntent pendingError = PendingIntent.getActivity(
					this, 0, intentContent, PendingIntent.FLAG_CANCEL_CURRENT);
			mBuilder.setContentIntent(pendingError)
					.setAutoCancel(true)
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.setTicker(getString(R.string.txt_error_sending))
					.setContentTitle(getString(R.string.txt_error_sending))
					.setContentText(getString(R.string.txt_error_try_again));
			break;
		case NOTIFICATION_NEED_AUTHORIZE:
			intentContent.setClass(this, PhoneToDesktopActivity.class);
			intentContent.setAction(Utils.ACTION_AUTHENTICATE);
			intentContent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
			PendingIntent pendingAuthorize = PendingIntent.getActivity(
					this, 0, intentContent, PendingIntent.FLAG_CANCEL_CURRENT);
			mBuilder.setContentIntent(pendingAuthorize)
					.setAutoCancel(true)
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.setTicker(getString(R.string.txt_error_sending))
					.setContentTitle(getString(R.string.txt_error_sending))
					.setContentText(getString(R.string.txt_need_authorize));
			break;

		default:
			break;
		}
		((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
			.notify(notif_id, mBuilder.build());
	}

	private void handleActionSend(long task_local_id)
			throws UserRecoverableAuthIOException, IOException {
		if (task_local_id != -1) {
			DatabaseHelper databaseHelper = DatabaseHelper.getInstance(this);
			LocalTask task = databaseHelper.getTask(task_local_id);
			
			Task new_task = new Task().setTitle(task.getTitle());
			client.tasks().insert(list_id, new_task).execute();
			
			preferences.saveLastSentText("");
			
			task.setStatus(Status.SENT)
				.persist();
		}
	}
	
	private void handleActionSendMultiple(long[] tasks_ids)
			throws UserRecoverableAuthIOException, IOException {
		Intent intent = new Intent(this, WaitListActivity.class);
		PendingIntent pending = PendingIntent.getActivity(this, 0,
				intent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pending);
		for (int i = 0; i < tasks_ids.length; i++) {
			String contentText = getString(R.string.txt_sending_multiple);
			contentText = String.format(contentText, i+1, tasks_ids.length);
			mBuilder.setContentText(contentText);
			((NotificationManager)getSystemService(NOTIFICATION_SERVICE))
				.notify(NOTIFICATION_SEND, mBuilder.build());
			handleActionSend(tasks_ids[i]);
		}
	}
	
	private void handleActionList()
			throws UserRecoverableAuthIOException, IOException {
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
	
	private boolean isOnline() {
	    ConnectivityManager cm =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	}
	
}
