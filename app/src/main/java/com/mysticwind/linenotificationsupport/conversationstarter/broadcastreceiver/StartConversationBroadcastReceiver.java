package com.mysticwind.linenotificationsupport.conversationstarter.broadcastreceiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;
import com.mysticwind.linenotificationsupport.conversationstarter.ChatKeywordDao;
import com.mysticwind.linenotificationsupport.conversationstarter.ConversationStarterNotificationManager;
import com.mysticwind.linenotificationsupport.conversationstarter.LineReplyActionDao;
import com.mysticwind.linenotificationsupport.conversationstarter.StartConversationActionBuilder;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisher;
import com.mysticwind.linenotificationsupport.reply.LineRemoteInputReplier;
import com.mysticwind.linenotificationsupport.reply.MyPersonLabelProvider;
import com.mysticwind.linenotificationsupport.reply.ReplyActionBuilder;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import lombok.Value;
import timber.log.Timber;

public class StartConversationBroadcastReceiver extends BroadcastReceiver {

    private final LineRemoteInputReplier lineRemoteInputReplier;
    private final ChatKeywordDao chatKeywordDao;
    private final LineReplyActionDao lineReplyActionDao;
    private final NotificationManager notificationManager;
    private final String packageName;
    private final ChatNameManager chatNameManager;
    private final MyPersonLabelProvider myPersonLabelProvider;
    private final ReplyActionBuilder replyActionBuilder;
    private final Supplier<NotificationPublisher> notificationPublisherSupplier;
    private final NotificationIdGenerator notificationIdGenerator;

    public StartConversationBroadcastReceiver(final LineRemoteInputReplier lineRemoteInputReplier,
                                              final ChatKeywordDao chatKeywordDao,
                                              final LineReplyActionDao lineReplyActionDao,
                                              final NotificationManager notificationManager,
                                              final String packageName,
                                              final ChatNameManager chatNameManager,
                                              final MyPersonLabelProvider myPersonLabelProvider,
                                              final ReplyActionBuilder replyActionBuilder,
                                              final Supplier<NotificationPublisher> notificationPublisherSupplier,
                                              final NotificationIdGenerator notificationIdGenerator) {
        this.lineRemoteInputReplier = Objects.requireNonNull(lineRemoteInputReplier);
        this.chatKeywordDao = Objects.requireNonNull(chatKeywordDao);
        this.lineReplyActionDao = Objects.requireNonNull(lineReplyActionDao);
        this.notificationManager = Objects.requireNonNull(notificationManager);
        this.packageName = Validate.notBlank(packageName);
        this.chatNameManager = Objects.requireNonNull(chatNameManager);
        this.replyActionBuilder = Objects.requireNonNull(replyActionBuilder);
        this.myPersonLabelProvider = Objects.requireNonNull(myPersonLabelProvider);
        this.notificationPublisherSupplier = Objects.requireNonNull(notificationPublisherSupplier);
        this.notificationIdGenerator = Objects.requireNonNull(notificationIdGenerator);
    }

    @Value
    class ChatIdAndMessage {
        private final String chatId;
        private final String message;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        processInput(context, intent);

        clearNotificationSpinner();
    }

    public void processInput(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (!StartConversationActionBuilder.START_CONVERSATION_ACTION.equals(action)) {
            return;
        }

        final Optional<String> messageWithKeyword = getInputMessageWithKeyword(intent);
        if (!messageWithKeyword.isPresent()) {
            return;
        }

        final Optional<ChatIdAndMessage> chatIdAndMessage = resolveChatIdAndMessage(messageWithKeyword.get());
        if (!chatIdAndMessage.isPresent()) {
            Timber.i("Cannot find matching chat ID from message [%s]", messageWithKeyword.get());
            return;
        }

        Timber.d("Resolved chat ID [%s] and message [%s]", chatIdAndMessage.get().getChatId(), chatIdAndMessage.get().getMessage());

        final Optional<Notification.Action> lineReplyAction = getLineReplyAction(chatIdAndMessage.get().getChatId());
        if (!lineReplyAction.isPresent()) {
            Timber.i("Cannot find matching Line Reply Action: chat ID [%s] message [%s]", chatIdAndMessage.get().getChatId(), messageWithKeyword.get());
            return;
        }

        Timber.i("Received start conversation action with message [%s]", messageWithKeyword);

        if (messageWithKeyword.isPresent() && lineReplyAction.isPresent()) {
            lineRemoteInputReplier.sendReply(lineReplyAction.get(), chatIdAndMessage.get().getMessage());

            generateNewConversationNotification(context, chatIdAndMessage.get().getChatId(), chatIdAndMessage.get().getMessage());
        }
    }

