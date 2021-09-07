package com.mysticwind.linenotificationsupport.bluetooth.impl;

import android.bluetooth.BluetoothAdapter;

import com.mysticwind.linenotificationsupport.bluetooth.BluetoothController;

import timber.log.Timber;

/**
 * Implemented by referencing https://stackoverflow.com/questions/3806536/how-to-enable-disable-bluetooth-programmatically-in-android
 */
public class AndroidBluetoothController implements BluetoothController {

    @Override
    public void enableBluetooth() {
        setBluetoothState(true);
    }

    @Override
    public void disableBluetooth() {
        setBluetoothState(false);
    }

    private void setBluetoothState(final boolean enable) {
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
