package com.mysticwind.linenotificationsupport.notification;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.NotificationHistoryEntry;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import timber.log.Timber;

public class HistoryProvidingNotificationPublisherDecorator implements NotificationPublisher {

    private final Multimap<String, NotificationHistoryEntry> chatIdToHistoryMap =
            Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final Map<Integer, String> notificationToChatIdMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> fallbackChatIdToNotificationIdMap = new ConcurrentHashMap<>();

    private final NotificationPublisher notificationPublisher;

    public HistoryProvidingNotificationPublisherDecorator(final NotificationPublisher notificationPublisher) {
        this.notificationPublisher = Objects.requireNonNull(notificationPublisher);
    }

    @Override
    public void publishNotification(final LineNotification lineNotification, final int notificationId) {

        insertOrUpdateHistory(lineNotification);

        final List<NotificationHistoryEntry> history = chatIdToHistoryMap.get(lineNotification.getChatId()).stream()
                .sorted((entry1, entry2) -> (int)(entry1.getTimestamp() - entry2.getTimestamp()))
                .collect(Collectors.toList());

        final NotificationHistoryEntry lastNotificationEntry = history.remove(history.size() - 1);

        int selectedNotificationId = resolveNotificationId(lineNotification.getChatId(), notificationId);

        Timber.d("Publishing notification with history: id [%s] chat ID [%s] history size [%d] latest message [%s]",
                selectedNotificationId,
                lineNotification.getChatId(),
                history.size(),
                lastNotificationEntry.getMessage());

        this.notificationPublisher.publishNotification(
                lineNotification.toBuilder()
                        .lineMessageId(lastNotificationEntry.getLineMessageId())
                        .message(lastNotificationEntry.getMessage())
                        .sender(lastNotificationEntry.getSender())
                        .timestamp(lastNotificationEntry.getTimestamp())
                        .lineStickerUrl(lastNotificationEntry.getLineStickerUrl().orElse(null))
                        .history(history)
                        .build(),
                selectedNotificationId);
    }

    private int resolveNotificationId(final String chatId, final int notificationId) {
        int hashCode = chatId.hashCode();
        final String storedChatId = notificationToChatIdMap.get(hashCode);
        if (storedChatId == null) {
            notificationToChatIdMap.put(hashCode, chatId);
            return hashCode;
        } else if (chatId.equals(storedChatId)) {
            return hashCode;
        } else {
            // fallback that should almost never happen
            final int selectedNotificationId = fallbackChatIdToNotificationIdMap.computeIfAbsent(chatId, id -> notificationId);
            // TODO what if there is a clash with the notification IDs and the hash codes?
            Timber.w("Chat ID [%s] hash [%d] has been used, using notification ID [%d] instead",
                    chatId, hashCode, selectedNotificationId);
            return selectedNotificationId;
        }
    }

    private void insertOrUpdateHistory(LineNotification lineNotification) {
        final Collection<NotificationHistoryEntry> history = chatIdToHistoryMap.get(lineNotification.getChatId());
        // remove existing entry for the "New Message" use case
        history.stream()
                .filter(entry -> StringUtils.equals(entry.getLineMessageId(), lineNotification.getLineMessageId()))
                .findAny()
                .ifPresent(entry ->
                        chatIdToHistoryMap.remove(lineNotification.getChatId(), entry)
                );

        // add new entry into history
        chatIdToHistoryMap.put(lineNotification.getChatId(),
                new NotificationHistoryEntry(
                        lineNotification.getLineMessageId(),
                        lineNotification.getMessage(),
                        lineNotification.getSender(),
                        lineNotification.getTimestamp(),
                        lineNotification.getLineStickerUrl()));
    }

    @Override
    public void republishNotification(LineNotification lineNotification, int notificationId) {
        // do nothing
        notificationPublisher.republishNotification(lineNotification, notificationId);
    }

    @Override
    public void updateNotificationDismissed(StatusBarNotification statusBarNotification) {
        final String group = statusBarNotification.getNotification().getGroup();
        // clean cache
        Timber.d("Cleaning notification history with group [%s], number of items [%d]",
                group, chatIdToHistoryMap.get(group).size());
        chatIdToHistoryMap.removeAll(group);

        notificationPublisher.updateNotificationDismissed(statusBarNotification);
    }

}
