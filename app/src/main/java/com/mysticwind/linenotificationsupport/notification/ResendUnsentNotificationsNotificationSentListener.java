package com.mysticwind.linenotificationsupport.notification;

import android.os.Handler;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Value;
import timber.log.Timber;

public class ResendUnsentNotificationsNotificationSentListener implements NotificationSentListener {

    private static final long NOTIFICATION_VERIFICATION_WAIT_TIME_MILLIS = 1_000L;
    private static final int MAX_RETRY_COUNT = 3;

    @Value
    private static class Item {
        LineNotification lineNotification;
        MutableInt retryCount;

        static Item newItem(LineNotification lineNotification) {
            return new Item(lineNotification, new MutableInt(0));
        }

        void incrementRetryCount() {
            retryCount.increment();
        }

        boolean reachedMaxRetry() {
            return retryCount.intValue() > MAX_RETRY_COUNT;
        }
    }

    private final Map<Integer, Item> idToItemMap = new ConcurrentHashMap();
    private final Handler handler;
    private final NotificationPublisherFactory notificationPublisherFactory;

    public ResendUnsentNotificationsNotificationSentListener(final Handler handler,
                                                             final NotificationPublisherFactory notificationPublisherFactory) {
        this.handler = Objects.requireNonNull(handler);
        this.notificationPublisherFactory = Objects.requireNonNull(notificationPublisherFactory);
    }

    @Override
    public void notificationSent(final LineNotification lineNotification, final int notificationId) {
        Objects.requireNonNull(lineNotification);

        final Item itemInMap = idToItemMap.get(notificationId);

        if (itemInMap == null) {
            Timber.d("Tracking notification id [%d] message [%s] sending status", notificationId, lineNotification.getMessage());
            idToItemMap.put(notificationId, Item.newItem(lineNotification));
        } else {
            Timber.i("Notification id [%d] is already tracked", notificationId);
        }

        handler.postDelayed(() -> {
            final Item item = idToItemMap.get(notificationId);
            if (item != null) {
                if (item.reachedMaxRetry()) {
                    Timber.w("Notification [%s] reached max retry [%s]", notificationId, item.getRetryCount().intValue());
                    idToItemMap.remove(notificationId);
                } else {
                    final LineNotification notification = item.getLineNotification();
                    Timber.w("Notification id [%d] message [%s] was not sent!", notificationId, notification.getMessage());

                    item.incrementRetryCount();
                    notificationPublisherFactory.get().republishNotification(lineNotification, notificationId);
                }
            }
        }, NOTIFICATION_VERIFICATION_WAIT_TIME_MILLIS);
    }

    public void notificationReceived(final int notificationId) {
        Timber.d("Marking notification id [%d] as sent", notificationId);

        idToItemMap.remove(notificationId);
    }

}
