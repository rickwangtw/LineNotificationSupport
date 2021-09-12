package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.debug.history.manager.NotificationHistoryManager;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;

import timber.log.Timber;

public class LineNotificationLoggingIncomingNotificationReactor implements IncomingNotificationReactor {

    private final StatusBarNotificationPrinter statusBarNotificationPrinter;
    private final NotificationHistoryManager notificationHistoryManager;
    private final String lineAppVersion;

    public LineNotificationLoggingIncomingNotificationReactor(final StatusBarNotificationPrinter statusBarNotificationPrinter,
                                                              final NotificationHistoryManager notificationHistoryManager,
                                                              final String lineAppVersion) {
        this.statusBarNotificationPrinter = Objects.requireNonNull(statusBarNotificationPrinter);
        this.notificationHistoryManager = Objects.requireNonNull(notificationHistoryManager);
        this.lineAppVersion = lineAppVersion;
    }

    @Override
    public Collection<String> interestedPackages() {
        return ImmutableSet.of(Constants.LINE_PACKAGE_NAME);
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return true;
    }

    @Override
    public Reaction reactToIncomingNotification(final StatusBarNotification statusBarNotification) {
        Objects.requireNonNull(statusBarNotification);

        statusBarNotificationPrinter.print("Received", statusBarNotification);

        notificationHistoryManager.record(statusBarNotification, lineAppVersion);

        if (isNewMessageWithoutContent(statusBarNotification)) {
            Timber.d("Detected potential new message without content: key [%s] title [%s] message [%s]",
                    statusBarNotification.getKey(), NotificationExtractor.getTitle(statusBarNotification.getNotification()),
                    statusBarNotification.getNotification().tickerText);
            // we should get a notification update for this message
        }

        return Reaction.NONE;
    }

    private boolean isNewMessageWithoutContent(final StatusBarNotification statusBarNotification) {
        // There are notifications that will not have actions and don't need to retry.
        // For example: notifications of someone added to a chat
        if (StringUtils.isBlank(NotificationExtractor.getLineMessageId(statusBarNotification.getNotification()))) {
            return false;
        }
        return StatusBarNotificationExtractor.isMessage(statusBarNotification) &&
                statusBarNotification.getNotification().actions == null;
    }

}
