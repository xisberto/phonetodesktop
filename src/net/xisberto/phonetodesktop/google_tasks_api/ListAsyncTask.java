package net.xisberto.phonetodesktop.google_tasks_api;

import java.io.IOException;

import net.xisberto.phonetodesktop.PhoneToDesktopActivity;

import com.google.api.services.samples.tasks.android.CommonAsyncTask;
import com.google.api.services.tasks.model.TaskList;

public class ListAsyncTask extends CommonAsyncTask {
	public static final int REQUEST_LOAD_LISTS = 0, REQUEST_SAVE_LIST = 1;

	protected final PhoneToDesktopActivity activity;
	protected final TaskListModel model;

	public ListAsyncTask(PhoneToDesktopActivity phoneToDesktopActivity, int request) {
		super(phoneToDesktopActivity, request);
		this.activity = phoneToDesktopActivity;
		this.model = phoneToDesktopActivity.model;
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
			newList.setTitle(PhoneToDesktopActivity.LIST_TITLE);
			TaskList createdList = client.tasklists().insert(newList).execute();
			// activity.listId will be saved on Preferences
			activity.listId = createdList.getId();
			break;

		default:
			break;
		}

	}

}
