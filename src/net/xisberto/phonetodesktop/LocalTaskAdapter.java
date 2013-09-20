package net.xisberto.phonetodesktop;

import net.xisberto.phonetodesktop.model.LocalTask.Status;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class LocalTaskAdapter extends ResourceCursorAdapter {

	public LocalTaskAdapter(Context context, Cursor c) {
		super(context, R.layout.listitem_localtask, c, 0);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		TextView task_title = (TextView) view.findViewById(R.id.task_title);
		ProgressBar progress = (ProgressBar) view.findViewById(R.id.progress);

		String title = cursor.getString(2) + cursor.getString(3);
		task_title.setText(title);
		
		Status status = Status.valueOf(cursor.getString(5));
		if (status == Status.SENDING) {
			progress.setVisibility(View.VISIBLE);
		}
	}

}
