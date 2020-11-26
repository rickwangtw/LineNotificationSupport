package com.mysticwind.linenotificationsupport.notification;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.ImageNotificationPublisherAsyncTask;

public class SimpleNotificationPublisher implements NotificationPublisher {

    private final Context context;
    private final String packageName;
    private final GroupIdResolver groupIdResolver;

    public SimpleNotificationPublisher(final Context context,
                                       final String packageName,
                                       final GroupIdResolver groupIdResolver) {
        this.context = context;
        this.packageName = packageName;
        this.groupIdResolver = groupIdResolver;
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        new ImageNotificationPublisherAsyncTask(context, lineNotification, notificationId)
                .execute();
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // do nothing
    }

}
