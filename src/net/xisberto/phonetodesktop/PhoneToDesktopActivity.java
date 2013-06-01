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

import java.util.List;

import net.xisberto.phonetodesktop.google_tasks_api.ListAsyncTask;
import net.xisberto.phonetodesktop.google_tasks_api.ListAsyncTask.TaskListTaskListener;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.view.Window;
import com.google.api.services.tasks.model.TaskList;

public class PhoneToDesktopActivity extends SyncActivity implements
		OnClickListener, TaskListTaskListener {

	private ListAsyncTask listManager;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra(Utils.EXTRA_UPDATING)) {
				updateMainLayout(intent.getBooleanExtra(Utils.EXTRA_UPDATING, false));
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		findViewById(R.id.btn_link_list).setOnClickListener(this);
		findViewById(R.id.btn_preferences).setOnClickListener(this);
		findViewById(R.id.btn_how_it_works).setOnClickListener(this);
		findViewById(R.id.btn_about).setOnClickListener(this);
		findViewById(R.id.btn_authorize).setOnClickListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			Log.d(TAG, "Result from Account Picker");
			if (resultCode == RESULT_OK && data != null
					&& data.hasExtra(AccountManager.KEY_ACCOUNT_NAME)) {
				String accountName = data.getExtras().getString(
						AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					preferences.saveAccountName(accountName);
					// If PhoneToDesktop has'nt been authorized by the user
					// this will lead to an UserRecoverableAuthIOException
					// that will generate an onActivityResult for
					// REQUEST_AUTHENTICATION
					asyncRequestLists();
				}
			} else {
				// User cancelled, or any other error during authorization
				updateMainLayout(false);
			}
			break;

		case REQUEST_GOOGLE_PLAY_SERVICES:
			Log.d(TAG, "Result from Play Services");
			authorize();
			break;
		case REQUEST_AUTHORIZATION:
			Log.d(TAG, "Result from Authorization");
			if (resultCode == RESULT_OK) {
				asyncRequestLists();
			} else {
				updateMainLayout(false);
			}
			break;

		default:
			break;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
				new IntentFilter(Utils.ACTION_AUTHENTICATE));
		updateMainLayout(false);
	}

	@Override
	protected void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_how_it_works:
			startActivity(new Intent(getApplicationContext(),
					TutorialActivity.class));
			break;
		case R.id.btn_about:
			startActivity(new Intent(getApplicationContext(),
					AboutActivity.class));
			break;
		case R.id.btn_link_list:
			startActivity(new Intent(getApplicationContext(),
					LinkListActivity.class));
			break;
		case R.id.btn_preferences:
			startActivity(new Intent(getApplicationContext(),
					SettingsActivity.class));
			break;
		case R.id.btn_authorize:
			updateMainLayout(true);
			authorize();
		}
	}

	private void updateMainLayout(boolean updating) {
		Button btn_authorize = (Button) findViewById(R.id.btn_authorize);
		TextView txt_authorize = (TextView) findViewById(R.id.txt_authorize);

		getSherlock().setProgressBarIndeterminateVisibility(updating);
		btn_authorize.setEnabled(!updating);

		if (updating) {
			txt_authorize.setText(R.string.txt_waiting_authorization);
		} else {
			Preferences prefs = new Preferences(this);
			String account_name = prefs.loadAccountName();
			if (account_name != null) {
				txt_authorize.setText(getResources().getString(
						R.string.txt_authorized_to)
						+ " " + account_name);
				getSherlock().getActionBar().setSubtitle(
						"List: " + prefs.loadListId());
			} else {
				txt_authorize.setText(R.string.txt_authorize);
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

	private void asyncRequestLists() {
		listManager = new ListAsyncTask(this, ListAsyncTask.REQUEST_LOAD_LISTS);
		listManager.execute();
	}

	private void asyncSaveList() {
		listManager = new ListAsyncTask(this, ListAsyncTask.REQUEST_SAVE_LIST);
		listManager.execute();
	}

	@Override
	public void selectList(List<TaskList> tasklists) {
		Log.d(TAG, "selectList");
		if (tasklists != null) {
			if (preferences.loadListId() == null) {
				// We don't have a list id saved. Search in the server
				// for a list with the title PhoneToDesktop
				String serverListId = null;
				for (TaskList taskList : tasklists) {
					if (taskList.getTitle().equals(Utils.LIST_TITLE)) {
						serverListId = taskList.getId();
						break;
					}
				}
				if (serverListId == null) {
					// The server doesn't have any list named PhoneToDesktop
					// We create it and save its id
					asyncSaveList();
					// returning at this point to keep the progress
					return;
				} else {
					// The server has a list named PhoneToDesktop
					// We save its id
					preferences.saveListId(serverListId);
				}
			} else {
				// We have a saved id. Let's search this id in server
				boolean serverHasList = false;
				for (TaskList taskList : tasklists) {
					if (taskList.getId().equals(preferences.loadListId())) {
						serverHasList = true;
						break;
					}
				}
				if (!serverHasList) {
					// The server has no list with this id
					// We create a new list and save its id
					asyncSaveList();
					// returning at this point to keep the progress
					return;
				}
				// else
				// We have the list id and found the same id in server
				// nothing to do here
			}
		}
		updateMainLayout(false);
	}

	@Override
	public void saveList(String listId) {
		Log.d(TAG, "saveList");
		preferences.saveListId(listId);
		updateMainLayout(false);
	}

	@Override
	public void refreshView() {
		// TODO Auto-generated method stub

	}

}
