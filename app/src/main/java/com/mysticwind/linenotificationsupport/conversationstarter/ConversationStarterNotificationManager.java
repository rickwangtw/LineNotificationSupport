package com.mysticwind.linenotificationsupport.conversationstarter;

import androidx.core.app.Person;

import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConversationStarterNotificationManager {

    public static final String CONVERSATION_STARTER_CHAT_ID = "CONVERSATION-STARTER-CHAT-ID";

    private final Supplier<NotificationPublisher> notificationPublisherSupplier;
    private final ChatKeywordDao chatKeywordDao;
    private final StartConversationActionBuilder startConversationActionBuilder;
    private final int notificationId;

    public ConversationStarterNotificationManager(final Supplier<NotificationPublisher> notificationPublisherSupplier,
                                                  final NotificationIdGenerator notificationIdGenerator,
                                                  final ChatKeywordDao chatKeywordDao,
                                                  final StartConversationActionBuilder startConversationActionBuilder) {
        this.notificationPublisherSupplier = Objects.requireNonNull(notificationPublisherSupplier);
        Objects.requireNonNull(notificationIdGenerator);
        this.chatKeywordDao = Objects.requireNonNull(chatKeywordDao);
        this.startConversationActionBuilder = Objects.requireNonNull(startConversationActionBuilder);
        this.notificationId = notificationIdGenerator.getNextNotificationId();
    }

    public void publishNotification() {
        if (!shouldPublishNotification()) {
            return;
        }

        final List<String> messages = chatKeywordDao.getKeywordToChatNameMap().entrySet().stream()
                .map(entry ->
                        String.format("Start a conversation for chat [%s] with keyword [%s]", entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());

        // let's see if we get bitten by building a fake LineNotification
        notificationPublisherSupplier.get().publishNotification(
                LineNotification.builder()
                        // TODO localization
                        .title("Start a conversation")
                        .messages(messages)
                        .timestamp(Instant.now().toEpochMilli())
                        .isSelfResponse(true)
                        .chatId(CONVERSATION_STARTER_CHAT_ID)
                        .sender(new Person.Builder()
                                .setName("Bot")
                                .build())
                        .action(startConversationActionBuilder.buildAction())
                        .build(),
                notificationId);
    }

    // TODO
    private boolean shouldPublishNotification() {
        return true;
    }

}
