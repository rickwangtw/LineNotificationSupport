package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.mysticwind.linenotificationsupport.log.TagBuilder;

import org.apache.commons.lang3.StringUtils;

public class StatusBarNotificationExtractor {

    private static final String TAG = TagBuilder.build(StatusBarNotificationExtractor.class);

    public static boolean isSummary(final StatusBarNotification statusBarNotification) {
        if ((statusBarNotification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) > 0) {
            Log.d(TAG, String.format("Summary notification with message [%s]: flag %s",
                    statusBarNotification.getNotification().tickerText, statusBarNotification.getNotification().flags));
            return true;
        }

        final String summaryText = statusBarNotification.getNotification().extras
                .getString(Notification.EXTRA_SUMMARY_TEXT);
        if (StringUtils.isNotBlank(summaryText)) {
            Log.d(TAG, String.format("Summary notification with message [%s]: it contains summary text [%s]",
                    statusBarNotification.getNotification().tickerText, summaryText));
            return true;
        }

        return false;

    }

}
