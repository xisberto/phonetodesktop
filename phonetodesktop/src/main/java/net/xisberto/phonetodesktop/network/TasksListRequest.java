package net.xisberto.phonetodesktop.network;

import android.content.Context;
import android.util.Log;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;
import com.octo.android.robospice.request.SpiceRequest;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;

import java.io.IOException;
import java.util.List;

/**
 * Created by xisberto on 29/10/15.
 */
public class TasksListRequest extends SpiceRequest<Void> {

    private Context mContext;
    private Preferences mPreferences;

    public TasksListRequest(Context context) {
        super(Void.class);
        mContext = context;
        mPreferences = Preferences.getInstance(context);
    }

    @Override
    public Void loadDataFromNetwork() throws Exception {
        Tasks client = Utils.getGoogleTasksClient(mContext);

        List<TaskList> taskLists = client.tasklists().list().execute().getItems();

        if (taskLists != null) {
            Log.d("TaskListRequest", String.format("server has %s lists", taskLists.size()));
            String localListId = mPreferences.loadListId();
            if (localListId == null) {
                // We don't have a list id saved. Search in the server
                // for a list with the title PhoneToDesktop
                Log.d("TaskListRequest", "No saved list");
                String serverListId = null;
                for (TaskList taskList : taskLists) {
                    if (taskList.getTitle().equals(Utils.LIST_TITLE)) {
                        serverListId = taskList.getId();
                        break;
                    }
                }
                if (serverListId == null) {
                    // The server doesn't have any list named PhoneToDesktop
                    // We create it and save its id
                    createNewList(client);
                } else {
                    // The server has a list named PhoneToDesktop
                    // We save its id
                    Log.d("TaskListRequest", String.format("server has list %s. Saving it.", serverListId));
                    mPreferences.saveListId(serverListId);
                }
            } else {
                // We have a saved id. Let's search this id in server
                Log.d("TaskListRequest", String.format("We have a local list: %s", localListId));
                boolean serverHasList = false;
                for (TaskList taskList : taskLists) {
                    if (taskList.getId().equals(localListId)) {
                        serverHasList = true;
                        break;
                    }
                }
                if (!serverHasList) {
                    // The server has no list with this id
                    // We create a new list and save its id
                    createNewList(client);
                }
                // else
                // We have the list id and found the same id in server
                // nothing to do here
            }
        }
        return null;
    }

    private void createNewList(Tasks client) throws IOException {
        Utils.log("Creating new list");
        TaskList newList = new TaskList();
        newList.setTitle(Utils.LIST_TITLE);
        TaskList taskList = client.tasklists().insert(newList).execute();
        Log.d("TaskListRequest", String.format("Saving new list with id: %s", taskList.getId()));
        mPreferences.saveListId(taskList.getId());
    }
}
