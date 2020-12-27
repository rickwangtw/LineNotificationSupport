package com.mysticwind.linenotificationsupport.notification;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.BigPictureStyleImageSupportedNotificationPublisherAsyncTask;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.MessageStyleImageSupportedNotificationPublisherAsyncTask;

import java.util.Collection;
import java.util.Collections;

public class SimpleNotificationPublisher implements NotificationPublisher {

    private final Context context;
    private final String packageName;
    private final GroupIdResolver groupIdResolver;
    private final PreferenceProvider preferenceProvider;
    private final Collection<NotificationSentListener> notificationSentListeners;

    public SimpleNotificationPublisher(final Context context,
                                       final String packageName,
                                       final GroupIdResolver groupIdResolver,
                                       final PreferenceProvider preferenceProvider) {
        this(context, packageName, groupIdResolver, preferenceProvider, Collections.emptyList());
    }

    public SimpleNotificationPublisher(final Context context,
                                       final String packageName,
                                       final GroupIdResolver groupIdResolver,
                                       final PreferenceProvider preferenceProvider,
                                       final NotificationSentListener notificationSentListener) {
        this(context, packageName, groupIdResolver, preferenceProvider, ImmutableList.of(notificationSentListener));
    }

    public SimpleNotificationPublisher(final Context context,
                                       final String packageName,
                                       final GroupIdResolver groupIdResolver,
                                       final PreferenceProvider preferenceProvider,
                                       final Collection<NotificationSentListener> notificationSentListeners) {
        this.context = context;
        this.packageName = packageName;
        this.groupIdResolver = groupIdResolver;
        this.preferenceProvider = preferenceProvider;
        this.notificationSentListeners = notificationSentListeners;
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        if (preferenceProvider.shouldUseLegacyStickerLoader()) {
            new BigPictureStyleImageSupportedNotificationPublisherAsyncTask(context, lineNotification, notificationId)
                    .execute();
        } else {
            new MessageStyleImageSupportedNotificationPublisherAsyncTask(context, lineNotification, notificationId)
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
