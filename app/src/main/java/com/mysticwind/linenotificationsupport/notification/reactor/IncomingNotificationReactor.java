package com.mysticwind.linenotificationsupport.notification.reactor;

import android.service.notification.StatusBarNotification;

import java.util.Collection;

public interface IncomingNotificationReactor {

    Collection<String> interestedPackages();
    boolean isInterestInNotificationGroup();
    Reaction reactToIncomingNotification(StatusBarNotification statusBarNotification);

}
