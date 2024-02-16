package de.dennisguse.notrustissues.profiles;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;

import java.nio.ByteBuffer;
import java.util.UUID;

import de.dennisguse.notrustissues.drivers.BarometerInternal;

/**
 * https://www.bluetooth.com/specifications/specs/environmental-sensing-service-1-0/
 * Only supports the pressure characteristics.
 */
public class EnvironmentalSensingServiceProfile implements IProfile {

    private static final UUID ENVIRONMENTAL_SENSING_SERVICE = UUID.fromString("0000181A-0000-1000-8000-00805f9b34fb");

    private static final UUID PRESSURE_CHARACTERISTIC = UUID.fromString("00002A6D-0000-1000-8000-00805f9b34fb");

    private NewDataListener newDataListener;

    private final BluetoothGattService service;

    private BluetoothGattCharacteristic pressureCharacteristic;

    private final BarometerInternal sensorDriver;

    private final BarometerInternal.MeasurementListener dataListener = data -> {
        // TODO data to []
        int value = (int)data.getPA();
        byte[] encoded = ByteBuffer.allocate(4).putInt(value).array();
        pressureCharacteristic.setValue(encoded);
        newDataListener.onChange(pressureCharacteristic);
    };

    public EnvironmentalSensingServiceProfile() {
        service = new BluetoothGattService(ENVIRONMENTAL_SENSING_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Pressure with notification support
        pressureCharacteristic = new BluetoothGattCharacteristic(
                PRESSURE_CHARACTERISTIC,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        pressureCharacteristic.addDescriptor(new BluetoothGattDescriptor(
                BLEUtils.CLIENT_CONFIG,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
        ));

        service.addCharacteristic(pressureCharacteristic);

        sensorDriver = new BarometerInternal(dataListener);
    }

    @Override
    public void start(Context context, Handler handler, NewDataListener newDataListener) {
        this.newDataListener = newDataListener;
        sensorDriver.connect(context, handler);
    }

    @Override
    public void stop() {
        sensorDriver.disconnect();
        newDataListener = null;
    }

    @Override
    public BluetoothGattService getService() {
        return service;
    }
}
