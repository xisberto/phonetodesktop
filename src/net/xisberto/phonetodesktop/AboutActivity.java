package net.xisberto.phonetodesktop;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AboutActivity extends SherlockFragmentActivity implements OnClickListener {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.layout_about);
		
		String versionName = "";
		try {
			versionName = " "+getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			versionName= "";
		}
		((TextView)findViewById(R.id.text_name_version)).setText(getResources().getString(R.string.app_name)+versionName);
		
		findViewById(R.id.btn_share_about_message).setOnClickListener(this);
		findViewById(R.id.btn_share_chrome).setOnClickListener(this);
		findViewById(R.id.btn_share_google_java_api).setOnClickListener(this);
		findViewById(R.id.btn_share_actionbarsherlock).setOnClickListener(this);
		
	}

	@Override
	public void onClick(View v) {
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("text/plain");
		switch (v.getId()) {
		case R.id.btn_share_about_message:
			i.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.link_about_message));
			break;
		case R.id.btn_share_chrome:
			i.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.link_chrome_extension));
			break;
		case R.id.btn_share_google_java_api:
			i.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.link_google_java_api));
			break;
		case R.id.btn_share_actionbarsherlock:
			i.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.link_actionbarsherlock));
			break;
		default:
			return;
		}
		Toast.makeText(getApplicationContext(), i.getStringExtra(Intent.EXTRA_TEXT), Toast.LENGTH_SHORT).show();
		startActivity(Intent.createChooser(i, getResources().getString(R.string.send_to)));
	}

}
