package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.utils.NotificationExtractor;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class LoggingDismissedNotificationReactor implements DismissedNotificationReactor {

    private final Set<String> interestedPackages;

    @Inject
    public LoggingDismissedNotificationReactor(@HiltQualifiers.PackageName final String packageName) {
        this.interestedPackages = ImmutableSet.of(packageName);
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
    public Reaction reactToDismissedNotification(StatusBarNotification statusBarNotification) {
        Timber.d("Dismissed LNS notification key [%s] id [%d] group [%s] isSummary [%s] title [%s] message [%s]",
                statusBarNotification.getKey(),
                statusBarNotification.getId(),
                statusBarNotification.getNotification().getGroup(),
                StatusBarNotificationExtractor.isSummary(statusBarNotification),
                NotificationExtractor.getTitle(statusBarNotification.getNotification()),
                NotificationExtractor.getMessage(statusBarNotification.getNotification()));

        return Reaction.NONE;
    }

}
