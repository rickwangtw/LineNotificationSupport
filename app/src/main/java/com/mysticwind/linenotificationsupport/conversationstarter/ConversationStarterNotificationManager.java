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
    private final ChatKeywordManager chatKeywordManager;
    private final StartConversationActionBuilder startConversationActionBuilder;
    private final int notificationId;

    public ConversationStarterNotificationManager(final Supplier<NotificationPublisher> notificationPublisherSupplier,
                                                  final NotificationIdGenerator notificationIdGenerator,
                                                  final ChatKeywordManager chatKeywordManager,
                                                  final StartConversationActionBuilder startConversationActionBuilder) {
        this.notificationPublisherSupplier = Objects.requireNonNull(notificationPublisherSupplier);
        Objects.requireNonNull(notificationIdGenerator);
        this.chatKeywordManager = Objects.requireNonNull(chatKeywordManager);
        this.startConversationActionBuilder = Objects.requireNonNull(startConversationActionBuilder);
        this.notificationId = notificationIdGenerator.getNextNotificationId();
    }

    public void publishNotification() {
        final List<String> messages = chatKeywordManager.getAvailableKeywordToChatNameMap().entrySet().stream()
                .map(entry ->
                        String.format("Start a conversation for chat [%s] with keyword [%s]", entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());

        // don't create the notification for starting conversation if there are no available keywords
        if (messages.isEmpty()) {
            return;
        }

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

}
