package com.mysticwind.linenotificationsupport;

import static com.mysticwind.linenotificationsupport.preference.PreferenceProvider.MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY;

import android.app.Dialog;
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

import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.permission.AndroidPermissionRequester;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

    @Inject
    NotificationGroupCreator notificationGroupCreator;

    @Inject
    SharedPreferences sharedPreferences;

    @Inject
    PreferenceProvider preferenceProvider;

    @Inject
    AndroidPermissionRequester androidPermissionRequester;

    private Dialog silentLineMessageNotificationSettingsDialog;
    private Dialog alertLineMessageNotificationSettingsDialog;
    private SharedPreferences.OnSharedPreferenceChangeListener onPreferenceChangeListener;

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }

        @Override
        public void onResume() {
            super.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();
        }
    }

    private void setupOnPreferenceChangeListener() {
        onPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String preferenceKey) {
                if (StringUtils.equals(MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY, preferenceKey)) {
                    boolean shouldMergeNotification = sharedPreferences.getBoolean(preferenceKey, false);
                    if (shouldMergeNotification) {
                        notificationGroupCreator.migrateToSingleNotificationChannelForMessages();
                    } else {
                        notificationGroupCreator.migrateToMultipleNotificationChannelsForMessages();
                    }
                }
                if (PreferenceProvider.MANAGE_LINE_MESSAGE_NOTIFICATIONS_PREFERENCE_KEY.equals(preferenceKey)) {
                    if (preferenceProvider.shouldManageLineMessageNotifications()) {
                        silentLineMessageNotificationSettingsDialog.show();
                    } else {
                        alertLineMessageNotificationSettingsDialog.show();
                    }
                }
                if (PreferenceProvider.BLUETOOTH_CONTROL_ONGOING_CALL_KEY.equals(preferenceKey)) {
                    androidPermissionRequester.requestBluetoothPermissionIfNecessary(SettingsActivity.this);
                }
            }
        };
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

        if (silentLineMessageNotificationSettingsDialog == null) {
            silentLineMessageNotificationSettingsDialog = createSilentLineMessageNotificationSettingsDialog();
        }
        if (alertLineMessageNotificationSettingsDialog == null) {
            alertLineMessageNotificationSettingsDialog = createAlertLineMessageNotificationSettingsDialog();
        }

        setupOnPreferenceChangeListener();
    }

    private Dialog createSilentLineMessageNotificationSettingsDialog() {
        return new AlertDialog.Builder(this)
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
        return new AlertDialog.Builder(this)
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

    @Override
    public void onResume() {
        super.onResume();

        sharedPreferences.registerOnSharedPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(onPreferenceChangeListener);
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