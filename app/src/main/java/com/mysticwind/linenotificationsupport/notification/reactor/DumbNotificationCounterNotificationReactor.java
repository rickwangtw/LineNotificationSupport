package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.notification.impl.DumbNotificationCounter;

import java.util.Collection;
import java.util.Set;

public class DumbNotificationCounterNotificationReactor implements IncomingNotificationReactor, DismissedNotificationReactor {

    private final Set<String> interestedPackages;
    private final DumbNotificationCounter dumbNotificationCounter;

    public DumbNotificationCounterNotificationReactor(final String thisPackageName,
                                                      final DumbNotificationCounter dumbNotificationCounter) {
        this.interestedPackages = ImmutableSet.of(thisPackageName);
        this.dumbNotificationCounter = dumbNotificationCounter;
    }

    @Override
    public Collection<String> interestedPackages() {
        return interestedPackages;
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return true;
    }

    @Override
    public void reactToIncomingNotification(StatusBarNotification statusBarNotification) {
        dumbNotificationCounter.notified(statusBarNotification.getNotification().getGroup(), statusBarNotification.getKey());
    }

    @Override
    public void reactToDismissedNotification(StatusBarNotification statusBarNotification) {
        dumbNotificationCounter.dismissed(statusBarNotification.getNotification().getGroup(), statusBarNotification.getKey());
    }

}
