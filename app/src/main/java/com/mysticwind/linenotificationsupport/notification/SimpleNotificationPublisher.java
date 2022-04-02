package com.mysticwind.linenotificationsupport.notification;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notificationgroup.NotificationGroupCreator;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.BigPictureStyleImageSupportedNotificationPublisherAsyncTask;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.MessageStyleImageSupportedNotificationPublisherAsyncTask;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SimpleNotificationPublisher implements NotificationPublisher {

    private final Context context;
    private final String packageName;
    private final GroupIdResolver groupIdResolver;
    private final PreferenceProvider preferenceProvider;
    private final NotificationGroupCreator notificationGroupCreator;

    private Collection<NotificationSentListener> notificationSentListeners = Collections.EMPTY_LIST;

    @Inject
    public SimpleNotificationPublisher(@ApplicationContext final Context context,
                                       @HiltQualifiers.PackageName final String packageName,
                                       final GroupIdResolver groupIdResolver,
                                       final PreferenceProvider preferenceProvider,
                                       final NotificationGroupCreator notificationGroupCreator) {
        this.context = context;
        this.packageName = packageName;
        this.groupIdResolver = groupIdResolver;
        this.preferenceProvider = preferenceProvider;
        this.notificationGroupCreator = Objects.requireNonNull(notificationGroupCreator);
    }

    public void setNotificationSentListeners(Collection<NotificationSentListener> notificationSentListeners) {
        this.notificationSentListeners = notificationSentListeners;
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        if (preferenceProvider.shouldUseLegacyStickerLoader()) {
            new BigPictureStyleImageSupportedNotificationPublisherAsyncTask(context, notificationGroupCreator, lineNotification, notificationId)
                    .execute();
        } else {
            final boolean useSingleNotificationConversations = preferenceProvider.shouldUseSingleNotificationForConversations();
            new MessageStyleImageSupportedNotificationPublisherAsyncTask(
                    context, notificationGroupCreator, lineNotification, notificationId, useSingleNotificationConversations)
                    .execute();
        }
        notificationSentListeners.forEach(
                listener -> listener.notificationSent(lineNotification, notificationId));
    }

    @Override
    public void republishNotification(LineNotification lineNotification, int notificationId) {
        publishNotification(lineNotification, notificationId);
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
    }

}
