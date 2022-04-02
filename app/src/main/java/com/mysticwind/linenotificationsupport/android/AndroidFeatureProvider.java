package com.mysticwind.linenotificationsupport.android;

import android.os.Build;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AndroidFeatureProvider {

    @Inject
    public AndroidFeatureProvider() {
    }

    public boolean hasNotificationChannelSupport() {
        // NotificationChannels are only supported API 26+
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

}
