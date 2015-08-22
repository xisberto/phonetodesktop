package net.xisberto.phonetodesktop.network;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.tasks.Tasks;
import com.google.api.services.tasks.model.Task;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;

import net.xisberto.phonetodesktop.Preferences;
import net.xisberto.phonetodesktop.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xisberto on 21/08/15.
 */
public class ListTasksRequest extends GoogleHttpClientSpiceRequest<ListTasksRequest.TaskList> {
    public class TaskList extends ArrayList<Task>{}

    private Context mContext;

    public ListTasksRequest(Context context) {
        super(TaskList.class);
        mContext = context;
    }

    @Override
    public TaskList loadDataFromNetwork() throws Exception {
        Preferences preferences = new Preferences(mContext);
        String list_id = preferences.loadListId();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(mContext, Utils.scopes);
        credential.setSelectedAccountName(preferences.loadAccountName());

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        GsonFactory gsonFactory = new GsonFactory();

        Tasks client = new Tasks.Builder(transport, gsonFactory, credential)
                .setApplicationName("PhoneToDesktop")
                .build();

        List<Task> list = client.tasks().list(list_id).execute().getItems();
        TaskList result = new TaskList();

        result.addAll(list);

        return result;
    }

}
