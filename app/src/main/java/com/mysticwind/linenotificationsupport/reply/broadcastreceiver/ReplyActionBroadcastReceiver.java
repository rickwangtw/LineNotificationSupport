package com.mysticwind.linenotificationsupport.reply.broadcastreceiver;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.notification.AndroidNotificationManager;
import com.mysticwind.linenotificationsupport.notification.NotificationFilterStrategy;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisherFactory;
import com.mysticwind.linenotificationsupport.reply.DefaultReplyActionBuilder;
import com.mysticwind.linenotificationsupport.reply.LineRemoteInputReplier;
import com.mysticwind.linenotificationsupport.reply.MyPersonLabelProvider;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

// TODO merge duplicated code with StartConversationBroadcastReceiver
@AndroidEntryPoint
public class ReplyActionBroadcastReceiver extends BroadcastReceiver {

    @Inject
    LineRemoteInputReplier lineRemoteInputReplier;

    @Inject
    ChatNameManager chatNameManager;

    @Inject
    MyPersonLabelProvider myPersonLabelProvider;

    @Inject
    NotificationPublisherFactory notificationPublisherFactory;

    @Inject
    NotificationIdGenerator notificationIdGenerator;

    @Inject
    AndroidNotificationManager androidNotificationManager;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (DefaultReplyActionBuilder.REPLY_MESSAGE_ACTION.equals(action)){

            final Optional<String> responseMessage = getResponseMessage(intent);
            final Optional<Notification.Action> lineReplyAction = getLineReplyAction(intent);
            Timber.i("Received reply action with response [%s] and line reply action [%s]",
                    responseMessage, lineReplyAction);

            if (responseMessage.isPresent() && lineReplyAction.isPresent()) {
                lineRemoteInputReplier.sendReply(lineReplyAction.get(), responseMessage.get());
                updateNotification(intent, responseMessage.get());
            }
        }
    }

    private Optional<String> getResponseMessage(final Intent intent) {
        final Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput == null) {
            Timber.d("ReplyActionBroadcastReceiver: Null RemoteInput");
            return Optional.empty();
        }
        final CharSequence response = remoteInput.getCharSequence(DefaultReplyActionBuilder.RESPONSE_REMOTE_INPUT_KEY);
        if (response == null) {
            Timber.d("ReplyActionBroadcastReceiver: Null response from %s", DefaultReplyActionBuilder.RESPONSE_REMOTE_INPUT_KEY);
            return Optional.empty();
        }
        if (StringUtils.isBlank(response.toString())) {
            Timber.d("ReplyActionBroadcastReceiver: Blank response: [%s]", response.toString());
            return Optional.empty();
        }
        return Optional.of(response.toString());
    }

    private Optional<Notification.Action> getLineReplyAction(final Intent intent) {
        final Notification.Action lineReplyAction = intent.getParcelableExtra(DefaultReplyActionBuilder.LINE_REPLY_ACTION_KEY);
        return Optional.ofNullable(lineReplyAction);
    }

    private void updateNotification(Intent intent, String response) {
        final String chatId = intent.getStringExtra(DefaultReplyActionBuilder.CHAT_ID_KEY);

        final Optional<StatusBarNotification> statusBarNotification = findNotificationOfChatId(chatId);

        if (!statusBarNotification.isPresent()) {
            Timber.e("Cannot find corresponding notification for chat ID [%s]", chatId);
            return;
        }

        final String chatName = chatNameManager.getChatName(chatId);

        final LineNotification responseLineNotification = LineNotification.builder()
                .lineMessageId(String.valueOf(Instant.now().toEpochMilli())) // just generate a fake one
                .title(chatName)
                .message(response)
                .sender(myPersonLabelProvider.getMyPerson())
                .chatId(chatId)
                .timestamp(Instant.now().toEpochMilli())
                .actions(ImmutableList.copyOf(statusBarNotification.get().getNotification().actions))
                .isSelfResponse(true)
                .build();

        notificationPublisherFactory.get().publishNotification(responseLineNotification, notificationIdGenerator.getNextNotificationId());
    }

    private Optional<StatusBarNotification> findNotificationOfChatId(final String chatId) {
        return androidNotificationManager.getOrderedLineNotificationSupportNotificationsOfChatId(chatId, NotificationFilterStrategy.EXCLUDE_SUMMARY).stream()
                .max(Comparator.comparing(notification -> notification.getNotification().when));
    }

}