package com.mysticwind.linenotificationsupport.preference;

import android.content.SharedPreferences;

public class PreferenceProvider {

    private static final String MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY = "merge_message_notification_channels";

    public static final String MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY = "max_notification_workaround";
    public static final String USE_LEGACY_STICKER_LOADER_PREFERENCE_KEY = "use_legacy_sticker_loader";

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

    public boolean shouldUseLegacyStickerLoader() {
        return sharedPreferences.getBoolean(USE_LEGACY_STICKER_LOADER_PREFERENCE_KEY, false);
    }

}
