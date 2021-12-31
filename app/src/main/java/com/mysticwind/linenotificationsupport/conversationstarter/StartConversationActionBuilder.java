package com.mysticwind.linenotificationsupport.conversationstarter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;

import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.ui.LocalizationDao;

import java.time.Instant;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class StartConversationActionBuilder {

    public static final String START_CONVERSATION_ACTION = "start_conversation_action";
    public static final String MESSAGE_REMOTE_INPUT_KEY = "message";

    private final Context context;
    private final LocalizationDao localizationDao;

    @Inject
    public StartConversationActionBuilder(@ApplicationContext final Context context,
                                          final LocalizationDao localizationDao) {
        this.context = Objects.requireNonNull(context);
        this.localizationDao = Objects.requireNonNull(localizationDao);
    }

    public Notification.Action buildAction() {
        final RemoteInput remoteInput = new RemoteInput.Builder(MESSAGE_REMOTE_INPUT_KEY)
                .setLabel(localizationDao.getLocalizedString(R.string.conversation_start_notification_action_button_message))
                .build();

        final PendingIntent replyPendingIntent =
                PendingIntent.getBroadcast(context,
                        (int) Instant.now().toEpochMilli(),
                        getMessageReplyIntent(),
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return new Notification.Action.Builder(null,
                localizationDao.getLocalizedString(R.string.conversation_start_notification_action_button), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    private Intent getMessageReplyIntent() {
        Intent intent = new Intent();
        intent.setAction(START_CONVERSATION_ACTION);
        return intent;
    }

}
