package com.mysticwind.linenotificationsupport.conversationstarter.activity;

import com.google.common.base.Converter;
import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;

public class KeywordEntryConverter extends Converter<KeywordEntry, MutableKeywordEntry> {

    @Override
    protected MutableKeywordEntry doForward(KeywordEntry keywordEntry) {
        return MutableKeywordEntry.builder()
                .chatId(keywordEntry.getChatId())
                .chatName(keywordEntry.getChatName())
                .keyword(keywordEntry.getKeyword().orElse(null))
                .hasReplyAction(keywordEntry.isHasReplyAction())
                .build();
    }

    @Override
    protected KeywordEntry doBackward(MutableKeywordEntry mutableKeywordEntry) {
        return KeywordEntry.builder()
                .chatId(mutableKeywordEntry.getChatId())
                .chatName(mutableKeywordEntry.getChatName())
                .keyword(mutableKeywordEntry.getKeyword())
                .hasReplyAction(mutableKeywordEntry.isHasReplyAction())
                .build();
    }
}
