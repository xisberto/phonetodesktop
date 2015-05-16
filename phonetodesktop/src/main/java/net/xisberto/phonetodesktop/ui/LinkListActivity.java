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
package net.xisberto.phonetodesktop.ui;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.network.GoogleTasksService;

import java.util.ArrayList;

public class LinkListActivity extends AppCompatActivity implements TasksArrayAdapter.TaskArraySelectionListener, SwipeRefreshLayout.OnRefreshListener {
    private static final String SELECTED_ITEMS = "selected_items";

	private ArrayList<String> ids;
    private ArrayList<String> titles;
    private boolean updating;
    private ProgressBar mProgress;
    private TasksArrayAdapter adapter;
    private ListView list_view;
    private SwipeRefreshLayout swipeRefreshLayout;
    private SparseArrayCompat<String> selectedItems;
    private ActionMode actionMode;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateLayout(intent);
        }
    };

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
		setContentView(R.layout.activity_link_list);

        selectedItems = new SparseArrayCompat<>();

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        list_view = (ListView) findViewById(android.R.id.list);

		if ((savedInstanceState != null)
				&& savedInstanceState.containsKey(Utils.EXTRA_TITLES)) {
			titles = savedInstanceState.getStringArrayList(Utils.EXTRA_TITLES);
			ids = savedInstanceState.getStringArrayList(Utils.EXTRA_IDS);
			updating = savedInstanceState.getBoolean(Utils.EXTRA_UPDATING);
            ArrayList<Integer> selection = savedInstanceState.getIntegerArrayList(SELECTED_ITEMS);
            for (int i = 0; i < selection.size(); i++) {
                selectedItems.put(selection.get(i), ids.get(i));
            }
        } else {
			titles = null;
			ids = null;
			updating = false;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
				new IntentFilter(Utils.ACTION_LIST_TASKS));
		if (titles == null) {
			refreshTasks();
		} else {
			updateLayout(getIntent());
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArrayList(Utils.EXTRA_TITLES, titles);
		outState.putStringArrayList(Utils.EXTRA_IDS, ids);
		outState.putBoolean(Utils.EXTRA_UPDATING, updating);
        ArrayList<Integer> selection = new ArrayList<>();
        for (int i = 0; i < selectedItems.size(); i++) {
            selection.add(selectedItems.keyAt(i));
        }
        outState.putIntegerArrayList(SELECTED_ITEMS, selection);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_link_list, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
    public void onBackPressed() {
        if (actionMode != null) {
            actionMode.finish();
        } else {
            super.onBackPressed();
        }
    }

    public static <C> ArrayList<C> asArrayList(SparseArrayCompat<C> sparseArray) {
        if (sparseArray == null) return null;
        ArrayList<C> arrayList = new ArrayList<C>(sparseArray.size());
        for (int i = 0; i < sparseArray.size(); i++)
            arrayList.add(sparseArray.valueAt(i));
        return arrayList;
    }

    private void refreshTasks() {
		updating = true;
        swipeRefreshLayout.setRefreshing(updating);
		Intent service = new Intent(this, GoogleTasksService.class);
		service.setAction(Utils.ACTION_LIST_TASKS);
		startService(service);
	}

	private void deleteTasks() {
        Utils.log(String.format("%s items selected", selectedItems.size()));
        Intent service = new Intent(this, GoogleTasksService.class);
		service.setAction(Utils.ACTION_REMOVE_TASKS);
		service.putExtra(Utils.EXTRA_TASKS_IDS, asArrayList(selectedItems));
		startService(service);
        updating = true;
        swipeRefreshLayout.setRefreshing(updating);
    }

    private void updateLayout(Intent intent) {
		if ((intent.getAction() != null)
				&& (intent.getAction().equals(Utils.ACTION_LIST_TASKS))) {
			ids = intent.getStringArrayListExtra(Utils.EXTRA_IDS);
			titles = intent.getStringArrayListExtra(Utils.EXTRA_TITLES);
			updating = intent.getBooleanExtra(Utils.EXTRA_UPDATING, false);
		}

        swipeRefreshLayout.setRefreshing(updating);

		TextView text = (TextView) findViewById(android.R.id.empty);
		list_view = (ListView) findViewById(android.R.id.list);
        list_view.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        if (!updating) {
            if (actionMode != null) {
                actionMode.finish();
            }
			if ((titles == null) || (titles.size() < 1)) {
				text.setText(R.string.txt_empty_list);
				list_view.setVisibility(View.GONE);
				text.setVisibility(View.VISIBLE);
				if (intent.getStringExtra(Utils.EXTRA_ERROR_TEXT) != null) {
					Toast.makeText(this,
							intent.getStringExtra(Utils.EXTRA_ERROR_TEXT),
							Toast.LENGTH_SHORT).show();
				}
			} else {
                if (adapter == null) {
                    adapter = new TasksArrayAdapter(this, ids, titles, this);
                    list_view.setAdapter(adapter);
                } else {
                    adapter.updateLists(ids, titles);
                }
                if (selectedItems.size() > 0) {
                    for (int i = 0; i < selectedItems.size(); i++) {
                        adapter.setChecked(selectedItems.keyAt(i), true);
                        onItemChecked(selectedItems.keyAt(i), true);
                    }
                    adapter.notifyDataSetChanged();
                }
				list_view.setVisibility(View.VISIBLE);
				text.setVisibility(View.GONE);
			}
		} else {
			list_view.setVisibility(View.GONE);
			text.setVisibility(View.GONE);
		}

	}

    @Override
    public void onItemChecked(int position, boolean checked) {
        Utils.log(String.format("position: %s - key: %s", position, ids.get(position)));

        list_view.setItemChecked(position, checked);

        if (checked) {
            selectedItems.put(position, ids.get(position));
        } else {
            selectedItems.remove(position);
        }

        if (selectedItems.size() > 0) {
            String title = getResources().getQuantityString(
                    R.plurals.txt_selected_items, selectedItems.size());
            title = String.format(title, selectedItems.size());
            if (actionMode == null) {
                actionMode = startSupportActionMode(new ActionModeCallback());
            }
            actionMode.setTitle(title);
        } else {
            if (actionMode != null) {
                actionMode.finish();
            }
        }
    }

    @Override
    public void onRefresh() {
        refreshTasks();
    }

    public static class YesNoDialog extends DialogFragment {
		public static YesNoDialog newInstance() {
			YesNoDialog dlg = new YesNoDialog();
			Bundle bundle = new Bundle();
			dlg.setArguments(bundle);
			return dlg;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog dialog = new AlertDialog.Builder(getActivity())
					.setMessage(R.string.txt_confirm)
					.setPositiveButton(R.string.btn_delete,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface arg0,
										int arg1) {
									((LinkListActivity) getActivity())
											.deleteTasks();
								}
							}).setNegativeButton(android.R.string.cancel, null)
					.create();

			//We will color the dialog's buttons only on Honeycomb+ devices
			//On older platforms, we use the default dialog themes
			/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				dialog.show();
				dialog.getButton(DialogInterface.BUTTON_POSITIVE)
						.setBackgroundResource(
								R.drawable.borderlessbutton_background_pdttheme);
				dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
						.setBackgroundResource(
								R.drawable.borderlessbutton_background_pdttheme);
			}*/

			return dialog;
		}

	}

    private class ActionModeCallback implements ActionMode.Callback {

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            getMenuInflater().inflate(R.menu.cab_wait_list, menu);
            menu.findItem(R.id.action_send_multiple).setVisible(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            if (menuItem.getItemId() == R.id.action_delete_multiple) {
                YesNoDialog.newInstance()
                        .show(getSupportFragmentManager(), "deleteDialog");
                Utils.log(String.format("%s items selected", selectedItems.size()));
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            for (int i = 0; i < list_view.getAdapter().getCount(); i++) {
                list_view.setItemChecked(i, false);
            }

            selectedItems.clear();
            adapter.clearSelections();

            if (mode == actionMode) {
                actionMode = null;
            }
        }
    }
}
