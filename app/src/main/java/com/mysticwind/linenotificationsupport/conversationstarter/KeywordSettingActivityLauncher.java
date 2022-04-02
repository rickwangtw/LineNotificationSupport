package com.mysticwind.linenotificationsupport.conversationstarter;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mysticwind.linenotificationsupport.conversationstarter.activity.KeywordSettingActivity;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class KeywordSettingActivityLauncher {

    private final Context context;

    @Inject
    public KeywordSettingActivityLauncher(@ApplicationContext final Context context) {
        this.context = Objects.requireNonNull(context);
    }

    public PendingIntent buildPendingIntent() {
        final Intent intent = new Intent(context, KeywordSettingActivity.class);
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

}
