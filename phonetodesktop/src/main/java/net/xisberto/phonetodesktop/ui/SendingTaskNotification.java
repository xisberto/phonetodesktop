package net.xisberto.phonetodesktop.ui;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import net.xisberto.phonetodesktop.R;
import net.xisberto.phonetodesktop.Utils;

public class SendingTaskNotification {
    /**
     * The unique identifier for this type of notification.
     */
    private static final String NOTIFICATION_TAG = "SendingTask";

    /**
     * Shows the notification, or updates a previously shown notification of
     * this type, with the given parameters.
     *
     * @see #cancel(Context)
     */
    public static void notify(final Context context, int current, final int total) {
        final Resources res = context.getResources();


        String title;
        if (total == 1) {
            title = res.getString(R.string.txt_sending);
        } else {
            title = res.getString(R.string.txt_sending_multiple, current + 1, total);
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)

                // Set required fields, including the small icon, the
                // notification title, and text.
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentTitle(title)
                .setContentText(title)

                        // All fields below this line are optional.

                        // Use a default priority (recognized on devices running Android
                        // 4.1 or later)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

                        // Set ticker text (preview) information for this notification.
                .setTicker(title)

                        // Show a number. This is useful when stacking notifications of
                        // a single type.
                .setNumber(total - current)

                        // Set the pending intent to be initiated when the user touches
                        // the notification.
                .setContentIntent(
                        PendingIntent.getActivity(
                                context,
                                0,
                                new Intent(context, WaitListActivity.class),
                                PendingIntent.FLAG_UPDATE_CURRENT))

                        // Automatically dismiss the notification when it is touched.
                .setAutoCancel(false);

        Utils.log("Notification " + current);

        notify(context, builder.build());
    }

    private static void notify(final Context context, final Notification notification) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_TAG, 42, notification);
    }

    /**
     * Cancels any notifications of this type previously shown using
     * {@link #notify(Context, int, int)}.
     */
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public static void cancel(final Context context) {
        NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_TAG, 42);
    }
}
