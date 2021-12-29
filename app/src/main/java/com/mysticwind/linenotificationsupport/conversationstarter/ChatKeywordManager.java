package com.mysticwind.linenotificationsupport.conversationstarter;

import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public class ChatKeywordManager {

    private final ChatKeywordDao chatKeywordDao;
    private final ChatNameManager chatNameManager;
    private final LineReplyActionDao lineReplyActionDao;

    public ChatKeywordManager(final ChatKeywordDao chatKeywordDao,
                              final ChatNameManager chatNameManager,
                              final LineReplyActionDao lineReplyActionDao) {
        this.chatKeywordDao = Objects.requireNonNull(chatKeywordDao);
        this.chatNameManager = Objects.requireNonNull(chatNameManager);
        this.lineReplyActionDao = Objects.requireNonNull(lineReplyActionDao);
    }

    public Map<String, String> getAvailableKeywordToChatNameMap() {
        Map<String, String> keywordToChatName = new TreeMap();
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
                keywordToChatName.put(keyword, chatName);
            }
        }

        return keywordToChatName;
    }

}
