package net.xisberto.phonetodesktop;

import java.io.IOException;

import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.os.AsyncTask;
import android.util.Log;

public class JSoupAsyncTask extends AsyncTask<String, Void, String[]> {

	public static final int TASK_UNSHORTEN = 0, TASK_GET_TITLE = 1;
	private static final String UNSHORTEN = "http://api.unshort.me/?r=URL&t=xml",
			TAG_SUCCESS = "success", TAG_RESOLVED = "resolvedURL";

	private int task;
	private JSoupAsyncListener listener;
	private Connection conn;

	public JSoupAsyncTask(JSoupAsyncListener listener, int task) {
		super();
		this.task = task;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		listener.setWaiting();
	}

	@Override
	protected String[] doInBackground(String... params) {
		switch (task) {
		case TASK_UNSHORTEN:
			return unshorten(params);
		case TASK_GET_TITLE:
			return getTitles(params);
		default:
			return null;
		}
	}

	private String[] unshorten(String... params) {
		String[] result = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			String url = UNSHORTEN.replace("URL", params[i]);
			conn = HttpConnection.connect(url);
			try {
				Document doc = conn.get();
				Element success = doc.getElementsByTag(TAG_SUCCESS).first();
				Log.d("unshorten", doc.text());
				if (success.text().equals("true")) {
					Element resolvedURL = doc.getElementsByTag(TAG_RESOLVED)
							.first();
					Log.d("JSoupAsync", resolvedURL.text());
					result[i] = resolvedURL.text();
				} else {
					result[i] = params[i];
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private String[] getTitles(String... params) {
		String[] result = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			conn = HttpConnection.connect(params[i]);
			try {
				Document doc = conn.get();
				String title = doc.title();
				if (title != null) {
					result[i] = title;
				} else {
					result[i] = params[i];
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	@Override
	protected void onPostExecute(String[] result) {
		switch (task) {
		case TASK_UNSHORTEN:
			listener.onPostUnshorten(result);
			break;
		case TASK_GET_TITLE:
			listener.onPostGetTitle(result);
			break;
		default:
			break;
		}
		listener.setDone();
	}

	public interface JSoupAsyncListener {
		public void setWaiting();
		
		public void setDone();

		public void onPostUnshorten(String[] result);

		public void onPostGetTitle(String[] result);
	}

}