package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;

public class NotificationExtractor {

    public static String getTitle(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_TITLE);
    }

    public static String getMessage(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_TEXT);
    }

}
