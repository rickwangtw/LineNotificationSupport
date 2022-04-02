package com.mysticwind.linenotificationsupport.conversationstarter;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.DismissNotificationBroadcastReceiver;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.conversationstarter.broadcastreceiver.DisableStartConversationFeatureBroadcastReceiver;
import com.mysticwind.linenotificationsupport.conversationstarter.broadcastreceiver.StartConversationBroadcastReceiver;
import com.mysticwind.linenotificationsupport.ui.LocalizationDao;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class StartConversationActionBuilder {

    public static final String START_CONVERSATION_ACTION = "start_conversation_action";
    public static final String MESSAGE_REMOTE_INPUT_KEY = "message";

    public static final String DISABLE_START_CONVERSATION_FEATURE_ACTION = "disable_start_conversation_feature_action";

    private final Context context;
    private final LocalizationDao localizationDao;

    @Inject
    public StartConversationActionBuilder(@ApplicationContext final Context context,
                                          final LocalizationDao localizationDao) {
        this.context = Objects.requireNonNull(context);
        this.localizationDao = Objects.requireNonNull(localizationDao);
    }

    public List<Notification.Action> buildActions() {
        return ImmutableList.of(
                buildRemoteInputAction(),
                buildDisableFeatureAction()
        );
    }

    public Notification.Action buildRemoteInputAction() {
        final RemoteInput remoteInput = new RemoteInput.Builder(MESSAGE_REMOTE_INPUT_KEY)
                .setLabel(localizationDao.getLocalizedString(R.string.conversation_start_notification_action_button_message))
                .build();

        final PendingIntent replyPendingIntent =
                PendingIntent.getBroadcast(context,
                        (int) Instant.now().toEpochMilli(),
                        getMessageReplyIntent(),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        return new Notification.Action.Builder(null,
                localizationDao.getLocalizedString(R.string.conversation_start_notification_action_button), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    private Intent getMessageReplyIntent() {
        Intent intent = new Intent(context, StartConversationBroadcastReceiver.class);
        intent.setAction(START_CONVERSATION_ACTION);
        return intent;
    }

    private Notification.Action buildDisableFeatureAction() {
        final Intent intent = new Intent(context, DisableStartConversationFeatureBroadcastReceiver.class);
        intent.setAction(DISABLE_START_CONVERSATION_FEATURE_ACTION);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(
                null,
                localizationDao.getLocalizedString(R.string.conversation_start_notification_disable_feature_action_button),
                pendingIntent)
                .build();
    }

}
