package com.mysticwind.linenotificationsupport.debug.history.manager.impl;

import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.mysticwind.linenotificationsupport.debug.history.manager.NotificationHistoryManager;

public enum NullNotificationHistoryManager implements NotificationHistoryManager {
    INSTANCE;

    private static final String TAG = NullNotificationHistoryManager.class.getSimpleName();

    @Override
    public void record(StatusBarNotification statusBarNotification, String lineVersion) {
        Log.d(TAG, "Dummy Record: " + statusBarNotification);
    }

}
