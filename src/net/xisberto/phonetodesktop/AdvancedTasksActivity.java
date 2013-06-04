package net.xisberto.phonetodesktop;

import java.net.MalformedURLException;
import java.net.URL;

import net.xisberto.phonetodesktop.JSoupAsyncTask.JSoupAsyncListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.api.services.tasks.model.Task;

public class AdvancedTasksActivity extends SherlockActivity
		implements OnCheckedChangeListener, OnClickListener, JSoupAsyncListener {

	private String extra_text;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_advanced_task);
		
		if (getIntent().getAction().equals(Intent.ACTION_SEND)
				&& getIntent().hasExtra(Intent.EXTRA_TEXT)) {
			extra_text = getIntent().getStringExtra(Intent.EXTRA_TEXT);
			setPreview(extra_text);
		} else {
			finish();
			return;
		}
		
		findViewById(R.id.btn_send).setOnClickListener(this);
		findViewById(R.id.btn_cancel).setOnClickListener(this);
		((CheckBox)findViewById(R.id.cb_only_links)).setOnCheckedChangeListener(this);
		((CheckBox)findViewById(R.id.cb_unshorten)).setOnCheckedChangeListener(this);
		((CheckBox)findViewById(R.id.cb_get_titles)).setOnCheckedChangeListener(this);
		
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
		case R.id.item_cancel:
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Build the {@link Task} object to be sent
	 * @return the Task to be sent
	 */
	public Task buildTask() {
		Task task = new Task();
		task.setTitle(((TextView) findViewById(R.id.text_preview))
				.getText().toString());
		return task;
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
		for (int i = 0; i < parts.length; i++) {
			try {
				URL u = new URL(parts[i]);
				result += parts[i] + " ";
			} catch (MalformedURLException e) {
				// do nothing
			}
		}
		return result;
	}
	
	private void unshortenLinks(String text) {
		String links = filterLinks(text);
		String[] parts = links.split(" ");
		JSoupAsyncTask jsoup = new JSoupAsyncTask(this, JSoupAsyncTask.TASK_UNSHORTEN);
		jsoup.execute(parts);
	}
	
	private void getTitles(String text) {
		String links = filterLinks(text);
		String[] parts = links.split(" ");
		JSoupAsyncTask jsoup = new JSoupAsyncTask(this, JSoupAsyncTask.TASK_GET_TITLE);
		jsoup.execute(parts);
	}
	
	private void setPreview(String text) {
		((TextView)findViewById(R.id.text_preview)).setText(text);
	}
	
	private String getPreview() {
		return ((TextView)findViewById(R.id.text_preview)).getText().toString();
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		String text_to_send = extra_text;
		switch (buttonView.getId()) {
		case R.id.cb_only_links:
			text_to_send = (isChecked ? filterLinks(extra_text) : extra_text);
			setPreview(text_to_send);
			break;
		case R.id.cb_unshorten:
			if (isChecked) {
				unshortenLinks(text_to_send);
			} else {
				setPreview(text_to_send);
			}
			break;
		case R.id.cb_get_titles:
			if (isChecked) {
				getTitles(text_to_send);
			} else {
				setPreview(text_to_send);
			}

		default:
			break;
		}
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btn_cancel:
			finish();
			break;
		case R.id.btn_send:
			Intent service = new Intent(this, GoogleTasksService.class);
			service.setAction(Utils.ACTION_SEND_TASK);
			service.putExtra(Intent.EXTRA_TEXT,
					((TextView)findViewById(R.id.text_preview)).getText().toString());
			startService(service);
			finish();
		default:
			break;
		}
		
	}

	@Override
	public void prepareUI() {
		findViewById(R.id.progress).setVisibility(View.VISIBLE);
		setPreview("");
	}

	@Override
	public void onPostUnshorten(String[] result) {
		StringBuilder builder = new StringBuilder();
		for (String string : result) {
			builder.append(string);
			builder.append(" ");
		}
		setPreview(builder.toString());
		findViewById(R.id.progress).setVisibility(View.GONE);
	}

	@Override
	public void onPostGetTitle(String[] result) {
		StringBuilder builder = new StringBuilder();
		for (String string : result) {
			builder.append(string);
			builder.append(" ");
		}
		setPreview(builder.toString());
		findViewById(R.id.progress).setVisibility(View.GONE);
	}

}
