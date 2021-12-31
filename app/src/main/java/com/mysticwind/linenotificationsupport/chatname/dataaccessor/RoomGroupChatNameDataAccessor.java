package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import com.mysticwind.linenotificationsupport.persistence.ChatGroupDatabase;
import com.mysticwind.linenotificationsupport.persistence.chatname.dto.GroupChatNameEntry;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class RoomGroupChatNameDataAccessor implements GroupChatNameDataAccessor {

    private final ChatGroupDatabase chatGroupDatabase;

    @Inject
    public RoomGroupChatNameDataAccessor(ChatGroupDatabase chatGroupDatabase) {
        this.chatGroupDatabase = Objects.requireNonNull(chatGroupDatabase);
    }

    @Override
    public void persistRelationship(final String chatId, final String chatGroupName) {
        try {
            GroupChatNameEntry entry = chatGroupDatabase.groupChatNameDao().getEntry(chatId);
            if (entry == null) {
                entry = new GroupChatNameEntry();
                entry.setChatId(chatId);
                entry.setChatGroupName(chatGroupName);
                entry.setCreatedAtTimestamp(Instant.now().toEpochMilli());
                entry.setUpdatedAtTimestamp(Instant.now().toEpochMilli());
            } else if (entry.getChatGroupName().equals(chatGroupName)) {
                return;
            } else {
                entry.setChatGroupName(chatGroupName);
                entry.setUpdatedAtTimestamp(Instant.now().toEpochMilli());
            }
            chatGroupDatabase.groupChatNameDao().insert(entry);
            Timber.i("Persisted entry with chat ID [%s] chat group name [%s] ",
                    chatId, chatGroupName);
        } catch (Exception e) {
            Timber.e(e, "Error recording entry chat ID [%s] chat group name [%s]: %s",
                    chatId, chatGroupName, e.getMessage());
        }
    }

    @Override
    public Optional<String> getChatGroupName(String chatId) {
        GroupChatNameEntry entry = chatGroupDatabase.groupChatNameDao().getEntry(chatId);
        if (entry == null) {
            return Optional.empty();
        } else {
            return Optional.of(entry.getChatGroupName());
        }
    }

    @Override
    public Map<String, String> getAllChatGroups() {
        return chatGroupDatabase.groupChatNameDao().getAllEntries().stream()
                .collect(Collectors.toMap(entry -> entry.getChatId(), entry -> entry.getChatGroupName()));
    }

}
