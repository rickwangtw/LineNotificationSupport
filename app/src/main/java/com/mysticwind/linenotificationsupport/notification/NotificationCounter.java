package com.mysticwind.linenotificationsupport.notification;

public interface NotificationCounter {

    int notified(String group, int notificationId);
    int dismissed(String group, int notificationId);
    boolean hasSlot(String group);

}
