package com.mysticwind.linenotificationsupport.utils;

public class NotificationIdGenerator {

    private static final int MESSAGE_ID_START = 0x1000;

    private static int lastMessageId = MESSAGE_ID_START;

    public synchronized int getNextNotificationId() {
        return ++lastMessageId;
    }

}
