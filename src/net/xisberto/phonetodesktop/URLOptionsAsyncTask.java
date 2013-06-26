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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.AsyncTask;

public class URLOptionsAsyncTask extends AsyncTask<String, Void, String[]> {

	public static final int TASK_UNSHORTEN = 0, TASK_GET_TITLE = 1;
	private static final Pattern TITLE_TAG = Pattern.compile(
			"\\<title>(.*?)\\</title>", Pattern.CASE_INSENSITIVE
					| Pattern.DOTALL), CHARSET_HEADER = Pattern.compile(
			"charset=([-_a-zA-Z0-9]+)", Pattern.CASE_INSENSITIVE
					| Pattern.DOTALL);

	private int task;
	private URLOptionsListener listener;

	public URLOptionsAsyncTask(URLOptionsListener listener, int task) {
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
		} catch (IOException ioe) {
			return null;
		} catch (NullPointerException npe) {
			return null;
		}
	}

	private String[] unshorten(String... params) throws IOException {
		String[] result = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			Utils.log("unshorten " + params[i]);

			URLConnection connection = new URL(params[i]).openConnection();
			connection.connect();
			InputStream instr = connection.getInputStream();
			instr.close();

			result[i] = connection.getURL().toString();
			Utils.log("got " + result[i]);
		}
		return result;
	}

	private String[] getTitles(String... params) throws IOException,
			NullPointerException {
		String[] result = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			Utils.log("getTitles " + params[i]);

			String title = getPageTitle(params[i]);
			if (title != null) {
				Utils.log("Found title " + title);
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

	/**
	 * Loads a url and search for a HTML title. <br>
	 * Based on the code found at
	 * http://www.gotoquiz.com/web-coding/programming/
	 * java-programming/how-to-extract-titles-from-web-pages-in-java/
	 * 
	 * @param url
	 *            the url to load
	 * @return the HTML title or {@code null} if it's not a HTML page or if no
	 *         title was found
	 * @throws IOException
	 */
	private String getPageTitle(String url) throws IOException,
			NullPointerException {
		HttpClient client = new DefaultHttpClient();
		HttpUriRequest request = new HttpGet(url);
		HttpResponse response = client.execute(request);

		// Make sure this URL goes to a HTML page
		String headerValue = "";
		for (Header header : response.getAllHeaders()) {
			Utils.log("header: " + header.getName());
			if (header.getName().equals("Content-Type")) {
				headerValue = header.getValue();
				break;
			}
		}

		Utils.log(headerValue);
		String contentType = "";
		Charset charset = Charset.forName("ISO-8859-1");
		int sep = headerValue.indexOf(";");
		if (sep != -1) {
			contentType = headerValue.substring(0, sep);
			Matcher matcherCharset = CHARSET_HEADER.matcher(headerValue);
			if (matcherCharset.find()) {
				charset = Charset.forName(matcherCharset.group(1));
			}
		} else {
			contentType = headerValue;
		}

		if (contentType.equals("text/html")) {
			// Now we can search for <title>
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					in, charset));
			int n = 0, totalRead = 0;
			char[] buffer = new char[1024];
			StringBuilder content = new StringBuilder();

			while (totalRead < 8192
					&& (n = reader.read(buffer, 0, buffer.length)) != -1) {
				content.append(buffer);
				totalRead += n;
				Matcher m = TITLE_TAG.matcher(content);
				Utils.log("Found: " + m.find());
			}
			reader.close();

			Utils.log(content.toString());

			Matcher matcher = TITLE_TAG.matcher(content);
			if (matcher.find()) {
				return matcher.group(1).replaceAll("[\\s\\<>]+", " ").trim();
			}
		}

		return null;
	}

	public interface URLOptionsListener {
		public void setWaiting();

		public void setDone();

		public void onPostUnshorten(String[] result);

		public void onPostGetTitle(String[] result);
	}

}
