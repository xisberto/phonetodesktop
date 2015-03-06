/*******************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop.network;

import java.io.IOException;
import java.util.List;

import net.xisberto.phonetodesktop.ui.MainActivity;
import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;

import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.model.TaskList;

public class ListAsyncTask extends AsyncTask<Integer, Void, String> {
	public static final int REQUEST_LOAD_LISTS = 0, REQUEST_SAVE_LIST = 1;
	private int request;

	protected GoogleAccountCredential credential;

	private com.google.api.services.tasks.Tasks client;

	private HttpTransport transport;

	private JsonFactory jsonFactory;

	private MainActivity listener;
	private List<TaskList> tasklists;

	public ListAsyncTask(MainActivity activity) {
		if (activity instanceof TaskListTaskListener) {
			listener = (MainActivity) activity;
		} else {
			throw new ClassCastException(
					"Activity must implement TaskListTaskListener");
		}

		transport = AndroidHttp.newCompatibleTransport();
		jsonFactory = new GsonFactory();

		Preferences preferences = new Preferences(listener);
		credential = GoogleAccountCredential
				.usingOAuth2(listener, Utils.scopes);
		credential.setSelectedAccountName(preferences.loadAccountName());

		client = new com.google.api.services.tasks.Tasks.Builder(transport,
				jsonFactory, credential).setApplicationName("PhoneToDesktop")
				.build();
	}

	@Override
	protected String doInBackground(Integer... params) {
		request = params[0];
		try {
			switch (request) {
			case REQUEST_LOAD_LISTS:
				Utils.log("Loading lists");
				tasklists = client.tasklists().list().execute().getItems();
				break;
			case REQUEST_SAVE_LIST:
				Utils.log("Saving list");
				TaskList newList = new TaskList();
				newList.setTitle(Utils.LIST_TITLE);
				TaskList createdList = client.tasklists().insert(newList)
						.execute();
				return createdList.getId();
			default:
				break;
			}
		} catch (final GooglePlayServicesAvailabilityIOException availabilityException) {
			Utils.log(Log.getStackTraceString(availabilityException));
			listener.showGooglePlayServicesAvailabilityErrorDialog(availabilityException
					.getConnectionStatusCode());
		} catch (final UserRecoverableAuthIOException userRecoverableException) {
			Utils.log(Log.getStackTraceString(userRecoverableException));
			listener.startActivityForResult(
					userRecoverableException.getIntent(),
					MainActivity.REQUEST_AUTHORIZATION);
		} catch (IOException e) {
			Utils.log(Log.getStackTraceString(e));
		} catch (IllegalArgumentException iae) {
			Log.e("PhoneToDesktop", Log.getStackTraceString(iae));
		}
		return null;
	}

	@Override
	protected void onPostExecute(String result) {
		switch (request) {
		case REQUEST_LOAD_LISTS:
			listener.selectList(tasklists);
			break;
		case REQUEST_SAVE_LIST:
			listener.saveList(result);
		default:
			break;
		}
	}

	public interface TaskListTaskListener {
		public void selectList(List<TaskList> tasklists);

		public void saveList(String listId);
		
		public void showRetryMessage(int request);
	}
}
