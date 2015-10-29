package net.xisberto.phonetodesktop.network;

import android.content.Context;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.TaskList;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;

import java.io.IOException;
import java.util.List;

/**
 * Created by xisberto on 29/10/15.
 */
public class TasksListRequest extends GoogleHttpClientSpiceRequest<Void> {

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
            if (mPreferences.loadListId() == null) {
                // We don't have a list id saved. Search in the server
                // for a list with the title PhoneToDesktop
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
                    // returning at this point to keep the progress
                    return null;
                } else {
                    // The server has a list named PhoneToDesktop
                    // We save its id
                    mPreferences.saveListId(serverListId);
                }
            } else {
                // We have a saved id. Let's search this id in server
                boolean serverHasList = false;
                for (TaskList taskList : taskLists) {
                    if (taskList.getId().equals(mPreferences.loadListId())) {
                        serverHasList = true;
                        break;
                    }
                }
                if (!serverHasList) {
                    // The server has no list with this id
                    // We create a new list and save its id
                    createNewList(client);
                    // returning at this point to keep the progress
                    return null;
                }
                // else
                // We have the list id and found the same id in server
                // nothing to do here
            }
        }
        return null;
    }

    private void createNewList(Tasks client) {
        Utils.log("Creating new list");
        TaskList newList = new TaskList();
        newList.setTitle(Utils.LIST_TITLE);
        try {
            TaskList createdList = client.tasklists().insert(newList)
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
