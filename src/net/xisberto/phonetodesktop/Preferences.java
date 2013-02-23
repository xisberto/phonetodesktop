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
			WHAT_TO_SEND = "whatToSend";

	public static final String[] WHAT_TO_SEND_VALUES = { "entire_text",
			"only_links", "always_ask" };

	private SharedPreferences prefs;

	public Preferences(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
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

	public void saveAccountName(String accountName) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(ACCOUNT_NAME, accountName);
		apply(editor);
	}

	public void saveAuthToken(String authToken) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(AUTH_TOKEN, authToken);
		apply(editor);
	}

	public void saveListId(String listId) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(LIST_ID, listId);
		apply(editor);
	}

	public void saveWhatToSend(int value) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(WHAT_TO_SEND, WHAT_TO_SEND_VALUES[value]);
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
