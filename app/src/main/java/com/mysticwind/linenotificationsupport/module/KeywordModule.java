package com.mysticwind.linenotificationsupport.module;

import com.mysticwind.linenotificationsupport.conversationstarter.InMemoryLineReplyActionDao;
import com.mysticwind.linenotificationsupport.conversationstarter.LineReplyActionDao;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class KeywordModule {

    @Singleton
    @Binds
    public abstract LineReplyActionDao bindLineReplyActionDao(InMemoryLineReplyActionDao inMemoryLineReplyActionDao);

}
