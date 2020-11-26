package com.mysticwind.linenotificationsupport.notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;

import timber.log.Timber;

public class NotificationCounter {

    private final Multimap<String, Integer> groupToNotificationIdsMultimap = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private final int maxNotifications;

    public NotificationCounter(final int maxNotifications) {
        this.maxNotifications = maxNotifications;
    }

    public int notified(String group, int notificationId) {
        Timber.d("Published notification ID (%d) group (%s)", notificationId, group);
        groupToNotificationIdsMultimap.put(group, notificationId);
        return getSlotsUsed();
    }

    public int dismissed(String group, int notificationId) {
        Timber.d("Dismissed notification ID (%d) group (%s)", notificationId, group);
        groupToNotificationIdsMultimap.get(group).remove(Integer.valueOf(notificationId));
        return getSlotsUsed();
    }

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
            // SHOULD only need one because we clear the original summary
            return remainingSlots >= 1;
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
