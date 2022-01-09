package com.mysticwind.linenotificationsupport.conversationstarter.broadcastreceiver;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mysticwind.linenotificationsupport.preference.PreferenceWriter;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class DisableStartConversationFeatureBroadcastReceiver extends BroadcastReceiver {

    @Inject
    PreferenceWriter preferenceWriter;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Timber.i("Received request to disable start conversation feature intent action [%s]", intent.getAction());

        preferenceWriter.disableShowConversationStarterNotification();
    }

}