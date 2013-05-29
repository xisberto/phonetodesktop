package net.xisberto.phonetodesktop;

import java.util.List;

import net.xisberto.phonetodesktop.google_tasks_api.ListAsyncTask;
import net.xisberto.phonetodesktop.google_tasks_api.TaskListModel;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.services.tasks.model.TaskList;

public class AuthorizationActivity extends SyncActivity {
	public static final String ACTION_AUTHENTICATE = "net.xisberto.phonetodesktop.authenticate";

	private ListAsyncTask listManager;
	public List<TaskList> tasklist = null;
	public String listId = null;

	public TaskListModel model;

	private TextView text_waiting_auth;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authorize);

		text_waiting_auth = (TextView) findViewById(R.id.txt_waiting_authorization);

		Intent intent = new Intent(ACTION_AUTHENTICATE);
		intent.putExtra("updating", false);
		sendBroadcast(intent);

		authorize();
	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d(TAG, "onActivityResult code: " + requestCode);
		switch (requestCode) {
		case REQUEST_AUTHORIZATION:
			Log.d(TAG, "loading lists");
			if (resultCode == RESULT_OK) {
				requestLists();
			} else {
				finish();
			}
			break;
		case REQUEST_ACCOUNT_PICKER:
			Log.d(TAG, "saving account name");
			if (resultCode == RESULT_OK && data != null
					&& data.getExtras() != null) {
				String accountName = data.getExtras().getString(
						AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					preferences.saveAccountName(accountName);
					/*
					 * When returning from first GoogleAccountCredential
					 * authorization, it's soon to make a call to the API. The
					 * system will still show some screens and maybe ask for a
					 * list of permissions. Setting this to true will allow that
					 * we load the lists on onResume
					 */
					// requestLists = true;
					requestLists();
				}
			} else {
				finish();
			}
			break;
		case REQUEST_GOOGLE_PLAY_SERVICES:
			authorize();
			break;

		default:
			break;
		}
	}

	private void requestLists() {
		text_waiting_auth.setText(R.string.txt_loading_lists);
		listManager = new ListAsyncTask(this, ListAsyncTask.REQUEST_LOAD_LISTS);
		listManager.execute();
	}
	
	private void saveList() {
		text_waiting_auth.setText(R.string.txt_saving_list);
		listManager = new ListAsyncTask(this,
				ListAsyncTask.REQUEST_SAVE_LIST);
		listManager.execute();	
	}

	@Override
	public void refreshView() {
		Log.d(TAG, "refreshView");
		if (listManager != null) {
			Log.d(TAG, "request: " + listManager.getRequest());
			if (listManager.getRequest() == ListAsyncTask.REQUEST_LOAD_LISTS) {
				listManager = null;
				initList();
			} else if (listManager.getRequest() == ListAsyncTask.REQUEST_SAVE_LIST) {
				Log.d(TAG, "listid: " + listId);
				preferences.saveListId(listId);
				listManager = null;
				finish();
			}
		}

	}

	private void authorize() {
		clearCredential();
		if (checkGooglePlayServicesAvailable()) {
			// ask user to choose account
			startActivityForResult(credential.newChooseAccountIntent(),
					REQUEST_ACCOUNT_PICKER);
		}
	}

	private void clearCredential() {
		credential.setSelectedAccountName(null);
		preferences.removeAuthToken();
		preferences.removeListId();
	}

	/** Check that Google Play services APK is installed and up to date. */
	private boolean checkGooglePlayServicesAvailable() {
		final int connectionStatusCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
			showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
			return false;
		}
		return true;
	}

	/**
	 * Initializes the Task List we will use in user's Google Tasks. Search the
	 * tasks lists for a list named "PhoneToDesktop" and create this list if it
	 * doesn't exist.
	 * 
	 */
	private void initList() {
		if (tasklist != null) {
			if (preferences.loadListId() == null) {
				// We don't have a list id saved. Search in the server
				// for a list with the title PhoneToDesktop
				String serverListId = null;
				for (TaskList taskList : tasklist) {
					if (taskList.getTitle().equals(Utils.LIST_TITLE)) {
						serverListId = taskList.getId();
						break;
					}
				}
				if (serverListId == null) {
					// The server doesn't have any list named PhoneToDesktop
					// We create it and save its id
					saveList();
					// returning at this point to avoid finishing this activity
					// before the end of this request
					return;
				} else {
					// The server has a list named PhoneToDesktop
					// We save its id
					preferences.saveListId(serverListId);
				}
			} else {
				// We have a saved id. Let's search this id in server
				boolean serverHasList = false;
				for (TaskList taskList : tasklist) {
					if (taskList.getId().equals(preferences.loadListId())) {
						serverHasList = true;
						break;
					}
				}
				if (!serverHasList) {
					// The server has no list with this id
					// We create a new list and save its id
					saveList();
					// returning at this point to avoid finishing this activity
					// before the end of this request
					return;
				}
				// else
				// We have the list id and found the same id in server
				// nothing to do here
			}
		}

		Intent intent = new Intent(ACTION_AUTHENTICATE);
		intent.putExtra("updating", false);
		sendBroadcast(intent);

		finish();
	}

}
