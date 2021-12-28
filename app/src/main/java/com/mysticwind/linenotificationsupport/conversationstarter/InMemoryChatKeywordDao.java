package com.mysticwind.linenotificationsupport.conversationstarter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class InMemoryChatKeywordDao implements ChatKeywordDao {

    private static final Map<String, String> KEYWORD_TO_CHAT_NAME_MAP = ImmutableMap.of(
            "寶貝", "愛的小窩"
    );

    @Override
    public Map<String, String> getKeywordToChatNameMap() {
        return KEYWORD_TO_CHAT_NAME_MAP;
    }


}
