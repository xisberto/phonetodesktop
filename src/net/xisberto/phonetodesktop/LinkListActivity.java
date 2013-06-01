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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class LinkListActivity extends SherlockFragmentActivity implements OnClickListener {
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateMainLayout(intent);
		}
	};
	
	private ArrayList<String> ids;
	private ArrayList<String> titles;
	private boolean done;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_link_list);

		if ((savedInstanceState != null)
				&& savedInstanceState.containsKey("titles")) {
			titles = savedInstanceState.getStringArrayList("titles");
			ids = savedInstanceState.getStringArrayList("ids");
			done = savedInstanceState.getBoolean("done");
		} else {
			titles = null;
			ids = null;
			done = true;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(receiver, new IntentFilter(Utils.ACTION_LIST_TASKS));
		if (titles == null) {
			refreshTasks();
		} else {
			updateMainLayout(getIntent());
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList("titles", titles);
		outState.putStringArrayList("ids", ids);
		outState.putBoolean("done", done);
	}

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_link_list, menu);
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
			if (!done) {
				return true;
			}
			refreshTasks();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		YesNoDialog dialog = YesNoDialog.newInstance(v.getTag().toString());
		dialog.show(getSupportFragmentManager(), "dialog");
	}
	
	private void refreshTasks() {
		Intent i = new Intent(getApplicationContext(), SimpleTasksActivity.class);
		i.setAction(Utils.ACTION_LIST_TASKS);
		startActivity(i);
	}
	
	private void deleteTask(String task_id) {
		Intent i = new Intent(getApplicationContext(), SimpleTasksActivity.class);
		i.setAction(Utils.ACTION_REMOVE_TASK);
		i.putExtra("task_id", task_id);
		startActivity(i);

		refreshTasks();
	}

	private void updateMainLayout(Intent intent) {
		if ((intent.getAction() != null)
				&& (intent.getAction().equals(Utils.ACTION_LIST_TASKS))) {
			ids = intent.getStringArrayListExtra("ids");
			titles = intent.getStringArrayListExtra("titles");
			done = intent.getBooleanExtra("done", false);
		}
		TextView text = (TextView) findViewById(R.id.textView_linkList);
		ListView list_view = (ListView) findViewById(R.id.listView_linkList);

		if (done) {
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
