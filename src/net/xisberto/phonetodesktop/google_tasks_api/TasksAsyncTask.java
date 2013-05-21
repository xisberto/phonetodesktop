package net.xisberto.phonetodesktop.google_tasks_api;

import java.io.IOException;

import net.xisberto.phonetodesktop.GoogleTasksActivity;
import android.util.Log;

import com.google.api.services.samples.tasks.android.CommonAsyncTask;
import com.google.api.services.tasks.model.Task;

public class TasksAsyncTask extends CommonAsyncTask {
	public static final int REQUEST_ADD_TASK = 0, REQUEST_LIST_TASKS = 1, REQUEST_DEL_TASK = 2;
	
	protected final GoogleTasksActivity activity;
	protected final TaskModel model;
	private String data;

	public TasksAsyncTask(GoogleTasksActivity googleTasksActivity, int request) {
		super(googleTasksActivity, request);
		this.activity = googleTasksActivity;
		this.model = googleTasksActivity.model;
		this.data = null;
	}
	
	public TasksAsyncTask(GoogleTasksActivity googleTasksActivity, int request, String data) {
		this(googleTasksActivity, request);
		this.data = data;
	}

	@Override
	protected void doInBackground() throws IOException {
		switch (request) {
		case REQUEST_ADD_TASK:
			if (data != null) {
				Task task = new Task().setTitle(data);
				String listId = activity.preferences.loadListId();
				Log.d("TasksAsyncTask", "Adding task to list "+listId);
				Task result = client.tasks().insert(listId, task).execute();
				Log.d("TasksAsyncTask", "New task id: "+result.getId());
			}
			break;

		default:
			break;
		}
	}

}
