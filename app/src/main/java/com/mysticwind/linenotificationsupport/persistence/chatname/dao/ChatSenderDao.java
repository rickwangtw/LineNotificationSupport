package com.mysticwind.linenotificationsupport.persistence.chatname.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mysticwind.linenotificationsupport.persistence.chatname.dto.ChatSenderEntry;

import java.util.List;

@Dao
public interface ChatSenderDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(ChatSenderEntry entry);

    @Query("SELECT * FROM chat_senders")
    List<ChatSenderEntry> getAllEntries();

    @Query("DELETE FROM chat_senders")
    void deleteAllEntries();

}
