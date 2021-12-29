package com.mysticwind.linenotificationsupport.conversationstarter;

import android.content.Context;

import com.mysticwind.linenotificationsupport.conversationstarter.persistence.KeywordRoomDatabase;
import com.mysticwind.linenotificationsupport.conversationstarter.persistence.dao.KeywordDao;
import com.mysticwind.linenotificationsupport.conversationstarter.persistence.dto.KeywordEntry;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RoomChatKeywordDao implements ChatKeywordDao {

    private final KeywordDao keywordDao;

    public RoomChatKeywordDao(final Context context) {
        final KeywordRoomDatabase database = KeywordRoomDatabase.getDatabase(context);
        keywordDao = database.keywordDao();
        KeywordRoomDatabase.databaseWriteExecutor.execute(
                () -> keywordDao.insert(
                        KeywordEntry.builder()
                                .chatId("caf2eecbb7109578bf0472dfcba4eca9e")
                                .keyword("寶貝")
                                .createdAtTimestamp(Instant.now().toEpochMilli())
                                .updatedAtTimestamp(Instant.now().toEpochMilli())
                                .build()));
    }

    @Override
    public Set<String> getKeywords() {
        return keywordDao.getAllEntries().stream()
                .map(entry -> entry.getKeyword())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<String> getChatId(String keyword) {
        return keywordDao.getAllEntries().stream()
                .filter(entry -> entry.getKeyword().equals(keyword))
                .map(entry -> entry.getChatId())
                .findFirst();
    }

}
