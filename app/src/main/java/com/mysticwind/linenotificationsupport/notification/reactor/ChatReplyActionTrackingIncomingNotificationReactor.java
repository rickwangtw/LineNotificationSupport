package com.mysticwind.linenotificationsupport.notification.reactor;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.reply.ChatReplyActionManager;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import timber.log.Timber;

import static com.mysticwind.linenotificationsupport.model.LineNotificationBuilder.MESSAGE_CATEGORY;

public class ChatReplyActionTrackingIncomingNotificationReactor implements IncomingNotificationReactor {

    private final ChatReplyActionManager chatReplyActionManager;

    public ChatReplyActionTrackingIncomingNotificationReactor(final ChatReplyActionManager chatReplyActionManager) {
        this.chatReplyActionManager = Objects.requireNonNull(chatReplyActionManager);
    }

    @Override
    public Collection<String> interestedPackages() {
        return ImmutableSet.of(Constants.LINE_PACKAGE_NAME);
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return false;
    }

    @Override
    public Reaction reactToIncomingNotification(final StatusBarNotification statusBarNotification) {
        Objects.requireNonNull(statusBarNotification);

        if (!isMessage(statusBarNotification)) {
            return Reaction.NONE;
        }

        final String chatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        if (StringUtils.isBlank(chatId)) {
            return Reaction.NONE;
        }

        Optional<Notification.Action> replyAction = resolveReplyAction(statusBarNotification.getNotification().actions);
        replyAction.ifPresent(action ->
                persistReplyAction(chatId, action)
        );

        return Reaction.NONE;
    }

    // TODO extract a common class for this shared with LineNotificationBuilder
    private boolean isMessage(StatusBarNotification statusBarNotification) {
        return MESSAGE_CATEGORY.equals(statusBarNotification.getNotification().category);
    }

    private Optional<Notification.Action> resolveReplyAction(Notification.Action[] actions) {
        if (actions == null) {
            return Optional.empty();
        }
        if (actions.length < 2) {
            return Optional.empty();
        }
        return Optional.of(actions[1]);
    }

    private void persistReplyAction(final String chatId, final Notification.Action action) {
        Timber.i("Persisted reply action chat ID [%s] title [%s] action [%s]", chatId, action.title, action);

        chatReplyActionManager.persistReplyAction(chatId, action);
    }

}
