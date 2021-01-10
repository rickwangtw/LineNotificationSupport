package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.notification.impl.SmartNotificationCounter;

import java.util.Collection;
import java.util.Set;

public class SmartNotificationCounterNotificationReactor implements IncomingNotificationReactor, DismissedNotificationReactor {

    private final Set<String> interestedPackages;
    private final SmartNotificationCounter smartNotificationCounter;

    public SmartNotificationCounterNotificationReactor(final String thisPackageName,
                                                       final SmartNotificationCounter smartNotificationCounter) {
        this.interestedPackages = ImmutableSet.of(thisPackageName);
        this.smartNotificationCounter = smartNotificationCounter;
    }

    @Override
    public Collection<String> interestedPackages() {
        return interestedPackages;
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return false;
    }

    @Override
    public Reaction reactToIncomingNotification(StatusBarNotification statusBarNotification) {
        smartNotificationCounter.notified(statusBarNotification.getNotification().getGroup(), statusBarNotification.getId());
        return Reaction.NONE;
    }

    @Override
    public Reaction reactToDismissedNotification(StatusBarNotification statusBarNotification) {
        smartNotificationCounter.dismissed(statusBarNotification.getNotification().getGroup(), statusBarNotification.getId());
        return Reaction.NONE;
    }

}
