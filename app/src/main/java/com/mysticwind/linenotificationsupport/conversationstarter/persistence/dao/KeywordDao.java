package com.mysticwind.linenotificationsupport.conversationstarter.persistence.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.mysticwind.linenotificationsupport.conversationstarter.persistence.dto.KeywordEntry;

import java.util.List;

@Dao
public interface KeywordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(KeywordEntry entry);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void update(KeywordEntry entry);

    @Query("SELECT * FROM chat_id_keywords")
    List<KeywordEntry> getAllEntries();

}
