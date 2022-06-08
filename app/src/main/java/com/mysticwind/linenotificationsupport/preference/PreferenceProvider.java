package com.mysticwind.linenotificationsupport.preference;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PreferenceProvider {

    public static final String MANAGE_LINE_MESSAGE_NOTIFICATIONS_PREFERENCE_KEY = "manage_line_message_notifications";
    public static final String AUTO_DISMISS_TRANSFORMED_MESSAGES_PREFERENCE_KEY = "auto_dismiss_line_notification_support_messages";
    public static final String MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY = "merge_message_notification_channels";
    public static final String MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY = "max_notification_workaround";
    public static final String USE_LEGACY_STICKER_LOADER_PREFERENCE_KEY = "use_legacy_sticker_loader";
    public static final String USE_MESSAGE_SPLITTER_PREFERENCE_KEY = "use_big_message_splitter";
    public static final String MESSAGE_SIZE_LIMIT_PREFERENCE_KEY = "message_size_limit";
    public static final String SPLIT_MESSAGE_MAX_PAGES_KEY = "split_message_max_pages";
    public static final String SINGLE_NOTIFICATION_CONVERSATIONS_KEY = "single_notification_with_history";
    public static final String GENERATE_SELF_RESPONSE_MESSAGE_KEY = "generate_self_response_message";
    public static final String BLUETOOTH_CONTROL_ONGOING_CALL_KEY = "bluetooth_control_in_calls";
    public static final String CONVERSATION_STARTER_KEY = "conversation_starter";
    public static final String CREATE_NEW_CONTINUOUS_CALL_NOTIFICATIONS_KEY = "create_new_continuous_call_notifications";

    private final SharedPreferences sharedPreferences;

    @Inject
    public PreferenceProvider(final SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean shouldManageLineMessageNotifications() {
        return sharedPreferences.getBoolean(MANAGE_LINE_MESSAGE_NOTIFICATIONS_PREFERENCE_KEY, false);
    }

    public boolean shouldAutoDismissLineNotificationSupportNotifications() {
        if (shouldManageLineMessageNotifications()) {
            return false;
        }
        return sharedPreferences.getBoolean(AUTO_DISMISS_TRANSFORMED_MESSAGES_PREFERENCE_KEY, true);
    }

    public boolean shouldUseMergeMessageChatId() {
        return sharedPreferences.getBoolean(MERGE_NOTIFICATION_CHANNEL_PREFERENCE_KEY, false);
    }

    public boolean shouldExecuteMaxNotificationWorkaround() {
        if (shouldUseSingleNotificationForConversations()) {
            return false;
        }
        return sharedPreferences.getBoolean(MAX_NOTIFICATION_WORKAROUND_PREFERENCE_KEY, true);
    }

    public boolean shouldUseLegacyStickerLoader() {
        if (shouldUseSingleNotificationForConversations()) {
            return false;
        }
        return sharedPreferences.getBoolean(USE_LEGACY_STICKER_LOADER_PREFERENCE_KEY, false);
    }

    public boolean shouldUseMessageSplitter() {
        if (shouldUseSingleNotificationForConversations()) {
            return false;
        }
        return sharedPreferences.getBoolean(USE_MESSAGE_SPLITTER_PREFERENCE_KEY, true);
    }

    public int getMessageSizeLimit() {
        if (!shouldUseMessageSplitter() || shouldUseSingleNotificationForConversations()) {
            throw new IllegalArgumentException(
                    String.format("Should not need message size limit. Use message splitter [%s] Use single notification [%]",
                            shouldUseMessageSplitter(), shouldUseSingleNotificationForConversations()));
        }
        return sharedPreferences.getInt(MESSAGE_SIZE_LIMIT_PREFERENCE_KEY, 60);
    }

    public int getMaxPageCount() {
        if (!shouldUseMessageSplitter() || shouldUseSingleNotificationForConversations()) {
            throw new IllegalArgumentException(
                    String.format("Should not need max page count. Use message splitter [%s] Use single notification [%]",
                            shouldUseMessageSplitter(), shouldUseSingleNotificationForConversations()));
        }
        return sharedPreferences.getInt(SPLIT_MESSAGE_MAX_PAGES_KEY, 5);
    }

    public boolean shouldUseSingleNotificationForConversations() {
        return sharedPreferences.getBoolean(SINGLE_NOTIFICATION_CONVERSATIONS_KEY, false);
    }

    public boolean shouldGenerateSelfResponseMessage() {
        return sharedPreferences.getBoolean(GENERATE_SELF_RESPONSE_MESSAGE_KEY, true);
    }

    public boolean shouldControlBluetoothDuringCalls() {
        return sharedPreferences.getBoolean(BLUETOOTH_CONTROL_ONGOING_CALL_KEY, false);
    }

    public boolean shouldShowConversationStarterNotification() {
        return sharedPreferences.getBoolean(CONVERSATION_STARTER_KEY, true);
    }

    public boolean shouldCreateNewContinuousCallNotifications() {
        return sharedPreferences.getBoolean(CREATE_NEW_CONTINUOUS_CALL_NOTIFICATIONS_KEY, true);
    }

}
