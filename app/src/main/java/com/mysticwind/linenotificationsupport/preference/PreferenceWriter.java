package com.mysticwind.linenotificationsupport.preference;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class PreferenceWriter {

    private final SharedPreferences sharedPreferences;

    @Inject
    public PreferenceWriter(final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void setControlBluetoothDuringCalls(final boolean value) {
        Timber.i("Updating preference [%s] value [%s]", PreferenceProvider.BLUETOOTH_CONTROL_ONGOING_CALL_KEY, value);

        sharedPreferences.edit()
                .putBoolean(PreferenceProvider.BLUETOOTH_CONTROL_ONGOING_CALL_KEY, value)
                .commit();
    }

    public void disableShowConversationStarterNotification() {
        sharedPreferences.edit()
                .putBoolean(PreferenceProvider.CONVERSATION_STARTER_KEY, false)
                .commit();
    }

}
