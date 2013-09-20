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
package net.xisberto.phonetodesktop.network;

import java.io.IOException;


import android.os.AsyncTask;
import android.util.Log;

public class URLOptionsAsyncTask extends AsyncTask<String, Void, String[]> {

	public static final int TASK_UNSHORTEN = 0, TASK_GET_TITLE = 1;
	private int task;
	private URLOptionsListener listener;
	private URLOptions urlOptions;

	public URLOptionsAsyncTask(URLOptionsListener listener, int task) {
		super();
		this.task = task;
		this.listener = listener;
		urlOptions = new URLOptions();
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
				return urlOptions.unshorten(params);
			case TASK_GET_TITLE:
				return urlOptions.getTitles(params);
			default:
				return null;
			}
		} catch (IOException ioe) {
			Log.e(URLOptionsAsyncTask.class.getName(), ioe.getMessage());
			return null;
		} catch (NullPointerException npe) {
			Log.e(URLOptionsAsyncTask.class.getName(), Log.getStackTraceString(npe));
			return null;
		}
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

	@Override
	protected void onCancelled() {
		super.onCancelled();
		urlOptions.cancel();
		listener.setDone();
	}

	@Override
	protected void onCancelled(String[] result) {
		urlOptions.cancel();
		switch (task) {
		case TASK_UNSHORTEN:
			listener.onPostUnshorten(null);
			break;
		case TASK_GET_TITLE:
			listener.onPostGetTitle(null);
			break;
		default:
			break;
		}
		listener.setDone();
	}

	

	public interface URLOptionsListener {
		public void setWaiting();

		public void setDone();

		public void onPostUnshorten(String[] result);

		public void onPostGetTitle(String[] result);
	}

}
