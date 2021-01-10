package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.notification.SummaryNotificationPublisher;

import java.util.Collection;
import java.util.Set;

public class SummaryNotificationPublisherIncomingNotificationReactor implements IncomingNotificationReactor {

    private final Set<String> interestedPackages;
    private final SummaryNotificationPublisher summaryNotificationPublisher;

    public SummaryNotificationPublisherIncomingNotificationReactor(final String thisPackageName,
                                                                   final SummaryNotificationPublisher summaryNotificationPublisher) {
        this.interestedPackages = ImmutableSet.of(thisPackageName);
        this.summaryNotificationPublisher = summaryNotificationPublisher;
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
    public Reaction reactToIncomingNotification(final StatusBarNotification statusBarNotification) {
        summaryNotificationPublisher.updateSummaryWhenNotificationsPublished(statusBarNotification.getNotification().getGroup());
        return Reaction.NONE;
    }

}
