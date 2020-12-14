package com.mysticwind.linenotificationsupport.preference;

import android.content.SharedPreferences;

public class PreferenceProvider {

    private static final String MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY = "merge_message_notification_channels";

    public static final String MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY = "max_notification_workaround";
    public static final String USE_LEGACY_STICKER_LOADER_PREFERENCE_KEY = "use_legacy_sticker_loader";
    public static final String MESSAGE_SIZE_LIMIT_PREFERENCE_KEY = "message_size_limit";
    public static final String SPLIT_MESSAGE_MAX_PAGES_KEY = "split_message_max_pages";

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

    public int getMessageSizeLimit() {
        return sharedPreferences.getInt(MESSAGE_SIZE_LIMIT_PREFERENCE_KEY, 60);
    }

    public int getMaxPageCount() {
        return sharedPreferences.getInt(SPLIT_MESSAGE_MAX_PAGES_KEY, 5);
    }

}
