package com.mysticwind.linenotificationsupport.conversationstarter;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ChatKeywordDao {

    void createOrUpdateKeyword(String chatId, String keyword);

    Set<String> getKeywords();

    /**
     * @return chatId to keyword map
     */
    Map<String, String> getAllKeywords();

    Optional<String> getChatId(String keyword);

}
