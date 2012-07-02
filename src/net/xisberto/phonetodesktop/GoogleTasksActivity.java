package net.xisberto.phonetodesktop;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.actionbarsherlock.app.SherlockActivity;
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

public class GoogleTasksActivity extends SherlockActivity {
	public static final String
		LIST_TITLE = "PhoneToDesktop",
		PREF_ACCOUNT_NAME = "accountName",
		PREF_AUTH_TOKEN = "authToken",
		PREF_LIST_ID = "listId";
	
	public static final String
		ACTION_SEND_TEXT = "net.xisberto.phonetodesktop.send_link",
		ACTION_AUTHENTICATE = "net.xisberto.phonetodesktop.authenticate";

	private static final int
		NOTIFICATION_SENDING = 0,
		NOTIFICATION_ERROR = 1,
		NOTIFICATION_NEED_AUTHORIZE = 2,
		NOTIFICATION_TIMEOUT = 3;
	
	private SharedPreferences settings;
	private GoogleAccountManager accountManager;
	private GoogleCredential credential;
	final HttpTransport transport = new NetHttpTransport();
	final JsonFactory jsonFactory = new JacksonFactory();
	private Tasks tasksService;
	
	private Looper looper;
	private Handler handler;
	
	private boolean debug = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		debug = true;
		
		//Configure a background thread
		HandlerThread thread = new HandlerThread("PhoneToDesktopThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        looper = thread.getLooper();
        handler = new Handler(looper);
        
        //Configure app's preferences
        settings = getPreferences(MODE_PRIVATE);
        accountManager = new GoogleAccountManager(this);

        //Configure GoogleCredential. loadAuthToken can return null
        credential = new GoogleCredential();
        log("Current saved token: "+loadAuthToken());
        
        //Configure and build the Tasks object
        tasksService = Tasks.builder(transport, jsonFactory)
			.setApplicationName("PhoneToDesktop")
			.setHttpRequestInitializer(credential)
			.setJsonHttpRequestInitializer(new GoogleKeyInitializer(GoogleTasksCredentialsDevelopment.APIKey))
			.build();
        
        if (getIntent().getAction().equals(ACTION_AUTHENTICATE)) {
			broadcastUpdatingStatus(true);
			authorize();
		} else if(getIntent().getAction().equals(Intent.ACTION_SEND)) {
			showNotification(NOTIFICATION_SENDING);
			addTask(getIntent().getStringExtra(Intent.EXTRA_TEXT));
		}

		finish();
	}
	
	private void log(String msg) {
		if (debug) {
			Log.i("PhoneToDesktop debug", msg);
		}
	}
	
