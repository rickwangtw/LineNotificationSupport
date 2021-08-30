package com.mysticwind.linenotificationsupport.reply;

import android.app.Notification;

import org.apache.commons.lang3.Validate;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatReplyActionManager {

    final Map<String, Notification.Action> chatIdToReplyActionMap = new ConcurrentHashMap<>();

    public void persistReplyAction(final String chatId, final Notification.Action replyAction) {
        Validate.notBlank(chatId);
        Objects.requireNonNull(replyAction);

        chatIdToReplyActionMap.put(chatId, replyAction);
    }

    public Set<String> getReplyableChatIds() {
        return chatIdToReplyActionMap.keySet();
    }

}
