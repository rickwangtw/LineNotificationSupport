package com.mysticwind.linenotificationsupport.model;

import android.app.Notification;
import android.graphics.Bitmap;

import androidx.core.app.Person;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineNotification {
    private final Person sender;
    private final String message;
    private final String title;
    private final String lineStickerUrl;
    private final String chatId;
    private final long timestamp;
    private final Notification.Action replyAction;
    private final Bitmap icon;

}
