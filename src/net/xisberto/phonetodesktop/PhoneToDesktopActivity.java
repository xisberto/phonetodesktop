/*******************************************************************************
 * Copyright (c) 2012 Humberto Fraga <xisberto@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Humberto Fraga <xisberto@gmail.com> - initial API and implementation
 ******************************************************************************/
package net.xisberto.phonetodesktop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

public class PhoneToDesktopActivity extends SherlockFragmentActivity implements
		OnClickListener {

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateMainLayout();
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		findViewById(R.id.btn_link_list).setOnClickListener(this);
		findViewById(R.id.btn_preferences).setOnClickListener(this);
		findViewById(R.id.btn_how_it_works).setOnClickListener(this);
		findViewById(R.id.btn_about).setOnClickListener(this);
		findViewById(R.id.button_authorize).setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver(receiver, new IntentFilter(
				GoogleTasksActivity.ACTION_AUTHENTICATE));
		updateMainLayout();
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_how_it_works:
			startActivity(new Intent(getApplicationContext(),
					TutorialActivity.class));
			break;
		case R.id.btn_about:
			startActivity(new Intent(getApplicationContext(),
					AboutActivity.class));
			break;
		case R.id.btn_link_list:
			startActivity(new Intent(getApplicationContext(),
					LinkListActivity.class));
			break;
		case R.id.btn_preferences:
			startActivity(new Intent(getApplicationContext(),
					SettingsActivity.class));
			break;
		case R.id.button_authorize:
			Preferences prefs = new Preferences(this);
			prefs.saveAccountName("");
			Intent auth_intent = new Intent(this,
					AuthorizationActivity.class);
			startActivity(auth_intent);
		}
	}

	private void updateMainLayout() {
		// Button btn_authorize = (Button) findViewById(R.id.btn_authorize);
		TextView txt_authorize = (TextView) findViewById(R.id.txt_authorize);
		// btn_authorize.setEnabled(!updating);

		Preferences prefs = new Preferences(this);
		String account_name = prefs.loadAccountName();
		if (account_name != null) {
			txt_authorize.setText(getResources().getString(
					R.string.txt_authorized_to)
					+ " " + account_name);
		} else {
			txt_authorize.setText(R.string.txt_authorize);
		}

		getSherlock().getActionBar().setSubtitle("List: " + prefs.loadListId());
	}

}
