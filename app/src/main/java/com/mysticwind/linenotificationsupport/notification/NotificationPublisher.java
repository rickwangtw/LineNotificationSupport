package com.mysticwind.linenotificationsupport.notification;

import com.mysticwind.linenotificationsupport.model.LineNotification;

public interface NotificationPublisher {

    void publishNotification(LineNotification lineNotification, int notificationId);

    void updateNotificationDismissed();

}
