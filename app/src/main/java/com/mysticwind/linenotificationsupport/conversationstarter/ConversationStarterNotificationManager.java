package com.mysticwind.linenotificationsupport.conversationstarter;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.conversationstarter.model.KeywordEntry;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notification.AndroidNotificationManager;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisherFactory;
import com.mysticwind.linenotificationsupport.reply.MyPersonLabelProvider;
import com.mysticwind.linenotificationsupport.ui.LocalizationDao;
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

    private final NotificationPublisherFactory notificationPublisherFactory;
    private final ChatKeywordManager chatKeywordManager;
    private final StartConversationActionBuilder startConversationActionBuilder;
    private final int notificationId;
    private final KeywordSettingActivityLauncher keywordSettingActivityLauncher;
    private final MyPersonLabelProvider myPersonLabelProvider;
    private final AndroidNotificationManager androidNotificationManager;
    private final LocalizationDao localizationDao;

    @Inject
    public ConversationStarterNotificationManager(final NotificationPublisherFactory notificationPublisherFactory,
                                                  final NotificationIdGenerator notificationIdGenerator,
                                                  final ChatKeywordManager chatKeywordManager,
                                                  final StartConversationActionBuilder startConversationActionBuilder,
                                                  final KeywordSettingActivityLauncher keywordSettingActivityLauncher,
                                                  final MyPersonLabelProvider myPersonLabelProvider,
                                                  final AndroidNotificationManager androidNotificationManager,
                                                  final LocalizationDao localizationDao) {
        this.notificationPublisherFactory = Objects.requireNonNull(notificationPublisherFactory);
        Objects.requireNonNull(notificationIdGenerator);
        this.chatKeywordManager = Objects.requireNonNull(chatKeywordManager);
        this.startConversationActionBuilder = Objects.requireNonNull(startConversationActionBuilder);
        this.notificationId = notificationIdGenerator.getNextNotificationId();
        this.keywordSettingActivityLauncher = Objects.requireNonNull(keywordSettingActivityLauncher);
        this.myPersonLabelProvider = Objects.requireNonNull(myPersonLabelProvider);
        this.androidNotificationManager = Objects.requireNonNull(androidNotificationManager);
        this.localizationDao = Objects.requireNonNull(localizationDao);
    }

    public Set<String> publishNotification() {
        final List<KeywordEntry> keywordEntryList = chatKeywordManager.getAvailableKeywordToChatNameMap();

        final List<String> messages = resolveMessages(keywordEntryList);

        // let's see if we get bitten by building a fake LineNotification
        notificationPublisherFactory.get().publishNotification(
                LineNotification.builder()
                        .title(localizationDao.getLocalizedString(R.string.conversation_start_notification_title))
                        .messages(messages)
                        .timestamp(Instant.now().toEpochMilli())
                        .isSelfResponse(true)
                        .chatId(CONVERSATION_STARTER_CHAT_ID)
                        .sender(myPersonLabelProvider.getMyPerson())
                        .actions(startConversationActionBuilder.buildActions())
                        .clickIntent(keywordSettingActivityLauncher.buildPendingIntent())
                        .build(),
                notificationId);
        return keywordEntryList.stream()
                .filter(keywordEntry -> keywordEntry.isHasReplyAction())
                .filter(keywordEntry -> keywordEntry.getKeyword().isPresent())
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
                            String.format("%s -> %s", entry.getKeyword().get(), entry.getChatName()))
                    .reduce((string1, string2) -> String.format("%s\n%s", string1, string2))
                    .orElse("");
            final KeywordEntry firstKeywordEntry = availableKeywordEntries.get(0);
            final String sampleMessage = localizationDao.getLocalizedString(R.string.conversation_start_notification_content_sample_message);
            final String sampleMessageWithKeyword = firstKeywordEntry.getKeyword().get() + " " + sampleMessage;
            return ImmutableList.of(
                    localizationDao.getLocalizedString(R.string.conversation_start_notification_content_guidance,
                            sampleMessageWithKeyword, firstKeywordEntry.getChatName(), sampleMessage),
                    localizationDao.getLocalizedString(R.string.conversation_start_notification_content_list_of_chat_prefix) + availableKeywordMessage
            );
        }

        if (!keywordEntryList.isEmpty()) {
            return ImmutableList.of(
                    localizationDao.getLocalizedString(R.string.conversation_start_notification_content_no_reply_action)
            );
        }

        return ImmutableList.of(
                localizationDao.getLocalizedString(R.string.conversation_start_notification_content_no_keywords)
        );
    }

    public void cancelNotification() {
        androidNotificationManager.cancelNotification(CONVERSATION_STARTER_CHAT_ID);
    }

}
