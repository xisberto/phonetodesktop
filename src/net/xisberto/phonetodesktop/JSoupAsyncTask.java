package net.xisberto.phonetodesktop;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.jsoup.Connection;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;

import android.os.AsyncTask;

public class JSoupAsyncTask extends AsyncTask<String, Void, String[]> {

	public static final int TASK_UNSHORTEN = 0, TASK_GET_TITLE = 1;

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
		try {
			switch (task) {
			case TASK_UNSHORTEN:
				return unshorten(params);
			case TASK_GET_TITLE:
				return getTitles(params);
			default:
				return null;
			}
		} catch (IOException e) {
			return null;
		}
	}

	private String[] unshorten(String... params) throws IOException {
		String[] result = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			
			URLConnection connection = new URL(params[i]).openConnection();
			connection.connect();
			InputStream instr = connection.getInputStream();
			instr.close();
			
			result[i] = connection.getURL().toString();
		}
		return result;
	}

	private String[] getTitles(String... params) throws IOException {
		String[] result = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			conn = HttpConnection.connect(params[i]);
			Document doc = conn.get();
			String title = doc.title();
			if (title != null) {
				result[i] = title;
			} else {
				result[i] = params[i];
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