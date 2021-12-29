package com.mysticwind.linenotificationsupport.conversationstarter;

import java.util.Optional;
import java.util.Set;

public interface ChatKeywordDao {

    Set<String> getKeywords();
    Optional<String> getChatId(String keyword);

}
