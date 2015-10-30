package net.xisberto.phonetodesktop.network;

import android.content.Context;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.octo.android.robospice.request.SpiceRequest;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.model.TaskList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xisberto on 21/08/15.
 */
public class DeleteTasksRequest extends SpiceRequest<TaskList> {

    private Context mContext;
    private ArrayList<String> tasks_ids;

    public DeleteTasksRequest(Context mContext, ArrayList<String> tasks_ids) {
        super(TaskList.class);
        this.mContext = mContext;
        this.tasks_ids = tasks_ids;
    }

    @Override
    public TaskList loadDataFromNetwork() throws Exception {
        String list_id = Preferences.getInstance(mContext).loadListId();

        Tasks client = Utils.getGoogleTasksClient(mContext);
        for (String task_id : tasks_ids) {
            client.tasks().delete(list_id, task_id).execute();
        }

        List<Task> list = client.tasks().list(list_id).execute().getItems();
        TaskList result = new TaskList();

        if (list != null) {
            result.items.addAll(list);
        }
        return result;
    }
}
