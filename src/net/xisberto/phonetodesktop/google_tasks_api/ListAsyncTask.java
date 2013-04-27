package net.xisberto.phonetodesktop.google_tasks_api;

import java.io.IOException;

import net.xisberto.phonetodesktop.AuthorizationActivity;

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
			activity.tasklist = client.tasklists().list().execute().getItems();
			break;
		case REQUEST_SAVE_LIST:
			TaskList newList = new TaskList();
			newList.setTitle(AuthorizationActivity.LIST_TITLE);
			TaskList createdList = client.tasklists().insert(newList).execute();
			// activity.listId will be saved on Preferences
			activity.listId = createdList.getId();
			break;

		default:
			break;
		}

	}

}
