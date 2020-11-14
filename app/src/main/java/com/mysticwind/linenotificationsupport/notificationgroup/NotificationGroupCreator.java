package com.mysticwind.linenotificationsupport.notificationgroup;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class NotificationGroupCreator {

    protected static final String MESSAGE_NOTIFICATION_GROUP_ID = "message_notification_group";
    protected static final String MESSAGE_NOTIFICATION_GROUP_NAME = "Messages";
    protected static final String CALL_NOTIFICATION_GROUP_ID = "call_notification_group";
    protected static final String CALL_NOTIFICATION_GROUP_NAME = "Calls";
    protected static final String OTHERS_NOTIFICATION_GROUP_ID = "others_notification_group";
    protected static final String OTHERS_NOTIFICATION_GROUP_NAME = "Others";

    private static final Map<String, String> NOTIFICATION_GROUP_ID_TO_NAME_MAP = ImmutableMap.of(
            MESSAGE_NOTIFICATION_GROUP_ID, MESSAGE_NOTIFICATION_GROUP_NAME,
            CALL_NOTIFICATION_GROUP_ID, CALL_NOTIFICATION_GROUP_NAME,
            OTHERS_NOTIFICATION_GROUP_ID, OTHERS_NOTIFICATION_GROUP_NAME
    );

    private static final Set<String> CALL_CHAT_IDS = ImmutableSet.of(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID);
    private static final Set<String> UNGROUPED_CHAT_ID = ImmutableSet.of(LineNotificationBuilder.DEFAULT_CHAT_ID);

    private final NotificationManager notificationManager;
    private final AndroidFeatureProvider androidFeatureProvider;

    public NotificationGroupCreator(final NotificationManager notificationManager, final AndroidFeatureProvider androidFeatureProvider) {
        this.notificationManager = notificationManager;
        this.androidFeatureProvider = androidFeatureProvider;
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
     * @param channelId
     * @param channelName
     */
    public void createNotificationChannel(final String channelId, String channelName) {
        if (!androidFeatureProvider.hasNotificationChannelSupport()) {
            return;
        }
        final String group = resolveNotificationChannelGroup(channelId);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        final String description = "Notification channel for " + channelName;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription(description);
        channel.enableVibration(true);
        channel.setGroup(group);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        notificationManager.createNotificationChannel(channel);
    }

}
