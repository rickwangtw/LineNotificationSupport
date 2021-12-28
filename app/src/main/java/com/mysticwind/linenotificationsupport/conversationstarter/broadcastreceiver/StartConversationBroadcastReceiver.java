package com.mysticwind.linenotificationsupport.conversationstarter.broadcastreceiver;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mysticwind.linenotificationsupport.conversationstarter.StartConversationActionBuilder;
import com.mysticwind.linenotificationsupport.reply.LineRemoteInputReplier;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

import timber.log.Timber;

public class StartConversationBroadcastReceiver extends BroadcastReceiver {

    private final LineRemoteInputReplier lineRemoteInputReplier;

    public StartConversationBroadcastReceiver(LineRemoteInputReplier lineRemoteInputReplier) {
        this.lineRemoteInputReplier = Objects.requireNonNull(lineRemoteInputReplier);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {

        final String action = intent.getAction();
        if (StartConversationActionBuilder.START_CONVERSATION_ACTION.equals(action)){

            final Optional<String> messageWithKeyword = getInputMessageWithKeyword(intent);
            final Optional<Notification.Action> lineReplyAction = getLineReplyAction(intent);
            Timber.i("Received start conversation action with message [%s]", messageWithKeyword);

            if (messageWithKeyword.isPresent() && lineReplyAction.isPresent()) {
                lineRemoteInputReplier.sendReply(lineReplyAction.get(), messageWithKeyword.get());

                // TODO update the existing notification after conversations are started
            }
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

    private Optional<Notification.Action> getLineReplyAction(Intent intent) {
        return Optional.empty();
    }

}
