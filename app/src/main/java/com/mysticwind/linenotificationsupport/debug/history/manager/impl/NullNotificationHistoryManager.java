package com.mysticwind.linenotificationsupport.debug.history.manager.impl;

import android.service.notification.StatusBarNotification;

import androidx.lifecycle.LiveData;

import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;
import com.mysticwind.linenotificationsupport.debug.history.manager.NotificationHistoryManager;

import java.util.List;

import timber.log.Timber;

public enum NullNotificationHistoryManager implements NotificationHistoryManager {
    INSTANCE;

    @Override
    public void record(StatusBarNotification statusBarNotification, String lineVersion) {
        Timber.d("Dummy Record: " + statusBarNotification);
    }

    @Override
    public LiveData<List<NotificationHistoryEntry>> getHistory() {
        // TODO return null object
        return null;
    }

}
