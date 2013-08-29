package net.xisberto.phonetodesktop;

import net.xisberto.phonetodesktop.database.DatabaseHelper;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.SherlockListActivity;
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private class ListLocalTask extends AsyncTask<Void, Void, Cursor> {

		@Override
		protected Cursor doInBackground(Void... params) {
			Utils.log("Thread id: "+android.os.Process.myTid());
			Utils.log(" priority "+ android.os.Process.getThreadPriority(android.os.Process.myTid()));
			
			DatabaseHelper databaseHelper = DatabaseHelper.getInstance(WaitListActivity.this);
			return databaseHelper.listTasksAsCursor();
		}

		@Override
		protected void onPostExecute(Cursor result) {
			getListView().setAdapter(
					new LocalTaskAdapter(WaitListActivity.this, result));
		}
		
	}

}
