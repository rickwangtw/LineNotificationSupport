package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.model.LineNotification;

public interface NotificationPublisher {

    void publishNotification(LineNotification lineNotification, int notificationId);

    void updateNotificationDismissed(StatusBarNotification statusBarNotification);

}
