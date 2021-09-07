package com.mysticwind.linenotificationsupport;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import timber.log.Timber;

public class SettingsUpdateRequestBroadcastReceiver extends BroadcastReceiver {

    private static final String SETTING_KEY_KEY = "setting-key";
    private static final String SETTING_VALUE_KEY = "setting-value";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Timber.i("Received request to update settings intent action [%s] key [%s] value [%s]",
                intent.getAction(), intent.getStringExtra(SETTING_KEY_KEY), intent.getStringExtra(SETTING_VALUE_KEY));
    }

}