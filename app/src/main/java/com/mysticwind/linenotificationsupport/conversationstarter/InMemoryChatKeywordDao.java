package com.mysticwind.linenotificationsupport.conversationstarter;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryChatKeywordDao implements ChatKeywordDao {

    private static final Map<String, String> KEYWORD_TO_CHAT_ID_MAP = ImmutableMap.of(
            "寶貝", "caf2eecbb7109578bf0472dfcba4eca9e"
    );

    @Override
    public Set<String> getKeywords() {
        return KEYWORD_TO_CHAT_ID_MAP.keySet();
    }

    @Override
    public Optional<String> getChatId(String keyword) {
        return Optional.ofNullable(KEYWORD_TO_CHAT_ID_MAP.get(keyword));
    }

}
