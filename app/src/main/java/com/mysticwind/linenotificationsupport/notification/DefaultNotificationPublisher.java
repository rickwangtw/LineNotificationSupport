package com.mysticwind.linenotificationsupport.notification;

import android.content.Context;

import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.ImageNotificationPublisherAsyncTask;

public class DefaultNotificationPublisher implements NotificationPublisher {

    private final Context context;
    private final GroupIdResolver groupIdResolver;

    public DefaultNotificationPublisher(final Context context, GroupIdResolver groupIdResolver) {
        this.context = context;
        this.groupIdResolver = groupIdResolver;
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        new ImageNotificationPublisherAsyncTask(
                context, lineNotification, notificationId, groupIdResolver)
                .execute();
    }

    @Override
    public void updateNotificationDismissed() {
        // do nothing
    }

}
