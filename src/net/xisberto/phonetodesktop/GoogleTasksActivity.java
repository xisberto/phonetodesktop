/*******************************************************************************
 * Copyright (c) 2012 Humberto Fraga <xisberto@gmail.com>.
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
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.GoogleKeyInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.Tasks.TasksOperations.Insert;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;

public class GoogleTasksActivity extends SherlockFragmentActivity implements
		OnItemClickListener {
	public static final String LIST_TITLE = "PhoneToDesktop";

	public static final String ACTION_AUTHENTICATE = "net.xisberto.phonetodesktop.authenticate",
			ACTION_LIST_TASKS = "net.xisberto.phonetodesktop.list_tasks",
			ACTION_REMOVE_TASKS = "net.xisberto.phonetodesktop.remove_task";

	private static final int NOTIFICATION_SENDING = 0, NOTIFICATION_ERROR = 1,
			NOTIFICATION_NEED_AUTHORIZE = 2, NOTIFICATION_TIMEOUT = 3;

	private SharedPreferences settings;
	private GoogleAccountManager accountManager;
	private GoogleCredential credential;
	public static GoogleTasksCredentials my_credentials = new GoogleTasksCredentials();

	final HttpTransport transport = new NetHttpTransport();
	final JsonFactory jsonFactory = new JacksonFactory();
	private Tasks tasksService;

	private Looper looper;
	private Handler handler;

	private WhatToSendDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Configure a background thread
		HandlerThread thread = new HandlerThread("PhoneToDesktopThread",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		looper = thread.getLooper();
		handler = new Handler(looper);

		// Configure app's preferences
		settings = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		accountManager = new GoogleAccountManager(getApplicationContext());

		// Configure GoogleCredential. loadAuthToken can return null
		credential = new GoogleCredential();
		log("Current saved token: " + loadAuthToken());

		// Configure and build the Tasks object
		tasksService = new Tasks.Builder(transport, jsonFactory, credential)
				.setApplicationName("PhoneToDesktop")
				.setJsonHttpRequestInitializer(
						new GoogleKeyInitializer(my_credentials.getAPIKey()))
				.build();

		if (getIntent().getAction().equals(ACTION_AUTHENTICATE)) {
			broadcastUpdatingStatus(ACTION_AUTHENTICATE, true);
			authorize();
		} else if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
			addTask(loadWhatToSend(),
					getIntent().getStringExtra(Intent.EXTRA_TEXT));
		} else if (getIntent().getAction().equals(ACTION_LIST_TASKS)) {
			broadcastUpdatingStatus(ACTION_LIST_TASKS, true);
			getTaskList();
		} else if (getIntent().getAction().equals(ACTION_REMOVE_TASKS)) {
			removeTask(getIntent().getStringExtra("task_id"));
		}

	}

	private void log(String msg) {
		if (my_credentials.getClass().equals(
				GoogleTasksCredentialsDevelopment.class)) {
			Log.i("PhoneToDesktop debug", msg);
		}
	}

	private void showNotification(int notification_type) {
		log("Notification shown: " + notification_type);
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(
				getApplicationContext(), 0, notificationIntent, 0);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				getApplicationContext());
		builder.setContentTitle(getResources().getString(R.string.app_name))
				.setContentIntent(contentIntent);
		switch (notification_type) {
		case NOTIFICATION_SENDING:
			builder.setSmallIcon(android.R.drawable.stat_sys_upload)
					.setAutoCancel(false)
					.setTicker(getResources().getString(R.string.txt_sending))
					.setContentText(
							getResources().getString(R.string.txt_sending));
			break;
		case NOTIFICATION_ERROR:
			notificationIntent.setClass(getApplicationContext(),
					PhoneToDesktopActivity.class);
			contentIntent = PendingIntent.getActivity(getApplicationContext(),
					0, notificationIntent, 0);
			builder.setContentIntent(contentIntent)
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.setAutoCancel(true)
					.setTicker(
							getResources()
									.getString(R.string.txt_error_sending))
					.setContentText(
							getResources().getString(
									R.string.txt_error_credentials));
			break;
		case NOTIFICATION_NEED_AUTHORIZE:
			notificationIntent.setClass(getApplicationContext(),
					PhoneToDesktopActivity.class);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			contentIntent = PendingIntent.getActivity(getApplicationContext(),
					0, notificationIntent, 0);
			builder.setContentIntent(contentIntent)
					.setSmallIcon(android.R.drawable.stat_notify_error)
					.setAutoCancel(true)
					.setTicker(
							getResources()
									.getString(R.string.txt_error_sending))
					.setContentText(
							getResources().getString(
									R.string.txt_need_authorize));
			break;
		case NOTIFICATION_TIMEOUT:
			builder.setSmallIcon(android.R.drawable.stat_notify_error)
					.setAutoCancel(true)
					.setTicker(
							getResources()
									.getString(R.string.txt_error_sending))
					.setContentText(
							getResources().getString(R.string.txt_timeout));
			break;
		default:
			return;
		}
		manager.notify(notification_type, builder.build());
	}

	private void dismissNotification(int notificationId) {
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(notificationId);
	}

	/**
	 * Gets a auth token from Google services for Google Tasks service. May
	 * launch a new Activity to ask the user for permission. Stores the account
	 * name and the auth token in preferences and executes callback.run().
	 * 
	 * @param account
	 *            the Account to ask the auth token.
	 * @param callback
	 *            {@link GoogleTasksCallback} object whose run method will be
	 *            executed when the new auth token is obtained.
	 */
	private void getAuthToken(final Account account,
			final GoogleTasksCallback callback) {
		AccountManagerCallback<Bundle> ac_callback = new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();
					// Here we got the auth token! Saving accountname and
					// authtoken
					String new_auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
					//saveAccountName(bundle.getString(AccountManager.KEY_ACCOUNT_NAME));
					saveAuthToken(new_auth_token);
					credential.setAccessToken(new_auth_token);
					log("Token obtained: " + new_auth_token);
					// And executing the callback function
					callback.run();
				} catch (OperationCanceledException canceledException) {
					// User has canceled operation
					broadcastUpdatingStatus(ACTION_AUTHENTICATE, false);
				} catch (SocketTimeoutException e) {
					broadcastUpdatingStatus(ACTION_AUTHENTICATE, false);
					log("Timeout");
					dismissNotification(NOTIFICATION_SENDING);
					showNotification(NOTIFICATION_TIMEOUT);
				} catch (IOException e) {
					if (e instanceof GoogleJsonResponseException) {
						log("Got an GoogleJson exception");
						if (handleGoogleException(e)) {
							getAuthToken(account, callback);
						}
					} else {
						e.printStackTrace();
					}
				} catch (AuthenticatorException e) {
					e.printStackTrace();
				} catch (Exception e) {
					broadcastUpdatingStatus(ACTION_AUTHENTICATE, false);
					dismissNotification(NOTIFICATION_SENDING);
					e.printStackTrace();
				}
			}
		};
		accountManager.getAccountManager().getAuthToken(account,
				"Manage your tasks", null, GoogleTasksActivity.this,
				ac_callback, handler);
	}

	private void authorize() {
		Account account = (Account) getIntent().getParcelableExtra("account");
		log("Starting authorization for " + account.name);
		clearCredential();
		getAuthToken(account, new GoogleTasksCallback() {
			@Override
			public void run() throws IOException {
				initList();
			}
		});
		finish();
	}

	/**
	 * Initializes the Task List we will use in user's Google Tasks. Search the
	 * tasks lists for a list named "PhoneToDesktop" and create this list if it
	 * doesn't exist.
	 * 
	 */
	private void initList() throws IOException {
		log("initList");
		List<TaskList> list = null;
		list = tasksService.tasklists().list().execute().getItems();
		if (list != null) {
			if (loadListId() == null) {
				// We don't have a list id saved search in the server
				// for a list with the title PhoneToDesktop
				String serverListId = null;
				for (TaskList taskList : list) {
					if (taskList.getTitle().equals(LIST_TITLE)) {
						serverListId = taskList.getId();
						break;
					}
				}
				if (serverListId == null) {
					// The server doesn't have any list named PhoneToDesktop
					// We create it and save its id
					doInitList();
				} else {
					// The server has a list named PhoneToDesktop
					// We save its id
					saveListId(serverListId);
				}
			} else {
				// We have a saved id. Let's search this id in server
				boolean serverHasList = false;
				for (TaskList taskList : list) {
					if (taskList.getId().equals(loadListId())) {
						serverHasList = true;
						break;
					}
				}
				if (!serverHasList) {
					// The server has no list with this id
					// We create a new list and save its id
					doInitList();
				}
				// else
				// We have the list id and found the same id in server
				// nothing to do here
			}
		}
		broadcastUpdatingStatus(ACTION_AUTHENTICATE, false);
		finish();
	}

	private void doInitList() throws IOException {
		TaskList newList = new TaskList();
		newList.setTitle(LIST_TITLE);
		TaskList createdList = tasksService.tasklists().insert(newList)
				.execute();
		saveListId(createdList.getId());
	}

	private void addTask(final String what_to_send, final String text) {
		Account acc = accountManager.getAccountByName(loadAccountName());
		if (acc == null) {
			log("Tried to send text without authorization");
			requestSelectAccount();
			finish();
		} else {
			String[] entryvalues_what_to_send = getResources().getStringArray(
					R.array.entryvalues_what_to_send);
			if ((what_to_send.equals(entryvalues_what_to_send[0]))
					|| what_to_send.equals(entryvalues_what_to_send[1])) {
				showNotification(NOTIFICATION_SENDING);
				getAuthToken(acc, new GoogleTasksCallback() {
					@Override
					public void run() throws IOException {
						doAddTask(what_to_send, text);
					}
				});
				finish();
			} else {
				log("Asking what to send");
				dialog = WhatToSendDialog.newInstance();
				dialog.show(getSupportFragmentManager(), "what_to_send_dialog");
			}
		}
	}

	private String filterLinks(String text) {
		String[] parts = text.split("\\s");
		String result = "";
		for (int i = 0; i < parts.length; i++) {
			try {
				URL u = new URL(parts[i]);
				result += parts[i] + " ";
			} catch (MalformedURLException e) {
				// do nothing
			}
		}
		return result;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		log("Selected item on WhatToSendDialog: " + position);
		CheckBox check_save_option = (CheckBox) dialog.getView().findViewById(
				R.id.check_save_option);
		if (check_save_option.isChecked()) {
			saveWhatToSend(position);
		} else {
			saveWhatToSend(2);
		}
		addTask(getResources().getStringArray(R.array.entryvalues_what_to_send)[position],
				getIntent().getStringExtra(Intent.EXTRA_TEXT));
	}

	public void doAddTask(String what_to_send, String text) throws IOException {
		Task task = new Task();
		String[] entryvalues_what_to_send = getResources().getStringArray(
				R.array.entryvalues_what_to_send);
		if (what_to_send.equals(entryvalues_what_to_send[0])) {
			log("Entire text");
			task.setTitle(text);
		} else if (what_to_send.equals(entryvalues_what_to_send[1])) {
			log("Only links");
			task.setTitle(filterLinks(text));
		} else {
			// Any other value to what_to_send will be ignored
			// The entire text will be sent, and next time we will ask the user
			// what to send
			log("Reverting to default");
			task.setTitle(text);
			saveWhatToSend(2);
		}

		Insert ins = null;
		ins = tasksService.tasks().insert(loadListId(), task);
		ins.execute();
		log("Text sent");
		dismissNotification(NOTIFICATION_SENDING);
	}

	private void removeTask(final String task_id) {
		Account acc = accountManager.getAccountByName(loadAccountName());
		if (acc == null) {
			log("Tried to remove task without authorization.");
			requestSelectAccount();
		} else {
			log("Removing task " + task_id);
			getAuthToken(acc, new GoogleTasksCallback() {
				@Override
				public void run() throws IOException {
					doRemoveTask(task_id);
				}
			});
		}
		finish();
	}

	private void doRemoveTask(String task_id) throws IOException {
		com.google.api.services.tasks.Tasks.TasksOperations.Delete del = null;
		del = tasksService.tasks().delete(loadListId(), task_id);
		del.execute();
		log("Task removed");
	}

	private void getTaskList() {
		Account acc = accountManager.getAccountByName(loadAccountName());
		if (acc == null) {
			log("Tried to get task list without authorization.");
			requestSelectAccount();
		} else {
			log("Getting task list");
			getAuthToken(acc, new GoogleTasksCallback() {
				@Override
				public void run() throws IOException {
					doGetTaskList();
				}
			});
		}
		finish();
	}

	private void doGetTaskList() throws IOException {
		com.google.api.services.tasks.model.Tasks tasks = tasksService.tasks()
				.list(loadListId()).execute();
		ArrayList<String> ids = new ArrayList<String>(), titles = new ArrayList<String>();

		if (tasks.getItems() != null) {
			List<Task> list = tasks.getItems();
			for (Task task : list) {
				ids.add(task.getId());
				titles.add(task.getTitle());
			}
		}

		broadcastTaskList(ids, titles);
	}

	private void clearCredential() {
		accountManager.invalidateAuthToken(credential.getAccessToken());
		credential.setAccessToken(null);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(getResources().getString(R.string.pref_auth_token));
		//editor.remove(getResources().getString(R.string.pref_account_name));
		editor.remove(getResources().getString(R.string.pref_list_id));
		apply(editor);
	}

	private void requestSelectAccount() {
		dismissNotification(NOTIFICATION_SENDING);
		broadcastUpdatingStatus(ACTION_AUTHENTICATE, false);
		broadcastTaskList(null, null);
		clearCredential();
		showNotification(NOTIFICATION_NEED_AUTHORIZE);
	}

	private boolean handleGoogleException(IOException e) {
		if (e instanceof GoogleJsonResponseException) {
			GoogleJsonResponseException exception = (GoogleJsonResponseException) e;
			switch (exception.getStatusCode()) {
			case 401:
				accountManager.invalidateAuthToken(credential.getAccessToken());
				credential.setAccessToken(null);
				saveAuthToken(null);
				return true;
			case 404:
				dismissNotification(NOTIFICATION_SENDING);
				broadcastUpdatingStatus(ACTION_AUTHENTICATE, false);
				clearCredential();
				showNotification(NOTIFICATION_ERROR);
				Log.e(getPackageName(), e.getMessage());
				return false;
			}
		}
		Log.e(getPackageName(), e.getMessage(), e);
		return false;
	}

	private void updateSetting(String key) {
		SharedPreferences settings_old = getPreferences(MODE_PRIVATE);
		if (settings_old.contains(key)) {
			String value = settings_old.getString(key, "");
			apply(settings.edit().putString(key, value));
			apply(settings_old.edit().remove(key));
		}
	}

	private String loadAccountName() {
		updateSetting(getResources().getString(R.string.pref_account_name));
		return settings.getString(
				getResources().getString(R.string.pref_account_name), null);
	}

	private String loadAuthToken() {
		updateSetting(getResources().getString(R.string.pref_auth_token));
		return settings.getString(
				getResources().getString(R.string.pref_auth_token), null);
	}

	private String loadListId() {
		updateSetting(getResources().getString(R.string.pref_list_id));
		return settings.getString(
				getResources().getString(R.string.pref_list_id), null);
	}

	private String loadWhatToSend() {
		updateSetting(getResources().getString(R.string.pref_what_to_send));
		return settings
				.getString(
						getResources().getString(R.string.pref_what_to_send),
						getResources().getStringArray(
								R.array.entryvalues_what_to_send)[2]);
	}

	private void saveAccountName(String accountName) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(getResources().getString(R.string.pref_account_name),
				accountName);
		apply(editor);
	}

	private void saveAuthToken(String authToken) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(getResources().getString(R.string.pref_auth_token),
				authToken);
		apply(editor);
	}

	private void saveListId(String listId) {
		log("Saving list id: " + listId);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(getResources().getString(R.string.pref_list_id),
				listId);
		apply(editor);
	}

	private void saveWhatToSend(int value) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(
				getResources().getString(R.string.pref_what_to_send),
				getResources().getStringArray(R.array.entryvalues_what_to_send)[value]);
		apply(editor);
	}

	@SuppressLint("NewApi")
	private void apply(Editor editor) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			editor.apply();
		} else {
			editor.commit();
		}
	}

	public void broadcastUpdatingStatus(String action, boolean updating) {
		Intent intent = new Intent();
		intent.setAction(action);
		intent.putExtra("updating", updating);
		sendBroadcast(intent);
	}

	public void broadcastTaskList(ArrayList<String> ids,
			ArrayList<String> titles) {
		Intent intent = new Intent();
		intent.setAction(ACTION_LIST_TASKS);
		intent.putStringArrayListExtra("ids", ids);
		intent.putStringArrayListExtra("titles", titles);
		intent.putExtra("done", true);
		sendBroadcast(intent);
	}

	public interface GoogleTasksCallback {
		public void run() throws IOException;
	}
}
