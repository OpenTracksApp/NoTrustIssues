package de.dennisguse.notrustissues.drivers;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;


public class BarometerInternal {

    private static final String TAG = BarometerInternal.class.getSimpleName();

    private static final int SAMPLING_PERIOD = (int) TimeUnit.SECONDS.toMicros(5);


    private final MeasurementListener listener;

    private Context context;

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!isConnected()) {
                Log.w(TAG, "Not connected to sensor, cannot process data.");
                return;
            }

            listener.onChange(AtmosphericPressure.ofHPA(event.values[0]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.w(TAG, "Sensor accuracy changes are (currently) ignored.");
        }
    };

    public BarometerInternal(@NonNull MeasurementListener listener) {
        this.listener = listener;

    }

    public void connect(Context context, Handler handler) {
        android.hardware.SensorManager sensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (pressureSensor == null) {
            throw new RuntimeException("No pressure sensor available.");
        }

        if (sensorManager.registerListener(sensorEventListener, pressureSensor, SAMPLING_PERIOD, handler)) {
            this.context = context;
            return;
        }

        disconnect();
    }

    public boolean isConnected() {
        return context != null;
    }

    public void disconnect() {
        if (!isConnected()) return;

        android.hardware.SensorManager sensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorEventListener);
        this.context = null;
    }



    public interface MeasurementListener {
        void onChange(AtmosphericPressure data);
    }
}
