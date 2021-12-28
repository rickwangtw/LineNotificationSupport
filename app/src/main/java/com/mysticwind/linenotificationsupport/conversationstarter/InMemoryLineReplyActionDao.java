package com.mysticwind.linenotificationsupport.conversationstarter;

import android.app.Notification;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryLineReplyActionDao implements LineReplyActionDao {

    private final Map<String, Notification.Action> chatIdToNotificationActionMap = new HashMap<>();

    @Override
    public void saveLineReplyAction(final String chatId, final Notification.Action lineReplyAction) {
        chatIdToNotificationActionMap.put(chatId, lineReplyAction);
    }

    @Override
    public Optional<Notification.Action> getLineReplyAction(final String chatId) {
        return Optional.ofNullable(chatIdToNotificationActionMap.get(chatId));
    }

}
