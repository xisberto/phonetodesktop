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
			LIST_ID = "listId", LAST_SENT_TEXT = "lastSentText";

	private static String ONLY_LINKS, UNSHORTEN, GET_TITLES, DONT_ASK;

	private SharedPreferences prefs;

	public Preferences(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context
				.getApplicationContext());
		ONLY_LINKS = context.getString(R.string.pref_only_links);
		UNSHORTEN = context.getString(R.string.pref_unshorten);
		GET_TITLES = context.getString(R.string.pref_get_titles);
		DONT_ASK = context.getString(R.string.pref_dont_ask);
	}

	public String loadAccountName() {
		return prefs.getString(ACCOUNT_NAME, null);
	}

	public String loadListId() {
		return prefs.getString(LIST_ID, null);
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

	public boolean loadDontAsk() {
		return prefs.getBoolean(DONT_ASK, true);
	}

	private void saveString(String key, String value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(key, value);
		apply(editor);
	}

	public void saveAccountName(String accountName) {
		saveString(ACCOUNT_NAME, accountName);
	}

	public void saveListId(String listId) {
		saveString(LIST_ID, listId);
	}

	public void saveLastSentText(String text) {
		saveString(LAST_SENT_TEXT, text);
	}

	private void saveBoolean(String key, boolean value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(key, value);
		apply(editor);
	}

	public void saveOnlyLinks(boolean value) {
		saveBoolean(ONLY_LINKS, value);
	}

	public void saveUnshorten(boolean value) {
		saveBoolean(UNSHORTEN, value);
	}

	public void saveGetTitles(boolean value) {
		saveBoolean(GET_TITLES, value);
	}

	public void saveDontAsk(boolean value) {
		saveBoolean(DONT_ASK, value);
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
