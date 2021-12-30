package com.mysticwind.linenotificationsupport.conversationstarter.model;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Optional;

import lombok.Builder;
import lombok.Value;

@Value
public class KeywordEntry {

    private final String chatId;
    private final String chatName;
    private final Optional<String> keyword;
    private final boolean hasReplyAction;

    @Builder
    public KeywordEntry(String chatId, String chatName, String keyword, boolean hasReplyAction) {
        this.chatId = Validate.notBlank(chatId);
        this.chatName = Validate.notBlank(chatName);
        if (StringUtils.isBlank(keyword)) {
            this.keyword = Optional.empty();
        } else {
            this.keyword = Optional.of(keyword);
        }
        this.hasReplyAction = hasReplyAction;
    }

    public Optional<String> getKeyword() {
        return keyword;
    }

}
