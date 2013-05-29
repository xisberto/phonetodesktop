package net.xisberto.phonetodesktop.google_tasks_api;

import java.io.IOException;

import net.xisberto.phonetodesktop.AuthorizationActivity;
import net.xisberto.phonetodesktop.SyncActivity;
import net.xisberto.phonetodesktop.Utils;

import android.util.Log;

import com.google.api.services.samples.tasks.android.CommonAsyncTask;
import com.google.api.services.tasks.model.TaskList;

public class ListAsyncTask extends CommonAsyncTask {
	public static final int REQUEST_LOAD_LISTS = 0, REQUEST_SAVE_LIST = 1;

	protected final TaskListModel model;
	protected final com.google.api.services.tasks.Tasks client;
	protected final AuthorizationActivity activity;

	public ListAsyncTask(AuthorizationActivity activity, int request) {
		super(activity, request);
		model = activity.model;
		client = activity.client;
		this.activity = activity;
	}

	@Override
	protected void doInBackground() throws IOException {
		switch (request) {
		case REQUEST_LOAD_LISTS:
			// activity.tasklist will be used by GoogleTasksActivity.initList
			Log.d(SyncActivity.TAG, "Loading lists");
			activity.tasklist = client.tasklists().list().execute().getItems();
			break;
		case REQUEST_SAVE_LIST:
			TaskList newList = new TaskList();
			newList.setTitle(Utils.LIST_TITLE);
			Log.d(SyncActivity.TAG, "Saving list");
			TaskList createdList = client.tasklists().insert(newList).execute();
			// activity.listId will be saved on Preferences
			Log.d(SyncActivity.TAG, "Saved list "+createdList.getId());
			activity.listId = createdList.getId();
			break;

		default:
			break;
		}

	}

}
