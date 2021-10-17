package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mysticwind.linenotificationsupport.persistence.ChatGroupDatabase;
import com.mysticwind.linenotificationsupport.persistence.chatname.dto.ChatSenderEntry;

import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.util.Objects;

public class RoomMultiPersonChatNameDataAccessor implements MultiPersonChatNameDataAccessor {

    private final ChatGroupDatabase chatGroupDatabase;

    public RoomMultiPersonChatNameDataAccessor(final ChatGroupDatabase chatGroupDatabase) {
        this.chatGroupDatabase = Objects.requireNonNull(chatGroupDatabase);
    }

    @Override
    public String addRelationshipAndGetChatGroupName(final String chatId, final String sender) {
        Validate.notBlank(chatId);
        Validate.notBlank(sender);

        final ChatSenderEntry entry = new ChatSenderEntry();
        entry.setChatId(chatId);
        entry.setSender(sender);
        entry.setCreatedAtTimestamp(Instant.now().toEpochMilli());
        entry.setUpdatedAtTimestamp(Instant.now().toEpochMilli());
        chatGroupDatabase.chatSenderDao().insert(entry);

        return sender;
    }

    @Override
    public Multimap<String, String> getAllChatIdToSenders() {
        final Multimap<String, String> chatIdToSenderMultimap = HashMultimap.create();

        chatGroupDatabase.chatSenderDao().getAllEntries().forEach(
                entry ->
                        chatIdToSenderMultimap.put(entry.getChatId(), entry.getSender()));
        return chatIdToSenderMultimap;
    }

    @Override
    public void deleteAllEntries() {
        chatGroupDatabase.chatSenderDao().deleteAllEntries();
    }

}
