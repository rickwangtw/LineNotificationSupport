package com.mysticwind.linenotificationsupport.module;

import android.content.Context;
import android.os.Build;
import android.os.Handler;

import com.mysticwind.linenotificationsupport.notification.SlotAvailabilityChecker;
import com.mysticwind.linenotificationsupport.notification.impl.DumbNotificationCounter;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class NotificationPublisherModule {

    /* Related classes using @Inject
      NotificationPublisherFactory
      PreferenceProvider
      NotificationIdGenerator
      DumbNotificationCounter
      GroupIdResolver
     */

    @Singleton
    @Provides
    public static Handler provideHandler() {
        return new android.os.Handler();
    }

    @Singleton
    @Binds
    public abstract SlotAvailabilityChecker bindSlotAvailabilityChecker(DumbNotificationCounter dumbNotificationCounter);

    @HiltQualifiers.MaxNotificationsPerApp
    @Singleton
    @Provides
    public static int getMaxNotificationsPerApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return 25;
        } else {
            return 50;
        }
    }

    @HiltQualifiers.PackageName
    @Singleton
    @Provides
    public static String providePackageName(@ApplicationContext final Context context) {
        return context.getPackageName();
    }

}
