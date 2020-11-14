package com.mysticwind.linenotificationsupport.android;

import android.os.Build;

public class AndroidFeatureProvider {

    public boolean hasNotificationChannelSupport() {
        // NotificationChannels are only supported API 26+
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

}
