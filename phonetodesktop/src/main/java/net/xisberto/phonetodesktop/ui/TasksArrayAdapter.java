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
package net.xisberto.phonetodesktop.ui;

import android.content.Context;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.api.services.tasks.model.Task;

import net.xisberto.phonetodesktop.R;

import java.util.ArrayList;

public class TasksArrayAdapter extends BaseAdapter {
    private Context context;
    private TaskArraySelectionListener listener;
    private ArrayList<Task> items;
    private ArrayList<Boolean> checked;

	public TasksArrayAdapter(Context context, ArrayList<Task> items, TaskArraySelectionListener listener) {
		super();
        this.context = context;
        this.listener = listener;
        this.items = items;
        if (items != null) {
            this.checked = new ArrayList<>(items.size());
            for (int i = 0; i < items.size(); i++) {
                checked.add(false);
            }
        } else {
            this.checked = new ArrayList<>();
        }
    }

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Task getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

        TaskViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.task_list_item, parent, false);

            holder = new TaskViewHolder();
            convertView.setTag(holder);

            holder.textView = (TextView) convertView.findViewById(R.id.text_task);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.check_task);

        } else {
            holder = (TaskViewHolder) convertView.getTag();
        }

        holder.checkBox.setChecked(isChecked(position));
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox cb = (CheckBox) v;
                checked.set(position, cb.isChecked());
                listener.onItemChecked(position, cb.isChecked());
            }
        });
        holder.textView.setText(items.get(position).getTitle());
        Linkify.addLinks(holder.textView, Linkify.WEB_URLS);

		return convertView;
	}

    public boolean isChecked(int position) {
        try {
            return checked.get(position);
        } catch (IndexOutOfBoundsException oobE) {
            setChecked(position, false);
            return checked.get(position);
        }
    }

    public void setChecked(int position, boolean checked) {
        this.checked.set(position, checked);
    }

    public void clearSelections() {
        for (int i = 0; i < checked.size(); i++) {
            checked.set(i, false);
        }
    }

    public void updateLists(ArrayList<Task> items) {
        this.items = items;
        this.checked = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            checked.add(false);
        }
        notifyDataSetChanged();
    }

    public interface TaskArraySelectionListener {
        void onItemChecked(int position, boolean checked);
    }

    public static class TaskViewHolder {
        private CheckBox checkBox;
        private TextView textView;

        public CheckBox getCheckBox() {
            return checkBox;
        }
    }
	
}
