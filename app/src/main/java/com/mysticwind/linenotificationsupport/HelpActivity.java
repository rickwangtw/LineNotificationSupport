package com.mysticwind.linenotificationsupport;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.common.collect.ImmutableMap;
import com.mysticwind.linenotificationsupport.debug.DebugModeProvider;

import java.util.Map;

import timber.log.Timber;

import static com.mysticwind.linenotificationsupport.line.Constants.LINE_PACKAGE_NAME;

public class HelpActivity extends AppCompatActivity {

    private static final DebugModeProvider DEBUG_MODE_PROVIDER = new DebugModeProvider();

    private static final Map<String, Integer> LINE_VERSION_TO_WARNING_MESSAGE_ID = ImmutableMap.of(
            "10.19.1", R.string.line_version_warning_10_19_1
    );

    private Dialog grantPermissionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_main);

        showLineVersionWarning();

        if (grantPermissionDialog == null) {
            grantPermissionDialog = createGrantPermissionDialog();
        }
    }

    private void showLineVersionWarning() {
        final String lineVersion = getLineAppVersion();
        Timber.d("Detected LINE with version: " + lineVersion);

        final Integer warningMessageId = LINE_VERSION_TO_WARNING_MESSAGE_ID.get(lineVersion);
        if (warningMessageId == null) {
            return;
        }

        ((TextView) findViewById(R.id.warning_message_text)).setText(warningMessageId);
        findViewById(R.id.warning_message_layout).setVisibility(View.VISIBLE);
    }

    private String getLineAppVersion() {
        // https://stackoverflow.com/questions/50795458/android-how-to-get-any-application-version-by-package-name
        final PackageManager packageManager = getPackageManager();
        try {
            final PackageInfo packageInfo = packageManager.getPackageInfo(LINE_PACKAGE_NAME, 0);
            return packageInfo.versionName;
        } catch (final PackageManager.NameNotFoundException e) {
            Timber.e(e, "LINE not installed. Package: " + LINE_PACKAGE_NAME);
            return null;
        }
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

    private boolean hasNotificationAccess() {
        final ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners =
                Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }

    private void redirectToNotificationSettingsPage() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        if (DEBUG_MODE_PROVIDER.isDebugMode()) {
            menu.getItem(1).setVisible(true);
            menu.getItem(2).setVisible(true);
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
    }

        return super.onOptionsItemSelected(item);
    }
}