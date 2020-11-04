package com.mysticwind.linenotificationsupport.model;

import android.app.Notification;
import android.graphics.Bitmap;

import androidx.core.app.Person;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LineNotification {

    public enum CallState {
        INCOMING,
        IN_A_CALL,
        MISSED_CALL,
    }

    private final Person sender;
    private final String message;
    private final String title;
    private final String lineStickerUrl;
    private final String chatId;
    private final CallState callState;
    private final long timestamp;
    // Actions ordered the same as the original LINE notifications
    private final List<Notification.Action> actions;
    private final Bitmap icon;

}
