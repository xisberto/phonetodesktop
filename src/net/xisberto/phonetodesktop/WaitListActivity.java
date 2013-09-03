package net.xisberto.phonetodesktop;

import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.network.GoogleTasksService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class WaitListActivity extends SherlockListActivity {
	LocalTaskAdapter adapter;
	BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Utils.log("WaitListActivity received broadcast");
			new ListLocalTask().execute();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Utils.log("Thread id: "+android.os.Process.myTid());
		Utils.log(" priority "+ android.os.Process.getThreadPriority(android.os.Process.myTid()));
		
		new ListLocalTask().execute();
	}

	@Override
	protected void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(this).registerReceiver(
				receiver, new IntentFilter(Utils.ACTION_LIST_LOCAL_TASKS));
	}

	@Override
	protected void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
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
				if (adapter == null) {
					adapter = new LocalTaskAdapter(WaitListActivity.this, result);
					getListView().setAdapter(adapter);
				} else {
					adapter.changeCursor(result);
				}
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
