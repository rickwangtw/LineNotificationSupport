package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class LineNotificationSupportLoggingIncomingNotificationReactor implements IncomingNotificationReactor {

    private final Set<String> packageNames;
    private final StatusBarNotificationPrinter statusBarNotificationPrinter;

    @Inject
    public LineNotificationSupportLoggingIncomingNotificationReactor(@HiltQualifiers.PackageName final String packageName,
                                                                     final StatusBarNotificationPrinter statusBarNotificationPrinter) {
        this.packageNames = ImmutableSet.of(Validate.notBlank(packageName));
        this.statusBarNotificationPrinter = Objects.requireNonNull(statusBarNotificationPrinter);
    }

    @Override
    public Collection<String> interestedPackages() {
        return packageNames;
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return true;
    }

    @Override
    public Reaction reactToIncomingNotification(final StatusBarNotification statusBarNotification) {
        Objects.requireNonNull(statusBarNotification);

        statusBarNotificationPrinter.print("LNS Published", statusBarNotification);

        return Reaction.NONE;
    }

}
