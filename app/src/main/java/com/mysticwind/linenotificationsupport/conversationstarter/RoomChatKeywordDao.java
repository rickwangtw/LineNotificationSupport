package com.mysticwind.linenotificationsupport.conversationstarter;

import android.content.Context;

import com.mysticwind.linenotificationsupport.conversationstarter.persistence.KeywordRoomDatabase;
import com.mysticwind.linenotificationsupport.conversationstarter.persistence.dao.KeywordDao;
import com.mysticwind.linenotificationsupport.conversationstarter.persistence.dto.KeywordEntry;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class RoomChatKeywordDao implements ChatKeywordDao {

    private final KeywordDao keywordDao;

    @Inject
    public RoomChatKeywordDao(@ApplicationContext final Context context) {
        final KeywordRoomDatabase database = KeywordRoomDatabase.getDatabase(context);
        keywordDao = database.keywordDao();
    }

    @Override
    public void createOrUpdateKeyword(String chatId, String keyword) {
        KeywordRoomDatabase.databaseWriteExecutor.execute(
                () -> keywordDao.insert(
                        KeywordEntry.builder()
                                .chatId(chatId)
                                .keyword(keyword)
                                .createdAtTimestamp(Instant.now().toEpochMilli())
                                // TODO fix the updated timestamp
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
    public Map<String, String> getAllKeywords() {
        return keywordDao.getAllEntries().stream()
                .collect(Collectors.toMap(entry -> entry.getChatId(), entry -> entry.getKeyword()));
    }

    @Override
    public Optional<String> getChatId(String keyword) {
        return keywordDao.getAllEntries().stream()
                .filter(entry -> entry.getKeyword().equals(keyword))
                .map(entry -> entry.getChatId())
                .findFirst();
    }

}
