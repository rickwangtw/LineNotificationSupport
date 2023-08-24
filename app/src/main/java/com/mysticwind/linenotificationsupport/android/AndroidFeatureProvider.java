package com.mysticwind.linenotificationsupport.android;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

@Singleton
public class AndroidFeatureProvider {

    private final Context context;

    @Inject
    public AndroidFeatureProvider(@ApplicationContext Context context) {
        this.context = Objects.requireNonNull(context);
    }

    public boolean hasNotificationChannelSupport() {
        // NotificationChannels are only supported API 26+
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public boolean canControlBluetooth() {
        // Bluetooth enable/disable are only supported before API 33
        // https://developer.android.com/reference/android/bluetooth/BluetoothAdapter?hl=en#enable()
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU;
    }

    public boolean hasBluetoothControlPermissions() {
        // Android 12 and above requires BLUETOOTH_CONNECT permission that requires additional approval
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("No permissions to control Bluetooth!!!");
            return false;
        } else {
            return true;
        }
    }

    public boolean hasPublishNotificationPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("No permissions to publish Notifications!!!");
            return false;
        } else {
            return true;
        }
    }

}
