package net.xisberto.phonetodesktop.google_tasks_api;

import java.io.IOException;

import net.xisberto.phonetodesktop.SyncActivity;
import android.util.Log;

import com.google.api.services.samples.tasks.android.CommonAsyncTask;
import com.google.api.services.tasks.model.Task;

public class TasksAsyncTask extends CommonAsyncTask {
	public static final int REQUEST_ADD_TASK = 0, REQUEST_LIST_TASKS = 1, REQUEST_DEL_TASK = 2;
	
	protected final SyncActivity activity;
	private String data;

	public TasksAsyncTask(SyncActivity syncActivity, int request) {
		super(syncActivity, request);
		this.activity = syncActivity;
		this.data = null;
	}
	
	public TasksAsyncTask(SyncActivity syncActivity, int request, String data) {
		this(syncActivity, request);
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
