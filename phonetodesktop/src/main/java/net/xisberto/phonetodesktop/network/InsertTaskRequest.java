package net.xisberto.phonetodesktop.network;

import android.content.Context;

import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;

/**
 * Created by xisberto on 20/05/15.
 */
public class InsertTaskRequest extends GoogleHttpClientSpiceRequest<Task> {
    private Context context;
    private String title;

    public InsertTaskRequest(Class<Task> clazz, Context context, String title) {
        super(clazz);
        this.context = context;
        this.title = title;
    }

    @Override
    public Task loadDataFromNetwork() throws Exception {

        Preferences preferences = new Preferences(context);
        String list_id = preferences.loadListId();

        Tasks client = Utils.getGoogleTasksClient(context);

        return client.tasks()
                .insert(
                        list_id,
                        new Task().setTitle(title))
                .execute();
    }
}
