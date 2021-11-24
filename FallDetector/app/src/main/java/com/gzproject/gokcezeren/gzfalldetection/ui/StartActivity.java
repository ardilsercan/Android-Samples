package com.gzproject.gokcezeren.gzfalldetection.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.gzproject.gokcezeren.gzfalldetection.core.Accelerometer;
import com.gzproject.gokcezeren.gzfalldetection.Constants;
import com.gzproject.gokcezeren.gzfalldetection.core.Plot;
import com.gzproject.gokcezeren.gzfalldetection.R;
import com.github.mikephil.charting.charts.LineChart;

import java.text.DateFormat;
import java.util.Date;

public class StartActivity extends AppCompatActivity{

    public static final String TAG = "StartActivity";

    private SwitchCompat mSwitchCompat;
//    private ToggleButton mToggle; // TODO: remove

    private SensorManager mSensorManager;
    private Sensor mSensor;

    private LineChart mLineChart;
    private Plot mPlot;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mSwitchCompat = (SwitchCompat) findViewById(R.id.switch1);

        mLineChart = (LineChart) findViewById(R.id.chart);


        mPlot = new Plot(mLineChart);
        mPlot.setUp();

        // accelerometer sensör ayarlaması
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        final Accelerometer accelerometer = new Accelerometer(mSensorManager, mSensor, mHandler);

        mSwitchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                if(isChecked) {
                    accelerometer.startListening();
                } else {
                    accelerometer.stopListening();
                }
            }
        });


    }

    private String getContact(){
        SharedPreferences preferences = getSharedPreferences(Constants.MyPREFERENCES, Context.MODE_PRIVATE);
        String code = preferences.getString(Constants.Code,null);
        String phone = preferences.getString(Constants.Phone,null);
        Log.w(TAG, "tel: +" + code + phone);
        return code+" "+phone;
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_CHANGED:
                    float value = msg.getData().getFloat(Constants.VALUE);
                    mPlot.addEntry(value);

                    break;
                case Constants.MESSAGE_EMERGENCY:
//
                    String contact = getContact();
                    if (contact != null) {
                        Toast.makeText(getApplicationContext(), "SMS Gönderiliyor, 20sn sonra arama yapıacak!", Toast.LENGTH_SHORT).show();
                        String messageToSend = "Application detect fall!" +
                                " - Message was sent automatically by FallDetector Application" +
                                "" ;
                        String number = contact;

                        SmsManager.getDefault().sendTextMessage(number, null, messageToSend, null,null);
                        try {
                            Thread.sleep(15000); // SMS gönderimi yapılır ve arama servisleri için 20 sn. beklenir..
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Intent callIntent = new Intent(android.content.Intent.ACTION_CALL, Uri.parse("tel:" + contact));
                        callIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(callIntent);
                    } else {
                        Toast.makeText(getApplicationContext(), "Lütfen iletişim bilgisi giriniz", Toast.LENGTH_SHORT).show();
                    }

                    String newHistory = DateFormat.getDateTimeInstance().format(new Date()) +"\n";

                    // save history to shared preferences
                    SharedPreferences sharedPreferences = getSharedPreferences(Constants.MyPREFERENCES, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    String oldHistory = sharedPreferences.getString(Constants.History,null); // get previous history
                    editor.putString(Constants.History, oldHistory + newHistory);
                    editor.commit();

                    // stop listening the sensor
                    mSwitchCompat.setChecked(false);

                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(),msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

}
