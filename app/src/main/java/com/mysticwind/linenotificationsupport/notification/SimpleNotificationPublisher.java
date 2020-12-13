package com.mysticwind.linenotificationsupport.notification;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.BigPictureStyleImageSupportedNotificationPublisherAsyncTask;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.MessageStyleImageSupportedNotificationPublisherAsyncTask;

public class SimpleNotificationPublisher implements NotificationPublisher {

    private final Context context;
    private final String packageName;
    private final GroupIdResolver groupIdResolver;
    private final PreferenceProvider preferenceProvider;

    public SimpleNotificationPublisher(final Context context,
                                       final String packageName,
                                       final GroupIdResolver groupIdResolver,
                                       final PreferenceProvider preferenceProvider) {
        this.context = context;
        this.packageName = packageName;
        this.groupIdResolver = groupIdResolver;
        this.preferenceProvider = preferenceProvider;
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
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
    }

}
