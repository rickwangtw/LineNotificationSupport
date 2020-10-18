package com.mysticwind.linenotificationsupport.utils;

import java.util.HashMap;
import java.util.Map;

public class GroupIdResolver {

    private static final int GROUP_ID_START = 0x4000;

    private final Map<String, Integer> chatIdToGroupIdMap = new HashMap<>();
    private int lastGroupId = GROUP_ID_START;

    public int resolveGroupId(final String chatId) {
        return chatIdToGroupIdMap.computeIfAbsent(chatId, chatIdWithoutGroupId ->
            getNextLastGroupId()
        );
    }

    private synchronized int getNextLastGroupId() {
        return lastGroupId++;
    }

}
