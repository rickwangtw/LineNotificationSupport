package com.mysticwind.linenotificationsupport.chatname.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public class DeleteFriendNameCacheBroadcastReceiver extends BroadcastReceiver {

    @Inject
    ChatNameManager chatNameManager;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Timber.i("Received intent [%s] to delete friend name cache", intent.getAction());

        chatNameManager.deleteFriendNameCache();
    }

}