package com.mysticwind.linenotificationsupport.conversationstarter;

import androidx.core.app.Person;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notification.AndroidNotificationManager;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisherFactory;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ConversationStarterNotificationManager {

    public static final String CONVERSATION_STARTER_CHAT_ID = "CONVERSATION-STARTER-CHAT-ID";

    private static final String SAMPLE_MESSAGE = "Hello!";

    private final NotificationPublisherFactory notificationPublisherFactory;
    private final ChatKeywordManager chatKeywordManager;
    private final StartConversationActionBuilder startConversationActionBuilder;
    private final int notificationId;
    private final KeywordSettingActivityLauncher keywordSettingActivityLauncher;
    private final AndroidNotificationManager androidNotificationManager;

    @Inject
    public ConversationStarterNotificationManager(final NotificationPublisherFactory notificationPublisherFactory,
                                                  final NotificationIdGenerator notificationIdGenerator,
                                                  final ChatKeywordManager chatKeywordManager,
                                                  final StartConversationActionBuilder startConversationActionBuilder,
                                                  final KeywordSettingActivityLauncher keywordSettingActivityLauncher,
                                                  final AndroidNotificationManager androidNotificationManager) {
        this.notificationPublisherFactory = Objects.requireNonNull(notificationPublisherFactory);
        Objects.requireNonNull(notificationIdGenerator);
        this.chatKeywordManager = Objects.requireNonNull(chatKeywordManager);
        this.startConversationActionBuilder = Objects.requireNonNull(startConversationActionBuilder);
        this.notificationId = notificationIdGenerator.getNextNotificationId();
        this.keywordSettingActivityLauncher = Objects.requireNonNull(keywordSettingActivityLauncher);
        this.androidNotificationManager = Objects.requireNonNull(androidNotificationManager);
    }

    public Set<String> publishNotification() {
        final List<KeywordEntry> keywordEntryList = chatKeywordManager.getAvailableKeywordToChatNameMap();

        final List<String> messages = resolveMessages(keywordEntryList);

        // let's see if we get bitten by building a fake LineNotification
        notificationPublisherFactory.get().publishNotification(
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

    private List<String> resolveMessages(final List<KeywordEntry> keywordEntryList) {

        final List<KeywordEntry> availableKeywordEntries = keywordEntryList.stream()
                .filter(keywordEntry -> keywordEntry.isHasReplyAction())
                .filter(keywordEntry -> keywordEntry.getKeyword().isPresent())
                .collect(Collectors.toList());

        if (!availableKeywordEntries.isEmpty()) {
            final String availableKeywordMessage = availableKeywordEntries.stream()
                    .map(entry ->
                            String.format("%s -> %s", entry.getChatName(), entry.getKeyword().get()))
                    .reduce((string1, string2) -> String.format("%s\n%s", string1, string2))
                    .orElse("");
            final KeywordEntry firstKeywordEntry = availableKeywordEntries.get(0);
            return ImmutableList.of(
                    // TODO localization
                    String.format("Start a conversation by starting your message with the keyword, for example, send \"%s\" to start a conversation for chat \"%s\" with message \"%s\"",
                            firstKeywordEntry.getKeyword().get() + " " + SAMPLE_MESSAGE, firstKeywordEntry.getChatName(), SAMPLE_MESSAGE),
                    "This is a list of available chats and keywords:\n" + availableKeywordMessage
            );
        }

        if (!keywordEntryList.isEmpty()) {
            return ImmutableList.of(
                    // TODO localization
                    "No messages from LINE received at the moment and you cannot start a conversation right now. A list of available chat will be shown here once you get LINE messages."
            );
        }

        return ImmutableList.of(
                // TODO localization
                "You have not configured any keywords to start conversations yet. Click me to configure keywords."
        );
    }

    public void cancelNotification() {
        androidNotificationManager.cancelNotification(CONVERSATION_STARTER_CHAT_ID);
    }

}
