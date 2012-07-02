package net.xisberto.phonetodesktop;

import net.xisberto.phonetodesktop.AccountListFragment.AccountSelector;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class PhoneToDesktopActivity extends SherlockFragmentActivity implements OnClickListener, AccountSelector {
	
	public static final int
		REQUEST_SELECT_ACCOUNT = 0;
	
	private DialogFragment dialog;
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
		updateMainLayout();
		
		findViewById(R.id.btn_authorize).setOnClickListener(this);
		
	}

	@Override
	protected void onStart() {
		super.onStart();
        registerReceiver(receiver, new IntentFilter(GoogleTasksActivity.ACTION_AUTHENTICATE));
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		return updating;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_authorize:
			//Let's choose an account to authorize the app
			//First, list the google.com accounts
			AccountManager accountManager = AccountManager.get(this);
			Account[] accounts = accountManager.getAccountsByType("com.google");
			dialog = AccountListFragment.newInstance(accounts);
			dialog.show(getSupportFragmentManager(), "dialog");
			break;
		default:
			break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_SELECT_ACCOUNT:
			if (resultCode == RESULT_OK) {
				//User selected an account let's authenticate
				Intent intent = new Intent(data);
				intent.setClass(getApplicationContext(), GoogleTasksActivity.class);
				intent.setAction(GoogleTasksActivity.ACTION_AUTHENTICATE);
				startActivity(intent);
			}
			break;

		default:
			break;
		}
	}
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = null;
		switch(item.getItemId()){
		case R.id.item_how_works:
			i = new Intent(getApplicationContext(), TutorialActivity.class);
			startActivity(i);
			break;
		case R.id.item_about:
			i = new Intent(getApplicationContext(), AboutActivity.class);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateMainLayout() {
		getSherlock().setProgressBarIndeterminateVisibility(updating);
		Button btn_authorize = (Button) findViewById(R.id.btn_authorize);
		TextView txt_authorize = (TextView) findViewById(R.id.txt_authorize);
		btn_authorize.setEnabled(!updating);
		
		if (updating) {
			btn_authorize.setText(R.string.btn_waiting_authorization);
			txt_authorize.setText(R.string.txt_authorize);
		} else {
			SharedPreferences prefs = getSharedPreferences(GoogleTasksActivity.class.getSimpleName(), MODE_PRIVATE);
			String account_name = prefs.getString(GoogleTasksActivity.PREF_ACCOUNT_NAME, null);
			if (account_name != null) {
				txt_authorize.setText(
						getResources().getString(R.string.txt_authorized_to)+
						" "+account_name);
				btn_authorize.setText(R.string.btn_authorize_again);
			} else {
				txt_authorize.setText(R.string.txt_authorize);
				btn_authorize.setText(R.string.btn_authorize);
			}
		}
	}
	
	private void updateMainLayout(Intent intent) {
		updating = intent.getBooleanExtra("updating", false);
		updateMainLayout();
	}

	@Override
	public void selectAccount(Account acc) {
		dialog.dismiss();
		Intent intent = new Intent(getApplicationContext(), GoogleTasksActivity.class);
		intent.setAction(GoogleTasksActivity.ACTION_AUTHENTICATE);
		intent.putExtra("account", acc);
		startActivity(intent);
	}

}