package com.mysticwind.linenotificationsupport.reply;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.ui.LocalizationDao;

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
    private final LocalizationDao localizationDao;

    @Inject
    public DefaultReplyActionBuilder(@ApplicationContext final Context context,
                                     final LocalizationDao localizationDao) {
        this.context = Objects.requireNonNull(context);
        this.localizationDao = Objects.requireNonNull(localizationDao);
    }

    @Override
    public Notification.Action buildReplyAction(final String chatId, final Notification.Action originalLineReplyAction) {
        final RemoteInput remoteInput = new RemoteInput.Builder(RESPONSE_REMOTE_INPUT_KEY)
                .setLabel(localizationDao.getLocalizedString(R.string.conversation_notification_action_button_message))
                .build();

        final PendingIntent replyPendingIntent =
                PendingIntent.getBroadcast(context,
                        chatId.hashCode(),
                        getMessageReplyIntent(chatId, originalLineReplyAction),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        final String buttonLabel = localizationDao.getLocalizedString(R.string.conversation_notification_action_button);

        return new Notification.Action.Builder(null, buttonLabel, replyPendingIntent)
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
