package com.mysticwind.linenotificationsupport.conversationstarter.persistence;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.mysticwind.linenotificationsupport.conversationstarter.persistence.dao.KeywordDao;
import com.mysticwind.linenotificationsupport.conversationstarter.persistence.dto.KeywordEntry;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {KeywordEntry.class}, version = 1, exportSchema = false)
public abstract class KeywordRoomDatabase extends RoomDatabase {

    public abstract KeywordDao keywordDao();

    private static volatile KeywordRoomDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static KeywordRoomDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (KeywordRoomDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(), KeywordRoomDatabase.class, "keyword_database")
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

}