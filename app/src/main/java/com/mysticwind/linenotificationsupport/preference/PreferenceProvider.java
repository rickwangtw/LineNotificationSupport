package com.mysticwind.linenotificationsupport.preference;

import android.content.SharedPreferences;

public class PreferenceProvider {

    private static final String MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY = "merge_message_notification_channels";

    public static final String MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY = "max_notification_workaround";

    private final SharedPreferences sharedPreferences;

    public PreferenceProvider(final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean shouldUseMergeMessageChatId() {
        return sharedPreferences.getBoolean(MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY, false);
    }

    public boolean shouldExecuteMaxNotificationWorkaround() {
        return sharedPreferences.getBoolean(MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY, true);
    }

}
