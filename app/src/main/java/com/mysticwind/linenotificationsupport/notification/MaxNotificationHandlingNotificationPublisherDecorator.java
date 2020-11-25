package com.mysticwind.linenotificationsupport.notification;

import android.app.NotificationManager;
import android.os.Handler;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.mysticwind.linenotificationsupport.log.TagBuilder;
import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.Value;

public class MaxNotificationHandlingNotificationPublisherDecorator implements NotificationPublisher {

    private static final String TAG = TagBuilder.build(MaxNotificationHandlingNotificationPublisherDecorator.class);

    // without the cool down, messages may not get sent if messages of the same group was just dismissed
    private static final long DISMISS_COOL_DOWN_IN_MILLIS = 500L;

    // tracks the messages that has not been sent
    private static final ConcurrentLinkedQueue<QueueItem> QUEUE_ITEMS = new ConcurrentLinkedQueue<>();

    // this is to make sure we have sufficient cool down for each chat after it being dismissed
    private static final Map<String, Instant> CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP = new ConcurrentHashMap<>();

    private final long maxNotificationsPerApp;
    private final NotificationManager notificationManager;
    private final Handler handler;
    private final NotificationPublisher notificationPublisher;
    private final String packageName;

    // TODO should we just have this class implemented and shared??
    @Value
    class QueueItem {
        private final LineNotification lineNotification;
        private final int notificationId;
    }

    public MaxNotificationHandlingNotificationPublisherDecorator(
            final long maxNotificationsPerApp,
            final NotificationManager notificationManager,
            final Handler handler,
            final NotificationPublisher notificationPublisher,
            final String packageName) {
        this.maxNotificationsPerApp = maxNotificationsPerApp - 1; // leave one for group as a buffer
        this.notificationManager = notificationManager;
        this.handler = handler;
        this.notificationPublisher = notificationPublisher;
        this.packageName = packageName;
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        final long remainingSlots = getRemainingSlots();
        if (remainingSlots <= 0) {
            Log.d(TAG, "Reached maximum notifications, add to queue: " + notificationId);
            QUEUE_ITEMS.add(new QueueItem(lineNotification, notificationId));
            return;
        }
        if (QUEUE_ITEMS.isEmpty()) {
            Log.d(TAG, "Publish new notification: " + notificationId);
            publish(lineNotification, notificationId);
            return;
        }
        QUEUE_ITEMS.add(new QueueItem(lineNotification, notificationId));

        for (int slotIndex = 0 ; slotIndex < remainingSlots ; ++slotIndex) {
            final Optional<QueueItem> firstItem = getFirstItem();
            if (!firstItem.isPresent()) {
                return;
            }
            firstItem.ifPresent(item -> {
                        Log.d(TAG, "Publish previously queued notification: " + item.getNotificationId());
                        publish(item.getLineNotification(), item.getNotificationId());
                    }
            );
        }
    }

    private long getRemainingSlots() {
        final long numberOfActiveNotifications = Arrays.stream(notificationManager.getActiveNotifications())
                .filter(notification -> notification.getPackageName().equals(packageName))
                .count();
        Log.d(TAG, "Number of active notifications: " + numberOfActiveNotifications);
        return maxNotificationsPerApp - numberOfActiveNotifications;
    }

    @Override
    public void updateNotificationDismissed(final StatusBarNotification statusBarNotification) {
        if (!StringUtils.equals(packageName, statusBarNotification.getPackageName())) {
            return;
        }
        if (isSummary(statusBarNotification)) {
            return;
        }
        final String chatId = statusBarNotification.getNotification().getGroup();
        Optional<QueueItem> firstItem = getFirstItem();
        if (!firstItem.isPresent()) {
            CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP.put(chatId, Instant.now());
            return;
        }
        final long delayInMillis = calculateDelayInMillis(firstItem.get().getLineNotification().getChatId());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, String.format("Publishing notification (after delay of %d): %s",
                        delayInMillis, firstItem.get().getLineNotification().getMessage()));
                publish(firstItem.get().getLineNotification(), firstItem.get().getNotificationId());
            }
        }, delayInMillis);
        CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP.put(chatId, Instant.now().plusMillis(delayInMillis));
    }

    // TODO deal with duplicated code
    private boolean isSummary(final StatusBarNotification statusBarNotification) {
        final String summaryText = statusBarNotification.getNotification().extras
                .getString("android.summaryText");
        return StringUtils.isNotBlank(summaryText);
    }

    private long calculateDelayInMillis(final String chatId) {
        final long lastDismissedTimestamp = CHAT_ID_TO_LAST_DISMISSED_INSTANT_MAP.getOrDefault(chatId, Instant.now()).toEpochMilli();
        final long now = Instant.now().toEpochMilli();
        if (lastDismissedTimestamp > now) {
            return lastDismissedTimestamp - now + 1;
        } else {
            return DISMISS_COOL_DOWN_IN_MILLIS;
        }
    }

    private Optional<QueueItem> getFirstItem() {
        return Optional.ofNullable(QUEUE_ITEMS.poll());
    }

    private void publish(final LineNotification lineNotification, final int notificationId) {
        notificationPublisher.publishNotification(lineNotification, notificationId);
    }

}
