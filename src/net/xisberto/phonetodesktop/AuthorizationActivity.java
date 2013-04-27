package net.xisberto.phonetodesktop;

import java.util.List;

import net.xisberto.phonetodesktop.google_tasks_api.ListAsyncTask;
import net.xisberto.phonetodesktop.google_tasks_api.TaskListModel;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.tasks.model.TaskList;

public class AuthorizationActivity extends SyncActivity {
	public static final String LIST_TITLE = "PhoneToDesktop";
	public static final String ACTION_AUTHENTICATE = "net.xisberto.phonetodesktop.authenticate";

	private ListAsyncTask listManager;
	public List<TaskList> tasklist = null;
	public String listId = null;

	public TaskListModel model;

	private boolean requestLists = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authorize);

		Intent intent = new Intent(ACTION_AUTHENTICATE);
		intent.putExtra("updating", false);
		sendBroadcast(intent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (requestLists) {
			listManager = new ListAsyncTask(this,
					ListAsyncTask.REQUEST_LOAD_LISTS);
			listManager.execute();
		} else {
			authorize();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			Log.d(TAG, "onActivityResult");
			if (resultCode == Activity.RESULT_OK && data != null
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
					requestLists = true;
				}
			} else {
				finish();
			}
			break;

		default:
			break;
		}
	}

	@Override
	public void refreshView() {
		if (listManager != null) {
			Log.d(TAG, "refreshView");
			if (listManager.getRequest() == ListAsyncTask.REQUEST_LOAD_LISTS) {
				initList();
				listManager = null;
			} else if (listManager.getRequest() == ListAsyncTask.REQUEST_SAVE_LIST) {
				preferences.saveListId(listId);
				listManager = null;
			}
		}

	}

	private void authorize() {
		clearCredential();
		if (checkGooglePlayServicesAvailable()) {
			getCredential();
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
	 * Check if there's already an account selected. If there's no account,
	 * start an activity to {@link GoogleAccountCredential}. If there's an
	 * account, initializes {@link ListAsyncTask}
	 */
	private void getCredential() {
		// check if there is already an account selected
		if (credential.getSelectedAccountName() == null) {
			// ask user to choose account
			startActivityForResult(credential.newChooseAccountIntent(),
					REQUEST_ACCOUNT_PICKER);
		} else {
			listManager = new ListAsyncTask(this,
					ListAsyncTask.REQUEST_LOAD_LISTS);
			listManager.execute();
		}
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
					if (taskList.getTitle().equals(LIST_TITLE)) {
						serverListId = taskList.getId();
						break;
					}
				}
				if (serverListId == null) {
					// The server doesn't have any list named PhoneToDesktop
					// We create it and save its id
					listManager = new ListAsyncTask(this,
							ListAsyncTask.REQUEST_SAVE_LIST);
					listManager.execute();
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
					listManager = new ListAsyncTask(this,
							ListAsyncTask.REQUEST_SAVE_LIST);
					listManager.execute();
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
