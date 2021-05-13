package com.mysticwind.linenotificationsupport.chatname.dataaccessor;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CachingGroupChatNameDataAccessorDecorator implements GroupChatNameDataAccessor {

    private final GroupChatNameDataAccessor groupChatNameDataAccessor;
    private final Map<String, String> chatIdToGroupChatNameMap;

    public CachingGroupChatNameDataAccessorDecorator(final GroupChatNameDataAccessor groupChatNameDataAccessor) {
        this.groupChatNameDataAccessor = Objects.requireNonNull(groupChatNameDataAccessor);

        this.chatIdToGroupChatNameMap = new HashMap<>(groupChatNameDataAccessor.getAllChatGroups());
    }

    @Override
    public void persistRelationship(String chatId, String chatGroupName) {
        Validate.notBlank(chatId);
        Validate.notBlank(chatGroupName);

        chatIdToGroupChatNameMap.put(chatId, chatGroupName);
        // TODO optimization to make this async
        groupChatNameDataAccessor.persistRelationship(chatId, chatGroupName);
    }

    @Override
    public Optional<String> getChatGroupName(String chatId) {
        Validate.notBlank(chatId);

        final String groupChatName = chatIdToGroupChatNameMap.get(chatId);
        return Optional.ofNullable(groupChatName);
    }

    @Override
    public Map<String, String> getAllChatGroups() {
        return ImmutableMap.copyOf(chatIdToGroupChatNameMap);
    }

}
