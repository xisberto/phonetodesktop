package net.xisberto.phonetodesktop;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.TextView;

public class LocalTaskAdapter extends ResourceCursorAdapter {

	public LocalTaskAdapter(Context context, Cursor c) {
		super(context, R.layout.listitem_localtask, c, 0);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView task_title = (TextView) view.findViewById(R.id.task_title);
		TextView task_status = (TextView) view.findViewById(R.id.task_status);
		
		task_title.setText(cursor.getString(2));
		String status = "Id: "+cursor.getString(0)
				+ " Status: "+cursor.getString(4);
		task_status.setText(status);
	}

}
