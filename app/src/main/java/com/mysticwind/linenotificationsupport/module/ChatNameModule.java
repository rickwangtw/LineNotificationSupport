package com.mysticwind.linenotificationsupport.module;

import android.content.Context;

import androidx.room.Room;

import com.mysticwind.linenotificationsupport.chatname.dataaccessor.CachingGroupChatNameDataAccessorDecorator;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.CachingMultiPersonChatNameDataAccessorDecorator;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.GroupChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.MultiPersonChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.RoomGroupChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.RoomMultiPersonChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.persistence.ChatGroupDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class ChatNameModule {

    /* Related classes using @Inject
      ChatNameManager
      RoomGroupChatNameDataAccessor
      RoomMultiPersonChatNameDataAccessor
     */

    @Singleton
    @Provides
    public static GroupChatNameDataAccessor bindGroupChatNameDataAccessor(RoomGroupChatNameDataAccessor roomGroupChatNameDataAccessor) {
        return new CachingGroupChatNameDataAccessorDecorator(roomGroupChatNameDataAccessor);
    }

    @Singleton
    @Provides
    public static ChatGroupDatabase bindChatGroupDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context,
                ChatGroupDatabase.class, "chat_group_database.db")
                .allowMainThreadQueries()
                .build();
    }

    @Singleton
    @Provides
    public static MultiPersonChatNameDataAccessor bindMultiPersonChatNameDataAccessor(RoomMultiPersonChatNameDataAccessor roomMultiPersonChatNameDataAccessor) {
        return new CachingMultiPersonChatNameDataAccessorDecorator(roomMultiPersonChatNameDataAccessor);
    }

}
