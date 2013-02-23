package net.xisberto.phonetodesktop;

import java.io.IOException;

import com.google.android.gms.internal.ac;
import com.google.api.services.samples.tasks.android.CommonAsyncTask;
import com.google.api.services.tasks.model.TaskList;

public class AsyncManageTaskLists extends CommonAsyncTask {
	public static final int ACTION_LOAD_LISTS = 0,
			ACTION_SAVE_LIST = 1;
	
	private int action;

	AsyncManageTaskLists(GoogleTasksActivity activity, int action) {
		super(activity);
		this.action = action;
	}

	@Override
	protected void doInBackground() throws IOException {
		switch (action) {
		case ACTION_LOAD_LISTS:
			activity.tasklist = client.tasklists().list().execute().getItems();
			break;
		case ACTION_SAVE_LIST:
			TaskList newList = new TaskList();
			newList.setTitle(GoogleTasksActivity.LIST_TITLE);
			TaskList createdList = client.tasklists().insert(newList).execute();
			break;

		default:
			break;
		}
		
	}

}
