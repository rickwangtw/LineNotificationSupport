package com.mysticwind.linenotificationsupport.notificationgroup;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationGroupCreator {

    protected static final String MESSAGE_NOTIFICATION_GROUP_ID = "message_notification_group";
    protected static final String MESSAGE_NOTIFICATION_GROUP_NAME = "Messages";
    protected static final String CALL_NOTIFICATION_GROUP_ID = "call_notification_group";
    protected static final String CALL_NOTIFICATION_GROUP_NAME = "Calls";
    protected static final String OTHERS_NOTIFICATION_GROUP_ID = "others_notification_group";
    protected static final String OTHERS_NOTIFICATION_GROUP_NAME = "Others";

    protected static final String CALL_CHANNEL_NAME = "Calls";
    protected static final String MERGED_MESSAGE_CHANNEL_ID = "merged_message_channel_id";
    protected static final String MERGED_MESSAGE_CHANNEL_NAME = "All Messages";
    protected static final String SELF_RESPONSE_CHANNEL_ID = "self_response_channel_id";
    protected static final String SELF_RESPONSE_CHANNEL_NAME = "My Responses";
    private static final String NO_CHANNEL_NAME_DEFAULT = "No title";

    private static final Map<String, String> NOTIFICATION_GROUP_ID_TO_NAME_MAP = ImmutableMap.of(
            MESSAGE_NOTIFICATION_GROUP_ID, MESSAGE_NOTIFICATION_GROUP_NAME,
            CALL_NOTIFICATION_GROUP_ID, CALL_NOTIFICATION_GROUP_NAME,
            OTHERS_NOTIFICATION_GROUP_ID, OTHERS_NOTIFICATION_GROUP_NAME
    );

    private static final Set<String> CALL_CHAT_IDS = ImmutableSet.of(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID);
    private static final Set<String> UNGROUPED_CHAT_ID = ImmutableSet.of(LineNotificationBuilder.DEFAULT_CHAT_ID);

    private final NotificationManager notificationManager;
    private final AndroidFeatureProvider androidFeatureProvider;
    private final PreferenceProvider preferenceProvider;

    public NotificationGroupCreator(final NotificationManager notificationManager,
                                    final AndroidFeatureProvider androidFeatureProvider,
                                    final PreferenceProvider preferenceProvider) {
        this.notificationManager = notificationManager;
        this.androidFeatureProvider = androidFeatureProvider;
        this.preferenceProvider = preferenceProvider;
    }

    public void createNotificationGroups() {
        if (!androidFeatureProvider.hasNotificationChannelSupport()) {
            return;
        }

        final Set<String> notificationChannelGroupIds = notificationManager.getNotificationChannelGroups().stream()
                .map(notificationChannelGroup -> notificationChannelGroup.getId())
                .collect(Collectors.toSet());

        createNotificationChannelGroupIfNotExist(notificationChannelGroupIds, CALL_NOTIFICATION_GROUP_ID);
        createNotificationChannelGroupIfNotExist(notificationChannelGroupIds, MESSAGE_NOTIFICATION_GROUP_ID);
        createNotificationChannelGroupIfNotExist(notificationChannelGroupIds, OTHERS_NOTIFICATION_GROUP_ID);

        for (final NotificationChannel notificationChannel : notificationManager.getNotificationChannels()) {
            if (!StringUtils.isBlank(notificationChannel.getGroup())) {
                continue;
            }
            addGroupToNotificationChannel(notificationChannel);

            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @SuppressLint("NewApi")
    private void createNotificationChannelGroupIfNotExist(final Set<String> existingNotificationChannelGroupIds,
                                                          final String notificationGroupId) {
        if (!existingNotificationChannelGroupIds.contains(notificationGroupId)) {
            final String notificationGroupName = NOTIFICATION_GROUP_ID_TO_NAME_MAP.get(notificationGroupId);
            notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(notificationGroupId, notificationGroupName));
        }
    }

    @SuppressLint("NewApi")
    private void createNotificationChannelGroupIfNotExist(final String notificationGroupId) {
        final Optional<String> notificationChannelGroup = notificationManager.getNotificationChannelGroups().stream()
                .map(group -> group.getId())
                .findFirst();
        if (!notificationChannelGroup.isPresent()) {
            final String notificationGroupName = NOTIFICATION_GROUP_ID_TO_NAME_MAP.get(notificationGroupId);
            notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(notificationGroupId, notificationGroupName));
        }
    }

    @SuppressLint("NewApi")
    private void addGroupToNotificationChannel(final NotificationChannel notificationChannel) {
        final String notificationGroupId = resolveNotificationChannelGroup(notificationChannel.getId());
        notificationChannel.setGroup(notificationGroupId);
    }

    private String resolveNotificationChannelGroup(final String notificationChannelId) {
        if (isCallNotificationChannel(notificationChannelId)) {
            return CALL_NOTIFICATION_GROUP_ID;
        } else if (isMessageNotificationChannel(notificationChannelId)) {
            return MESSAGE_NOTIFICATION_GROUP_ID;
        } else {
            return OTHERS_NOTIFICATION_GROUP_ID;
        }
    }

    @SuppressLint("NewApi")
    private boolean isCallNotificationChannel(final String channelId) {
        return CALL_CHAT_IDS.contains(channelId);
    }

    @SuppressLint("NewApi")
    private boolean isMessageNotificationChannel(String channelId) {
        if (isCallNotificationChannel(channelId)) {
            return false;
        }
        if (UNGROUPED_CHAT_ID.contains(channelId)) {
            return false;
        }
        return true;
    }

    // TODO missing unit tests
    /**
     * Creates the notification channel. This assumes the notification groups have been created.
     * @param chatId
     * @param messageTitle
     */
    public Optional<String> createNotificationChannel(final String chatId, final String messageTitle) {
        if (!androidFeatureProvider.hasNotificationChannelSupport()) {
            return Optional.empty();
        }
        final String channelId = getChannelId(chatId);
        final String channelName = getChannelName(channelId, messageTitle);

        createNotificationChannelWithChannelIdAndName(channelId, channelName);

        return Optional.of(channelId);
    }

    public Optional<String> createSelfResponseNotificationChannel() {
        if (!androidFeatureProvider.hasNotificationChannelSupport()) {
            return Optional.empty();
        }
        createNotificationChannelWithChannelIdAndName(SELF_RESPONSE_CHANNEL_ID,
                SELF_RESPONSE_CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN, false);

        return Optional.of(SELF_RESPONSE_CHANNEL_ID);
    }

    // TODO unit tests
    private String getChannelId(String chatId) {
        if (LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID.equals(chatId) ||
                LineNotificationBuilder.DEFAULT_CHAT_ID.equals(chatId)) {
            return chatId;
        }
        if (preferenceProvider.shouldUseMergeMessageChatId()) {
            return MERGED_MESSAGE_CHANNEL_ID;
        }
        return chatId;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannelWithChannelIdAndName(final String channelId, final String channelName) {
        createNotificationChannelWithChannelIdAndName(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT, true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannelWithChannelIdAndName(final String channelId,
                                                               final String channelName,
                                                               final int importance,
                                                               final boolean vibrate) {
        final String description = "Notification channel for " + channelName;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription(description);
        channel.enableVibration(vibrate);

        final String group = resolveNotificationChannelGroup(channelId);
        createNotificationChannelGroupIfNotExist(group);
        channel.setGroup(group);

        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        notificationManager.createNotificationChannel(channel);
    }

    private String getChannelName(String channelId, String defaultChannelName) {
        if (LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID.equals(channelId)) {
            return CALL_CHANNEL_NAME;
        } else if (MERGED_MESSAGE_CHANNEL_ID.equals(channelId)) {
            return MERGED_MESSAGE_CHANNEL_NAME;
        }
        // TODO unit tests
        if (StringUtils.isBlank(defaultChannelName)) {
            return NO_CHANNEL_NAME_DEFAULT;
        }
        return defaultChannelName;
    }

    public void migrateToSingleNotificationChannelForMessages() {
        if (!androidFeatureProvider.hasNotificationChannelSupport()) {
            return;
        }

        final Set<String> messageNotificationChannelIds = notificationManager.getNotificationChannels().stream()
                .map(notificationChannel -> notificationChannel.getId())
                .filter(notificationChannelId -> isMessageNotificationChannel(notificationChannelId))
                .collect(Collectors.toSet());

        if (!messageNotificationChannelIds.contains(MERGED_MESSAGE_CHANNEL_ID)) {
            createNotificationChannelWithChannelIdAndName(MERGED_MESSAGE_CHANNEL_ID, MERGED_MESSAGE_CHANNEL_NAME);
        }

        // delete all other notification channels
        messageNotificationChannelIds.stream()
                .filter(notificationChannelId -> !MERGED_MESSAGE_CHANNEL_ID.equals(notificationChannelId))
                .forEach(notificationChannelId ->
                        notificationManager.deleteNotificationChannel(notificationChannelId)
                );
    }

    public void migrateToMultipleNotificationChannelsForMessages() {
        if (!androidFeatureProvider.hasNotificationChannelSupport()) {
            return;
        }

        notificationManager.deleteNotificationChannel(MERGED_MESSAGE_CHANNEL_ID);
    }

}