    private Optional<String> getInputMessageWithKeyword(final Intent intent) {
        final Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput == null) {
            Timber.d("Null RemoteInput");
            return Optional.empty();
        }
        final CharSequence messageWithKeyword = remoteInput.getCharSequence(StartConversationActionBuilder.MESSAGE_REMOTE_INPUT_KEY);
        if (messageWithKeyword == null) {
            Timber.d("Null message from %s", StartConversationActionBuilder.MESSAGE_REMOTE_INPUT_KEY);
            return Optional.empty();
        }
        if (StringUtils.isBlank(messageWithKeyword.toString())) {
            Timber.d("Blank message: [%s]", messageWithKeyword.toString());
            return Optional.empty();
        }
        return Optional.of(messageWithKeyword.toString());
    }

    private Optional<ChatIdAndMessage> resolveChatIdAndMessage(final String messageWithKeyword) {
        final Set<String> keywords = chatKeywordDao.getKeywords();
        final Optional<String> matchingKeyword = keywords.stream()
                .filter(keyword -> messageWithKeyword.startsWith(keyword))
                .findFirst();
        if (!matchingKeyword.isPresent()) {
            return Optional.empty();
        }
        Optional<String> chatId = chatKeywordDao.getChatId(matchingKeyword.get());
        if (!chatId.isPresent()) {
            return Optional.empty();
        }
        final String messageWithoutKeyword = messageWithKeyword.replaceFirst(matchingKeyword.get(), "").trim();
        if (StringUtils.isBlank(messageWithoutKeyword)) {
            Timber.i("Blank message after removing keyword [%s]", messageWithKeyword);
            return Optional.empty();
        }
        return Optional.of(new ChatIdAndMessage(chatId.get(), messageWithoutKeyword));
    }

    private Optional<Notification.Action> getLineReplyAction(final String chatId) {
        return lineReplyActionDao.getLineReplyAction(chatId);
    }

    private void generateNewConversationNotification(final Context context, final String chatId, final String message) {
        final String chatName = chatNameManager.getChatName(chatId);
        final String myPersonLabel = myPersonLabelProvider.getMyPersonLabel().get();
        final Notification.Action lineReplyAction = getLineReplyAction(chatId).get();
        final LineNotification lineNotification = LineNotification.builder()
                .lineMessageId(String.valueOf(Instant.now().toEpochMilli())) // just generate a fake one
                .title(chatName)
                .message(message)
                .sender(new Person.Builder()
                        .setName(myPersonLabel)
                        .setIcon(IconCompat.createWithResource(context, R.drawable.outline_person_24))
                        .build())
                .chatId(chatId)
                .timestamp(Instant.now().toEpochMilli())
                .actions(ImmutableList.of(replyActionBuilder.buildReplyAction(chatId, lineReplyAction)))
                .isSelfResponse(true)
                .build();

        notificationPublisherSupplier.get().publishNotification(lineNotification, notificationIdGenerator.getNextNotificationId());
    }

    private void clearNotificationSpinner() {
        final Optional<StatusBarNotification> statusBarNotification = Arrays.stream(notificationManager.getActiveNotifications())
                .filter(notification -> packageName.equals(notification.getPackageName()))
                .filter(notification -> ConversationStarterNotificationManager.CONVERSATION_STARTER_CHAT_ID.equals(
                        NotificationExtractor.getLineNotificationSupportChatId(notification.getNotification()).orElse(null)))
                .findFirst();
        statusBarNotification.ifPresent(notification -> {
                    Timber.d("Clear notification spinner with ID [%d]", notification.getId());
                    notificationManager.notify(notification.getId(), notification.getNotification());
                }
        );
    }

}
