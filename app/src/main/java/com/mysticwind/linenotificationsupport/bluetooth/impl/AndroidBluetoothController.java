package com.mysticwind.linenotificationsupport.bluetooth.impl;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;

import com.mysticwind.linenotificationsupport.android.AndroidFeatureProvider;
import com.mysticwind.linenotificationsupport.bluetooth.BluetoothController;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

/**
 * Implemented by referencing https://stackoverflow.com/questions/3806536/how-to-enable-disable-bluetooth-programmatically-in-android
 */
@Singleton
public class AndroidBluetoothController implements BluetoothController {

    private final AndroidFeatureProvider androidFeatureProvider;

    @Inject
    public AndroidBluetoothController(AndroidFeatureProvider androidFeatureProvider) {
        this.androidFeatureProvider = Objects.requireNonNull(androidFeatureProvider);
    }

    @Override
    public void enableBluetooth() {
        setBluetoothState(true);
    }

    @Override
    public void disableBluetooth() {
        setBluetoothState(false);
    }

    @SuppressLint("MissingPermission") // this is verified in androidFeatureProvider.hasBluetoothControlPermissions()
    private void setBluetoothState(final boolean enable) {
        if (!androidFeatureProvider.hasBluetoothControlPermissions()) {
            return;
        }
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final boolean isEnabled = bluetoothAdapter.isEnabled();
        if (enable && !isEnabled) {
            Timber.i("Enabling Bluetooth");
            bluetoothAdapter.enable();
        } else if(!enable && isEnabled) {
            Timber.i("Disabling Bluetooth");
            bluetoothAdapter.disable();
        }
        // No need to change bluetooth state
    }

}
