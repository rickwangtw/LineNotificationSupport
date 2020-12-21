package com.mysticwind.linenotificationsupport.notification;

import android.os.Handler;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Value;
import timber.log.Timber;

public class MaxNotificationHandlingNotificationPublisherDecorator implements NotificationPublisher {

    // without the cool down, messages may not get sent if messages of the same group was just dismissed
    private static final long DISMISS_COOL_DOWN_IN_MILLIS = 500L;
    private static final long NOTIFICATION_CHECK_PERIOD_IN_MILLIS = 500L;

    // tracks the messages that has not been sent
    private static final ConcurrentLinkedQueue<QueueItem> QUEUE_ITEMS = new ConcurrentLinkedQueue<>();

    // this is to make sure we have sufficient cool down for each chat after it being dismissed
    private static final Map<String, Instant> CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP = new ConcurrentHashMap<>();

    private final Handler handler;
    private final NotificationPublisher notificationPublisher;
    private final NotificationCounter notificationCounter;

    // TODO should we just have this class implemented and shared??
    @Value
    class QueueItem {
        private final LineNotification lineNotification;
        private final int notificationId;
    }

    public MaxNotificationHandlingNotificationPublisherDecorator(final Handler handler,
                                                                 final NotificationPublisher notificationPublisher,
                                                                 final NotificationCounter notificationCounter) {
        this.handler = handler;
        this.notificationPublisher = notificationPublisher;
        this.notificationCounter = notificationCounter;
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        if (!notificationCounter.hasSlot(lineNotification.getChatId())) {
            Timber.d("Reached maximum notifications, add to queue: " + notificationId);
            QUEUE_ITEMS.add(new QueueItem(lineNotification, notificationId));
            return;
        }
        if (QUEUE_ITEMS.isEmpty()) {
            Timber.d("Publish new notification: " + notificationId);
            publish(lineNotification, notificationId);
            return;
        }
        QUEUE_ITEMS.add(new QueueItem(lineNotification, notificationId));

        final Optional<QueueItem> firstItem = getFirstItem();
        if (!firstItem.isPresent()) {
            return;
        }
        firstItem.ifPresent(item -> {
                    Timber.d("Publish previously queued notification: " + item.getNotificationId());
                    publish(item.getLineNotification(), item.getNotificationId());
                }
        );
    }

    @Override
    public void updateNotificationDismissed(final StatusBarNotification statusBarNotification) {
        final String chatId = statusBarNotification.getNotification().getGroup();

        if (!notificationCounter.hasSlot(chatId)) {
            return;
        }

        Optional<QueueItem> firstItem = getFirstItem();
        if (!firstItem.isPresent()) {
            CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP.put(chatId, Instant.now());
            return;
        }
        final long delayInMillis = calculateDelayInMillis(firstItem.get().getLineNotification().getChatId());
        delayedPublish(firstItem.get(), delayInMillis);
        CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP.put(chatId, Instant.now().plusMillis(delayInMillis));

        // TODO why are there cases where there are available slots but the queue still has items???
        if (!QUEUE_ITEMS.isEmpty()) {
            scheduleSlotCheck(delayInMillis + NOTIFICATION_CHECK_PERIOD_IN_MILLIS);
        }
    }

    private void delayedPublish(final QueueItem queueItem, final long delayInMillis) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Timber.d(String.format("Publishing notification (after delay of %d): %s",
                        delayInMillis, queueItem.getLineNotification().getMessage()));
                publish(queueItem.getLineNotification(), queueItem.getNotificationId());
            }
        }, delayInMillis);
    }

    private long calculateDelayInMillis(final String chatId) {
        final long lastDismissedTimestamp = CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP.getOrDefault(chatId, Instant.now()).toEpochMilli(); final long now = Instant.now().toEpochMilli();
        if (lastDismissedTimestamp > now) {
            return lastDismissedTimestamp - now + 1;
        } else {
            return DISMISS_COOL_DOWN_IN_MILLIS;
        }
    }

    private void scheduleSlotCheck(final long delayInMillis) {
        Timber.d("Scheduling slot checks ...");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                republishIfSlotsAvailable();
            }
        }, delayInMillis);
    }

    private void republishIfSlotsAvailable() {
        Timber.d("Running slot checks ...");
        final QueueItem item = QUEUE_ITEMS.peek();
        if (item == null) {
            return;
        }
        if (notificationCounter.hasSlot(item.getLineNotification().getChatId())) {
            QUEUE_ITEMS.remove(item);

            final long delayInMillis = calculateDelayInMillis(item.getLineNotification().getChatId());
            Timber.d(String.format("Scheduled notification publishing (after delay of %d): %s",
                    delayInMillis, item.getLineNotification().getMessage()));
            delayedPublish(item, delayInMillis);

            if (!QUEUE_ITEMS.isEmpty()) {
                scheduleSlotCheck(delayInMillis + NOTIFICATION_CHECK_PERIOD_IN_MILLIS);
            }
        }
    }

    private Optional<QueueItem> getFirstItem() {
        return Optional.ofNullable(QUEUE_ITEMS.poll());
    }

    private void publish(final LineNotification lineNotification, final int notificationId) {
        notificationPublisher.publishNotification(lineNotification, notificationId);
    }

}
