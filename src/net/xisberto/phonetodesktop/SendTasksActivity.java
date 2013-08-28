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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.xisberto.phonetodesktop.URLOptionsAsyncTask.URLOptionsListener;
import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.model.LocalTask;
import net.xisberto.phonetodesktop.model.LocalTask.PersistCallback;
import net.xisberto.phonetodesktop.model.LocalTask.Status;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
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
		URLOptionsListener, android.content.DialogInterface.OnClickListener {

	private String text_from_extra, text_to_send;
	private String[] cache_unshorten = null, cache_titles = null;
	private SendFragment send_fragment;
	private boolean restoreFromPreferences;
	private URLOptionsAsyncTask async_unshorten;
	private URLOptionsAsyncTask async_titles;
	private static final String SAVE_CACHE_UNSHORTEN = "cache_unshorten",
			SAVE_CACHE_TITLES = "cache_titles";
	private static final Pattern urlPattern = Pattern
			.compile(
					"\\b((?:https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])",
					Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
							| Pattern.DOTALL);

	private PersistCallback persistCallback = new PersistCallback() {
		@Override
		public void done() {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					Utils.log("Local tasks finished");
					Utils.log("Current task id: "+localTask.getLocalId());
					if (localTask.getLocalId() != -1) {
						LocalTask savedTask = databaseHelper.getTask(localTask.getLocalId());
						if (savedTask != null) {
							Utils.log("Saved task id: "+savedTask.getLocalId());
							Utils.log("Saved status: "+savedTask.getStatus().toString());
						}
					}
				}
			});
			t.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
			t.start();
		}
	};
	private DatabaseHelper databaseHelper;
	private LocalTask localTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.log("onCreate");

		if (getIntent().getAction().equals(Intent.ACTION_SEND)
				&& getIntent().hasExtra(Intent.EXTRA_TEXT)) {
			text_from_extra = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			text_to_send = text_from_extra;

			databaseHelper = DatabaseHelper
					.getInstance(getApplicationContext());
			localTask = new LocalTask(databaseHelper);
			localTask.setTitle(text_to_send).persist(persistCallback);

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

		if (savedInstanceState != null) {
			cache_unshorten = savedInstanceState
					.getStringArray(SAVE_CACHE_UNSHORTEN);
			cache_titles = savedInstanceState.getStringArray(SAVE_CACHE_TITLES);
			restoreFromPreferences = false;
		} else {
			restoreFromPreferences = true;
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		if (restoreFromPreferences) {
			Preferences prefs = new Preferences(this);
			send_fragment.cb_only_links.setChecked(prefs.loadOnlyLinks());
			send_fragment.cb_unshorten.setChecked(prefs.loadUnshorten());
			send_fragment.cb_get_titles.setChecked(prefs.loadGetTitles());
		}
		processCheckBoxes();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArray(SAVE_CACHE_UNSHORTEN, cache_unshorten);
		outState.putStringArray(SAVE_CACHE_TITLES, cache_titles);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (isFinishing()) {
			if (async_unshorten != null) {
				async_unshorten.cancel(true);
			}
			if (async_titles != null) {
				async_titles.cancel(true);
			}
		}
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
		case R.id.item_cancel:
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(DialogInterface dialogInterface, int whichButton) {
		switch (whichButton) {
		case DialogInterface.BUTTON_POSITIVE:
			sendText();
		case DialogInterface.BUTTON_NEGATIVE:
			finish();
		}
	}

	private void sendText() {
		Intent service = new Intent(this, GoogleTasksService.class);
		service.setAction(Utils.ACTION_SEND_TASK);
		service.putExtra(Intent.EXTRA_TEXT, text_to_send);
		service.putExtra(GoogleTasksService.EXTRA_LOCAL_TASK_ID, localTask.getLocalId());
		startService(service);

		localTask.setStatus(Status.SENDING).persist(persistCallback);

		Preferences prefs = new Preferences(this);
		prefs.saveOnlyLinks(send_fragment.cb_only_links.isChecked());
		prefs.saveUnshorten(send_fragment.cb_unshorten.isChecked());
		prefs.saveGetTitles(send_fragment.cb_get_titles.isChecked());
		prefs.saveLastSentText(text_to_send);
	}

	/**
	 * Filter the URLs in {@code text} and return them separated by spaces
	 * 
	 * @param text
	 *            the text to search the URLs in
	 * @return the found URLs separated by space
	 */
	private String filterLinks(String text) {
		String result = "";
		Matcher matcher = urlPattern.matcher(text);
		while (matcher.find()) {
			result += matcher.group() + " ";
		}

		return result;
	}

	private void processCheckBoxes() {
		text_to_send = text_from_extra;
		String links = filterLinks(text_to_send).trim();
		if (links.equals("")) {
			Toast.makeText(this, R.string.txt_no_links, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		if (send_fragment.cb_only_links.isChecked()) {
			text_to_send = links;
		} else {
			text_to_send = text_from_extra;
		}

		if (send_fragment.cb_unshorten.isChecked()) {
			unshortenLinks(links);
		}

		if (send_fragment.cb_get_titles.isChecked()) {
			getTitles(links);
		}

		send_fragment.setPreview(text_to_send);
	}

	private void unshortenLinks(String links) {
		if (cache_unshorten != null) {
			onPostUnshorten(cache_unshorten);
		} else {
			localTask.setStatus(Status.PROCESSING_UNSHORTEN).persist(
					persistCallback);
			String[] parts = links.split(" ");
			async_unshorten = new URLOptionsAsyncTask(this,
					URLOptionsAsyncTask.TASK_UNSHORTEN);
			async_unshorten.execute(parts);
		}
	}

	private void getTitles(String links) {
		if (cache_titles != null) {
			onPostGetTitle(cache_titles);
		} else {
			localTask.setStatus(Status.PROCESSING_TITLE).persist(
					persistCallback);
			String[] parts = links.split(" ");
			async_titles = new URLOptionsAsyncTask(this,
					URLOptionsAsyncTask.TASK_GET_TITLE);
			async_titles.execute(parts);
		}
	}

	@Override
	public void setWaiting() {
		Utils.log(this.toString());
		send_fragment.setWaiting(true);
	}

	@Override
	public void setDone() {
		Utils.log(this.toString());
		send_fragment.setWaiting(false);
	}

	@Override
	public void onPostUnshorten(String[] result) {
		if (result == null) {
			if (!isFinishing()) {
				Toast.makeText(this, R.string.txt_error_timeout,
						Toast.LENGTH_SHORT).show();
			}
			send_fragment.cb_unshorten.setChecked(false);
			return;
		}
		int index = 0;
		Matcher matcher = urlPattern.matcher(text_to_send);
		while (matcher.find()) {
			text_to_send = text_to_send.replace(matcher.group(), result[index]);
			index++;
		}

		cache_unshorten = result;
		send_fragment.setPreview(text_to_send);
		localTask.setTitle(text_to_send).setStatus(Status.READY)
				.persist(persistCallback);
	}

	@Override
	public void onPostGetTitle(String[] result) {
		if (result == null) {
			if (!isFinishing()) {
				Toast.makeText(this, R.string.txt_error_timeout,
						Toast.LENGTH_SHORT).show();
			}
			send_fragment.cb_get_titles.setChecked(false);
			return;
		}

		Utils.log("Got " + result.length + " titles");
		int index = 0;
		Matcher matcher = urlPattern.matcher(text_to_send);
		while (matcher.find()) {
			if (!matcher.group().equals(result[index])) {
				// don't replace when we have the URL and not a title
				text_to_send = text_to_send.replace(matcher.group(),
						matcher.group() + " [" + result[index] + "]");
			}
			index++;
		}

		cache_titles = result;
		send_fragment.setPreview(text_to_send);
		localTask.setTitle(text_to_send).setStatus(Status.READY)
				.persist(persistCallback);
	}

	public static class SendFragment extends SherlockDialogFragment implements
			OnClickListener {
		private CheckBox cb_only_links, cb_unshorten, cb_get_titles;
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
			Utils.log("onCreateView");
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
			Utils.log("createView");
			v = inflater.inflate(R.layout.layout_send_task, container, false);
			((TextView) v.findViewById(R.id.text_preview))
					.setText(getArguments().getString(Intent.EXTRA_TEXT));

			cb_only_links = ((CheckBox) v.findViewById(R.id.cb_only_links));
			cb_only_links.setOnClickListener(this);
			cb_unshorten = ((CheckBox) v.findViewById(R.id.cb_unshorten));
			cb_unshorten.setOnClickListener(this);
			cb_get_titles = ((CheckBox) v.findViewById(R.id.cb_get_titles));
			cb_get_titles.setOnClickListener(this);

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
