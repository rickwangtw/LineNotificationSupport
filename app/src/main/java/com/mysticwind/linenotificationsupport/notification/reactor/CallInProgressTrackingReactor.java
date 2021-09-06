package com.mysticwind.linenotificationsupport.notification.reactor;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.line.Constants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import timber.log.Timber;

public class CallInProgressTrackingReactor implements IncomingNotificationReactor, DismissedNotificationReactor {

    private Set<String> callNotificationKeys = new HashSet<>();

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
        final String notificationKey = statusBarNotification.getKey();
        if (!isLineCallInProgressNotification(statusBarNotification)) {
            Timber.d("Notification [%s] not a call", notificationKey);
            return Reaction.NONE;
        }
        Timber.d("Notification [%s] IS a call", statusBarNotification.getKey());
        if (callNotificationKeys.contains(notificationKey)) {
            Timber.d("Call notification [%s] already present", notificationKey);
            return Reaction.NONE;
        }
        if (callNotificationKeys.size() > 0) {
            Timber.e("Already exists call notifications [%s] and will be replaced with key [%s]",
                    callNotificationKeys, notificationKey);
            callNotificationKeys.clear();
            callNotificationKeys.add(notificationKey);
            return Reaction.NONE;
        }
        callNotificationKeys.add(notificationKey);
        return Reaction.NONE;
    }

    @Override
    public Reaction reactToDismissedNotification(final StatusBarNotification statusBarNotification) {
        final String notificationKey = statusBarNotification.getKey();
        if (callNotificationKeys.contains(notificationKey)) {
            Timber.i("Notification [%s] IS for an in progress call and dismissed", notificationKey);
            callNotificationKeys.remove(notificationKey);
            return Reaction.NONE;
        }
        Timber.d("Notification [%s] is not a in progress call notification", notificationKey);
        return Reaction.NONE;
    }

    // TODO unit tests
    private boolean isLineCallInProgressNotification(final StatusBarNotification statusBarNotification) {
        // essentially, if a notification is persistent, it is due to an active call
        return ((statusBarNotification.getNotification().flags & Notification.FLAG_ONGOING_EVENT) > 0) &&
                ((statusBarNotification.getNotification().flags & Notification.FLAG_NO_CLEAR) > 0);
    }

}
