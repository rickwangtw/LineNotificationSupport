package com.mysticwind.linenotificationsupport.model;

import androidx.core.app.Person;

import java.util.Optional;

import lombok.Value;

@Value
public class NotificationHistoryEntry {

    private final String lineMessageId;
    private final String message;
    private final Person sender;
    private final long timestamp;
    private final String lineStickerUrl;

    public Optional<String> getLineStickerUrl() {
        return Optional.ofNullable(lineStickerUrl);
    }

}
