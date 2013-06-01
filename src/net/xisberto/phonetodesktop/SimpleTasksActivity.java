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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SimpleTasksActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent().getAction().equals(Intent.ACTION_SEND)
				&& getIntent().hasExtra(Intent.EXTRA_TEXT)) {
			// We came from an ACTION_SEND, so send this task and end the
			// activity
			Intent service = new Intent(this, GoogleTasksService.class);
			service.setAction(Utils.ACTION_SEND_TASK);
			service.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
			startService(service);
			finish();
		} else {
			// Maybe show an error dialog?
			finish();
		}

	}

	// TODO Update or delete following methods
/*
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

	*//**
	 * This will be called by a {@link CommonAsyncTask} when the ask finishes We
	 * use the request code to know which is the next step
	 *//*
	@Override
	public void refreshView() {
		Log.d(TAG, "returning from background");
		dismissNotification(NOTIFICATION_SENDING);

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
*/
}
