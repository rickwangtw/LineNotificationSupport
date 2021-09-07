package com.mysticwind.linenotificationsupport.preference;

import android.content.SharedPreferences;

import timber.log.Timber;

public class PreferenceWriter {

    private final SharedPreferences sharedPreferences;

    public PreferenceWriter(final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void setControlBluetoothDuringCalls(final boolean value) {
        Timber.i("Updating preference [%s] value [%s]", PreferenceProvider.BLUETOOTH_CONTROL_ONGOING_CALL_KEY, value);

        sharedPreferences.edit()
                .putBoolean(PreferenceProvider.BLUETOOTH_CONTROL_ONGOING_CALL_KEY, value)
                .commit();
    }

}
