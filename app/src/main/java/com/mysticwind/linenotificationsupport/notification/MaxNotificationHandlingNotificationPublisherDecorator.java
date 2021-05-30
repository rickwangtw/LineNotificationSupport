package com.mysticwind.linenotificationsupport.notification;

import android.os.Handler;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import lombok.Value;
import timber.log.Timber;

public class MaxNotificationHandlingNotificationPublisherDecorator implements NotificationPublisher {

    // without the cool down, messages may not get sent if messages of the same group was just dismissed
    private static final long DISMISS_COOL_DOWN_IN_MILLIS = 500L;
    private static final long NOTIFICATION_CHECK_PERIOD_IN_MILLIS = 500L;

    // tracks the messages that has not been sent
    private static final ConcurrentLinkedDeque<QueueItem> QUEUE_ITEMS = new ConcurrentLinkedDeque<>();

    // this is to make sure we have sufficient cool down for each chat after it being dismissed
    private static final Map<String, Instant> GROUP_TO_LAST_DISMISSED_INSTANT_MAP = new ConcurrentHashMap<>();

    private final Handler handler;
    private final NotificationPublisher notificationPublisher;
    private final SlotAvailabilityChecker slotAvailabilityChecker;

    private Runnable republishEventsIfSlotsAvailableRunnable =
            new Runnable() {
                @Override
                public void run() {
                    republishIfSlotsAvailable();
                }
            };

    // TODO should we just have this class implemented and shared??
    @Value
    class QueueItem {
        private final LineNotification lineNotification;
        private final int notificationId;
    }

    public MaxNotificationHandlingNotificationPublisherDecorator(final Handler handler,
                                                                 final NotificationPublisher notificationPublisher,
                                                                 final SlotAvailabilityChecker slotAvailabilityChecker) {
        this.handler = handler;
        this.notificationPublisher = notificationPublisher;
        this.slotAvailabilityChecker = slotAvailabilityChecker;
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        Objects.requireNonNull(lineNotification);

        publishNotification(lineNotification, notificationId, item -> QUEUE_ITEMS.add(item));
    }

    @Override
    public void republishNotification(final LineNotification lineNotification, final int notificationId) {
        Objects.requireNonNull(lineNotification);

        publishNotification(lineNotification, notificationId, item -> QUEUE_ITEMS.addFirst(item));
    }

    public void publishNotification(final LineNotification lineNotification, final int notificationId, Consumer<QueueItem> itemAddingFunction) {
        if (!slotAvailabilityChecker.hasSlot(lineNotification.getChatId())) {
            Timber.d("Reached maximum notifications, add to queue: " + notificationId);
            QUEUE_ITEMS.add(new QueueItem(lineNotification, notificationId));
            itemAddingFunction.accept(new QueueItem(lineNotification, notificationId));
            return;
        }
        if (QUEUE_ITEMS.isEmpty()) {
            Timber.d("Publish new notification: " + notificationId);
            publish(lineNotification, notificationId);
            return;
        }
        itemAddingFunction.accept(new QueueItem(lineNotification, notificationId));

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
        // when should this actually happen??
        notificationPublisher.updateNotificationDismissed(statusBarNotification);

        final String group = statusBarNotification.getNotification().getGroup();

        if (!slotAvailabilityChecker.hasSlot(group)) {
            return;
        }

        Optional<QueueItem> firstItem = getFirstItem();
        if (!firstItem.isPresent()) {
            GROUP_TO_LAST_DISMISSED_INSTANT_MAP.put(group, Instant.now());
            return;
        }
        final long delayInMillis = calculateDelayInMillis(firstItem.get().getLineNotification().getChatId());
        delayedPublish(firstItem.get(), delayInMillis);
        GROUP_TO_LAST_DISMISSED_INSTANT_MAP.put(group, Instant.now().plusMillis(delayInMillis));

        // TODO why are there cases where there are available slots but the queue still has items???
        if (!QUEUE_ITEMS.isEmpty()) {
            scheduleSlotCheck(delayInMillis + NOTIFICATION_CHECK_PERIOD_IN_MILLIS);
        }
    }

    private void delayedPublish(final QueueItem queueItem, final long delayInMillis) {
        Timber.d("Scheduling a delayed publish for item: " + queueItem.getLineNotification().getMessage());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Timber.d("Publishing notification (after delay of %d): %s",
                        delayInMillis, queueItem.getLineNotification().getMessage());
                publish(queueItem.getLineNotification(), queueItem.getNotificationId());
            }
        }, delayInMillis);
    }

    private long calculateDelayInMillis(final String chatId) {
        final long lastDismissedTimestamp = GROUP_TO_LAST_DISMISSED_INSTANT_MAP.getOrDefault(chatId, Instant.now()).toEpochMilli();
        final long now = Instant.now().toEpochMilli();
        if (lastDismissedTimestamp > now) {
            return lastDismissedTimestamp - now + 1;
        } else {
            return DISMISS_COOL_DOWN_IN_MILLIS;
        }
    }

    private void scheduleSlotCheck(final long delayInMillis) {
        Timber.d("Cancelling scheduled slot checks ...");
        handler.removeCallbacks(republishEventsIfSlotsAvailableRunnable);

        Timber.d("Scheduling slot checks ...");
        handler.postDelayed(republishEventsIfSlotsAvailableRunnable, delayInMillis);
    }

    private void republishIfSlotsAvailable() {
        Timber.d("Running slot checks ...");
        final QueueItem item = QUEUE_ITEMS.peek();
        if (item == null) {
            return;
        }
        if (slotAvailabilityChecker.hasSlot(item.getLineNotification().getChatId())) {
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
