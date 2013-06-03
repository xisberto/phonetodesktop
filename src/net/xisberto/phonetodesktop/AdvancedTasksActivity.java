package net.xisberto.phonetodesktop;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.google.api.services.tasks.model.Task;

public class AdvancedTasksActivity extends Activity implements OnCheckedChangeListener, OnClickListener {

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
	
	private void setPreview(String text) {
		((TextView)findViewById(R.id.text_preview)).setText(text);
	}

	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch (buttonView.getId()) {
		case R.id.cb_only_links:
			setPreview(isChecked ? filterLinks(extra_text) : extra_text);
			break;

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

}
