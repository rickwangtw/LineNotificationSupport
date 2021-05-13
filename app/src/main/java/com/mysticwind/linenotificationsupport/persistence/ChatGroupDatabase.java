package com.mysticwind.linenotificationsupport.persistence;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.mysticwind.linenotificationsupport.persistence.chatname.dao.ChatSenderDao;
import com.mysticwind.linenotificationsupport.persistence.chatname.dao.GroupChatNameDao;
import com.mysticwind.linenotificationsupport.persistence.chatname.dto.ChatSenderEntry;
import com.mysticwind.linenotificationsupport.persistence.chatname.dto.GroupChatNameEntry;

@Database(entities = {GroupChatNameEntry.class, ChatSenderEntry.class}, version = 1)
public abstract class ChatGroupDatabase extends RoomDatabase {

    public abstract GroupChatNameDao groupChatNameDao();
    public abstract ChatSenderDao chatSenderDao();

}
