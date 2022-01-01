package com.mysticwind.linenotificationsupport.model;

import static java.util.Collections.EMPTY_LIST;

import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;

import androidx.core.app.Person;

import java.util.List;
import java.util.Optional;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder(toBuilder = true, builderClassName = "LineNotificationDefaultValueBuilder")
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
    private final PendingIntent clickIntent;
    private final long timestamp;
    // Actions ordered as defined during build time
    @Singular
    private final List<Notification.Action> actions;
    private final Bitmap icon;
    private final List<NotificationHistoryEntry> history;
    private final boolean isSelfResponse;

    public static class LineNotificationDefaultValueBuilder {
        private boolean isSelfResponse = false;
    }

    public List<String> getMessages() {
        if (messages == null) {
            return EMPTY_LIST;
        }
        return messages;
    }

    public List<NotificationHistoryEntry> getHistory() {
        if (history == null) {
            return EMPTY_LIST;
        }
        return history;
    }

    public Optional<PendingIntent> getClickIntent() {
        return Optional.ofNullable(clickIntent);
    }

}
