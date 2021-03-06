package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.NotificationHistoryEntry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HistoryProvidingNotificationPublisherDecorator implements NotificationPublisher {

    private final Multimap<String, NotificationHistoryEntry> chatIdToHistoryMap =
            Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final Map<String, Integer> chatIdToNotificationIdMap = new ConcurrentHashMap<>();

    private final NotificationPublisher notificationPublisher;

    public HistoryProvidingNotificationPublisherDecorator(final NotificationPublisher notificationPublisher) {
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher);
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {
        final List<NotificationHistoryEntry> history = chatIdToHistoryMap.get(lineNotification.getChatId()).stream()
                .sorted((entry1, entry2) -> (int)(entry1.getTimestamp() - entry2.getTimestamp()))
                .collect(Collectors.toList());


        // add new entry into history
        chatIdToHistoryMap.put(lineNotification.getChatId(),
                new NotificationHistoryEntry(
                        lineNotification.getMessage(),
                        lineNotification.getSender(),
                        lineNotification.getTimestamp(),
                        lineNotification.getLineStickerUrl())
        );

        int selectedNotificationId =
                chatIdToNotificationIdMap.computeIfAbsent(lineNotification.getChatId(), chatId -> notificationId);

        this.notificationPublisher.publishNotification(
                lineNotification.toBuilder()
                        .history(history)
                        .build(),
                selectedNotificationId);
    }

    @Override
    public void republishNotification(LineNotification lineNotification, int notificationId) {
        // do nothing
        notificationPublisher.republishNotification(lineNotification, notificationId);
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        // clean cache
        chatIdToHistoryMap.removeAll(statusBarNotification.getNotification().getGroup());

        notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }

}
