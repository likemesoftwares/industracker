/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 */

package com.industracker;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mbientlab.metawear.AsyncOperation;
import com.mbientlab.metawear.Message;
import com.mbientlab.metawear.MetaWearBleService;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.RouteManager;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.module.Bmi160Accelerometer;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.Logging;

public class MainActivity extends Activity implements ServiceConnection {

    private static final String LOG_TAG = "INDUS_TRACKER";
    private MetaWearBleService.LocalBinder serviceBinder;
    private MetaWearBoard mwBoard;
    private Bmi160Accelerometer accelModule;
    private Debug debugModule;
    private Logging loggingModule;
    private boolean freshStart=true;
    private View dangerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getApplicationContext().bindService(new Intent(this, MetaWearBleService.class), this, Context.BIND_AUTO_CREATE);

        findViewById(R.id.start_accel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSampling();
            }
        });
        findViewById(R.id.stop_accel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSampling();

            }
        });
        findViewById(R.id.reset_board).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mwBoard != null) {
                    hideImage();
                    freshStart=true;
                    writeToDB("nofall");
                }
            }
        });

        dangerImage = findViewById(R.id.imageView);
        dangerImage.setVisibility(View.INVISIBLE);
    }

    private void startSampling() {
        if(mwBoard != null && accelModule != null) {
            accelModule.enableAxisSampling();
            accelModule.start();
            hideImage();
            freshStart=true;
            Log.d(LOG_TAG,"Strated Axis Sampling");
        }
    }

    private void stopSampling() {
        if(mwBoard != null && accelModule!= null) {
            freshStart=true;
            writeToDB("nofall");
            accelModule.stop();
            accelModule.disableAxisSampling();
            Log.d(LOG_TAG,"Stoped Axis Sampling");
        }
    }

    private void hideImage() {
        dangerImage.post(new Runnable() {
            @Override
            public void run() {
                dangerImage.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }


    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        serviceBinder = (MetaWearBleService.LocalBinder) service;

        String mwMacAddress = "E0:CC:86:A3:F3:77";   ///< Put your board's MAC address here
        BluetoothManager btManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (btManager == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth Not On", Toast.LENGTH_SHORT).show();
            Log.d(LOG_TAG, "Bluetooth Not On");
        } else {
            if (btManager.getAdapter() != null) {
                BluetoothDevice btDevice = btManager.getAdapter().getRemoteDevice(mwMacAddress);
                if (btDevice != null) {
                    configureBoardBMI16(btDevice);
                } else {
                    Toast.makeText(getApplicationContext(), "Mbient Device Not found", Toast.LENGTH_SHORT).show();
                    Log.d(LOG_TAG, "Mbient Device Not found");
                }
            } else {
                Log.d(LOG_TAG, "Bluetooth Adapter Not found");
            }
        }
    }



    private void configureBoardBMI16(BluetoothDevice btDevice) {
        mwBoard = serviceBinder.getMetaWearBoard(btDevice);
        mwBoard.setConnectionStateHandler(new MetaWearBoard.ConnectionStateHandler() {
                                              @Override
                                              public void connected() {
                                                  Log.d(LOG_TAG, "Connected==============================");
                                                  try {
                                                      accelModule = mwBoard.getModule(Bmi160Accelerometer.class);
                                                      accelModule.enableLowHighDetection(false, true, true, true);
                                                      accelModule.configureLowHighDetection();
                                                      accelModule.routeData().fromLowHigh().stream("low_high_stream").commit()
                                                              .onComplete(new AsyncOperation.CompletionHandler<RouteManager>() {
                                                                  @Override
                                                                  public void success(RouteManager result) {
                                                                      result.subscribe("low_high_stream", new RouteManager.MessageHandler() {
                                                                          @Override
                                                                          public void process(Message msg) {
                                                                              Log.d(LOG_TAG,"Got the message");
                                                                              Log.d(LOG_TAG, "High motion! " + msg.getData(Bmi160Accelerometer.LowHighResponse.class));
                                                                              writeToDB("fall");
                                                                              showImage();
                                                                          }
                                                                      });
                                                                  }
                                                              });
                                                  } catch (UnsupportedModuleException e) {
                                                      e.printStackTrace();
                                                  }
                                              }

                                              @Override
                                              public void disconnected() {
                                                  Log.d(LOG_TAG, "Disconnected=====================================");
                                              }

                                          }

        );
        mwBoard.connect();
    }


    @Override
    public void onServiceDisconnected(ComponentName name) {

    }


    private void writeToDB(String status) {
        if(freshStart) {
            freshStart=false;
            Log.d(LOG_TAG, "Writing to DB");
            DatabaseReference userTable;
            userTable = FirebaseDatabase.getInstance().getReference("UserDB");
            String email = new String("abc@yahoo.com");
            userTable.child("User").child(email.replace("@", "a").replace(".", "b")).setValue(status);
            Log.d(LOG_TAG, "Writing to DB finished with value " + status);
        }else{
            Log.d(LOG_TAG,"Skipping writing to DB" + status);
        }
    }

    private void showImage() {
        dangerImage.post(new Runnable() {

            @Override
            public void run() {
                Log.d(LOG_TAG,"Showing the Danger Image");
                dangerImage.setVisibility(View.VISIBLE);
            }
        });
    }


    @Override
    public void onBackPressed() {
        freshStart=false;
        super.onBackPressed();
        if(mwBoard != null){
            mwBoard.disconnect();
        }
    }
}