package com.mysticwind.linenotificationsupport;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.StringUtils;

public class SettingsActivity extends AppCompatActivity {

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private static final String MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY = "merge_message_notification_channels";

        private final SharedPreferences.OnSharedPreferenceChangeListener onPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
                if (!StringUtils.equals(MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY, preferenceKey)) {
                    return;
                }

                final NotificationGroupCreator notificationGroupCreator = new NotificationGroupCreator(
                        (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE),
                        new AndroidFeatureProvider(), getPreferenceProvider());

                boolean shouldMergeNotification = sharedPreferences.getBoolean(preferenceKey, false);
                if (shouldMergeNotification) {
                    notificationGroupCreator.migrateToSingleNotificationChannelForMessages();
                } else {
                    notificationGroupCreator.migrateToMultipleNotificationChannelsForMessages();
                }
            }

            private PreferenceProvider getPreferenceProvider() {
                return new PreferenceProvider(PreferenceManager.getDefaultSharedPreferences(getContext()));
            }

        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(onPreferenceChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onPreferenceChangeListener);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // handles the back button on the action bar
        if (id == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

}