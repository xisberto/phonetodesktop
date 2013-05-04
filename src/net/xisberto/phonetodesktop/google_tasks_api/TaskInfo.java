package net.xisberto.phonetodesktop.google_tasks_api;

import com.google.api.services.tasks.model.Task;

public class TaskInfo implements Comparable<TaskInfo>, Cloneable {

	static final String FIELDS = "id,title";
	static final String FEED_FIELDS = "items(" + FIELDS + ")";

	public String id;
	public String title;

	public TaskInfo(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public TaskInfo(Task task) {
		update(task);
	}

	@Override
	public int compareTo(TaskInfo other) {
		return title.compareTo(other.title);
	}

	void update(Task task) {
		id = task.getId();
		title = task.getTitle();
	}
	
}
