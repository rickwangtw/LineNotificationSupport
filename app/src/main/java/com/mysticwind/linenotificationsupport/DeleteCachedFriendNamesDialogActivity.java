package com.mysticwind.linenotificationsupport;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
                .setMessage("Are you sure?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Timber.i("Confirmed cleaning database");

                    }
                })
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