package com.mysticwind.linenotificationsupport.notification.reactor;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.bluetooth.BluetoothController;
import com.mysticwind.linenotificationsupport.line.Constants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import timber.log.Timber;

public class CallInProgressTrackingReactor implements IncomingNotificationReactor, DismissedNotificationReactor {

    private final Set<String> callNotificationKeys = new HashSet<>();
    private final BluetoothController bluetoothController;

    public CallInProgressTrackingReactor(final BluetoothController bluetoothController) {
        this.bluetoothController = Objects.requireNonNull(bluetoothController);
    }

    @Override
    public Collection<String> interestedPackages() {
        return ImmutableSet.of(Constants.LINE_PACKAGE_NAME);
    }

    @Override
    public boolean isInterestInNotificationGroup() {
        return false;
    }

    @Override
    public Reaction reactToIncomingNotification(final StatusBarNotification statusBarNotification) {
        final String notificationKey = statusBarNotification.getKey();
        if (!isLineCallInProgressNotification(statusBarNotification)) {
            Timber.d("Notification [%s] not a call", notificationKey);
            return Reaction.NONE;
        }
        // TODO differentiate between incoming call and call in progress
        Timber.d("Notification [%s] IS a call", statusBarNotification.getKey());
        if (callNotificationKeys.contains(notificationKey)) {
            Timber.d("Call notification [%s] already present", notificationKey);
            return Reaction.NONE;
        }
        if (callNotificationKeys.size() > 0) {
            Timber.e("Already exists call notifications [%s] and will be replaced with key [%s]",
                    callNotificationKeys, notificationKey);
            callNotificationKeys.clear();
            callNotificationKeys.add(notificationKey);
            return Reaction.NONE;
        }
        callNotificationKeys.add(notificationKey);

        // TODO 1. we probably should do a Toast here
        // TODO 2. integrate with preference
        Timber.i("Disabling bluetooth");
        bluetoothController.disableBluetooth();

        return Reaction.NONE;
    }

    @Override
    public Reaction reactToDismissedNotification(final StatusBarNotification statusBarNotification) {
        final String notificationKey = statusBarNotification.getKey();
        if (callNotificationKeys.contains(notificationKey)) {
            Timber.i("Notification [%s] IS for an in progress call and dismissed", notificationKey);
            callNotificationKeys.remove(notificationKey);

            // TODO 1. we probably should do a Toast here
            // TODO 2. we probably want to revert to the original bluetooth status
            // TODO 3. integrate with preference
            Timber.i("Re-enabling bluetooth");
            bluetoothController.enableBluetooth();

            return Reaction.NONE;
        }
        Timber.d("Notification [%s] is not a in progress call notification", notificationKey);
        return Reaction.NONE;
    }

    // TODO unit tests
    private boolean isLineCallInProgressNotification(final StatusBarNotification statusBarNotification) {
        // essentially, if a notification is persistent, it is due to an active call
        return ((statusBarNotification.getNotification().flags & Notification.FLAG_ONGOING_EVENT) > 0) &&
                ((statusBarNotification.getNotification().flags & Notification.FLAG_NO_CLEAR) > 0);
    }

}
