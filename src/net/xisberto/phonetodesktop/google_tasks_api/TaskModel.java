package net.xisberto.phonetodesktop.google_tasks_api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.services.tasks.model.Task;

public class TaskModel {

	private final Map<String, TaskInfo> tasks = new HashMap<String, TaskInfo>();
	
	public int size() {
		synchronized (tasks) {
			return tasks.size();
		}
	}
	
	public TaskInfo get(String id) {
		synchronized (tasks) {
			return tasks.get(id);
		}
	}
	
	public void add(Task task) {
		synchronized (tasks) {
			TaskInfo found = get(task.getId());
			if (found == null) {
				tasks.put(task.getId(), new TaskInfo(task));
			} else {
				found.update(task);
			}
		}
	}
	
	public void remove(String id) {
		synchronized (tasks) {
			tasks.remove(id);
		}
	}
	
	public void reset(List<Task> tasksToAdd) {
		synchronized (tasks) {
			tasks.clear();
			for (Task task : tasksToAdd) {
				add(task);
			}
		}
	}
	
	public TaskInfo[] toSortedArray() {
		synchronized (tasks) {
			List<TaskInfo> result = new ArrayList<TaskInfo>();
			for (TaskInfo taskInfo : tasks.values()) {
				result.add(taskInfo);
			}
			Collections.sort(result);
			return result.toArray(new TaskInfo[0]);
		}
	}
}
