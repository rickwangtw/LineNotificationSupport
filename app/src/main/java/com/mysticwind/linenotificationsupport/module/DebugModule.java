package com.mysticwind.linenotificationsupport.module;

import android.content.Context;

import androidx.room.Room;

import com.mysticwind.linenotificationsupport.debug.DebugModeProvider;
import com.mysticwind.linenotificationsupport.debug.history.manager.NotificationHistoryManager;
import com.mysticwind.linenotificationsupport.debug.history.manager.impl.NullNotificationHistoryManager;
import com.mysticwind.linenotificationsupport.debug.history.manager.impl.RoomNotificationHistoryManager;
import com.mysticwind.linenotificationsupport.persistence.AppDatabase;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DebugModule {

    /* Related classes using @Inject
      DebugModeProvider
     */

    @Singleton
    @Provides
    public static NotificationHistoryManager provideNotificationHistoryManager(DebugModeProvider debugModeProvider,
                                                                               @ApplicationContext Context context,
                                                                               StatusBarNotificationPrinter statusBarNotificationPrinter) {
        if (debugModeProvider.isDebugMode()) {
            AppDatabase appDatabase = Room.databaseBuilder(context, AppDatabase.class, "database").build();

            return new RoomNotificationHistoryManager(appDatabase, statusBarNotificationPrinter);
        } else {
            return NullNotificationHistoryManager.INSTANCE;
        }
    }

}
