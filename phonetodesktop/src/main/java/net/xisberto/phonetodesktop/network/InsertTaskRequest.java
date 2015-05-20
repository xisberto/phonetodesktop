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

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, Utils.scopes);
        credential.setSelectedAccountName(preferences.loadAccountName());

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        GsonFactory gsonFactory = new GsonFactory();

        Tasks client = new Tasks.Builder(transport, gsonFactory, credential)
                .setApplicationName("PhoneToDesktop")
                .build();

        return client.tasks()
                .insert(
                        list_id,
                        new Task().setTitle(title))
                .execute();
    }
}
