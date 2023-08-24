package com.mysticwind.linenotificationsupport.permission;

import android.Manifest;
import android.app.Activity;

import androidx.core.app.ActivityCompat;

import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AndroidPermissionRequester {

    private final PreferenceProvider preferenceProvider;
    private final AndroidFeatureProvider androidFeatureProvider;

    @Inject
    public AndroidPermissionRequester(final PreferenceProvider preferenceProvider,
                                      final AndroidFeatureProvider androidFeatureProvider) {
        this.preferenceProvider = Objects.requireNonNull(preferenceProvider);
        this.androidFeatureProvider = Objects.requireNonNull(androidFeatureProvider);
    }

    public void requestBluetoothPermissionIfNecessary(final Activity activity) {
        if (noBluetoothPermissionWhileRequired()) {
            ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1 /* any request code */);
        }
    }

    private boolean noBluetoothPermissionWhileRequired() {
        return preferenceProvider.shouldControlBluetoothDuringCalls() && !androidFeatureProvider.hasBluetoothControlPermissions();
    }

    public void requestPublishNotificationPermissionIfNecessary(final Activity activity) {
        if (androidFeatureProvider.hasPublishNotificationPermission()) {
            return;
        }
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2 /* any request code */);
    }

}
