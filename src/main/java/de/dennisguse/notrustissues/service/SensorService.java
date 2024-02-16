package de.dennisguse.notrustissues.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

import java.util.Set;

import de.dennisguse.notrustissues.profiles.EnvironmentalSensingServiceProfile;


public class SensorService extends Service {

    private static final String TAG = SensorService.class.getSimpleName();

    private final Binder binder = new Binder();

    private BluetoothSender bluetoothSender;

    private SensorServiceNotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = new SensorServiceNotificationManager(this);

        bluetoothSender = new BluetoothSender(Set.of(new EnvironmentalSensingServiceProfile()));
        bluetoothSender.start(this, new Handler(Looper.myLooper()));

        ServiceCompat.startForeground(this, SensorServiceNotificationManager.NOTIFICATION_ID, notificationManager.getNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        bluetoothSender.stop();
        bluetoothSender = null;

        notificationManager = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class Binder extends android.os.Binder {

        private Binder() {
            super();
        }

        public SensorService getService() {
            return SensorService.this;
        }
    }
}
