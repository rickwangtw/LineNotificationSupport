package com.mysticwind.linenotificationsupport.module;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.NotificationManager;
import android.content.Context;

import com.mysticwind.linenotificationsupport.notification.AndroidNotificationManager;
import com.mysticwind.linenotificationsupport.notification.impl.DefaultAndroidNotificationManager;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class NotificationManagementModule {

    /* Related classes using @Inject
        NotificationGroupCreator
        AndroidFeatureProvider
     */

    @Singleton
    @Binds
    public abstract AndroidNotificationManager bindAndroidNotificationManager(DefaultAndroidNotificationManager defaultAndroidNotificationManager);

    @Singleton
    @Provides
    public static NotificationManager provideNotificationManager(@ApplicationContext Context context) {
        return (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    }

}
