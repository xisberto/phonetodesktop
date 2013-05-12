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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import net.xisberto.phonetodesktop.AdvancedTaskFragment.OnAdvancedTaskOptionsListener;
import net.xisberto.phonetodesktop.google_tasks_api.TaskModel;
import net.xisberto.phonetodesktop.google_tasks_api.TasksAsyncTask;
import android.accounts.Account;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.services.samples.tasks.android.CommonAsyncTask;

public class GoogleTasksActivity extends SyncActivity implements
		OnClickListener,
		OnItemClickListener, OnTabChangeListener, OnAdvancedTaskOptionsListener {

	public static final String ACTION_AUTHENTICATE = "net.xisberto.phonetodesktop.authenticate",
			ACTION_LIST_TASKS = "net.xisberto.phonetodesktop.list_tasks",
			ACTION_REMOVE_TASKS = "net.xisberto.phonetodesktop.remove_task",
			TAG_SIMPLE = "simple",
			TAG_ADVANCED = "advanced",
			SELECTED_TAB_ID = "selected_tab_tag";

	private static final int NOTIFICATION_SENDING = 0, NOTIFICATION_ERROR = 1,
			NOTIFICATION_NEED_AUTHORIZE = 2, NOTIFICATION_TIMEOUT = 3;

	private WhatToSendDialog dialog;

	public String taskId = null;
	public ArrayList<String> list_ids = null, list_titles = null;

	private TasksAsyncTask taskManager;
	public TaskModel model;

	private TabHost mTabHost;
	private SimpleTaskFragment simple_fragment;
	private AdvancedTaskFragment advanced_fragment;

	
	private class TabContent implements TabContentFactory {
		private Context mContext;

		public TabContent(Context context) {
			mContext = context;
		}

		@Override
		public View createTabContent(String arg0) {
			View v = new View(mContext);
			v.setMinimumHeight(0);
			v.setMinimumWidth(0);
			return v;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
			setContentView(R.layout.activity_add_task);
			
			initializeTabs();
			if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_TAB_ID)) {
				mTabHost.setCurrentTabByTag(savedInstanceState.getString(SELECTED_TAB_ID));
			}
			findViewById(R.id.btn_send).setOnClickListener(this);
			return;
		} else {
			finish();
		}

