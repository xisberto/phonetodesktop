package net.xisberto.phonetodesktop;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class LinkListActivity extends SherlockActivity {
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateMainLayout(intent);
		}
	};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_list);
    }

	@Override
	protected void onStart() {
		super.onStart();
        registerReceiver(receiver, new IntentFilter(GoogleTasksActivity.ACTION_LIST_LINKS));
        Intent i = new Intent(getApplicationContext(), GoogleTasksActivity.class);
        i.setAction(GoogleTasksActivity.ACTION_LIST_LINKS);
        startActivity(i);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(receiver);
	}

    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_link_list, menu);
        return true;
    }

    
    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void updateMainLayout(Intent intent){
    	if (intent.getAction().equals(GoogleTasksActivity.ACTION_LIST_LINKS)) {
    		TextView text = (TextView) findViewById(R.id.textView_linkList);
    		ListView list_view = (ListView) findViewById(R.id.listView_linkList);
    		ArrayList<String>
    			ids = null, titles = null;
    		ids = intent.getStringArrayListExtra("ids");
    		titles = intent.getStringArrayListExtra("titles");
    		if (intent.getBooleanExtra("done", false)) {
    			findViewById(R.id.progressBar_linkList).setVisibility(View.GONE);
        		if (titles == null) {
        			text.setText("Empty list");
        			list_view.setVisibility(View.GONE);
        			text.setVisibility(View.VISIBLE);
        		} else {
        			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.task_list_item, titles);
        			list_view.setAdapter(adapter);
        			list_view.setVisibility(View.VISIBLE);
        			text.setVisibility(View.GONE);
        		}
    		} else {
    			findViewById(R.id.progressBar_linkList).setVisibility(View.VISIBLE);
    			list_view.setVisibility(View.GONE);
    			text.setVisibility(View.GONE);
    		}
    	}
    }

}
