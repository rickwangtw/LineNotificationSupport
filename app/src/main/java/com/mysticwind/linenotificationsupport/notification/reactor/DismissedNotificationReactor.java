package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import java.util.Collection;

public interface DismissedNotificationReactor {

    Collection<String> interestedPackages();
    boolean isInterestInNotificationGroup();
    void reactToDismissedNotification(StatusBarNotification statusBarNotification);

}
