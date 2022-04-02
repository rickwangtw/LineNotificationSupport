package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisherFactory;

import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class PublishedNotificationTrackerIncomingNotificationReactor implements IncomingNotificationReactor {

    private final String packageName;
    private final NotificationPublisherFactory notificationPublisherFactory;

    @Inject
    public PublishedNotificationTrackerIncomingNotificationReactor(@HiltQualifiers.PackageName final String packageName,
                                                                   NotificationPublisherFactory notificationPublisherFactory) {
        this.packageName = Validate.notBlank(packageName);
        this.notificationPublisherFactory = Objects.requireNonNull(notificationPublisherFactory);
    }

    @Override
    public Collection<String> interestedPackages() {
        return ImmutableList.of(packageName);
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return false;
    }

    @Override
    public Reaction reactToIncomingNotification(StatusBarNotification statusBarNotification) {
        Timber.d("Tracking published notification: [%d]", statusBarNotification.getId());
        notificationPublisherFactory.trackNotificationPublished(statusBarNotification.getId());
        return Reaction.NONE;
    }

}
