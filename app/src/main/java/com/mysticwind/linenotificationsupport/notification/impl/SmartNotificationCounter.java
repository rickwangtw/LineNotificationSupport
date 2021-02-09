package com.mysticwind.linenotificationsupport.notification.impl;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.mysticwind.linenotificationsupport.notification.NotificationCounter;

import java.util.Collection;

import timber.log.Timber;

public class SmartNotificationCounter implements NotificationCounter {

    private final Multimap<String, Integer> groupToNotificationIdsMultimap = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final int maxNotifications;

    public SmartNotificationCounter(final int maxNotifications) {
        this.maxNotifications = maxNotifications;
    }

    @Override
    public int notified(String group, int notificationId) {
        Timber.d("Published notification ID (%d) group (%s)", notificationId, group);
        groupToNotificationIdsMultimap.put(group, notificationId);
        return getSlotsUsed();
    }

    @Override
    public int dismissed(String group, int notificationId) {
        Timber.d("Dismissed notification ID (%d) group (%s)", notificationId, group);
        groupToNotificationIdsMultimap.get(group).remove(Integer.valueOf(notificationId));
        return getSlotsUsed();
    }

    @Override
    public boolean hasSlot(String group) {
        final int remainingSlots = maxNotifications - getSlotsUsed();
        final Collection<Integer> notifications = groupToNotificationIdsMultimap.get(group);
        if (notifications.isEmpty()){
            // need one
            return remainingSlots >= 1;
        } else if (notifications.size() == 1) {
            // need one for the notification and another one for the group
            return remainingSlots >= 2;
        } else {
            // even if there is summary, we will still need 2 slots to update the summary
            return remainingSlots >= 2;
        }
    }

    private int getSlotsUsed() {
        int slotsUsed = 0;
        for (final String key : groupToNotificationIdsMultimap.keySet()) {
            final Collection<Integer> notifications = groupToNotificationIdsMultimap.get(key);
            slotsUsed += notifications.size();
            if (notifications.size() > 1) {
                slotsUsed++;
            }
        }
        Timber.d("Slot used [%d] remaining [%d]", slotsUsed, maxNotifications - slotsUsed);
        return slotsUsed;
    }

}
