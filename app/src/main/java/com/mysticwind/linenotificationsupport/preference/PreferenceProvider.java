package com.mysticwind.linenotificationsupport.preference;

import android.content.SharedPreferences;

public class PreferenceProvider {

    private static final String MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY = "merge_message_notification_channels";

    private final SharedPreferences sharedPreferences;

    public PreferenceProvider(final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean shouldUseMergeMessageChatId() {
        return sharedPreferences.getBoolean(MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY, false);
    }

}
