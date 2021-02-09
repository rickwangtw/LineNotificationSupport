package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class NotificationMergingNotificationPublisherDecorator implements NotificationPublisher {

    private final Cache<String, Integer> lineMessageIdToNotificationIdCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<String, Integer>() {
                @Override
                public void onRemoval(RemovalNotification<String, Integer> notification) {
                    Timber.d("LINE message ID cache removal detected: reason [%s] LINE message ID [%s] notification ID [%s]",
                            notification.getCause(), notification.getKey(), notification.getValue());
                }
            })
            .build();

    private final NotificationPublisher notificationPublisher;

    public NotificationMergingNotificationPublisherDecorator(final NotificationPublisher notificationPublisher) {
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher);
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        if (StringUtils.isBlank(lineNotification.getLineMessageId())) {
            notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        }
        final String lineMessageId = lineNotification.getLineMessageId();
        Integer previousNotificationId = lineMessageIdToNotificationIdCache.getIfPresent(lineMessageId);
        if (previousNotificationId == null) {
            lineMessageIdToNotificationIdCache.put(lineMessageId, notificationId);
            notificationPublisher.publishNotification(lineNotification, notificationId);
            return;
        } else {
            Timber.d("Detected previous published notification that should use the same notification ID previous [%d] new [%d] message [%s]",
                    previousNotificationId, notificationId, lineNotification.getMessage());
            notificationPublisher.publishNotification(lineNotification, previousNotificationId);
        }

    }

    @Override
    public void republishNotification(final LineNotification lineNotification, final int notificationId) {
        // do nothing
        notificationPublisher.republishNotification(lineNotification, notificationId);
    }

    @Override
    public void updateNotificationDismissed(final StatusBarNotification statusBarNotification) {
        // do nothing
        notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }

}
