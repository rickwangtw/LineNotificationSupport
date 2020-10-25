package com.mysticwind.linenotificationsupport;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    private static final String TAG = HelpActivity.class.getSimpleName();

    private Dialog grantPermissionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.help_main);

        if (grantPermissionDialog == null) {
            grantPermissionDialog = createGrantPermissionDialog();
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
                // TODO localization
                .setMessage("You'll need to grant permissions for this app to access Line notifications.")
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}