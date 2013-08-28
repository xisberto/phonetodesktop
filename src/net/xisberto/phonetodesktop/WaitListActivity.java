package net.xisberto.phonetodesktop;

import net.xisberto.phonetodesktop.database.DatabaseHelper;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Process;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;

public class WaitListActivity extends SherlockListActivity {
	private Thread t = new Thread(new Runnable() {
		@Override
		public void run() {
			Utils.log("Thread id: "+android.os.Process.myTid());
			Utils.log(" priority"+ android.os.Process.getThreadPriority(android.os.Process.myTid()));
			DatabaseHelper databaseHelper = DatabaseHelper.getInstance(WaitListActivity.this);
			final Cursor cursor = databaseHelper.listTasksAsCursor();
			getListView().post(new Runnable() {
				@Override
				public void run() {
					getListView().setAdapter(
							new LocalTaskAdapter(WaitListActivity.this, cursor));
				}
			});
		}
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.listitem_localtask);
		
		Utils.log("Thread id: "+android.os.Process.myTid());
		Utils.log(" priority"+ android.os.Process.getThreadPriority(android.os.Process.myTid()));
		
		t.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
		t.start();
		
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

}
