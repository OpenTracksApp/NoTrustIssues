package de.dennisguse.notrustissues.profiles;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;

public interface IProfile {

    BluetoothGattService getService();

    void start(Context context, Handler handler, EnvironmentalSensingServiceProfile.NewDataListener newDataListener);

    void stop();

    interface NewDataListener {

        void onChange(BluetoothGattCharacteristic characteristic);

    }
}
