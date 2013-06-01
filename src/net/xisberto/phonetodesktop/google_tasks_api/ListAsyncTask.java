package net.xisberto.phonetodesktop.google_tasks_api;

import java.io.IOException;
import java.util.List;

import net.xisberto.phonetodesktop.SyncActivity;
import net.xisberto.phonetodesktop.Utils;
import android.util.Log;

import com.google.api.services.samples.tasks.android.CommonAsyncTask;
import com.google.api.services.tasks.model.TaskList;

public class ListAsyncTask extends CommonAsyncTask {
	public static final int REQUEST_LOAD_LISTS = 0, REQUEST_SAVE_LIST = 1;

	public interface TaskListTaskListener {
		public void selectList(List<TaskList> tasklists);

		public void saveList(String listId);
	}

	protected com.google.api.services.tasks.Tasks client;
	protected TaskListTaskListener listener;

	public ListAsyncTask(SyncActivity activity, int request) {
		super(activity, request);
		if (activity instanceof TaskListTaskListener) {
			listener = (TaskListTaskListener) activity;
		} else {
			throw new ClassCastException(
					"Activity must implement TaskListTaskListener");
		}
		client = activity.client;
	}

	@Override
	protected void doInBackground() throws IOException {
		switch (request) {
		case REQUEST_LOAD_LISTS:
			Log.d(SyncActivity.TAG, "Async loading lists");
			final List<TaskList> tasklists = client.tasklists().list().execute()
					.getItems();
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					listener.selectList(tasklists);
				}
			});
			break;
		case REQUEST_SAVE_LIST:
			TaskList newList = new TaskList();
			newList.setTitle(Utils.LIST_TITLE);
			Log.d(SyncActivity.TAG, "Async saving list");
			TaskList createdList = client.tasklists().insert(newList).execute();
			Log.d(SyncActivity.TAG, "Async saved list " + createdList.getId());
			final String listId = createdList.getId();
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					listener.saveList(listId);
				}
			});
			break;

		default:
			break;
		}

	}

}
