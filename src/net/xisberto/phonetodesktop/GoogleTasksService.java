package net.xisberto.phonetodesktop;

import java.io.IOException;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.model.Task;
import com.google.api.services.tasks.model.TaskList;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GoogleTasksService extends IntentService {
	protected com.google.api.services.tasks.Tasks client;
	private Preferences preferences;
	private GoogleAccountCredential credential;
	private HttpTransport transport;
	private JsonFactory jsonFactory;

	public GoogleTasksService() {
		super("GoogleTasksService");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		preferences = new Preferences(this);

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
			Log.i(getPackageName(), "TasksService: " + action);
			try {
				if (action.equals(Utils.ACTION_SEND_TASK)) {
					notifySend();
					handleActionSend(intent.getStringExtra(Intent.EXTRA_TEXT));
					cancelNotifySend();
				}
			} catch (GooglePlayServicesAvailabilityIOException availabilityException) {
				Intent broadcastAvailability = new Intent(
						Utils.ACTION_SHOW_AVAILABILITY_ERROR);
				broadcastAvailability.putExtra(
						Utils.EXTRA_CONNECTION_STATUS_CODE,
						availabilityException.getConnectionStatusCode());
				LocalBroadcastManager.getInstance(this).sendBroadcast(
						broadcastAvailability);
			} catch (UserRecoverableAuthIOException userRecoverableException) {
				Log.d(getPackageName(), userRecoverableException.toString());
				PendingIntent pendingIntent = PendingIntent.getActivity(this,
						SyncActivity.REQUEST_AUTHORIZATION,
						userRecoverableException.getIntent(), 0);
				Notification notification = new NotificationCompat.Builder(this)
						.setContentIntent(pendingIntent)
						.setSmallIcon(android.R.drawable.stat_notify_error)
						.setContentTitle(
								getResources().getString(R.string.app_name))
						.setAutoCancel(true)
						.setTicker(
								getResources().getString(
										R.string.txt_error_sending))
						.setContentText(
								getResources().getString(
										R.string.txt_need_authorize)).build();
				
				((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
						.notify(0, notification);

				// Intent broadcastUserRecoverable = new Intent(
				// userRecoverableException.getIntent());
				// broadcastUserRecoverable
				// .setAction(Utils.ACTION_SHOW_USER_RECOVERABLE)
				// .putExtra(
				// "package",
				// userRecoverableException.getIntent()
				// .getComponent().getPackageName())
				// .putExtra(
				// "class",
				// userRecoverableException.getIntent()
				// .getComponent().getClassName())
				// .setComponent(
				// new ComponentName(
				// getApplicationContext(),
				// PhoneToDesktopActivity.GoogleTasksListReceiver.class));
				// ComponentName cn = broadcastUserRecoverable
				// .resolveActivity(getPackageManager());
				// Log.i(getPackageName(), cn.flattenToShortString());
				//
				// LocalBroadcastManager.getInstance(this).sendBroadcast(
				// broadcastUserRecoverable);
			} catch (IOException ioException) {
				Log.e(getPackageName(), ioException.getLocalizedMessage());
			}
		}
	}

	private void notifySend() {
		Notification notification = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_stat_sending)
				.setTicker(getString(R.string.txt_sending))
				.setContentTitle(getString(R.string.txt_sending))
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(false)
				.setOngoing(true)
				.build();
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.notify(0, notification);
	}
	
	private void cancelNotifySend() {
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.cancel(0);
	}
	
	private void handleActionSend(String text) throws IOException,
			GooglePlayServicesAvailabilityIOException,
			UserRecoverableAuthIOException {
		Task new_task = new Task().setTitle(text);
		String listId = preferences.loadListId();
		Log.d("TasksAsyncTask", "Adding task to list "+listId);
		Task result = client.tasks().insert(listId, new_task).execute();
		Log.d("TasksAsyncTask", "New task id: "+result.getId());
		stopForeground(true);
	}

	private void handleActionSaveList() throws IOException,
			GooglePlayServicesAvailabilityIOException,
			UserRecoverableAuthIOException {
		TaskList newList = new TaskList();
		newList.setTitle(Utils.LIST_TITLE);
		TaskList createdList = client.tasklists().insert(newList).execute();
		Log.i(getPackageName(), "got the list id");
		Intent broadcastList = new Intent(Utils.ACTION_SAVE_LIST);
		broadcastList.putExtra(Utils.EXTRA_LISTID, createdList.getId());
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		manager.sendBroadcast(broadcastList);
	}
}
