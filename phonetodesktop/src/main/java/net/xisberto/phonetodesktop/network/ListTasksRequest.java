package net.xisberto.phonetodesktop.network;

import android.content.Context;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.octo.android.robospice.request.SpiceRequest;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.model.TaskList;

import java.util.List;

/**
 * Created by xisberto on 21/08/15.
 */
public class ListTasksRequest extends SpiceRequest<TaskList> {

    private Context mContext;

    public ListTasksRequest(Context context) {
        super(TaskList.class);
        mContext = context;
    }

    @Override
    public TaskList loadDataFromNetwork() throws Exception {
        String list_id = Preferences.getInstance(mContext).loadListId();

        if (list_id == null) {
            throw new NullPointerException("tasklist must not be null");
        }

        Tasks client = Utils.getGoogleTasksClient(mContext);

        List<Task> list = client.tasks().list(list_id).execute().getItems();
        TaskList result = new TaskList();

        if (list != null) {
            result.items.addAll(list);
        }

        return result;
    }

}
