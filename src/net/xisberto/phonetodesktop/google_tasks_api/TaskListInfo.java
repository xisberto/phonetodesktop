package net.xisberto.phonetodesktop.google_tasks_api;

import com.google.api.services.tasks.model.TaskList;

public class TaskListInfo implements Comparable<TaskListInfo>, Cloneable {

	static final String FIELDS = "id,title";
	static final String FEED_FIELDS = "items(" + FIELDS + ")";

	public String id;
	public String title;

	public TaskListInfo(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public TaskListInfo(TaskList list) {
		update(list);
	}

	@Override
	public int compareTo(TaskListInfo other) {
		return title.compareTo(other.title);
	}

	void update(TaskList list) {
		id = list.getId();
		title = list.getTitle();
	}
	
}
