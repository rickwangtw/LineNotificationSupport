package com.mysticwind.linenotificationsupport.notification;

public interface NotificationCounter extends SlotAvailabilityChecker {

    int notified(String group, int notificationId);
    int dismissed(String group, int notificationId);

}
