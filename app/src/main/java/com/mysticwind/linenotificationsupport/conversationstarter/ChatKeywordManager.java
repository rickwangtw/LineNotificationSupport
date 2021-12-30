package com.mysticwind.linenotificationsupport.conversationstarter;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import lombok.Value;

@Singleton
public class ChatKeywordManager {

    private final ChatKeywordDao chatKeywordDao;
    private final ChatNameManager chatNameManager;
    private final LineReplyActionDao lineReplyActionDao;

    @Inject
    public ChatKeywordManager(final ChatKeywordDao chatKeywordDao,
                              final ChatNameManager chatNameManager,
                              final LineReplyActionDao lineReplyActionDao) {
        this.chatKeywordDao = Objects.requireNonNull(chatKeywordDao);
        this.chatNameManager = Objects.requireNonNull(chatNameManager);
        this.lineReplyActionDao = Objects.requireNonNull(lineReplyActionDao);
    }

    @Value
    public static class KeywordEntry {
        String keyword;
        String chatId;
        String chatName;
    }

    public List<KeywordEntry> getAvailableKeywordToChatNameMap() {
        final List<KeywordEntry> keywordEntryList = new ArrayList<>();
        Set<String> keywords = chatKeywordDao.getKeywords();
        for (final String keyword : keywords) {
            final Optional<String> chatId = chatKeywordDao.getChatId(keyword);
            if (!chatId.isPresent()) {
                continue;
            }
            if (!lineReplyActionDao.getLineReplyAction(chatId.get()).isPresent()) {
                continue;
            }
            final String chatName = chatNameManager.getChatName(chatId.get());
            if (StringUtils.isNotBlank(chatName)) {
                keywordEntryList.add(new KeywordEntry(keyword, chatId.get(), chatName));
            }
        }

        Collections.sort(keywordEntryList, Comparator.comparing(entry -> entry.chatName));

        return ImmutableList.copyOf(keywordEntryList);
    }

}
