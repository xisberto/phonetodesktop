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

import net.xisberto.phonetodesktop.MainFragment.PhoneToDesktopAuthorization;
import net.xisberto.phonetodesktop.network.ListAsyncTask;
import net.xisberto.phonetodesktop.network.ListAsyncTask.TaskListTaskListener;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.tasks.model.TaskList;

public class MainActivity extends SherlockFragmentActivity implements
		OnClickListener, TaskListTaskListener, PhoneToDesktopAuthorization {

	public static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
	public static final int REQUEST_AUTHORIZATION = 1;
	public static final int REQUEST_ACCOUNT_PICKER = 2;

	private static final String TAG_MAIN = "mainFragment";

	private GoogleAccountCredential credential;
	public Preferences preferences;

	private ListAsyncTask listManager;

	private MainFragment mainFragment;
	private Fragment currentFragment;

	private boolean showWelcome;
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.hasExtra(Utils.EXTRA_UPDATING)) {
				updateMainLayout(intent.getBooleanExtra(Utils.EXTRA_UPDATING, false));
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

		preferences = new Preferences(this);

		credential = GoogleAccountCredential.usingOAuth2(this, Utils.scopes);
		credential.setSelectedAccountName(preferences.loadAccountName());

		if (savedInstanceState == null) {
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();
			if (preferences.loadAccountName() == null) {
				showWelcome = true;
				currentFragment = WelcomeFragment.newInstance();
				transaction.replace(R.id.main_frame, currentFragment);
			} else {
				showWelcome = false;
				mainFragment = MainFragment.newInstance();
				transaction.replace(R.id.main_frame, mainFragment, TAG_MAIN);
			}
			transaction.commit();
		} else {
			mainFragment = (MainFragment) getSupportFragmentManager().findFragmentByTag(
					TAG_MAIN);
		}

		String action = getIntent().getAction();
		if (action != null && action.equals(Utils.ACTION_AUTHENTICATE)) {
			updateMainLayout(true);
			startAuthorization();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_ACCOUNT_PICKER:
			Utils.log("Result from Account Picker");
			if (resultCode == RESULT_OK && data != null
					&& data.hasExtra(AccountManager.KEY_ACCOUNT_NAME)) {
				String accountName = data.getExtras().getString(
						AccountManager.KEY_ACCOUNT_NAME);
				Utils.log("Saving account " + accountName);
				if (accountName != null) {
					if (currentFragment instanceof WelcomeFragment) {
						mainFragment = (MainFragment) getSupportFragmentManager()
								.findFragmentByTag(TAG_MAIN);
						if (mainFragment == null) {
							mainFragment = MainFragment.newInstance();
						}
						updateMainLayout(true);
						getSupportFragmentManager().beginTransaction()
								.replace(R.id.main_frame, mainFragment)
								.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
								.commit();
					}
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
				// updateMainLayout(false);
			}
			break;

		case REQUEST_GOOGLE_PLAY_SERVICES:
			Utils.log("Result from Play Services error");
			startAuthorization();
			break;
		case REQUEST_AUTHORIZATION:
			Utils.log("Result from Authorization");
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
	}

	@Override
	protected void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	@Override
	public void onClick(View v) {

	}

	@Override
	public void startAuthorization() {
		if (checkGooglePlayServicesAvailable()) {
			// ask user to choose account
			startActivityForResult(credential.newChooseAccountIntent(),
					REQUEST_ACCOUNT_PICKER);
		}
	}

	/** Check that Google Play services APK is installed and up to date. */
	public boolean checkGooglePlayServicesAvailable() {
		final int connectionStatusCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
			showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
			return false;
		}
		return true;
	}

	public void showGooglePlayServicesAvailabilityErrorDialog(
			final int connectionStatusCode) {
		runOnUiThread(new Runnable() {
			public void run() {
				Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
						connectionStatusCode, MainActivity.this,
						REQUEST_GOOGLE_PLAY_SERVICES);
				dialog.show();
			}
		});
	}

	private void updateMainLayout(boolean updating) {
		Utils.log("updating layout " + updating);
		if (mainFragment != null) {
			mainFragment.setUpdating(updating);
			if (mainFragment.isVisible()) {
				mainFragment.updateMainLayout();
			}
		}
	}

	private void asyncRequestLists() {
		listManager = new ListAsyncTask(this);
		listManager.execute(ListAsyncTask.REQUEST_LOAD_LISTS);
	}

	private void asyncSaveList() {
		listManager = new ListAsyncTask(this);
		listManager.execute(ListAsyncTask.REQUEST_SAVE_LIST);
	}

	@Override
	public void selectList(List<TaskList> tasklists) {
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
		if (showWelcome) {
			startActivity(new Intent(this, TutorialActivity.class));
			showWelcome = false;
		}
	}

	@Override
	public void saveList(String listId) {
		preferences.saveListId(listId);
		updateMainLayout(false);
	}

	@Override
	public void showRetryMessage(int request) {
		switch (request) {
		case ListAsyncTask.REQUEST_LOAD_LISTS:
			RetryDialog dialog = RetryDialog.newInstance(this);
			dialog.show(getSupportFragmentManager(), "retry_dialog");
			break;

		default:
			break;
		}
	}

	public static class RetryDialog extends DialogFragment implements
			DialogInterface.OnClickListener {
		private MainActivity activity;

		public static RetryDialog newInstance(MainActivity act) {
			RetryDialog dialog = new RetryDialog();
			dialog.activity = act;
			return dialog;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
					.setTitle(R.string.app_name).setMessage(R.string.txt_retry)
					.setPositiveButton(android.R.string.yes, this)
					.setNegativeButton(android.R.string.no, this);
			return dialogBuilder.create();
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				activity.asyncRequestLists();
				dialog.dismiss();
			case DialogInterface.BUTTON_NEGATIVE:
				activity.updateMainLayout(false);
				dialog.dismiss();
			}
		}

	}
}
