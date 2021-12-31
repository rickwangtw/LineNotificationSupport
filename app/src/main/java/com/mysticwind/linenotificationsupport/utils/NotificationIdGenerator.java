package com.mysticwind.linenotificationsupport.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NotificationIdGenerator {

    private static final int MESSAGE_ID_START = 0x1000;

    private static int lastMessageId = MESSAGE_ID_START;

    @Inject
    public NotificationIdGenerator() {
    }

    public synchronized int getNextNotificationId() {
        return ++lastMessageId;
    }

}
