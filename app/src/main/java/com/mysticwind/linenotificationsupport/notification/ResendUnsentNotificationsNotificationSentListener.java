package com.mysticwind.linenotificationsupport.notification;

import android.os.Handler;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import timber.log.Timber;

public class ResendUnsentNotificationsNotificationSentListener implements NotificationSentListener {

    private static final long NOTIFICATION_VERIFICATION_WAIT_TIME_MILLIS = 1_000L;

    private final Map<Integer, LineNotification> idToNotificationMap = new ConcurrentHashMap();
    private final Handler handler;
    private final Supplier<NotificationPublisher> notificationPublisherSupplier;

    public ResendUnsentNotificationsNotificationSentListener(final Handler handler,
                                                             final Supplier<NotificationPublisher> notificationPublisherSupplier) {
        this.handler = Objects.requireNonNull(handler);
        this.notificationPublisherSupplier = Objects.requireNonNull(notificationPublisherSupplier);
    }

    @Override
    public void notificationSent(final LineNotification lineNotification, final int notificationId) {
        Objects.requireNonNull(lineNotification);

        Timber.d("Tracking notification id [%d] message [%s] sending status", notificationId, lineNotification.getMessage());
        idToNotificationMap.put(notificationId, lineNotification);

        handler.postDelayed(() -> {
            LineNotification notification = idToNotificationMap.get(notificationId);
            if (notification != null) {
                Timber.w("Notification id [%d] message [%s] was not sent!", notificationId, notification.getMessage());

                notificationPublisherSupplier.get().republishNotification(lineNotification, notificationId);
            }
        }, NOTIFICATION_VERIFICATION_WAIT_TIME_MILLIS);
    }

    public void notificationReceived(final int notificationId) {
        Timber.d("Marking notification id [%d] as sent", notificationId);

        idToNotificationMap.remove(notificationId);
    }

}
