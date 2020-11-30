package com.mysticwind.linenotificationsupport.model;

import android.app.Notification;
import android.graphics.Bitmap;

import androidx.core.app.Person;

import java.util.List;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true)
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
    // Actions ordered as defined during build time
    @Singular
    private final List<Notification.Action> actions;
    private final Bitmap icon;

}
