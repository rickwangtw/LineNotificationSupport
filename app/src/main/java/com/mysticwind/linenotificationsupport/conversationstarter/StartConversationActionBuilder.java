package com.mysticwind.linenotificationsupport.conversationstarter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;

import java.time.Instant;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class StartConversationActionBuilder {

    public static final String START_CONVERSATION_ACTION = "start_conversation_action";
    public static final String MESSAGE_REMOTE_INPUT_KEY = "message";

    private static final String DEFAULT_START_CONVERSATION_LABEL = "Start Conversation";

    private final Context context;

    @Inject
    public StartConversationActionBuilder(@ApplicationContext final Context context) {
        this.context = Objects.requireNonNull(context);
    }

    public Notification.Action buildAction() {
        final RemoteInput remoteInput = new RemoteInput.Builder(MESSAGE_REMOTE_INPUT_KEY)
                .setLabel(DEFAULT_START_CONVERSATION_LABEL)
                .build();

        final PendingIntent replyPendingIntent =
                PendingIntent.getBroadcast(context,
                        (int) Instant.now().toEpochMilli(),
                        getMessageReplyIntent(),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Action.Builder(null, DEFAULT_START_CONVERSATION_LABEL, replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    private Intent getMessageReplyIntent() {
        Intent intent = new Intent();
        intent.setAction(START_CONVERSATION_ACTION);
        return intent;
    }

}
