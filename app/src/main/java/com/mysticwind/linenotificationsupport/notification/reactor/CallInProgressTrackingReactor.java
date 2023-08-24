package com.mysticwind.linenotificationsupport.notification.reactor;

import android.app.Notification;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableSet;
import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.bluetooth.BluetoothController;
import com.mysticwind.linenotificationsupport.line.Constants;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationExtractor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class CallInProgressTrackingReactor implements IncomingNotificationReactor, DismissedNotificationReactor {

    private final PreferenceProvider preferenceProvider;
    private final BluetoothController bluetoothController;
    private final AndroidFeatureProvider androidFeatureProvider;
    private final Set<String> callNotificationKeys = new HashSet<>();

    @Inject
    public CallInProgressTrackingReactor(final PreferenceProvider preferenceProvider,
                                         final BluetoothController bluetoothController,
                                         final AndroidFeatureProvider androidFeatureProvider) {
        this.preferenceProvider = Objects.requireNonNull(preferenceProvider);
        this.bluetoothController = Objects.requireNonNull(bluetoothController);
        this.androidFeatureProvider = Objects.requireNonNull(androidFeatureProvider);
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

        if (shouldControlBluetooth()) {
            // TODO we probably should do a Toast here
            Timber.i("Disabling bluetooth");
            bluetoothController.disableBluetooth();
        }

        return Reaction.NONE;
    }

    @Override
    public Reaction reactToDismissedNotification(final StatusBarNotification statusBarNotification) {
        final String notificationKey = statusBarNotification.getKey();
        if (callNotificationKeys.contains(notificationKey)) {
            Timber.i("Notification [%s] IS for an in progress call and dismissed", notificationKey);
            callNotificationKeys.remove(notificationKey);

            if (shouldControlBluetooth()) {
                // TODO 1. we probably should do a Toast here
                // TODO 2. we probably want to revert to the original bluetooth status
                Timber.i("Re-enabling bluetooth");
                bluetoothController.enableBluetooth();
            }

            return Reaction.NONE;
        }
        Timber.d("Notification [%s] is not a in progress call notification", notificationKey);
        return Reaction.NONE;
    }

    // TODO unit tests
    private boolean isLineCallInProgressNotification(final StatusBarNotification statusBarNotification) {
        // essentially, if a notification is a message and persistent, it is due to an active call
        return StatusBarNotificationExtractor.isMessage(statusBarNotification) &&
                ((statusBarNotification.getNotification().flags & Notification.FLAG_ONGOING_EVENT) > 0) &&
                ((statusBarNotification.getNotification().flags & Notification.FLAG_NO_CLEAR) > 0);
    }

    private boolean shouldControlBluetooth() {
            return preferenceProvider.shouldControlBluetoothDuringCalls() && androidFeatureProvider.canControlBluetooth();
        }

}
