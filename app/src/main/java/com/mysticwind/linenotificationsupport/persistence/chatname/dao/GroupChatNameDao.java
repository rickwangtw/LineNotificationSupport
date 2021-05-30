package com.mysticwind.linenotificationsupport.persistence.chatname.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mysticwind.linenotificationsupport.persistence.chatname.dto.GroupChatNameEntry;

import java.util.List;

@Dao
public interface GroupChatNameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(GroupChatNameEntry entry);

    @Query("SELECT * FROM group_chat_names WHERE chat_id = :chatId")
    GroupChatNameEntry getEntry(String chatId);

    @Query("SELECT * FROM group_chat_names")
    List<GroupChatNameEntry> getAllEntries();

}
