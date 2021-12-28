package com.mysticwind.linenotificationsupport.notification.reactor;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.conversationstarter.LineReplyActionDao;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import timber.log.Timber;

// TODO address duplicates here and LineNotificationBuilder
public class LineReplyActionPersistenceIncomingNotificationReactor implements IncomingNotificationReactor {

    private static final String MISSED_CALL_TAG = "NOTIFICATION_TAG_MISSED_CALL";
    private static final String GENERAL_NOTIFICATION_CHANNEL = "jp.naver.line.android.notification.GeneralNotifications";
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
        if (!isMessage(statusBarNotification)) {
            return Reaction.NONE;
        }
        // mute and reply buttons
        List<Notification.Action> lineAction = extractActionsOfIndices(statusBarNotification, 1);
        if (lineAction.isEmpty()) {
            return Reaction.NONE;
        }
        final String lineChatId = NotificationExtractor.getLineChatId(statusBarNotification.getNotification());
        Timber.d("Persisting chat ID [%s] and action [%s]", lineChatId, lineAction.get(0));
        lineReplyActionDao.saveLineReplyAction(lineChatId, lineAction.get(0));
        return Reaction.NONE;
    }

    private boolean isMessage(final StatusBarNotification statusBarNotification) {
        if (resolveCallState(statusBarNotification).isPresent()) {
            return false;
        }
        return StatusBarNotificationExtractor.isMessage(statusBarNotification);
    }

    private Optional<LineNotification.CallState> resolveCallState(final StatusBarNotification statusBarNotification) {
        if (StatusBarNotificationExtractor.isCall(statusBarNotification)) {
            return Optional.of(LineNotification.CallState.INCOMING);
        } else if (MISSED_CALL_TAG.equals(statusBarNotification.getTag())) {
            return Optional.of(LineNotification.CallState.MISSED_CALL);
            // if not incoming, not missed, it is probably in a call ... (but not guaranteed, we'll see)
        } else if (GENERAL_NOTIFICATION_CHANNEL.equals(statusBarNotification.getNotification().getChannelId()) &&
                StringUtils.isBlank(statusBarNotification.getNotification().getGroup())) {
            return Optional.of(LineNotification.CallState.IN_A_CALL);
        }
        return Optional.empty();
    }

    private List<Notification.Action> extractActionsOfIndices(final StatusBarNotification notificationFromLine,
                                                              final int... indices) {
        final List<Notification.Action> extractedActions = new ArrayList<>();
        if (notificationFromLine.getNotification().actions == null) {
            return extractedActions;
        }
        for (final int index : indices) {
            if (index < notificationFromLine.getNotification().actions.length) {
                extractedActions.add(notificationFromLine.getNotification().actions[index]);
            }
        }
        return extractedActions;
    }

}
