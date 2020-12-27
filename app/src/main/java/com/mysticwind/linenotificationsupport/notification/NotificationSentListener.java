package com.mysticwind.linenotificationsupport.notification;

import com.mysticwind.linenotificationsupport.model.LineNotification;

public interface NotificationSentListener {

    void notificationSent(LineNotification lineNotification, int notificationId);

}
