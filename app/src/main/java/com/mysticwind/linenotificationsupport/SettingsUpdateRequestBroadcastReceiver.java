package com.mysticwind.linenotificationsupport;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.preference.PreferenceManager;

import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.preference.PreferenceWriter;

import java.util.Map;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class SettingsUpdateRequestBroadcastReceiver extends BroadcastReceiver {

    private static final Map<String, BiConsumer<PreferenceWriter, Boolean>> BOOLEAN_SETTING_TO_WRITER_FUNCTION_MAP = ImmutableMap.of(
            PreferenceProvider.BLUETOOTH_CONTROL_ONGOING_CALL_KEY, (preferenceWriter, value) -> preferenceWriter.setControlBluetoothDuringCalls(value)
    );

    private static final String SETTING_KEY_KEY = "setting-key";
    private static final String SETTING_VALUE_KEY = "setting-value";

    @Inject
    PreferenceWriter preferenceWriter;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Timber.i("Received request to update settings intent action [%s] key [%s] value [%s]",
                intent.getAction(), intent.getStringExtra(SETTING_KEY_KEY), intent.getStringExtra(SETTING_VALUE_KEY));

        final String settingKey = intent.getStringExtra(SETTING_KEY_KEY);
        final String settingValue = intent.getStringExtra(SETTING_VALUE_KEY);

        if (BOOLEAN_SETTING_TO_WRITER_FUNCTION_MAP.containsKey(settingKey)) {
            final boolean value = Boolean.parseBoolean(settingValue);

            final BiConsumer<PreferenceWriter, Boolean> writerFunction = BOOLEAN_SETTING_TO_WRITER_FUNCTION_MAP.get(settingKey);
            writerFunction.accept(preferenceWriter, value);

            Timber.i("Successfully updated preference setting key [%s] value [%s]", settingKey, settingValue);
            setResultCode(RESULT_OK);
            return;
        }

        Timber.e("Unsupported intent action [%s] key [%s] value [%s]",
                intent.getAction(), settingKey, settingValue);
        setResultCode(RESULT_CANCELED);
    }

}