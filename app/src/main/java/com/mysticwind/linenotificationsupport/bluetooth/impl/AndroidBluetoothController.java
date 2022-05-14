package com.mysticwind.linenotificationsupport.bluetooth.impl;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import com.mysticwind.linenotificationsupport.bluetooth.BluetoothController;

import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;
import timber.log.Timber;

/**
 * Implemented by referencing https://stackoverflow.com/questions/3806536/how-to-enable-disable-bluetooth-programmatically-in-android
 */
@Singleton
public class AndroidBluetoothController implements BluetoothController {

    private final Context context;

    @Inject
    public AndroidBluetoothController(@ApplicationContext final Context context) {
        this.context = Objects.requireNonNull(context);
    }

    @Override
    public void enableBluetooth() {
        setBluetoothState(true);
    }

    @Override
    public void disableBluetooth() {
        setBluetoothState(false);
    }

    private void setBluetoothState(final boolean enable) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Timber.w("No permissions to control Bluetooth!!!");
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
