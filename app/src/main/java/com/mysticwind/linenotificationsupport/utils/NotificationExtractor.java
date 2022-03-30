package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;

import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.model.NotificationExtraConstants;

import java.util.Optional;

public class NotificationExtractor {

    public static String getTitle(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_TITLE);
    }

    public static String getMessage(Notification notification) {
        return notification.extras.getString(Notification.EXTRA_TEXT);
    }

    public static String getSubText(Notification notification) {
        // around LINE version 12.2.2, the conversation title has been replaced by subtext
        return notification.extras.getString(Notification.EXTRA_SUB_TEXT);
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

    public static Optional<String> getLineNotificationSupportMessageId(Notification notification) {
        return Optional.ofNullable(notification.extras.getString(NotificationExtraConstants.MESSAGE_ID));
    }

    public static Optional<String> getLineNotificationSupportChatId(Notification notification) {
        return Optional.ofNullable(notification.extras.getString(NotificationExtraConstants.CHAT_ID));
    }

    public static Optional<String> getLineNotificationSupportStickerUrl(Notification notification) {
        return Optional.ofNullable(notification.extras.getString(NotificationExtraConstants.STICKER_URL));
    }

}
