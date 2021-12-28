package com.mysticwind.linenotificationsupport.conversationstarter;

import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryChatKeywordDao implements ChatKeywordDao {

    private static final Map<String, String> KEYWORD_TO_CHAT_NAME_MAP = ImmutableMap.of(
            "寶貝", "愛的小窩"
    );

    private static final Map<String, String> CHAT_NAME_TO_CHAT_ID_MAP = ImmutableMap.of(
            "愛的小窩", "caf2eecbb7109578bf0472dfcba4eca9e"
    );

    @Override
    public Set<String> getKeywords() {
        return KEYWORD_TO_CHAT_NAME_MAP.keySet();
    }

    @Override
    public Optional<String> getChatId(String keyword) {
        String chatName = KEYWORD_TO_CHAT_NAME_MAP.get(keyword);
        if (StringUtils.isBlank(chatName)) {
            return Optional.empty();
        }
        return Optional.ofNullable(CHAT_NAME_TO_CHAT_ID_MAP.get(chatName));
    }

    @Override
    public Map<String, String> getKeywordToChatNameMap() {
        return KEYWORD_TO_CHAT_NAME_MAP;
    }


}
