package com.mysticwind.linenotificationsupport.module;

import com.mysticwind.linenotificationsupport.conversationstarter.ChatKeywordDao;
import com.mysticwind.linenotificationsupport.conversationstarter.InMemoryLineReplyActionDao;
import com.mysticwind.linenotificationsupport.conversationstarter.LineReplyActionDao;
import com.mysticwind.linenotificationsupport.conversationstarter.RoomChatKeywordDao;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
// TODO how to make this depend on ChatNameModule?
public abstract class KeywordModule {

    /* Related classes using @Inject
      ConversationStarterNotificationManager
      ChatKeywordManager
      KeywordSettingActivityLauncher
      StartConversationActionBuilder
     */

    @Singleton
    @Binds
    public abstract ChatKeywordDao bindChatKeywordDao(RoomChatKeywordDao roomChatKeywordDao);

    @Singleton
    @Binds
    public abstract LineReplyActionDao bindLineReplyActionDao(InMemoryLineReplyActionDao inMemoryLineReplyActionDao);

}
