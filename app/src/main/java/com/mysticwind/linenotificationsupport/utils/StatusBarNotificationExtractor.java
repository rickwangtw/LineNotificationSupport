package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import org.apache.commons.lang3.StringUtils;

import timber.log.Timber;

public class StatusBarNotificationExtractor {

    public static boolean isSummary(final StatusBarNotification statusBarNotification) {
        if ((statusBarNotification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) > 0) {
            return true;
        }

        final String summaryText = statusBarNotification.getNotification().extras
                .getString(Notification.EXTRA_SUMMARY_TEXT);
        if (StringUtils.isNotBlank(summaryText)) {
            Timber.d(String.format("Summary notification with message [%s]: it contains summary text [%s]",
                    NotificationExtractor.getMessage(statusBarNotification.getNotification()), summaryText));
            return true;
        }

        return false;
    }

}
