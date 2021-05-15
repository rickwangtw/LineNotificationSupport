package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;

import com.mysticwind.linenotificationsupport.model.NotificationExtraConstants;

import java.util.Optional;

public class LineNotificationSupportMessageExtractor {

    public static Optional<String> getMessageId(final Notification.MessagingStyle.Message message) {
        return Optional.ofNullable(message.getExtras().getString(NotificationExtraConstants.MESSAGE_ID));
    }

    public static Optional<String> getChatId(final Notification.MessagingStyle.Message message) {
        return Optional.ofNullable(message.getExtras().getString(NotificationExtraConstants.CHAT_ID));
    }

    public static Optional<String> getStickerUrl(Notification.MessagingStyle.Message message) {
        return Optional.ofNullable(message.getExtras().getString(NotificationExtraConstants.STICKER_URL));
    }

}
