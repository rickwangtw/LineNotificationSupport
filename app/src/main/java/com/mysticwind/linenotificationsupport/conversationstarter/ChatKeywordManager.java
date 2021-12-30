package com.mysticwind.linenotificationsupport.conversationstarter;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;
import com.mysticwind.linenotificationsupport.chatname.model.Chat;
import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

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
                keywordEntryList.add(
                        KeywordEntry.builder()
                                .chatId(chatId.get())
                                .chatName(chatName)
                                .keyword(keyword)
                                .build()
                );
            }
        }

        sortKeywordEntryList(keywordEntryList);

        return ImmutableList.copyOf(keywordEntryList);
    }

    private void sortKeywordEntryList(List<KeywordEntry> keywordEntryList) {
        Collections.sort(keywordEntryList, Comparator.comparing(entry -> entry.getChatName()));
    }

    public List<KeywordEntry> getAllChatsWithConfiguredKeywords() {
        final Set<Chat> chats = chatNameManager.getAllChats();
        final Map<String, String> chatIdToKeywordMap = chatKeywordDao.getAllKeywords();

        List<KeywordEntry> keywordEntries = new ArrayList<>();
        for (final Chat chat : chats) {
            final String keyword = chatIdToKeywordMap.get(chat.getId());
            keywordEntries.add(
                    KeywordEntry.builder()
                            .chatId(chat.getId())
                            .chatName(chat.getName())
                            .keyword(keyword)
                            .build()
            );
        }
        sortKeywordEntryList(keywordEntries);
        return keywordEntries;
    }

}
