package com.mysticwind.linenotificationsupport;

import static android.content.Intent.FLAG_RECEIVER_FOREGROUND;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mysticwind.linenotificationsupport.chatname.broadcastreceiver.DeleteFriendNameCacheBroadcastReceiver;
import com.mysticwind.linenotificationsupport.service.NotificationListenerService;

import timber.log.Timber;

public class DeleteCachedFriendNamesDialogActivity extends AppCompatActivity {

    private Dialog confirmationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.confirmationDialog = createConfirmationDialog();
    }

    private Dialog createConfirmationDialog() {
        return new AlertDialog.Builder(this)
                .setMessage(R.string.delete_cache_confirmation_dialog_title)
                .setPositiveButton(R.string.delete_cache_confirmation_dialog_yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Timber.i("Confirmed cleaning database");

                        final Intent intent = new Intent(getApplicationContext(), DeleteFriendNameCacheBroadcastReceiver.class);
                        intent.setAction(NotificationListenerService.DELETE_FRIEND_NAME_CACHE_ACTION);
                        intent.setFlags(FLAG_RECEIVER_FOREGROUND);
                        sendBroadcast(intent);

                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setOnDismissListener((dialog) -> {
                    Timber.i("Dialog dismissed");

                    DeleteCachedFriendNamesDialogActivity.this.finish();
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(true)
                .create();
    }

    @Override
    protected void onResume() {
        super.onResume();

        confirmationDialog.show();
    }

}