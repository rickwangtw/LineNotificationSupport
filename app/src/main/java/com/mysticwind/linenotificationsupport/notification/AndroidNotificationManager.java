package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import java.util.List;

public interface AndroidNotificationManager {

    List<StatusBarNotification> getNotificationsOfPackage(String packageName);
    List<StatusBarNotification> getOrderedLineNotificationSupportNotificationsOfChatId(final String chatId, int filterStrategy);
    List<StatusBarNotification> getOrderedLineNotificationSupportNotifications(String group, int filterStrategy);
    void cancelNotificationById(int notificationId);
    void cancelNotification(String chatId);
    void cancelNotificationOfPackage(String key);
    void clearRemoteInputNotificationSpinner(String chatId);

}
