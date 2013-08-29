package net.xisberto.phonetodesktop;

import net.xisberto.phonetodesktop.database.DatabaseHelper;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class WaitListActivity extends SherlockListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Utils.log("Thread id: "+android.os.Process.myTid());
		Utils.log(" priority "+ android.os.Process.getThreadPriority(android.os.Process.myTid()));
		
		ListLocalTask tasker = new ListLocalTask();
		tasker.execute();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_wait_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.action_sendall:
			ListLocalTask tasker = new ListLocalTask();
			tasker.execute(0l);
		}
		return super.onOptionsItemSelected(item);
	}
	
	private class ListLocalTask extends AsyncTask<Long, Void, Cursor> {
		private static final int ACTION_LIST = 1, ACTION_SEND_ALL = 2;
		private int action;

		@Override
		protected Cursor doInBackground(Long... params) {
			Utils.log("Thread id: "+android.os.Process.myTid());
			Utils.log(" priority "+ android.os.Process.getThreadPriority(android.os.Process.myTid()));

			DatabaseHelper databaseHelper = DatabaseHelper.getInstance(WaitListActivity.this);
			if (params.length < 1) {
				action = ACTION_LIST;
				return databaseHelper.listTasksAsCursor();
			} else {
				action = ACTION_SEND_ALL;
				return databaseHelper.listTaskQueueAsCursor();
			}
		}

		@Override
		protected void onPostExecute(Cursor result) {
			switch (action) {
			case ACTION_LIST:
				getListView().setAdapter(
						new LocalTaskAdapter(WaitListActivity.this, result));
				break;
			case ACTION_SEND_ALL:
				if (result.getCount() == 0) {
					return;
				}
				long[] tasks_ids = new long[result.getCount()];
				for (int i = 0; i < result.getCount(); i++) {
					result.moveToNext();
					tasks_ids[i] = result.getLong(0);
				}
				Intent service = new Intent(WaitListActivity.this, GoogleTasksService.class);
				service.setAction(Utils.ACTION_SEND_MULTIPLE_TASKS);
				service.putExtra(Utils.EXTRA_TASKS_IDS, tasks_ids);
				startService(service);
				break;
			default:
				break;
			}
			
		}
		
	}

}
