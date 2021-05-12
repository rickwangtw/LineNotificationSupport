package com.mysticwind.linenotificationsupport.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

public class GroupIdResolver {

    private static final int GROUP_ID_START = 0x4000;

    private final Map<Integer, String> groupIdToChatIdMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> fallbackChatIdToGroupIdMap = new ConcurrentHashMap<>();
    private int lastGroupId = GROUP_ID_START;

    public GroupIdResolver() {
    }

    public GroupIdResolver(final int lastGroupId) {
        this.lastGroupId = lastGroupId;
    }

    public int resolveGroupId(final String chatId) {
        final int calculatedGroupId = chatId.hashCode();
        final String storedChatId = groupIdToChatIdMap.get(calculatedGroupId);
        if (storedChatId == null) {
            groupIdToChatIdMap.put(calculatedGroupId, chatId);
            return calculatedGroupId;
        } else if (storedChatId.equals(chatId)) {
            return calculatedGroupId;
        } else {
            // fallback that should almost never happen
            final int selectedGroupId = fallbackChatIdToGroupIdMap.computeIfAbsent(chatId, id -> getNextLastGroupId());
            // TODO what if there is a clash with the group IDs and the hash codes?
            Timber.w("Chat ID [%s] hash [%d] has been used, using group ID [%d] instead",
                    chatId, calculatedGroupId, selectedGroupId);
            return selectedGroupId;
        }
    }

    private synchronized int getNextLastGroupId() {
        return lastGroupId++;
    }

}
