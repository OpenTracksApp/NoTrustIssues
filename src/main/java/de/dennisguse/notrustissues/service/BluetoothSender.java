package de.dennisguse.notrustissues.service;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.dennisguse.notrustissues.profiles.BLEUtils;
import de.dennisguse.notrustissues.profiles.EnvironmentalSensingServiceProfile;
import de.dennisguse.notrustissues.profiles.IProfile;

@SuppressLint("MissingPermission")
public class BluetoothSender {

    private final String TAG = BluetoothSender.class.getSimpleName();

    private BluetoothManager bluetoothManager;
    private BluetoothGattServer bluetoothGattServer;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            throw new RuntimeException("LE Advertising failed to start with error code: " + errorCode);
        }
    };

    private final BluetoothGattServerCallback gattConnectionCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
                }
                case BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                    registeredDevices.remove(device);
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            // descriptor.getCharacteristic().getUuid() TODO we need to check the  characteristic as well.
            if (BLEUtils.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (registeredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, returnValue);
                return;
            }

            Log.w(TAG, "Unknown descriptor read request");
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            if (BLEUtils.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    registeredDevices.add(device);
                    device.createBond();
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    registeredDevices.remove(device);
                }
                if (responseNeeded) {
                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
                return;
            }

            Log.w(TAG, "Unknown descriptor write request");
            if (responseNeeded) {
                bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
            }
        }
    };

    private final Set<BluetoothDevice> registeredDevices = new HashSet<>();

    private final Set<IProfile> profiles;

    private final EnvironmentalSensingServiceProfile.NewDataListener listener = (characteristic) -> {
        Log.i(TAG, "Sending data to " + registeredDevices);
        registeredDevices.forEach((device) -> {
            boolean success = bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, false);
            Log.i(TAG, device + " send with success " + success);
        });
    };

    public BluetoothSender(Set<IProfile> profiles) {
        this.profiles = Collections.unmodifiableSet(profiles);
    }

    public void start(Context context, Handler handler) {
        startServer(context);
        startAdvertising();
        profiles.forEach(it -> it.start(context, handler, listener));
    }

    public void stop() {
        profiles.forEach(IProfile::stop);

        if (bluetoothLeAdvertiser == null) return;
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);

        if (bluetoothGattServer == null) return;
        bluetoothGattServer.close();
    }

    private void startServer(Context context) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothGattServer = bluetoothManager.openGattServer(context, gattConnectionCallback);
        if (bluetoothGattServer == null) {
            throw new RuntimeException("Unable to create GATT server");
        }

        profiles.forEach(it -> {
            bluetoothGattServer.addService(it.getService());
        });
    }

    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData.Builder advertiseBuilder = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false);

        profiles.forEach(it -> advertiseBuilder.addServiceUuid(new ParcelUuid(it.getService().getUuid())));

        bluetoothLeAdvertiser
                .startAdvertising(settings, advertiseBuilder.build(), advertiseCallback);
    }
}
