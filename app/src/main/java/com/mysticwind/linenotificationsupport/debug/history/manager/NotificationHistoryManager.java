package com.mysticwind.linenotificationsupport.debug.history.manager;

import android.service.notification.StatusBarNotification;

import androidx.lifecycle.LiveData;

import com.mysticwind.linenotificationsupport.debug.history.dto.NotificationHistoryEntry;

import java.util.List;

public interface NotificationHistoryManager {

    void record(final StatusBarNotification statusBarNotification, final String lineVersion);

    LiveData<List<NotificationHistoryEntry>> getHistory();

}
