package com.mysticwind.linenotificationsupport.conversationstarter;

import android.app.Notification;

import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

public class InMemoryLineReplyActionDao implements LineReplyActionDao {

    private final Map<String, Notification.Action> chatIdToNotificationActionMap = new HashMap<>();

    @Inject
    public InMemoryLineReplyActionDao() {
    }

    @Override
    public void saveLineReplyAction(final String chatId, final Notification.Action lineReplyAction) {
        Validate.notBlank(chatId);
        Objects.requireNonNull(lineReplyAction);

        chatIdToNotificationActionMap.put(chatId, lineReplyAction);
    }

    @Override
    public Optional<Notification.Action> getLineReplyAction(final String chatId) {
        return Optional.ofNullable(chatIdToNotificationActionMap.get(chatId));
    }

}
