package com.mysticwind.linenotificationsupport.module;

import android.content.res.Resources;

import com.mysticwind.linenotificationsupport.reply.MyPersonLabelProvider;
import com.mysticwind.linenotificationsupport.reply.impl.LocalizedMyPersonLabelProvider;
import com.mysticwind.linenotificationsupport.ui.LocaleDao;
import com.mysticwind.linenotificationsupport.ui.LocalizationDao;
import com.mysticwind.linenotificationsupport.ui.UserAlertDao;
import com.mysticwind.linenotificationsupport.ui.impl.AndroidLocaleDao;
import com.mysticwind.linenotificationsupport.ui.impl.AndroidLocalizationDao;
import com.mysticwind.linenotificationsupport.ui.impl.ToastUserAlertDao;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public abstract class UiModule {

    /* Related classes using @Inject
     */

    @Singleton
    @Binds
    public abstract MyPersonLabelProvider bindMyPersonLabelProvider(LocalizedMyPersonLabelProvider localizedMyPersonLabelProvider);

    @Singleton
    @Binds
    public abstract LocaleDao bindLocaleDao(AndroidLocaleDao androidLocaleDao);

    @Singleton
    @Provides
    public static Resources resources() {
        return Resources.getSystem();
    }

    @Singleton
    @Binds
    public abstract UserAlertDao bindUserAlertDao(ToastUserAlertDao toastUserAlertDao);

    @Singleton
    @Binds
    public abstract LocalizationDao bindLocalizationDao(AndroidLocalizationDao androidLocalizationDao);

}
