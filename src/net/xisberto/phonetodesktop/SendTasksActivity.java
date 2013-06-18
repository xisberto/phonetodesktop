package net.xisberto.phonetodesktop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.xisberto.phonetodesktop.URLOptionsAsyncTask.URLOptionsListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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

public class SendTasksActivity extends SherlockFragmentActivity
		implements URLOptionsListener, android.content.DialogInterface.OnClickListener {

	private String text_from_extra, text_to_send;
	private String[]
			cache_unshorten = null,
			cache_titles = null;
	private SendFragment send_fragment;
	private static final String
			SAVE_CACHE_UNSHORTEN = "cache_unshorten",
			SAVE_CACHE_TITLES = "cache_titles";
	private static final Pattern urlPattern = Pattern.compile(
			"\\b((?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])",
	        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.log("onCreate");
		
		if (getIntent().getAction().equals(Intent.ACTION_SEND)
				&& getIntent().hasExtra(Intent.EXTRA_TEXT)) {
			text_from_extra = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			text_to_send = text_from_extra;

			send_fragment = (SendFragment) getSupportFragmentManager().findFragmentByTag("send_fragment");
			if (send_fragment == null) {
				send_fragment = SendFragment.newInstance(text_from_extra);
			}
			if (! send_fragment.isAdded()) {
				if (getResources().getBoolean(R.bool.is_tablet)) {
						send_fragment.show(getSupportFragmentManager(), "send_fragment");
				} else {
					setTheme(R.style.Theme_Pdttheme);
					getSupportFragmentManager().beginTransaction()
							.replace(android.R.id.content, send_fragment, "send_fragment")
							.commit();
				}
			}
		} else {
			finish();
			return;
		}
		
		if (savedInstanceState != null) {
			cache_unshorten = savedInstanceState.getStringArray(SAVE_CACHE_UNSHORTEN);
			cache_titles = savedInstanceState.getStringArray(SAVE_CACHE_TITLES);
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		Utils.log("onResume");
		Preferences prefs = new Preferences(this);
		send_fragment.cb_only_links.setChecked(prefs.loadOnlyLinks());
		send_fragment.cb_unshorten.setChecked(prefs.loadUnshorten());
		send_fragment.cb_get_titles.setChecked(prefs.loadGetTitles());
		processCheckBoxes();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArray(SAVE_CACHE_UNSHORTEN, cache_unshorten);
		outState.putStringArray(SAVE_CACHE_TITLES, cache_titles);
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
		startService(service);
		Preferences prefs = new Preferences(this);
		prefs.saveOnlyLinks(send_fragment.cb_only_links.isChecked());
		prefs.saveUnshorten(send_fragment.cb_unshorten.isChecked());
		prefs.saveGetTitles(send_fragment.cb_get_titles.isChecked());
		prefs.saveLastSentText(text_to_send);
	}

	/**
	 * Filter the URLs in {@code text} and return
	 * them separated by spaces
	 * @param text the text to search the URLs in
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
		if (send_fragment.cb_only_links.isChecked()) {
			text_to_send = filterLinks(text_from_extra);
		} else {
			text_to_send = text_from_extra;
		}
		
		if (send_fragment.cb_unshorten.isChecked()) {
			unshortenLinks(text_to_send);
		}

		if (send_fragment.cb_get_titles.isChecked()) {
			getTitles(text_to_send);
		}

		send_fragment.setPreview(text_to_send);
	}
	
	private void unshortenLinks(String text) {
		if (cache_unshorten != null) {
			onPostUnshorten(cache_unshorten);
		} else {
			String links = filterLinks(text).trim();
			String[] parts = links.split(" ");
			URLOptionsAsyncTask jsoup = new URLOptionsAsyncTask(this, URLOptionsAsyncTask.TASK_UNSHORTEN);
			jsoup.execute(parts);
		}
	}
	
	private void getTitles(String text) {
		if (cache_titles != null) {
			onPostGetTitle(cache_titles);
		} else {
			String links = filterLinks(text).trim();
			String[] parts = links.split(" ");
			URLOptionsAsyncTask jsoup = new URLOptionsAsyncTask(this, URLOptionsAsyncTask.TASK_GET_TITLE);
			jsoup.execute(parts);
		}
	}

	@Override
	public void setWaiting() {
		send_fragment.setWaiting(true);
	}

	@Override
	public void setDone() {
		send_fragment.setWaiting(false);
	}

	@Override
	public void onPostUnshorten(String[] result) {
		if (result == null) {
			Toast.makeText(this, R.string.txt_error_timeout, Toast.LENGTH_SHORT).show();
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
	}

	@Override
	public void onPostGetTitle(String[] result) {
		if (result == null) {
			Toast.makeText(this, R.string.txt_error_timeout, Toast.LENGTH_SHORT).show();
			send_fragment.cb_get_titles.setChecked(false);
			return;
		}

		int index = 0;
		Matcher matcher = urlPattern.matcher(text_to_send);
		while (matcher.find()) {
			if (! matcher.group().equals(result[index])) {
				//don't replace when we have the URL and not a title
				text_to_send = text_to_send.replace(
						matcher.group(),
						matcher.group() + " [" + result[index] + "]");
			}
			index++;
		}
		
		cache_titles = result;
		send_fragment.setPreview(text_to_send);
	}
	
    public static class SendFragment extends SherlockDialogFragment implements OnClickListener {
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
        		throw new ClassCastException("Activity must be SendTasksActivity");
        	}
		}

		@Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setView(createView())
                    .setIcon(R.drawable.ic_launcher)
                    .setTitle(R.string.filter_title)
                    .setPositiveButton(R.string.send, (SendTasksActivity) getActivity())
                    .setNegativeButton(android.R.string.cancel, (SendTasksActivity) getActivity())
                    .create();

            return dialog;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        	Utils.log("onCreateView");
            if (getDialog() == null) {
                return createView();
            } else {
                return super.onCreateView(inflater, container, savedInstanceState);
            }
        }

        private View createView() {
        	Utils.log("createView");
            v = getActivity().getLayoutInflater().inflate(R.layout.layout_send_task, null);
            ((TextView)v.findViewById(R.id.text_preview)).setText(getArguments().getString(Intent.EXTRA_TEXT));
    		
    		cb_only_links = ((CheckBox)v.findViewById(R.id.cb_only_links));
    		cb_only_links.setOnClickListener(this);
    		cb_unshorten = ((CheckBox)v.findViewById(R.id.cb_unshorten));
    		cb_unshorten.setOnClickListener(this);
    		cb_get_titles = ((CheckBox)v.findViewById(R.id.cb_get_titles));
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
    		((TextView)v.findViewById(R.id.text_preview)).setText(text);
    	}
    	
    	private void setWaiting(boolean is_waiting) {
    		if (is_waiting) {
    			v.findViewById(R.id.progress).setVisibility(View.VISIBLE);
    			v.findViewById(R.id.text_preview).setEnabled(false);
    		} else {
    			v.findViewById(R.id.progress).setVisibility(View.GONE);
    			v.findViewById(R.id.text_preview).setEnabled(true);
    		}
    	}
    }

}
