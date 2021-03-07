package com.mysticwind.linenotificationsupport;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

public class DismissNotificationBroadcastReceiver extends BroadcastReceiver {

    public static final String NOTIFICATION_ID = "notificationId";

    @Override
    public void onReceive(Context context, Intent intent) {
        final int notificationId = intent.getIntExtra(NOTIFICATION_ID, 0);

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);

        Timber.d("Cancelling notification ID: %d", notificationId);
    }

}