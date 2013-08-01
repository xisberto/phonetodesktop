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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.preference.PreferenceManager;

public class Preferences {
	private static final String ACCOUNT_NAME = "accountName",
			AUTH_TOKEN = "authToken", LIST_ID = "listId",
			WHAT_TO_SEND = "whatToSend", LAST_SENT_TEXT = "lastSentText",
			ONLY_LINKS = "only_links",
			UNSHORTEN = "unshorten", GET_TITLES = "getTitles";

	public static final String[] WHAT_TO_SEND_VALUES = { "entire_text",
			"only_links", "always_ask" };

	private SharedPreferences prefs;

	public Preferences(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
	}

	public String loadAccountName() {
		return prefs.getString(ACCOUNT_NAME, null);
	}

	public String loadAuthToken() {
		return prefs.getString(AUTH_TOKEN, null);
	}

	public String loadListId() {
		return prefs.getString(LIST_ID, null);
	}

	public String loadWhatToSend() {
		return prefs.getString(WHAT_TO_SEND, WHAT_TO_SEND_VALUES[2]);
	}
	
	public String loadLastSentText() {
		return prefs.getString(LAST_SENT_TEXT, null);
	}
	
	public boolean loadOnlyLinks() {
		return prefs.getBoolean(ONLY_LINKS, false);
	}
	
	public boolean loadUnshorten() {
		return prefs.getBoolean(UNSHORTEN, false);
	}
	
	public boolean loadGetTitles() {
		return prefs.getBoolean(GET_TITLES, false);
	}
	
	private void saveString(String key, String value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		apply(editor);
	}

	public void saveAccountName(String accountName) {
		saveString(ACCOUNT_NAME, accountName);
	}

	public void saveAuthToken(String authToken) {
		saveString(AUTH_TOKEN, authToken);
	}

	public void saveListId(String listId) {
		saveString(LIST_ID, listId);
	}

	public void saveWhatToSend(int value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(WHAT_TO_SEND, WHAT_TO_SEND_VALUES[value]);
		apply(editor);
	}
	
	public void saveLastSentText(String text) {
		saveString(LAST_SENT_TEXT, text);
	}
	
	public void saveOnlyLinks(boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(ONLY_LINKS, value);
		apply(editor);
	}
	
	public void saveUnshorten(boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(UNSHORTEN, value);
		apply(editor);
	}
	
	public void saveGetTitles(boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(GET_TITLES, value);
		apply(editor);
	}

	public void removeAuthToken() {
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(AUTH_TOKEN);
		apply(editor);
	}
	
	public void removeListId() {
		SharedPreferences.Editor editor = prefs.edit();
		editor.remove(LIST_ID);
		apply(editor);
	}

	@SuppressLint("NewApi")
	public void apply(Editor editor) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			editor.apply();
		} else {
			editor.commit();
		}
	}

}
