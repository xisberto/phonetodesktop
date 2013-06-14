/*******************************************************************************
 * Copyright (c) 2012 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class LinkListActivity extends SherlockFragmentActivity implements OnClickListener {
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateLayout(intent);
		}
	};
	
	private ArrayList<String> ids;
	private ArrayList<String> titles;
	private boolean updating;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_link_list);

		if ((savedInstanceState != null)
				&& savedInstanceState.containsKey(Utils.EXTRA_TITLES)) {
			titles = savedInstanceState.getStringArrayList(Utils.EXTRA_TITLES);
			ids = savedInstanceState.getStringArrayList(Utils.EXTRA_IDS);
			updating = savedInstanceState.getBoolean(Utils.EXTRA_UPDATING);
		} else {
			titles = null;
			ids = null;
			updating = false;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(this)
				.registerReceiver(receiver, new IntentFilter(Utils.ACTION_LIST_TASKS));
		if (titles == null) {
			refreshTasks();
		} else {
			updateLayout(getIntent());
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(this)
				.unregisterReceiver(receiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(Utils.EXTRA_TITLES, titles);
		outState.putStringArrayList(Utils.EXTRA_IDS, ids);
		outState.putBoolean(Utils.EXTRA_UPDATING, updating);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_link_list, menu);
		menu.findItem(R.id.item_refresh).setVisible(!updating);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.item_refresh:
			if (updating) {
				return true;
			}
			refreshTasks();
			return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		YesNoDialog dialog = YesNoDialog.newInstance(v.getTag().toString());
		dialog.show(getSupportFragmentManager(), "dialog");
	}
	
	private void refreshTasks() {
		updating = true;
		updateLayout();
		Intent service = new Intent(this, GoogleTasksService.class);
		service.setAction(Utils.ACTION_LIST_TASKS);
		startService(service);
	}
	
	private void deleteTask(String task_id) {
		updating = true;
		updateLayout();
		Intent service = new Intent(this, GoogleTasksService.class);
		service.setAction(Utils.ACTION_REMOVE_TASK);
		service.putExtra(Utils.EXTRA_TASK_ID, task_id);
		startService(service);
	}
	
	/**
	 * Updates the status of the layout according to field {@code updating}
	 */
	private void updateLayout() {
		supportInvalidateOptionsMenu();
		setProgressBarIndeterminateVisibility(updating);
	}

	private void updateLayout(Intent intent) {
		if ((intent.getAction() != null)
				&& (intent.getAction().equals(Utils.ACTION_LIST_TASKS))) {
			ids = intent.getStringArrayListExtra(Utils.EXTRA_IDS);
			titles = intent.getStringArrayListExtra(Utils.EXTRA_TITLES);
			updating = intent.getBooleanExtra(Utils.EXTRA_UPDATING, false);
		}
		Utils.log("updating: "+updating);
		updateLayout();
		
		TextView text = (TextView) findViewById(R.id.textView_linkList);
		ListView list_view = (ListView) findViewById(R.id.listView_linkList);

		if (!updating) {
			findViewById(R.id.progressBar_linkList).setVisibility(View.GONE);
			if ((titles == null)
					|| (titles.size() < 1)) {
				text.setText(R.string.txt_empty_list);
				list_view.setVisibility(View.GONE);
				text.setVisibility(View.VISIBLE);
			} else {
				//We're using LinkedHashMap to guarantee the order of insertion
				HashMap<String, String> tasks = new LinkedHashMap<String, String>();
				for (int i = 0; i < titles.size(); i++) {
					tasks.put(ids.get(i), titles.get(i));
				}
				TasksArrayAdapter adapter = new TasksArrayAdapter(this, tasks);
				list_view.setAdapter(adapter);
				list_view.setVisibility(View.VISIBLE);
				text.setVisibility(View.GONE);
			}
		} else {
			findViewById(R.id.progressBar_linkList).setVisibility(View.VISIBLE);
			list_view.setVisibility(View.GONE);
			text.setVisibility(View.GONE);
		}

	}

	public static class YesNoDialog extends SherlockDialogFragment {
		public static YesNoDialog newInstance(String task_id) {
			YesNoDialog dlg = new YesNoDialog();
			Bundle bundle = new Bundle();
			bundle.putString("task_id", task_id);
			dlg.setArguments(bundle);
			return dlg;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.title_confirm)
				.setMessage(R.string.txt_confirm)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						((LinkListActivity) getActivity()).deleteTask(getArguments().getString("task_id"));
					}
				})
				.setNegativeButton(android.R.string.no, null)
				.create();
		}
		
		
	}
}
