package com.mysticwind.linenotificationsupport.conversationstarter;

import androidx.core.app.Person;

import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ConversationStarterNotificationManager {

    public static final String CONVERSATION_STARTER_CHAT_ID = "CONVERSATION-STARTER-CHAT-ID";

    private final Supplier<NotificationPublisher> notificationPublisherSupplier;
    private final ChatKeywordManager chatKeywordManager;
    private final StartConversationActionBuilder startConversationActionBuilder;
    private final int notificationId;
    private final KeywordSettingActivityLauncher keywordSettingActivityLauncher;

    public ConversationStarterNotificationManager(final Supplier<NotificationPublisher> notificationPublisherSupplier,
                                                  final NotificationIdGenerator notificationIdGenerator,
                                                  final ChatKeywordManager chatKeywordManager,
                                                  final StartConversationActionBuilder startConversationActionBuilder,
                                                  final KeywordSettingActivityLauncher keywordSettingActivityLauncher) {
        this.notificationPublisherSupplier = Objects.requireNonNull(notificationPublisherSupplier);
        Objects.requireNonNull(notificationIdGenerator);
        this.chatKeywordManager = Objects.requireNonNull(chatKeywordManager);
        this.startConversationActionBuilder = Objects.requireNonNull(startConversationActionBuilder);
        this.notificationId = notificationIdGenerator.getNextNotificationId();
        this.keywordSettingActivityLauncher = Objects.requireNonNull(keywordSettingActivityLauncher);
    }

    public Set<String> publishNotification() {
        final List<KeywordEntry> keywordEntryList = chatKeywordManager.getAvailableKeywordToChatNameMap();
        // don't create the notification for starting conversation if there are no available keywords
        if (keywordEntryList.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        final List<String> messages = keywordEntryList.stream()
                .map(entry ->
                        String.format("Start a conversation for chat [%s] with keyword [%s]", entry.getChatName(), entry.getKeyword()))
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
                        .clickIntent(keywordSettingActivityLauncher.buildPendingIntent())
                        .build(),
                notificationId);
        return keywordEntryList.stream()
                .map(entry -> entry.getChatId())
                .collect(Collectors.toSet());
    }

}
