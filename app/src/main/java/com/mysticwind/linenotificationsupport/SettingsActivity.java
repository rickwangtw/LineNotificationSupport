package com.mysticwind.linenotificationsupport;

import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.StringUtils;

public class SettingsActivity extends AppCompatActivity {

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private static final String MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY = "merge_message_notification_channels";

        private Dialog silentLineMessageNotificationSettingsDialog;
        private Dialog alertLineMessageNotificationSettingsDialog;

        private final SharedPreferences.OnSharedPreferenceChangeListener onPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
                if (StringUtils.equals(MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY, preferenceKey)) {
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
                if (PreferenceProvider.MANAGE_LINE_MESSAGE_NOTIFICATIONS_PREFERENCE_KEY.equals(preferenceKey)) {
                    if (getPreferenceProvider().shouldManageLineMessageNotifications()) {
                        silentLineMessageNotificationSettingsDialog.show();
                    } else {
                        alertLineMessageNotificationSettingsDialog.show();
                    }
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

            if (silentLineMessageNotificationSettingsDialog == null) {
                silentLineMessageNotificationSettingsDialog = createSilentLineMessageNotificationSettingsDialog();
            }
            if (alertLineMessageNotificationSettingsDialog == null) {
                alertLineMessageNotificationSettingsDialog = createAlertLineMessageNotificationSettingsDialog();
            }

            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(onPreferenceChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();

            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(onPreferenceChangeListener);
        }

        private Dialog createSilentLineMessageNotificationSettingsDialog() {
            return new AlertDialog.Builder(getContext())
                    .setMessage(R.string.make_line_message_notification_silent)
                    .setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, Constants.LINE_PACKAGE_NAME)
                                    .putExtra(Settings.EXTRA_CHANNEL_ID, Constants.NEW_MESSAGE_NOTIFICATION_CHANNEL_NAME);
                            startActivity(intent);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(true)
                    .create();
        }

        private Dialog createAlertLineMessageNotificationSettingsDialog() {
            return new AlertDialog.Builder(getContext())
                    .setMessage(R.string.reminder_for_enabling_line_message_notification)
                    .setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, Constants.LINE_PACKAGE_NAME)
                                    .putExtra(Settings.EXTRA_CHANNEL_ID, Constants.NEW_MESSAGE_NOTIFICATION_CHANNEL_NAME);
                            startActivity(intent);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(true)
                    .create();
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