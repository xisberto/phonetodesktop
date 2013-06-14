package net.xisberto.phonetodesktop;

import java.util.Arrays;
import java.util.Collection;

import android.util.Log;

import com.google.api.services.tasks.TasksScopes;

public class Utils {
	public static Collection<String> scopes = Arrays.asList(TasksScopes.TASKS);
	public static final String
			ACTION_AUTHENTICATE = "net.xisberto.phonetodesktop.action.AUTHENTICATE",
			ACTION_LOAD_LISTS = "net.xisberto.phonetodesktop.action.LOAD_LISTS",
			ACTION_SAVE_LIST = "net.xisberto.phonetodesktop.action.SAVE_LISTS",
			ACTION_SEND_TASK = "net.xisberto.phonetodesktop.action.SEND_TASK",
			ACTION_LIST_TASKS = "net.xisberto.phonetodesktop.action.LIST_TASKS",
			ACTION_REMOVE_TASK = "net.xisberto.phonetodesktop.action.REMOVE_TASK",
			ACTION_SHOW_AVAILABILITY_ERROR = "net.xisberto.phonetodesktop.action.SHOW_AVAILABILITY_ERROR",
			ACTION_SHOW_USER_RECOVERABLE = "net.xisberto.phonetodesktop.action.SHOW_USER_RECOVERABLE",
			EXTRA_LISTS = "net.xisberto.phonetodesktop.extra.LISTS",
			EXTRA_LISTNAME = "net.xisberto.phonetodesktop.extra.LISTNAME",
			EXTRA_LISTID = "net.xisberto.phonetodesktop.extra.LISTID",
			EXTRA_CONNECTION_STATUS_CODE = "net.xisberto.phonetodesktop.extra.CONNECTION_SATUS_CODE",
			EXTRA_UPDATING = "net.xisberto.phonetodesktop.extra.UPDATING",
			EXTRA_TITLES = "net.xisberto.phonetodesktop.extra.TITLES",
			EXTRA_IDS = "net.xisberto.phonetodesktop.extra.IDS",
			EXTRA_TASK_ID = "net.xisberto.phonetodesktop.extra.TASK_ID",
			LIST_TITLE = "PhoneToDesktop";
	
	public static void log(String message) {
		if (BuildConfig.DEBUG){
			Log.d(LIST_TITLE, message);
		}
	}
}
