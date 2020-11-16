package com.mysticwind.linenotificationsupport.debug.history.manager;

import android.service.notification.StatusBarNotification;

public interface NotificationHistoryManager {

    void record(final StatusBarNotification statusBarNotification, final String lineVersion);

}
