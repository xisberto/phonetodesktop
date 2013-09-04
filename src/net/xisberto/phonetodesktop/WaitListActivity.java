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
import android.util.SparseArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class WaitListActivity extends SherlockListActivity implements OnItemClickListener {
	LocalTaskAdapter adapter;
	ActionMode actionMode;
	SparseArray<Long> selectedItems;
	
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
		selectedItems = new SparseArray<Long>();
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		getListView().setOnItemClickListener(this);
		
		new ListLocalTask().execute();
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

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Utils.log("Item id: "+id);

		if (selectedItems.get(position) == null) {
			selectedItems.put(position, id);
			getListView().setItemChecked(position, true);
		} else {
			selectedItems.remove(position);
			getListView().setItemChecked(position, false);
		}
		
		Utils.log("selectedItems: "+selectedItems.size());
		
		if (selectedItems.size() > 0) {
			if (actionMode == null) {
				actionMode = startActionMode(new ActionModeCallback());
			}
		} else {
			if (actionMode != null) {
				actionMode.finish();
			}
		}
	}
	
	private class ListLocalTask extends AsyncTask<Long, Void, Cursor> {
		private static final int ACTION_LIST = 1, ACTION_SEND_ALL = 2;
		private int action;

		@Override
		protected Cursor doInBackground(Long... params) {
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
	
	private final class ActionModeCallback implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			getSupportMenuInflater().inflate(R.menu.cab_wait_list, menu);
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			long[] selected_ids = new long[selectedItems.size()];
			for (int i = 0; i < selected_ids.length; i++) {
				selected_ids[i] = selectedItems.get(selectedItems.keyAt(i));
			}
			switch (item.getItemId()) {
			case R.id.action_delete_multiple:
				
				return true;
			case R.id.action_send_multiple:
				
				return true;
			default:
				return false;
			}
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			for (int i = 0; i < getListView().getAdapter().getCount(); i++) {
                getListView().setItemChecked(i, false);
			}
			
			selectedItems.clear();
 
            if (mode == actionMode) {
                actionMode = null;
            }			
		}
		
	}
}
