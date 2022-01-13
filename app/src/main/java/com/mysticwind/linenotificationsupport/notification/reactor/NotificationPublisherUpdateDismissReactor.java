package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.R;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notification.NotificationPublisherFactory;

import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class NotificationPublisherUpdateDismissReactor implements DismissedNotificationReactor {

    private final String packageName;
    private final NotificationPublisherFactory notificationPublisherFactory;

    @Inject
    public NotificationPublisherUpdateDismissReactor(@HiltQualifiers.PackageName final String packageName,
                                                     final NotificationPublisherFactory notificationPublisherFactory) {
        this.packageName = Validate.notBlank(packageName);
        this.notificationPublisherFactory = Objects.requireNonNull(notificationPublisherFactory);
    }

    @Override
    public Collection<String> interestedPackages() {
        return ImmutableSet.of(packageName);
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return false;
    }

    @Override
    public Reaction reactToDismissedNotification(StatusBarNotification statusBarNotification) {
        Timber.d("Received notification dismiss [%d]", statusBarNotification.getId());

        notificationPublisherFactory.get().updateNotificationDismissed(statusBarNotification);

        return Reaction.NONE;
    }
}
