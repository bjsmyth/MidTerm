package com.seniordesign.brandon.midtermproject;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class JoystickControl extends AppCompatActivity {
    private final static String TAG = JoystickControl.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattService mJoystickService;

    private final String CONTROL_SERVICE_UUID = "19B10000-E8F2-537E-4F6C-D104768A1214".toLowerCase();
    private final String SERVO_CHARACTERISTIC_UUID = "19B10001-E8F2-537E-4F6C-D104768A1214".toLowerCase();
    private final String MOTOR_CHARACTERISTIC_UUID = "19B10001-E8F2-537E-4F6C-D104768A1215".toLowerCase();
    private final String DIRECTION_CHARACTERISTIC_UUID = "19B10001-E8F2-537E-4F6C-D104768A1216".toLowerCase();

    private int sendOffset = 0;
    private int sendAngle = 0;
    private int direction = 0;
    private BluetoothGattCharacteristic servoCharacteristic;
    private BluetoothGattCharacteristic motorCharacteristic;
    private BluetoothGattCharacteristic directionCharacteristic;
    private boolean controlWrite = false;
    private int writeIterator = 0;
    private int[] changedValues = {0, 0, 0};

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();


            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mJoystickService = mBluetoothLeService.getSupportedGattServices().get(2);

                Log.e("Service", mJoystickService.getUuid().toString());

                servoCharacteristic = mJoystickService.getCharacteristics().get(0);
                motorCharacteristic = mJoystickService.getCharacteristics().get(1);
                directionCharacteristic = mJoystickService.getCharacteristics().get(2);
                final int servoProp = servoCharacteristic.getProperties();
                final int motorProp = motorCharacteristic.getProperties();
                final int directionProp = directionCharacteristic.getProperties();

                controlWrite = ((servoProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)  //All characteristics MUST be writable
                        && ((motorProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
                        && ((directionProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.joystick);

        Bundle b = getIntent().getExtras();

        //mDeviceName = b.getString("deviceName");
        //mDeviceAddress = b.getString("deviceAddress");

        mDeviceName = "LineFoll";
        mDeviceAddress = "98:4F:EE:0F:96:0F";

        Log.e("NameAddress", mDeviceName + mDeviceAddress);

        getSupportActionBar().setTitle("Manual Control");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        final TextView angleView = (TextView) findViewById(R.id.tv_angle);
        final TextView offsetView = (TextView) findViewById(R.id.tv_offset);

        final String angleNoneString = "Angle: none";
        final String angleValueString = "Angle: %d";
        final String offsetNoneString = "Offset: none";

        Joystick joystick = (Joystick) findViewById(R.id.joystick);

        joystick.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {

            }

            @Override
            public void onDrag(float degrees, float offset) {

                float temp = offset * 255f;
                sendOffset = (int) temp;

                sendAngle = (int) degrees;

                if (sendAngle < 0) {
                    sendAngle = -sendAngle;
                    direction = 0;
                } else {
                    direction = 1;
                }

                angleView.setText(String.format(angleValueString, sendAngle));
                offsetView.setText("Offset: " + sendOffset);
                long sleep = 100;
                if (mConnected && controlWrite) {
                    switch (writeIterator) {
                        case 0:
                            if (sendAngle != changedValues[0]) {
                                servoCharacteristic.setValue(sendAngle, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                                mBluetoothLeService.writeCharacteristic(servoCharacteristic);
                                try {
                                    Thread.sleep(sleep);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            break;
                        case 1:
                            motorCharacteristic.setValue(sendOffset, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            mBluetoothLeService.writeCharacteristic(motorCharacteristic);
                            try {
                                Thread.sleep(sleep);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                        case 2:
                            directionCharacteristic.setValue(direction, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                            mBluetoothLeService.writeCharacteristic(directionCharacteristic);
                            try {
                                Thread.sleep(sleep);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                    writeIterator++;
                    writeIterator %= 3;
                }
            }

            @Override
            public void onUp() {
                angleView.setText(angleNoneString);
                offsetView.setText(offsetNoneString);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mConnectionState.setText(resourceId);
            }
        });
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
