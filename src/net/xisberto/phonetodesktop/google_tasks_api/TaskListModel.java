package net.xisberto.phonetodesktop.google_tasks_api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.services.tasks.model.TaskList;

public class TaskListModel {

	private final Map<String, TaskListInfo> lists = new HashMap<String, TaskListInfo>();
	
	public int size() {
		synchronized (lists) {
			return lists.size();
		}
	}
	
	public TaskListInfo get(String id) {
		synchronized (lists) {
			return lists.get(id);
		}
	}
	
	public void add(TaskList list) {
		synchronized (lists) {
			TaskListInfo found = get(list.getId());
			if (found == null) {
				lists.put(list.getId(), new TaskListInfo(list));
			} else {
				found.update(list);
			}
		}
	}
	
	public void remove(String id) {
		synchronized (lists) {
			lists.remove(id);
		}
	}
	
	public void reset(List<TaskList> listsToAdd) {
		synchronized (lists) {
			lists.clear();
			for (TaskList taskList : listsToAdd) {
				add(taskList);
			}
		}
	}
	
	public TaskListInfo[] toSortedArray() {
		synchronized (lists) {
			List<TaskListInfo> result = new ArrayList<TaskListInfo>();
			for (TaskListInfo taskListInfo : lists.values()) {
				result.add(taskListInfo);
			}
			Collections.sort(result);
			return result.toArray(new TaskListInfo[0]);
		}
	}
}
