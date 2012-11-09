package net.xisberto.phonetodesktop;

import android.accounts.Account;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.util.Log;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;

public class SettingsActivity extends SherlockPreferenceActivity implements
		OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Prepare the account list to show
		GoogleAccountManager accountManager = new GoogleAccountManager(
				getApplicationContext());
		Account[] accounts = accountManager.getAccounts();
		String[] accounts_names = new String[accounts.length];
		for (int i = 0; i < accounts.length; i++) {
			accounts_names[i] = accounts[i].name;
		}
		ListPreference pref_account_name = (ListPreference) findPreference(getResources()
				.getString(R.string.pref_account_name));
		pref_account_name.setEntries(accounts_names);
		pref_account_name.setEntryValues(accounts_names);

		// Register a PreferencesChangeListener
		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);

	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.i(getPackageName(), "Preference changed: "+key);
		if (key.equals(getResources().getString(R.string.pref_account_name))) {
			Account account = new GoogleAccountManager(getApplicationContext())
					.getAccountByName(sharedPreferences.getString(key, ""));
			Log.i(getPackageName(), "Account obtained: "+account);
			if (account != null) {
				Intent intent = new Intent(getApplicationContext(), GoogleTasksActivity.class);
				intent.setAction(GoogleTasksActivity.ACTION_AUTHENTICATE);
				intent.putExtra("account", account);
				startActivity(intent);
			}
		}
	}

}
