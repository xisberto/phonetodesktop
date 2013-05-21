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
import net.xisberto.phonetodesktop.google_tasks_api.TaskListModel;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.view.Window;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.services.tasks.model.TaskList;

public class PhoneToDesktopActivity extends SyncActivity implements
		OnClickListener {
	public static final String LIST_TITLE = "PhoneToDesktop";

	public TaskListModel model;
	private ListAsyncTask listManager;
	public List<TaskList> tasklist = null;
	public String listId = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		findViewById(R.id.btn_link_list).setOnClickListener(this);
		findViewById(R.id.btn_preferences).setOnClickListener(this);
		findViewById(R.id.btn_how_it_works).setOnClickListener(this);
		findViewById(R.id.btn_about).setOnClickListener(this);
		findViewById(R.id.button_authorize).setOnClickListener(this);

		updateMainLayout(false);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == Activity.RESULT_OK && data != null
					&& data.getExtras() != null) {
				String accountName = data.getExtras().getString(
						AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					preferences.saveAccountName(accountName);

					listManager = new ListAsyncTask(this,
							ListAsyncTask.REQUEST_LOAD_LISTS);
					listManager.execute();
				}
			} else {
				clearCredential();
				updateMainLayout(false);
			}
			break;

		default:
			break;
		}
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
		case R.id.button_authorize:
			authorize();
			updateMainLayout(true);
		}
	}

	@Override
	public void refreshView() {
		if (listManager != null) {
			switch (listManager.getRequest()) {
			case ListAsyncTask.REQUEST_LOAD_LISTS:
				initList();
				break;
			case ListAsyncTask.REQUEST_SAVE_LIST:
				preferences.saveListId(listId);
				break;
			}
			listManager = null;
		}
	}

	private void updateMainLayout(boolean updating) {
		Button btn_authorize = (Button) findViewById(R.id.button_authorize);
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
			} else {
				txt_authorize.setText(R.string.txt_authorize);
			}
			// TODO remove before publishing
			getSherlock().getActionBar().setSubtitle("List: " + prefs.loadListId());
		}

	}

	private void authorize() {
		clearCredential();
		if (checkGooglePlayServicesAvailable()) {
			startActivityForResult(credential.newChooseAccountIntent(),
					REQUEST_ACCOUNT_PICKER);
		}
	}

	private void clearCredential() {
		credential.setSelectedAccountName(null);
		preferences.saveAccountName(null);
		preferences.removeAuthToken();
		preferences.removeListId();
	}

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
					// The id will be saved on refreshView()
					listManager = new ListAsyncTask(this,
							ListAsyncTask.REQUEST_SAVE_LIST);
					listManager.execute();
				}
				// else
				// We have the list id and found the same id in server
				// nothing to do here
			}
		}

		updateMainLayout(false);
	}
}
