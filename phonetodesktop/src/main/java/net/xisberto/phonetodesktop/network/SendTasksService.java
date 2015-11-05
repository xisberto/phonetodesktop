package net.xisberto.phonetodesktop.network;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.ui.SendingTaskNotification;

public class SendTasksService extends Service {

    private static final String EXTRA_TASKS_IDS = "net.xisberto.phonetodesktop.network.extra.TASKS_IDS";

    private SpiceManager mSpiceManager = new SpiceManager(GoogleTasksSpiceService.class);

    public static void sendTasks(Context context, long... tasks_ids) {
        Intent intent = new Intent(context, SendTasksService.class);
        intent.putExtra(EXTRA_TASKS_IDS, tasks_ids);
        context.startService(intent);
    }

    public SendTasksService() {
//        super("SendTasksService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSpiceManager.start(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSpiceManager.isStarted()) {
            mSpiceManager.shouldStop();
        }
        Utils.log("stopping service");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onHandleIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    protected void onHandleIntent(Intent intent) {
        if (intent != null && intent.hasExtra(EXTRA_TASKS_IDS)) {
            final long[] tasks_ids = intent.getLongArrayExtra(EXTRA_TASKS_IDS);
            handleSendTasks(tasks_ids);
        }
    }

    private void handleSendTasks(long[] tasks_ids) {
        InsertMultipleTasksRequest request = new InsertMultipleTasksRequest(this, tasks_ids);
        mSpiceManager.execute(request, new RequestListener<Void>() {
            @Override
            public void onRequestFailure(SpiceException spiceException) {
                Utils.startAuthentication(getApplicationContext());
                SendingTaskNotification.cancel(SendTasksService.this);
                stopSelf();
            }

            @Override
            public void onRequestSuccess(Void aVoid) {
                SendingTaskNotification.cancel(SendTasksService.this);
                stopSelf();
            }
        });

    }

}
