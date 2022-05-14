package com.mysticwind.linenotificationsupport;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.conversationstarter.activity.KeywordSettingActivity;
import com.mysticwind.linenotificationsupport.debug.DebugModeProvider;
import com.mysticwind.linenotificationsupport.line.LineAppVersionProvider;
import com.mysticwind.linenotificationsupport.permission.AndroidPermissionRequester;
import com.mysticwind.linenotificationsupport.provision.FeatureProvisionStateProvider;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class HelpActivity extends AppCompatActivity {

    private static final DebugModeProvider DEBUG_MODE_PROVIDER = new DebugModeProvider();

    private static final Map<String, Integer> LINE_VERSION_TO_WARNING_MESSAGE_ID = ImmutableMap.of(
            "10.19.1", R.string.line_version_warning_10_19_1
    );

    @Inject
    LineAppVersionProvider lineAppVersionProvider;

    @Inject
    AndroidPermissionRequester androidPermissionRequester;

    private FeatureProvisionStateProvider featureProvisionStateProvider;
    private Dialog grantPermissionDialog;
    private Dialog disablePowerOptimizationTipDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_main);

        final TextView recommendedSettingsTextView = findViewById(R.id.recommended_settings_text_view);
        recommendedSettingsTextView.setMovementMethod(LinkMovementMethod.getInstance());

        showLineVersionWarning();

        if (grantPermissionDialog == null) {
            grantPermissionDialog = createGrantPermissionDialog();
        }
        if (disablePowerOptimizationTipDialog == null) {
            disablePowerOptimizationTipDialog = createDisablePowerOptimizationTipDialog();
        }

        androidPermissionRequester.requestBluetoothPermissionIfNecessary(this);
    }

    private void showLineVersionWarning() {
        final Optional<String> lineAppVersion = lineAppVersionProvider.getLineAppVersion();
        if (!lineAppVersion.isPresent()) {
            return;
        }

        Timber.d("Detected LINE with version: " + lineAppVersion.get());

        final Integer warningMessageId = LINE_VERSION_TO_WARNING_MESSAGE_ID.get(lineAppVersion.get());
        if (warningMessageId == null) {
            return;
        }

        ((TextView) findViewById(R.id.warning_message_text)).setText(warningMessageId);
        findViewById(R.id.warning_message_layout).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasNotificationAccess()) {
            if (grantPermissionDialog.isShowing()) {
                grantPermissionDialog.dismiss();
            }
        } else {
            grantPermissionDialog.show();
        }

        perhapsShowDisablePowerOptimizationTip();
    }

    private void perhapsShowDisablePowerOptimizationTip() {
        if (grantPermissionDialog.isShowing()) {
            return;
        }

        getFeatureProvisionStateProvider()
                .isDisablePowerOptimizationTipShown()
                .onErrorReturn((error) -> {
                    Timber.e(error, "Error returning isDisablePowerOptimizationTipShown: [%s]", error.getMessage());
                    return false;
                })
                .subscribe(isShownBefore -> {
                    if (isShownBefore) {
                        dismissDisablePowerOptimizationTipDialog();
                        return;
                    }
                    if (!isPowerOptimizationEnabled()) {
                        dismissDisablePowerOptimizationTipDialog();
                        return;
                    }
                    this.runOnUiThread(() -> disablePowerOptimizationTipDialog.show());
                });
    }

    private void dismissDisablePowerOptimizationTipDialog() {
        if (disablePowerOptimizationTipDialog.isShowing()) {
            disablePowerOptimizationTipDialog.dismiss();
        }
    }

    private FeatureProvisionStateProvider getFeatureProvisionStateProvider() {
        if (featureProvisionStateProvider != null) {
            return featureProvisionStateProvider;
        } else {
            featureProvisionStateProvider = new FeatureProvisionStateProvider(this);
            return featureProvisionStateProvider;
        }
    }

    // https://stackoverflow.com/questions/39256501/check-if-battery-optimization-is-enabled-or-not-for-an-app
    private boolean isPowerOptimizationEnabled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(getPackageName());
        Timber.d("isIgnoringBatteryOptimizations [%s]", isIgnoringBatteryOptimizations);
        return !isIgnoringBatteryOptimizations;
    }

    private Dialog createGrantPermissionDialog() {
        return new AlertDialog.Builder(this)
                .setMessage(R.string.permission_request_dialog_message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        redirectToNotificationSettingsPage();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .create();
    }

    private Dialog createDisablePowerOptimizationTipDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.disable_power_optimization_tip_dialog_title)
                .setMessage(R.string.power_optimization_settings_summary)
                .setPositiveButton(R.string.disable_power_optimization_tip_dialog_yes,
                        (dialog, which) -> {
                            final Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            HelpActivity.this.startActivity(intent);
                        })
                .setNeutralButton(android.R.string.cancel,
                        (dialog, which) ->
                                getFeatureProvisionStateProvider().setDisablePowerOptimizationTipShown()
                )
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .create();
    }

    private boolean hasNotificationAccess() {
        final ContentResolver contentResolver = getContentResolver();
        // it would look something like this: net.dinglisch.android.taskerm.NotificationListenerService:com.mysticwind.linenotificationsupport.donate/com.mysticwind.linenotificationsupport.service.NotificationListenerService
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        Timber.w("enabled_notification_listeners: " + enabledNotificationListeners);
        if (StringUtils.isBlank(enabledNotificationListeners)) {
            return false;
        }
        return Arrays.stream(enabledNotificationListeners.split(":"))
                .filter(enabledNotificationListener -> enabledNotificationListener.startsWith(packageName + "/"))
                .findAny()
                .isPresent();
    }

    private void redirectToNotificationSettingsPage() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    @Override
    protected void onPause() {
        super.onPause();

        final FeatureProvisionStateProvider providerToShutdown = this.featureProvisionStateProvider;
        this.featureProvisionStateProvider = null;
        if (providerToShutdown != null) {
            providerToShutdown.shutdown();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (DEBUG_MODE_PROVIDER.isDebugMode()) {
            menu.getItem(1).setVisible(true);
            menu.getItem(2).setVisible(true);
            menu.getItem(3).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_debug) {
            Intent intent = new Intent(this, NotificationHistoryDebugActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_test_notifications) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_keyword_settings) {
            Intent intent = new Intent(this, KeywordSettingActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}