//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
//					addTask(preferences.loadWhatToSend(), getIntent()
//							.getStringExtra(Intent.EXTRA_TEXT));
//				} else if (getIntent().getAction().equals(ACTION_LIST_TASKS)) {
//					broadcastUpdatingStatus(ACTION_LIST_TASKS, true);
//					getTaskList();
//				} else if (getIntent().getAction().equals(ACTION_REMOVE_TASKS)) {
//					removeTask(getIntent().getStringExtra("task_id"));
//				}
//			}
//		}).start();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(SELECTED_TAB_ID, mTabHost.getCurrentTabTag());
	}

	private void initializeTabs() {
		mTabHost = (TabHost) findViewById(android.R.id.tabhost);
		mTabHost.setup();

		TabSpec tabSimple = mTabHost.newTabSpec(TAG_SIMPLE).setIndicator(getString(R.string.title_simple));
		tabSimple.setContent(new TabContent(this));
		TabSpec tabAdvanced = mTabHost.newTabSpec(TAG_ADVANCED).setIndicator(getString(R.string.title_advanced));
		tabAdvanced.setContent(new TabContent(this));

		mTabHost.addTab(tabSimple);
		mTabHost.addTab(tabAdvanced);

		mTabHost.setOnTabChangedListener(this);
		this.onTabChanged(TAG_SIMPLE);
	}

	@Override
	public void onTabChanged(String tabId) {
		String extra_text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
		FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
		if (tabId.equals(TAG_SIMPLE)) {
			if (simple_fragment == null) {
				simple_fragment = SimpleTaskFragment.newInstance(extra_text);
			}
			transaction.replace(android.R.id.tabcontent, simple_fragment);
		} else if (tabId.equals(TAG_ADVANCED)) {
			if (advanced_fragment == null) {
				advanced_fragment = AdvancedTaskFragment.newInstance(extra_text);
			}
			transaction.replace(android.R.id.tabcontent, advanced_fragment);
		}
		transaction.commit();

	}

	@Override
	public void onFragmentInteraction(Uri uri) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_send:
			
			break;

		default:
			break;
		}
	}
	
	//TODO Update or delete following methods
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_GOOGLE_PLAY_SERVICES:
			if (resultCode == Activity.RESULT_OK) {
				// getCredential();
			} else {
				checkGooglePlayServicesAvailable();
			}
			break;
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				// startActivity(new Intent(this, CalendarListActivity.class));
			} else {
				// chooseAccount();
			}
			break;
		case REQUEST_ACCOUNT_PICKER:
			break;
		}
	}

	private void showNotification(int notification_type) {
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

	private void addTask(String what_to_send, String text) {
		Account acc = credential.getSelectedAccount();
		if (acc == null) {
			requestSelectAccount();
		} else {
			String[] entryvalues_what_to_send = getResources().getStringArray(
					R.array.entryvalues_what_to_send);
			if (what_to_send.equals(entryvalues_what_to_send[2])) {
				dialog = WhatToSendDialog.newInstance();
				dialog.show(getSupportFragmentManager(), "what_to_send_dialog");
			} else {
				if (what_to_send.equals(entryvalues_what_to_send[0])) {
					preferences.saveLastSentText(text);
				} else {
					preferences.saveLastSentText(filterLinks(text));
				}
				showNotification(NOTIFICATION_SENDING);
				// taskManager = new AsyncManageTasks(this,
				// AsyncManageTasks.REQUEST_ADD_TASK);
				// taskManager.execute();
			}
		}
		finish();
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
		CheckBox check_save_option = (CheckBox) dialog.getView().findViewById(
				R.id.check_save_option);
		if (check_save_option.isChecked()) {
			preferences.saveWhatToSend(position);
		} else {
			preferences.saveWhatToSend(2);
		}
		addTask(getResources().getStringArray(R.array.entryvalues_what_to_send)[position],
				getIntent().getStringExtra(Intent.EXTRA_TEXT));
	}

	private void removeTask(final String task_id) {
		Account acc = credential.getSelectedAccount();
		if (acc == null) {
			requestSelectAccount();
		} else {
			// taskManager = new AsyncManageTasks(this,
			// AsyncManageTasks.REQUEST_REMOVE_TASK);
			// taskManager.execute();
		}
		finish();
	}

	private void getTaskList() {
		Account acc = credential.getSelectedAccount();
		if (acc == null) {
			requestSelectAccount();
		} else {
			// taskManager = new AsyncManageTasks(this,
			// AsyncManageTasks.REQUEST_LOAD_TASKS);
			// taskManager.execute();
		}
		finish();
	}

	private void requestSelectAccount() {
		dismissNotification(NOTIFICATION_SENDING);
		broadcastUpdatingStatus(ACTION_AUTHENTICATE, false);
		broadcastTaskList(null, null);
		// clearCredential();
		showNotification(NOTIFICATION_NEED_AUTHORIZE);
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
	 * This will be called by a {@link CommonAsyncTask} when the ask finishes We
	 * use the request code to know which is the next step
	 */
	@Override
	public void refreshView() {
		Log.d(TAG, "returning from background");

		// if (taskManager != null) {
		// Log.d(TAG, "taskManager.request = "+ taskManager.getRequest());
		// if (taskManager.getRequest() == AsyncManageTasks.REQUEST_ADD_TASK) {
		// dismissNotification(NOTIFICATION_SENDING);
		// taskManager = null;
		// } else if (taskManager.getRequest() ==
		// AsyncManageTasks.REQUEST_REMOVE_TASK) {
		// taskManager = null;
		// } else if (taskManager.getRequest() ==
		// AsyncManageTasks.REQUEST_LOAD_TASKS) {
		// broadcastTaskList(list_ids, list_titles);
		// taskManager = null;
		// }
		// }
	}


}
