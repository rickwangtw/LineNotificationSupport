package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;

import com.mysticwind.linenotificationsupport.line.Constants;

public class NotificationExtractor {

    public static String getTitle(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_TITLE);
    }

    public static String getMessage(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_TEXT);
    }

    public static String getConversationTitle(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_CONVERSATION_TITLE);
    }

    public static String getLineMessageId(Notification notification) {
        return notification.extras.getString(Constants.LINE_MESSAGE_ID_EXTRA_KEY);
    }

    public static String getLineChatId(Notification notification) {
        return notification.extras.getString(Constants.LINE_CHAT_ID_EXTRA_KEY);
    }

}
