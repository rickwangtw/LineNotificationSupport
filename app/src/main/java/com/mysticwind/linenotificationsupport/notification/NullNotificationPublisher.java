package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.model.LineNotification;

public enum NullNotificationPublisher implements NotificationPublisher {

    INSTANCE;

    @Override
    public void publishNotification(LineNotification lineNotification, int notificationId) {
        // do nothing
    }

    @Override
    public void republishNotification(LineNotification lineNotification, int notificationId) {
        // do nothing
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
    }

}
