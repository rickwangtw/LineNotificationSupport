package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notification.impl.DumbNotificationCounter;

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DumbNotificationCounterNotificationReactor implements IncomingNotificationReactor, DismissedNotificationReactor {

    private final Set<String> interestedPackages;
    private final DumbNotificationCounter dumbNotificationCounter;

    @Inject
    public DumbNotificationCounterNotificationReactor(@HiltQualifiers.PackageName final String thisPackageName,
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
    public Reaction reactToIncomingNotification(StatusBarNotification statusBarNotification) {
        dumbNotificationCounter.notified(statusBarNotification.getNotification().getGroup(), statusBarNotification.getKey());
        return Reaction.NONE;
    }

    @Override
    public Reaction reactToDismissedNotification(StatusBarNotification statusBarNotification) {
        dumbNotificationCounter.dismissed(statusBarNotification.getNotification().getGroup(), statusBarNotification.getKey());
        return Reaction.NONE;
    }

}
