package net.xisberto.phonetodesktop;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AboutActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.layout_about);
		
		TextView text_name_version = (TextView)findViewById(R.id.text_name_version);
		String versionName = "";
		try {
			versionName = " "+getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			versionName= "";
		}
		text_name_version.setText(getResources().getString(R.string.app_name)+versionName);
		
		TextView text_about_message = (TextView)findViewById(R.id.text_about_message);
		text_about_message.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Intent i = new Intent(Intent.ACTION_SEND);
				i.setType("text/plain");
				i.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.link_about_message));
				startActivity(Intent.createChooser(i, getResources().getString(R.string.send_to)));
				return true;
			}
		});
		
	}

}
