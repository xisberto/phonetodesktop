package net.xisberto.phonetodesktop.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.exception.NoNetworkException;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import net.xisberto.phonetodesktop.Utils;
import net.xisberto.phonetodesktop.database.DatabaseHelper;
import net.xisberto.phonetodesktop.ui.SendingTaskNotification;

public class SendTasksService extends Service {

//    private static final String EXTRA_TASKS_IDS = "net.xisberto.phonetodesktop.network.extra.TASKS_IDS";

    private SpiceManager mSpiceManager = new SpiceManager(GoogleTasksSpiceService.class);

    public SendTasksService() {
//        super("SendTasksService");
    }

    public static void sendTasks(Context context) {
        Intent intent = new Intent(context, SendTasksService.class);
        context.startService(intent);
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
        onHandleIntent();
        return super.onStartCommand(intent, flags, startId);
    }

    protected void onHandleIntent() {
        handleSendTasks();
    }

    private void handleSendTasks() {
        //Sends all waiting tasks
        DatabaseHelper databaseHelper = DatabaseHelper
                .getInstance(this);
        Cursor wait_list = databaseHelper.listTaskQueueAsCursor();
        if (wait_list.getCount() == 0) {
            return;
        }
        long[] tasks_ids = new long[wait_list.getCount()];
        for (int i = 0; i < wait_list.getCount(); i++) {
            wait_list.moveToNext();
            tasks_ids[i] = wait_list.getLong(0);
        }
        InsertMultipleTasksRequest request = new InsertMultipleTasksRequest(this, tasks_ids);
        mSpiceManager.execute(request, new RequestListener<Void>() {
            @Override
            public void onRequestFailure(SpiceException spiceException) {
                if (!(spiceException instanceof NoNetworkException)) {
                    Utils.startAuthentication(getApplicationContext());
                }
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
