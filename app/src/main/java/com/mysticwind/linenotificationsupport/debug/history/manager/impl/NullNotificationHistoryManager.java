package com.mysticwind.linenotificationsupport.debug.history.manager.impl;

import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;
import com.mysticwind.linenotificationsupport.debug.history.manager.NotificationHistoryManager;

import java.util.List;

public enum NullNotificationHistoryManager implements NotificationHistoryManager {
    INSTANCE;

    private static final String TAG = NullNotificationHistoryManager.class.getSimpleName();

    @Override
    public void record(StatusBarNotification statusBarNotification, String lineVersion) {
        Log.d(TAG, "Dummy Record: " + statusBarNotification);
    }

    @Override
    public LiveData<List<NotificationHistoryEntry>> getHistory() {
        // TODO return null object
        return null;
    }

}