	private void showNotification(int notification_type) {
		log("Notification shown: "+notification_type);
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
		builder
			.setContentTitle(getResources().getString(R.string.app_name))
			.setContentIntent(contentIntent);
		switch (notification_type) {
		case NOTIFICATION_SENDING:
			builder
				.setSmallIcon(android.R.drawable.stat_sys_upload)
				.setAutoCancel(false)
				.setTicker(getResources().getString(R.string.txt_sending))
				.setContentText(getResources().getString(R.string.txt_sending));
			break;
		case NOTIFICATION_ERROR:
			notificationIntent.setClass(getApplicationContext(), PhoneToDesktopActivity.class);
			contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
			builder
				.setContentIntent(contentIntent)
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.setAutoCancel(true)
				.setTicker(getResources().getString(R.string.txt_error_sending))
				.setContentText(getResources().getString(R.string.txt_error_credentials));
			break;
		case NOTIFICATION_NEED_AUTHORIZE:
			notificationIntent.setClass(getApplicationContext(), PhoneToDesktopActivity.class);
			contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);
			builder
				.setContentIntent(contentIntent)
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.setAutoCancel(true)
				.setTicker(getResources().getString(R.string.txt_error_sending))
				.setContentText(getResources().getString(R.string.txt_need_authorize));
			break;
		case NOTIFICATION_TIMEOUT:
			builder
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.setAutoCancel(true)
				.setTicker(getResources().getString(R.string.txt_error_sending))
				.setContentText(getResources().getString(R.string.txt_timeout));
			break;
		default:
			return;
		}
		manager.notify(notification_type, builder.getNotification());
	}
	
	private void dismissNotification(int notificationId) {
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(notificationId);
	}

	/**
	 * Gets a auth token from Google services for Google Tasks service. May
	 *  launch a new Activity to ask the user for permission. Stores the 
	 *  account name and the auth token in preferences and executes callback.run().
	 * @param account the Account to ask the auth token.
	 * @param callback {@link GoogleTasksCallback} object whose run method will
	 * be executed when the new auth token is obtained.
	 */
	private void getAuthToken(final Account account, final GoogleTasksCallback callback){
		AccountManagerCallback<Bundle> ac_callback =  new AccountManagerCallback<Bundle>() {
			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();
					//Here we got the auth token! Saving accountname and authtoken
					String new_auth_token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
					saveAccountName(bundle.getString(AccountManager.KEY_ACCOUNT_NAME));
					saveAuthToken(new_auth_token);
					credential.setAccessToken(new_auth_token);
					log("Token obtained: "+new_auth_token);
					//And executing the callback function
					callback.run();
				} catch (OperationCanceledException canceledException) {
					//User has canceled operation
					broadcastUpdatingStatus(false);
				} catch (SocketTimeoutException e) {
					broadcastUpdatingStatus(false);
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
					broadcastUpdatingStatus(false);
					dismissNotification(NOTIFICATION_SENDING);
					e.printStackTrace();
				}
			}
		};
		accountManager.getAccountManager().getAuthToken(account, "Manage your tasks", null, GoogleTasksActivity.this, ac_callback, handler);
	}
	
	private void authorize() {
		Account account = (Account) getIntent().getParcelableExtra("account");
		log("Starting authorization for "+account.name);
		clearCredential();
		getAuthToken(account, new GoogleTasksCallback() {
			@Override
			public void run() throws IOException {
				initList();
			}
		});
	}

	/** Initializes the Task List we will use in user's Google Tasks.
	 * Search the tasks lists for a list named "PhoneToDesktop"
	 * and create this list if it doesn't exist.
	 * 
	 */
	private void initList() throws IOException {
		log("initList");
		List<TaskList> list = null;
		list = tasksService.tasklists().list().execute().getItems();
		if (list != null) {
			if (loadListId() == null) {
				//We don't have a list id saved search in the server
				//for a list with the title PhoneToDesktop
				String serverListId = null;
				for (TaskList taskList : list) {
					if (taskList.getTitle().equals(LIST_TITLE)) {
						serverListId = taskList.getId();
						break;
					}
				}
				if (serverListId == null) {
					//The server doesn't have any list named PhoneToDesktop
					//We create it and save its id
					createAndSaveList();
				} else {
					//The server has a list named PhoneToDesktop
					//We save its id
					saveListId(serverListId);
				}
			} else {
				//We have a saved id. Let's search this id in server
				boolean serverHasList = false;
				for (TaskList taskList : list) {
					if (taskList.getId().equals(loadListId())) {
						serverHasList = true;
						break;
					}
				}
				if (!serverHasList) {
					//The server has no list with this id
					//We create a new list and save its id
					createAndSaveList();
				}
				//else
				//We have the list id and found the same id in server
				//nothing to do here
			}
		}
		broadcastUpdatingStatus(false);
	}
	
	private void createAndSaveList() throws IOException{
		TaskList newList = new TaskList();
		newList.setTitle(LIST_TITLE);
		TaskList createdList = tasksService.tasklists().insert(newList).execute();
		saveListId(createdList.getId());
	}
	
	private void addTask(final String text) {
		Account acc = accountManager.getAccountByName(loadAccountName());
		if (acc == null) {
			log("Tried to send text without authorization");
			requestSelectAccount();
		} else {
			log("Sending text");
			getAuthToken(acc, new GoogleTasksCallback() {
				@Override
				public void run() throws IOException {
					createAndPostTask(text);
				}
			});
		}
	}

	public void createAndPostTask(String text) throws IOException {
		Task task = new Task();
		task.setTitle(text);
		Insert ins = null;
		ins = tasksService.tasks().insert(loadListId(), task);
		ins.execute();
		log("Text sended");
		dismissNotification(NOTIFICATION_SENDING);
	}

	private void clearCredential() {
		accountManager.invalidateAuthToken(credential.getAccessToken());
		credential.setAccessToken(null);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove(PREF_AUTH_TOKEN);
		editor.remove(PREF_ACCOUNT_NAME);
		editor.remove(PREF_LIST_ID);
		editor.commit();
	}
	
	private void requestSelectAccount() {
		dismissNotification(NOTIFICATION_SENDING);
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
				broadcastUpdatingStatus(false);
				clearCredential();
				showNotification(NOTIFICATION_ERROR);
				Log.e(getPackageName(), e.getMessage());
				return false;
			}
		}
		Log.e(getPackageName(), e.getMessage(), e);
		return false;
	}

	private String loadAccountName() {
		return settings.getString(PREF_ACCOUNT_NAME, null);
	}
	
	private String loadAuthToken() {
		return settings.getString(PREF_AUTH_TOKEN, null);
	}
	
	private String loadListId() {
		return settings.getString(PREF_LIST_ID, null);
	}
	
	private void saveAccountName(String accountName) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_ACCOUNT_NAME, accountName);
		editor.commit();
	}

	private  void saveAuthToken(String authToken) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_AUTH_TOKEN, authToken);
		editor.commit();
	}
	
	private void saveListId(String listId) {
		log("Saving list id: "+listId);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_LIST_ID, listId);
		editor.commit();
	}
	
	public void broadcastUpdatingStatus(boolean updating) {
		Intent intent = new Intent();
		intent.setAction(ACTION_AUTHENTICATE);
		intent.putExtra("updating", updating);
		sendBroadcast(intent);
	}
	
	public interface GoogleTasksCallback {
		public void run() throws IOException;
	}
}
