/*******************************************************************************
 * Copyright (c) 2013 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop;

import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.model.LocalTask;
import net.xisberto.phonetodesktop.model.LocalTask.Options;
import net.xisberto.phonetodesktop.model.LocalTask.PersistCallback;
import net.xisberto.phonetodesktop.network.GoogleTasksService;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class SendTasksActivity extends SherlockFragmentActivity implements
		android.content.DialogInterface.OnClickListener {

	private String text_from_extra;
	private String[] cache_unshorten = null, cache_titles = null;
	private SendFragment send_fragment;
	private boolean restoreFromPreferences, isWaiting = false;
	private static final String SAVE_CACHE_UNSHORTEN = "cache_unshorten",
			SAVE_CACHE_TITLES = "cache_titles",
			SAVE_LOCAL_TASK_ID = "local_task_id",
			SAVE_IS_WAITING = "is_waiting";
	private DatabaseHelper databaseHelper;
	private LocalTask localTask;

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long taskId = intent.getLongExtra(Utils.EXTRA_TASK_ID, -1);
			if (taskId != -1) {
				cache_unshorten = intent
						.getStringArrayExtra(Utils.EXTRA_CACHE_UNSHORTEN);
				cache_titles = intent
						.getStringArrayExtra(Utils.EXTRA_CACHE_TITLES);
				localTask = databaseHelper.getTask(taskId);
				send_fragment.setPreview(localTask.getTitle());
				setDone();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.log("onCreate " + this.toString());

		if (getIntent().getAction().equals(Intent.ACTION_SEND)
				&& getIntent().hasExtra(Intent.EXTRA_TEXT)) {
			text_from_extra = getIntent().getStringExtra(Intent.EXTRA_TEXT);

			databaseHelper = DatabaseHelper
					.getInstance(getApplicationContext());
			
			if (savedInstanceState != null) {
				cache_unshorten = savedInstanceState
						.getStringArray(SAVE_CACHE_UNSHORTEN);
				cache_titles = savedInstanceState.getStringArray(SAVE_CACHE_TITLES);
				long local_id = savedInstanceState.getLong(SAVE_LOCAL_TASK_ID);
				localTask = databaseHelper.getTask(local_id);
				isWaiting = savedInstanceState.getBoolean(SAVE_IS_WAITING);
				restoreFromPreferences = false;
			} else {
				localTask = new LocalTask(this);
				localTask.setTitle(text_from_extra);
				Preferences prefs = new Preferences(this);
				if (prefs.loadDontAsk()) {
					// If this is set, we process and send the task without showing
					// the activity. processPreferences calls sentText on
					// localTask's persist callback
					processPreferences(prefs);
					finish();
					return;
				} else {
					// If we will show the activity, change the theme
					setTheme(R.style.Theme_PhoneToDesktop_ActivityOrDialog);
				}
				restoreFromPreferences = true;
			}

			send_fragment = (SendFragment) getSupportFragmentManager()
					.findFragmentByTag("send_fragment");
			if (send_fragment == null) {
				send_fragment = SendFragment.newInstance(text_from_extra);
			}
			if (!send_fragment.isAdded()) {
				if (getResources().getBoolean(R.bool.is_tablet)) {
					send_fragment.show(getSupportFragmentManager(),
							"send_fragment");
				} else {
					getSupportFragmentManager()
							.beginTransaction()
							.replace(android.R.id.content, send_fragment,
									"send_fragment").commit();
				}
			}
		} else {
			finish();
			return;
		}

	}

	@Override
	protected void onStart() {
		super.onStart();
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
				new IntentFilter(Utils.ACTION_RESULT_PROCESS_TASK));
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (restoreFromPreferences) {
			Preferences prefs = new Preferences(this);
			send_fragment.cb_only_links.setChecked(prefs.loadOnlyLinks());
			send_fragment.cb_unshorten.setChecked(prefs.loadUnshorten());
			send_fragment.cb_get_titles.setChecked(prefs.loadGetTitles());
			send_fragment.cb_dont_ask.setChecked(prefs.loadDontAsk());
		}
		if (!isWaiting) {
			Utils.log("not waiting, processCheckBoxes");
			processCheckBoxes();
		} else {
			Utils.log("waiting, setWaiting");
			setWaiting();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArray(SAVE_CACHE_UNSHORTEN, cache_unshorten);
		outState.putStringArray(SAVE_CACHE_TITLES, cache_titles);
		outState.putBoolean(SAVE_IS_WAITING, isWaiting);
		outState.putLong(SAVE_LOCAL_TASK_ID, localTask.getLocalId());
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// TODO delete the localTask when exiting the activity
		// localTask.delete();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		// During beta, we will inform the user that the task will be saved
		Toast.makeText(
				this,
				"Saving this task on Waiting List "
						+ "(after beta period, the task will be discarded)",
				Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!getResources().getBoolean(R.bool.is_tablet)) {
			getSupportMenuInflater().inflate(R.menu.activity_send, menu);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_send:
			sendText();
			saveCheckBoxes();
			finish();
			break;
		case R.id.item_cancel:
			localTask.delete();
			finish();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int whichButton) {
		switch (whichButton) {
		case DialogInterface.BUTTON_POSITIVE:
			sendText();
			saveCheckBoxes();
			finish();
			break;
		case DialogInterface.BUTTON_NEGATIVE:
			localTask.delete();
			finish();
			break;
		}
	}

	private void sendText() {
		Intent service = new Intent(this, GoogleTasksService.class);
		service.setAction(Utils.ACTION_SEND_TASKS);
		service.putExtra(Utils.EXTRA_TASKS_IDS,
				new long[] { localTask.getLocalId() });
		startService(service);
	}

	private void saveCheckBoxes() {
		Preferences prefs = new Preferences(this);
		prefs.saveOnlyLinks(send_fragment.cb_only_links.isChecked());
		prefs.saveUnshorten(send_fragment.cb_unshorten.isChecked());
		prefs.saveGetTitles(send_fragment.cb_get_titles.isChecked());
		prefs.saveDontAsk(send_fragment.cb_dont_ask.isChecked());
	}

	private void processPreferences(Preferences prefs) {
		processOptions(prefs.loadOnlyLinks(), prefs.loadUnshorten(),
				prefs.loadGetTitles(), true);
	}

	private void processCheckBoxes() {
		processOptions(send_fragment.cb_only_links.isChecked(),
				send_fragment.cb_unshorten.isChecked(),
				send_fragment.cb_get_titles.isChecked(), false);
	}

	private void processOptions(boolean only_links, boolean unshorten,
			boolean get_titles, final boolean send_immediately) {
		String links = Utils.filterLinks(text_from_extra).trim();
		if (links.equals("")) {
			Toast.makeText(this, R.string.txt_no_links, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		localTask.setOptions(0);

		if (only_links) {
			localTask.setTitle(links);
		} else {
			localTask.setTitle(text_from_extra);
		}

		if (unshorten) {
			if (cache_unshorten != null) {
				localTask.setTitle(Utils.replace(localTask.getTitle(),
						cache_unshorten));
			} else {
				localTask.addOption(Options.OPTION_UNSHORTEN);
			}
		} else {
			localTask.removeOption(Options.OPTION_UNSHORTEN);
		}

		if (get_titles) {
			if (cache_titles != null) {
				localTask.setTitle(Utils.appendInBrackets(localTask.getTitle(),
						cache_titles));
			} else {
				localTask.addOption(Options.OPTION_GETTITLES);
			}
		} else {
			localTask.removeOption(Options.OPTION_GETTITLES);
		}

		localTask.persist(new PersistCallback() {
			@Override
			public void run() {
				if (localTask.hasOption(Options.OPTION_UNSHORTEN)
						|| localTask.hasOption(Options.OPTION_GETTITLES)) {
					// Only start service if there's some option to process
					startProcessingTask();
					setWaiting();
				}
				if (send_immediately) {
					sendText();
				}
			}
		});

		if (send_fragment != null) {
			send_fragment.setPreview(localTask.getTitle());
		}
	}

	private void startProcessingTask() {
		Intent service = new Intent(SendTasksActivity.this,
				GoogleTasksService.class);
		service.setAction(Utils.ACTION_PROCESS_TASK);
		service.putExtra(Utils.EXTRA_TASK_ID, localTask.getLocalId());
		startService(service);
	}

	public void setWaiting() {
		Utils.log("Waiting " + this.toString());
		isWaiting = true;
		if (send_fragment != null) {
			send_fragment.setWaiting(true);
		}
	}

	public void setDone() {
		Utils.log("Done " + this.toString());
		isWaiting = false;
		send_fragment.setWaiting(false);
	}

	public static class SendFragment extends SherlockDialogFragment implements
			OnClickListener {
		private CheckBox cb_only_links, cb_unshorten, cb_get_titles,
				cb_dont_ask;
		private View v;

		public static SendFragment newInstance(String text) {
			SendFragment fragment = new SendFragment();
			Bundle args = new Bundle();
			args.putString(Intent.EXTRA_TEXT, text);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public void onAttach(Activity activity) {
			if (activity instanceof SendTasksActivity) {
				super.onAttach(activity);
			} else {
				throw new ClassCastException(
						"Activity must be SendTasksActivity");
			}
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog dialog = new AlertDialog.Builder(getActivity())
					.setView(createView())
					.setIcon(R.drawable.ic_launcher)
					.setTitle(R.string.filter_title)
					.setPositiveButton(R.string.send,
							(SendTasksActivity) getActivity())
					.setNegativeButton(android.R.string.cancel,
							(SendTasksActivity) getActivity()).create();

			return dialog;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			if (getDialog() == null) {
				return createView(inflater, container);
			} else {
				return super.onCreateView(inflater, container,
						savedInstanceState);
			}
		}

		private View createView() {
			return createView(getActivity().getLayoutInflater(), null);
		}

		private View createView(LayoutInflater inflater, ViewGroup container) {
			v = inflater.inflate(R.layout.layout_send_task, container, false);
			((TextView) v.findViewById(R.id.text_preview))
					.setText(getArguments().getString(Intent.EXTRA_TEXT));

			cb_only_links = ((CheckBox) v.findViewById(R.id.cb_only_links));
			cb_only_links.setOnClickListener(this);
			cb_unshorten = ((CheckBox) v.findViewById(R.id.cb_unshorten));
			cb_unshorten.setOnClickListener(this);
			cb_get_titles = ((CheckBox) v.findViewById(R.id.cb_get_titles));
			cb_get_titles.setOnClickListener(this);
			cb_dont_ask = (CheckBox) v.findViewById(R.id.cb_dont_ask);

			return v;
		}

		@Override
		public void onCancel(DialogInterface dialog) {
			super.onCancel(dialog);
			getActivity().finish();
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.cb_only_links:
			case R.id.cb_unshorten:
			case R.id.cb_get_titles:
				((SendTasksActivity) getActivity()).processCheckBoxes();
				break;

			default:
				break;
			}
		}

		private void setPreview(String text) {
			((TextView) v.findViewById(R.id.text_preview)).setText(text);
		}

		private void setWaiting(boolean is_waiting) {
			if (v == null) {
				return;
			}
			if (is_waiting) {
				v.findViewById(R.id.progress).setVisibility(View.VISIBLE);
				v.findViewById(R.id.text_preview).setEnabled(false);
				cb_only_links.setEnabled(false);
				cb_unshorten.setEnabled(false);
				cb_get_titles.setEnabled(false);
			} else {
				v.findViewById(R.id.progress).setVisibility(View.GONE);
				v.findViewById(R.id.text_preview).setEnabled(true);
				cb_only_links.setEnabled(true);
				cb_unshorten.setEnabled(true);
				cb_get_titles.setEnabled(true);
			}
		}
	}

}
