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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Window;

public class PhoneToDesktopActivity extends SherlockFragmentActivity implements OnClickListener {
	
	private boolean updating = false;
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateMainLayout(intent);
		}
	};
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);
		
		Object last_config = getLastCustomNonConfigurationInstance();
		if (last_config == null) {
			updating = false;
		} else
			updating = ((Boolean) last_config).booleanValue();
		
		findViewById(R.id.btn_link_list).setOnClickListener(this);
		findViewById(R.id.btn_preferences).setOnClickListener(this);
		findViewById(R.id.btn_how_it_works).setOnClickListener(this);
		findViewById(R.id.btn_about).setOnClickListener(this);
	}

	@Override
	protected void onStart() {
		super.onStart();
        registerReceiver(receiver, new IntentFilter(GoogleTasksActivity.ACTION_AUTHENTICATE));
		updateMainLayout();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return updating;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()){
		case R.id.btn_how_it_works:
			startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
			break;
		case R.id.btn_about:
			startActivity(new Intent(getApplicationContext(), AboutActivity.class));
			break;
		case R.id.btn_link_list:
			startActivity(new Intent(getApplicationContext(), LinkListActivity.class));
			break;
		case R.id.btn_preferences:
			startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
			break;
		}
	}

	private void updateMainLayout() {
		getSherlock().setProgressBarIndeterminateVisibility(updating);
		//Button btn_authorize = (Button) findViewById(R.id.btn_authorize);
		TextView txt_authorize = (TextView) findViewById(R.id.txt_authorize);
		//btn_authorize.setEnabled(!updating);
		
		if (updating) {
			//btn_authorize.setText(R.string.btn_waiting_authorization);
			txt_authorize.setText(R.string.txt_authorize);
		} else {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			String account_name = prefs.getString(getResources().getString(R.string.pref_account_name), null);
			if (account_name != null) {
				txt_authorize.setText(
						getResources().getString(R.string.txt_authorized_to)+
						" "+account_name);
			} else {
				txt_authorize.setText(R.string.txt_authorize);
			}
		}
		
		if (GoogleTasksActivity.my_credentials.getClass().equals(GoogleTasksCredentialsDevelopment.class)) {
			getSherlock().getActionBar().setSubtitle("Development version");
		}
	}
	
	private void updateMainLayout(Intent intent) {
		updating = intent.getBooleanExtra("updating", false);
		updateMainLayout();
	}

}
