package com.mysticwind.linenotificationsupport.notification.impl;

import android.service.notification.StatusBarNotification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mysticwind.linenotificationsupport.module.HiltQualifiers;
import com.mysticwind.linenotificationsupport.notification.SlotAvailabilityChecker;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class DumbNotificationCounter implements SlotAvailabilityChecker {

    private final Multimap<String, String> groupToNotificationKeyMultimap =
            Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final int maxNotifications;

    @Inject
    public DumbNotificationCounter(@HiltQualifiers.MaxNotificationsPerApp final int maxNotifications) {
        this.maxNotifications = maxNotifications;
    }

    // TODO refactor NotificationCounter interface to use notification keys
    public int notified(String group, String notificationKey) {
        Timber.d("Published notification key (%s) group (%s)", notificationKey, group);
        groupToNotificationKeyMultimap.put(group, notificationKey);
        return getSlotsUsed();
    }

    public int dismissed(String group, String notificationKey) {
        Timber.d("Dismissed notification key (%s) group (%s)", notificationKey, group);
        groupToNotificationKeyMultimap.get(group).remove(notificationKey);
        return getSlotsUsed();
    }

    @Override
    public boolean hasSlot(String group) {
        final int remainingSlots = maxNotifications - getSlotsUsed();
        final Collection<String> notificationKeys = groupToNotificationKeyMultimap.get(group);
        if (notificationKeys.isEmpty()){
            return remainingSlots >= 1;
        } else {
            return remainingSlots >= 2;
        }
    }

    private int getSlotsUsed() {
        final int slotsUsed = groupToNotificationKeyMultimap.values().size();
        Timber.d("Slots used [%d] remaining [%d]", slotsUsed, maxNotifications - slotsUsed);
        return slotsUsed;
    }

    // This probably risks concurrency
    // returns whether or not it is valid (not changed)
    public boolean validateNotifications(final Multimap<String, String> currentGroupToNotificationKeyMultimap) {
        if (!groupToNotificationKeyMultimap.equals(currentGroupToNotificationKeyMultimap)) {
            Timber.w("Notifications being tracked are different! Tracked [%s] Current [%s]",
                    groupToNotificationKeyMultimap.values(), currentGroupToNotificationKeyMultimap.values());
            groupToNotificationKeyMultimap.clear();
            groupToNotificationKeyMultimap.putAll(currentGroupToNotificationKeyMultimap);
            return false;
        } else {
            Timber.d("Verified notifications are the same");
            return true;
        }
    }

    public void updateStateFromExistingNotifications(final List<StatusBarNotification> existingNotifications) {
        for (final StatusBarNotification notification : existingNotifications) {
            groupToNotificationKeyMultimap.clear();
            Timber.d("Restoring notification group [%s] key [%s]",
                    notification.getNotification().getGroup(), notification.getKey());
            groupToNotificationKeyMultimap.put(notification.getNotification().getGroup(), notification.getKey());
        }
    }

}
