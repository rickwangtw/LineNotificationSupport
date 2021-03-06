package com.mysticwind.linenotificationsupport.model;

import android.app.Notification;
import android.graphics.Bitmap;

import androidx.core.app.Person;

import java.util.Collections;
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
    // TODO migrate to use the list version of messages
    private final List<String> messages;
    private final String title;
    private final String lineMessageId;
    private final String lineStickerUrl;
    private final String chatId;
    private final CallState callState;
    private final long timestamp;
    // Actions ordered as defined during build time
    @Singular
    private final List<Notification.Action> actions;
    private final Bitmap icon;
    private final List<NotificationHistoryEntry> history;

    public List<String> getMessages() {
        if (messages == null) {
            return Collections.emptyList();
        }
        return messages;
    }

}
