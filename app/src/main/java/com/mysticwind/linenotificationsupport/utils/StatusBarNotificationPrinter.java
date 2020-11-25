package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.google.common.base.MoreObjects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;

import timber.log.Timber;

public class StatusBarNotificationPrinter {

    public void print(final String message, final StatusBarNotification statusBarNotification) {
        final String prefix = buildPrefix(message);

        Timber.i(String.format("%sNotification (%s): %s",
                prefix,
                statusBarNotification.getPackageName(),
                stringifyNotification(statusBarNotification))
        );
    }

    private String buildPrefix(final String message) {
        if (StringUtils.isBlank(message)) {
            return "";
        }
        return String.format("[%s] ", message);
    }

    public void print(final StatusBarNotification statusBarNotification) {
        print("", statusBarNotification);
    }

    private String stringifyNotification(final StatusBarNotification statusBarNotification) {
        return MoreObjects.toStringHelper(statusBarNotification)
                .add("packageName", statusBarNotification.getPackageName())
                .add("groupKey", statusBarNotification.getGroupKey())
                .add("key", statusBarNotification.getKey())
                .add("id", statusBarNotification.getId())
                .add("tag", statusBarNotification.getTag())
                .add("user", statusBarNotification.getUser() == null ? "N/A" : statusBarNotification.getUser().toString())
                .add("overrideGroupKey", statusBarNotification.getOverrideGroupKey())
                .add("notification", ToStringBuilder.reflectionToString(statusBarNotification.getNotification()))
                .add("actionLabels", extractActionLabels(statusBarNotification))
                .toString();
    }

    private String extractActionLabels(StatusBarNotification statusBarNotification) {
        final Notification.Action[] actions = statusBarNotification.getNotification().actions;
        if (ArrayUtils.isEmpty(actions)) {
            return "N/A";
        }
        return Arrays.stream(actions)
                .filter(action -> action.title != null)
                .map(action -> action.title.toString())
                .reduce((title1, title2) -> title1 + "," + title2)
                .orElse("No title");
    }

    public void printError(final String message, final StatusBarNotification statusBarNotification) {
        final String prefix = buildPrefix(message);

        Timber.e(String.format("%sNotification (%s): %s",
                prefix,
                statusBarNotification.getPackageName(),
                stringifyNotification(statusBarNotification))
        );
    }

    public String toString(final StatusBarNotification statusBarNotification) {
        return stringifyNotification(statusBarNotification);
    }

}
