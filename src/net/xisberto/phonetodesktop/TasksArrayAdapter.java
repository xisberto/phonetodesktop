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

import java.util.HashMap;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TasksArrayAdapter extends BaseAdapter {
	private HashMap<String, String> items;
	private String[] keys;
	Activity activity;
	private LayoutInflater inflater;
	
	public TasksArrayAdapter(Activity activity, HashMap<String, String> items) {
		super();
		this.items = items;
		this.activity = activity;
		
		keys = items.keySet().toArray(new String[items.size()]);
		inflater = LayoutInflater.from(activity);
		
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(keys[position]);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = inflater.inflate(R.layout.task_list_item, null);
		String key = keys[position];
		String title = getItem(position).toString();
		
		((TextView) convertView.findViewById(R.id.text_task_title)).setText(title);
		convertView.findViewById(R.id.btn_remove_task).setTag(key);
		if (activity instanceof OnClickListener) {
			convertView.findViewById(R.id.btn_remove_task).setOnClickListener((OnClickListener) activity);
		}
		
		return convertView;
	}

	
}
