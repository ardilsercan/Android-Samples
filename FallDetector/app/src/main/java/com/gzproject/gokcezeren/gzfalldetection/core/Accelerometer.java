package com.gzproject.gokcezeren.gzfalldetection.core;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gzproject.gokcezeren.gzfalldetection.Constants;


public class Accelerometer implements SensorEventListener {

    public static final String TAG = "Accelerometer";

    private Sensor mSensor;
    private SensorManager mSensorManager;

    private final Handler mHandler;

    private long lastUpdate = -1;

    private float accel_values[];
    private float last_accel_values[];


//    private int fallThreshold = 10;

    private float mAccelCurrent = SensorManager.GRAVITY_EARTH;
    private float mAccelLast = SensorManager.GRAVITY_EARTH;
    private float mAccel = 0.00f;

    private final static int CHECK_INTERVAL = 100; // [msec]

    private Window mWindow;

    public Accelerometer(SensorManager sm, Sensor s, Handler h){
        mSensorManager = sm;
        mSensor = s;
        mHandler = h;
        mWindow = new Window(1000/CHECK_INTERVAL); //sampling for 1 sec
    }

    public void startListening(){
        if (mSensor == null) {
            // error message to the Activity
            Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString(Constants.TOAST, "Unable to find accelerometer");
            msg.setData(bundle);
            mHandler.sendMessage(msg);

        } else {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST); // 0.2 sec for sampling on acc sens.
        }
    }

    public void stopListening(){
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { /*Safe not to implement*/ }


    @Override
    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            if ((curTime - lastUpdate) > CHECK_INTERVAL) {

                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                accel_values = event.values.clone();

                if (last_accel_values != null) {

                    mAccelLast = mAccelCurrent;
                    mAccelCurrent =(float)Math.sqrt(accel_values[0]* accel_values[0] + accel_values[1]*accel_values[1]
                            + accel_values[2]*accel_values[2]);



                    // value was sent to the Activity
                    Message msg = mHandler.obtainMessage(Constants.MESSAGE_CHANGED);
                    Bundle bundle = new Bundle();
                    bundle.putFloat(Constants.VALUE, mAccelCurrent);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);

                    mWindow.add(mAccelCurrent);
                    if (mWindow.isFull() && mWindow.isFallDetected()){
                        Log.w(TAG, "Fall detected by window class" + mAccelLast);
                        mWindow.clear();
                        msg = mHandler.obtainMessage(Constants.MESSAGE_EMERGENCY);
                        mHandler.sendMessage(msg);
                    }
//                    Inital approach
//                    =====================================
//                    if (mAccel > fallThreshold) {
//
//                        Log.w(TAG, "acceleration greater than threshold");
//                        // Send the value back to the Activity
//                        msg = mHandler.obtainMessage(Constants.MESSAGE_EMERGENCY);
//                        mHandler.sendMessage(msg);
//                    }
                }

                last_accel_values = accel_values.clone();
            }
        }
    }

}
