package com.mysticwind.linenotificationsupport.notification.reactor;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.conversationstarter.LineReplyActionDao;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import timber.log.Timber;

public class LineReplyActionPersistenceIncomingNotificationReactor implements IncomingNotificationReactor {

    private static final Set<String> INTERESTED_PACKAGES = ImmutableSet.of(Constants.LINE_PACKAGE_NAME);

    private final LineReplyActionDao lineReplyActionDao;

    public LineReplyActionPersistenceIncomingNotificationReactor(final LineReplyActionDao lineReplyActionDao) {
        this.lineReplyActionDao = Objects.requireNonNull(lineReplyActionDao);
    }

    @Override
    public Collection<String> interestedPackages() {
        return INTERESTED_PACKAGES;
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

    private boolean isMessage(StatusBarNotification statusBarNotification) {
        return StatusBarNotificationExtractor.isMessage(statusBarNotification);
    }

    private Optional<Notification.Action> resolveReplyAction(Notification.Action[] actions) {
        if (actions == null) {
            return Optional.empty();
        }
        // mute and reply buttons
        if (actions.length < 2) {
            return Optional.empty();
        }
        return Optional.of(actions[1]);
    }

    private void persistReplyAction(final String chatId, final Notification.Action action) {
        Timber.i("Persisted reply action chat ID [%s] title [%s] action [%s]", chatId, action.title, action);

        lineReplyActionDao.saveLineReplyAction(chatId, action);
    }

}
