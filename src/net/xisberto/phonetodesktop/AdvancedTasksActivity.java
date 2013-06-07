package net.xisberto.phonetodesktop;

import java.net.MalformedURLException;
import java.net.URL;

import net.xisberto.phonetodesktop.JSoupAsyncTask.JSoupAsyncListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class AdvancedTasksActivity extends SherlockActivity
		implements OnClickListener, JSoupAsyncListener {

	private String text_from_extra, text_to_send;
	private CheckBox cb_only_links, cb_unshorten, cb_get_titles;
	private String[]
			cache_unshorten = null,
			cache_titles = null;
	private static final String
			SAVE_CACHE_UNSHORTEN = "cache_unshorten",
			SAVE_CACHE_TITLES = "cache_titles";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_advanced_task);
		
		if (getIntent().getAction().equals(Intent.ACTION_SEND)
				&& getIntent().hasExtra(Intent.EXTRA_TEXT)) {
			text_from_extra = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			text_to_send = text_from_extra;
		} else {
			finish();
			return;
		}
		
		Preferences prefs = new Preferences(this);
		
		cb_only_links = ((CheckBox)findViewById(R.id.cb_only_links));
		cb_only_links.setOnClickListener(this);
		cb_unshorten = ((CheckBox)findViewById(R.id.cb_unshorten));
		cb_unshorten.setOnClickListener(this);
		cb_get_titles = ((CheckBox)findViewById(R.id.cb_get_titles));
		cb_get_titles.setOnClickListener(this);
//		((CheckBox)findViewById(R.id.cb_links_as_tasks)).setOnClickListener(this);
		
		if (savedInstanceState != null) {
			cache_unshorten = savedInstanceState.getStringArray(SAVE_CACHE_UNSHORTEN);
			cache_titles = savedInstanceState.getStringArray(SAVE_CACHE_TITLES);
		} else {
			cb_only_links.setChecked(prefs.loadOnlyLinks());
			cb_unshorten.setChecked(prefs.loadUnshorten());
			cb_get_titles.setChecked(prefs.loadGetTitles());
		}
		
	}

	@Override
	protected void onResume() {
		super.onResume();
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
		getSupportMenuInflater().inflate(R.menu.activity_advanced, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_send:
			Intent service = new Intent(this, GoogleTasksService.class);
			service.setAction(Utils.ACTION_SEND_TASK);
			service.putExtra(Intent.EXTRA_TEXT,
					((TextView)findViewById(R.id.text_preview)).getText().toString());
			startService(service);
			Preferences prefs = new Preferences(this);
			prefs.saveOnlyLinks(cb_only_links.isChecked());
			prefs.saveUnshorten(cb_unshorten.isChecked());
			prefs.saveGetTitles(cb_get_titles.isChecked());
		case R.id.item_cancel:
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.cb_only_links:
		case R.id.cb_unshorten:
		case R.id.cb_get_titles:
			processCheckBoxes();
			break;

		default:
			break;
		}
	}

	/**
	 * Filter the URLs in {@code text} and return
	 * them separated by spaces
	 * @param text the text to search the URLs in
	 * @return the found URLs separated by space
	 */
	private String filterLinks(String text) {
		String[] parts = text.split("\\s");
		String result = "";
		for (String part : parts) {
			try {
				URL u = new URL(part);
				result += part + " ";
			} catch (MalformedURLException e) {
				// do nothing
			}
		}
		return result;
	}
	
	/**
	 * Separate URLs in {@code text} from it.
	 * @param text
	 * @return at index 0, there's {@code text} but each URL
	 * found is replaced by [index]. In the following indexes
	 * there are the URLs found.
	 */
	/*private ArrayList<String> separateLinks(String text) {
		String[] parts = text.split("\\s");
		ArrayList<String> result = new ArrayList<String>(parts.length);
		String reconstructed = "";
		result.add("");
		for (String part : parts) {
			try {
				URL u = new URL(part);
				int size = result.size();
				result.add("[" + size + "] " + part);
				part = "[" + size + "]";
			} catch (MalformedURLException e) {
				// do nothing
			}
			reconstructed += part + " ";
		}
		result.set(0, reconstructed);
		return result;
	}*/
	
	private void unshortenLinks(String text) {
		if (cache_unshorten != null) {
			onPostUnshorten(cache_unshorten);
		} else {
			String links = filterLinks(text);
			String[] parts = links.split(" ");
			JSoupAsyncTask jsoup = new JSoupAsyncTask(this, JSoupAsyncTask.TASK_UNSHORTEN);
			jsoup.execute(parts);
		}
	}
	
	private void getTitles(String text) {
		if (cache_titles != null) {
			onPostGetTitle(cache_titles);
		} else {
			String links = filterLinks(text);
			String[] parts = links.split(" ");
			JSoupAsyncTask jsoup = new JSoupAsyncTask(this, JSoupAsyncTask.TASK_GET_TITLE);
			jsoup.execute(parts);
		}
	}
	
	private void setPreview(String text) {
		((TextView)findViewById(R.id.text_preview)).setText(text);
	}
	
	private void processCheckBoxes() {
		if (cb_only_links.isChecked()) {
			text_to_send = filterLinks(text_from_extra);
		} else {
			text_to_send = text_from_extra;
		}
		
		if (cb_unshorten.isChecked()) {
			unshortenLinks(text_to_send);
		}

		if (cb_get_titles.isChecked()) {
			getTitles(text_to_send);
		}
		/*
		if (((CheckBox)findViewById(R.id.cb_links_as_tasks)).isChecked()) {
			ArrayList<String> separated = separateLinks(text_to_send);
			text_to_send = separated.get(0);
			for (int i = 1; i < separated.size(); i++) {
				text_to_send += "\n- " + separated.get(i);
			}
		}
		*/
		setPreview(text_to_send);
	}

	@Override
	public void setWaiting() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
		findViewById(R.id.text_preview).setEnabled(false);
	}

	@Override
	public void setDone() {
		findViewById(R.id.progress).setVisibility(View.GONE);
		findViewById(R.id.text_preview).setEnabled(true);
	}

	@Override
	public void onPostUnshorten(String[] result) {
		if (result == null) {
			Toast.makeText(this, R.string.txt_error_timeout, Toast.LENGTH_SHORT).show();
			cb_unshorten.setChecked(false);
			return;
		}
		int index = 0;
		String[] parts = text_to_send.split("\\s");
		String output = "";
		for (String part : parts) {
			try {
				URL u = new URL(part);
				part = result[index];
				index++;
			} catch (MalformedURLException e) {
				// do nothing
			}
			output += part + " ";
		}
		cache_unshorten = result;
		text_to_send = output;
		setPreview(text_to_send);
	}

	@Override
	public void onPostGetTitle(String[] result) {
		if (result == null) {
			Toast.makeText(this, R.string.txt_error_timeout, Toast.LENGTH_SHORT).show();
			cb_get_titles.setChecked(false);
			return;
		}
		int index = 0;
		String[] parts = text_to_send.split("\\s");
		String output = "";
		for (String part : parts) {
			try {
				URL u = new URL(part);
				part = part + " [" + result[index] + "]";
				index++;
			} catch (MalformedURLException e) {
				// do nothing
			}
			output += part + " ";
		}
		cache_titles = result;
		text_to_send = output;
		setPreview(text_to_send);
	}

}
