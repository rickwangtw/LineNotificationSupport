package com.mysticwind.linenotificationsupport.conversationstarter.broadcastreceiver;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mysticwind.linenotificationsupport.conversationstarter.ChatKeywordDao;
import com.mysticwind.linenotificationsupport.conversationstarter.LineReplyActionDao;
import com.mysticwind.linenotificationsupport.conversationstarter.StartConversationActionBuilder;
import com.mysticwind.linenotificationsupport.reply.LineRemoteInputReplier;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.Value;
import timber.log.Timber;

public class StartConversationBroadcastReceiver extends BroadcastReceiver {

    private final LineRemoteInputReplier lineRemoteInputReplier;
    private final ChatKeywordDao chatKeywordDao;
    private final LineReplyActionDao lineReplyActionDao;

    public StartConversationBroadcastReceiver(final LineRemoteInputReplier lineRemoteInputReplier,
                                              final ChatKeywordDao chatKeywordDao,
                                              final LineReplyActionDao lineReplyActionDao) {
        this.lineRemoteInputReplier = Objects.requireNonNull(lineRemoteInputReplier);
        this.chatKeywordDao = Objects.requireNonNull(chatKeywordDao);
        this.lineReplyActionDao = Objects.requireNonNull(lineReplyActionDao);
    }

    @Value
    class ChatIdAndMessage {
        private final String chatId;
        private final String message;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

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

            // TODO update the existing notification after conversations are started
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

}
