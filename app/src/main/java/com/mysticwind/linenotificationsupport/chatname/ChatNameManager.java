package com.mysticwind.linenotificationsupport.chatname;

import com.google.common.collect.Multimap;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.GroupChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.MultiPersonChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.model.Chat;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class ChatNameManager {

    private final GroupChatNameDataAccessor groupChatNameDataAccessor;
    private final MultiPersonChatNameDataAccessor multiPersonChatNameDataAccessor;

    @Inject
    public ChatNameManager(final GroupChatNameDataAccessor groupChatNameDataAccessor,
                           final MultiPersonChatNameDataAccessor multiPersonChatNameDataAccessor) {
        this.groupChatNameDataAccessor = Objects.requireNonNull(groupChatNameDataAccessor);
        this.multiPersonChatNameDataAccessor = Objects.requireNonNull(multiPersonChatNameDataAccessor);
    }

    public String getChatName(final String chatId) {
        return getChatName(chatId, null, null);
    }

    public String getChatName(final String chatId, final String sender) {
        return getChatName(chatId, sender, null);
    }

    public String getChatName(final String chatId, final String sender, final String highConfidenceChatGroupName) {
        final Optional<String> chatGroupName = groupChatNameDataAccessor.getChatGroupName(chatId);
        if (chatGroupName.isPresent()) {
            if (StringUtils.isNotBlank(highConfidenceChatGroupName) &&
                    !chatGroupName.get().equals(highConfidenceChatGroupName)) {
                groupChatNameDataAccessor.persistRelationship(chatId, highConfidenceChatGroupName);
                return highConfidenceChatGroupName;
            }
            if (StringUtils.isBlank(highConfidenceChatGroupName)) {
                Timber.w("Override with chat room name: " + chatGroupName.get());
            }
            return chatGroupName.get();
        }
        if (StringUtils.isNotBlank(highConfidenceChatGroupName)) {
            groupChatNameDataAccessor.persistRelationship(chatId, highConfidenceChatGroupName);
            return highConfidenceChatGroupName;
        }
        final String chatName = multiPersonChatNameDataAccessor.addRelationshipAndGetChatGroupName(chatId, sender);
        return chatName;
    }

    public Set<Chat> getAllChats() {
        final Map<String, String> chatGroups = groupChatNameDataAccessor.getAllChatGroups();
        final Multimap<String, String> chatIdToSenders = multiPersonChatNameDataAccessor.getAllChatIdToSenders();

        Set<Chat> chats = new HashSet<>();
        chatGroups.entrySet().stream()
                .forEach(entry ->
                        chats.add(new Chat(entry.getKey(), entry.getValue()))
                );

        chatIdToSenders.asMap().entrySet().stream()
                .forEach(entry -> {
                    final String chatId = entry.getKey();
                    final Collection<String> senders = entry.getValue();
                    if (chatGroups.keySet().contains(chatId)) {
                        // remove confirmed chat groups
                        return;
                    }
                    // hack to get the merged name
                    final String chatName = multiPersonChatNameDataAccessor.addRelationshipAndGetChatGroupName(chatId, null);
                    // chat name may be null. TODO fix API
                    if (StringUtils.isNotBlank(chatName)) {
                        chats.add(new Chat(chatId, chatName));
                    }
                });

        return chats;
    }

    public void deleteFriendNameCache() {
        multiPersonChatNameDataAccessor.deleteAllEntries();
    }

}
