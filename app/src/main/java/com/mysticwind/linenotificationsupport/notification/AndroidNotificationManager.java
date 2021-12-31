package com.mysticwind.linenotificationsupport.notification;

public interface AndroidNotificationManager {

    void cancelNotification(String chatId);
    void clearRemoteInputNotificationSpinner(String chatId);

}
