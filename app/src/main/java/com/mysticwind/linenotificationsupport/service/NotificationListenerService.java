package com.mysticwind.linenotificationsupport.service;

import android.content.Intent;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import static android.app.Notification.EXTRA_TITLE;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private static final String TAG = "LINE_NOTIFICATION_SUPPORT";

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        // ignore messages from ourselves
        if (statusBarNotification.getPackageName().startsWith(getPackageName())) {
            return;
        }

        final String packageName = statusBarNotification.getPackageName();

        // let's just focus on Line notifications for now
        if (!packageName.equals("jp.naver.line.android")) {
            return;
        }

        Log.i(TAG, "Line notification: " + statusBarNotification.getNotification().extras.getString(EXTRA_TITLE));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
