package com.mysticwind.linenotificationsupport.reply;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;

import org.apache.commons.lang3.Validate;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class DefaultReplyActionBuilder implements  ReplyActionBuilder {

    public static final String REPLY_MESSAGE_ACTION = "reply_message";
    public static final String RESPONSE_REMOTE_INPUT_KEY = "response";
    public static final String LINE_REPLY_ACTION_KEY = "line.reply.action";
    public static final String CHAT_ID_KEY = "chat.id";

    private static final String DEFAULT_REPLY_LABEL = "Reply";

    private final Context context;
    private final String replyLabel;

    @Inject
    public DefaultReplyActionBuilder(@ApplicationContext final Context context) {
        this(context, DEFAULT_REPLY_LABEL);
    }

    public DefaultReplyActionBuilder(final Context context, final String replyLabel) {
        this.context = Objects.requireNonNull(context);
        this.replyLabel = Validate.notBlank(replyLabel);
    }

    @Override
    public Notification.Action buildReplyAction(final String chatId, final Notification.Action originalLineReplyAction) {
        final RemoteInput remoteInput = new RemoteInput.Builder(RESPONSE_REMOTE_INPUT_KEY)
                .setLabel(replyLabel)
                .build();

        final PendingIntent replyPendingIntent =
                PendingIntent.getBroadcast(context,
                        chatId.hashCode(),
                        getMessageReplyIntent(chatId, originalLineReplyAction),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Action.Builder(null, replyLabel, replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    private Intent getMessageReplyIntent(final String chatId, final Notification.Action originalLineReplyAction) {
        Intent intent = new Intent();
        intent.setAction(REPLY_MESSAGE_ACTION);
        intent.putExtra(CHAT_ID_KEY, chatId);
        intent.putExtra(LINE_REPLY_ACTION_KEY, originalLineReplyAction);
        return intent;
    }

}
