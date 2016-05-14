package net.xisberto.phonetodesktop.network;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.octo.android.robospice.request.SpiceRequest;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.model.LocalTask;
import net.xisberto.phonetodesktop.ui.SendingTaskNotification;

public class InsertMultipleTasksRequest extends SpiceRequest<Void> {
    private Context context;
    private long[] tasks_ids;

    public InsertMultipleTasksRequest(Context context, long... tasks_ids) {
        super(Void.class);
        this.context = context;
        this.tasks_ids = tasks_ids;
    }

    @Override
    public Void loadDataFromNetwork() throws Exception {

        String list_id = Preferences.getInstance(context).loadListId();

        if (list_id == null) {
            throw new NullPointerException("tasklist must not be null");
        }

        DatabaseHelper databaseHelper = DatabaseHelper.getInstance(context);
        Tasks client = Utils.getGoogleTasksClient(context);

        LocalTask.PersistCallback callback = new LocalTask.PersistCallback() {
            @Override
            public void run() {
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(
                                new Intent(Utils.ACTION_LIST_LOCAL_TASKS));
            }
        };

        int i = 0;
        while (i < tasks_ids.length) {
            long id = tasks_ids[i];
            SendingTaskNotification.notify(context, i, tasks_ids.length);
            LocalTask task = databaseHelper.getTask(id);
            task.setStatus(LocalTask.Status.SENDING).persist();
            client.tasks()
                    .insert(list_id, new Task().setTitle(task.getTitle()))
                    .execute();
            task.setStatus(LocalTask.Status.SENT).persist(callback);
            i++;
        }

        SendingTaskNotification.cancel(context);

        return null;
    }
}
