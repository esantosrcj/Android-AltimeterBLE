/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private Map<UUID, BluetoothGattCharacteristic> map = new HashMap<UUID, BluetoothGattCharacteristic>();

    private Button mButtonRead;
    private Button mButtonWrite;
    private Button mButtonGraph;

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
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

                // Displays the data from the RX Characteristic
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                displayData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_activity);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        mButtonRead = (Button) findViewById(R.id.button_read);
        mButtonRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.w(TAG, "Sending READ command");
                BluetoothGattCharacteristic txChar = map.get(BluetoothLeService.UUID_BLE_TX);

                byte b = 0x00;
                byte[] temp = "read".getBytes();
                byte[] tx = new byte[temp.length + 1];
                tx[0] = b;

                for (int i = 1; i < temp.length + 1; i++) {

                    tx[i] = temp[i - 1];
                }

                txChar.setValue(tx);
                mBluetoothLeService.writeCharacteristic(txChar);
            }
        });

        mButtonWrite = (Button) findViewById(R.id.button_write);
        mButtonWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.w(TAG, "Sending WRITE command");
                BluetoothGattCharacteristic characteristic = map.get(BluetoothLeService.UUID_BLE_TX);

                byte b = 0x00;
                byte[] temp = "write".getBytes();
                byte[] tx = new byte[temp.length + 1];
                tx[0] = b;

                for (int i = 1; i < temp.length + 1; i++) {

                    tx[i] = temp[i - 1];
                }

                characteristic.setValue(tx);
                mBluetoothLeService.writeCharacteristic(characteristic);
            }
        });

        mButtonGraph = (Button)findViewById(R.id.graph);
        mButtonGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //@Override
                //protected void onListItemClick(ListView l, View v, int position, long id) {
                    //final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                    //if (device == null) return;
                    //final Intent intent = new Intent(this, GraphActivity.class);
                    //intent.putExtra(GraphActivity.EXTRAS_DEVICE_NAME, device.getName());
                    //intent.putExtra(GraphActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    //if (mScanning) {
                      //  mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        //mScanning = false;
                    //}
                    //startActivity(intent);
                //}
                graphData(v);
            }
        });

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
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
        switch(item.getItemId()) {
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
                mConnectionState.setText(resourceId);
            }
        });
    }

    public void displayData(byte[] byteArray) {

        if (byteArray != null) {

            float value = ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).getFloat();
            mDataField.setText(String.format("%.2f", value));
        }
    }

    private void getGattService(BluetoothGattService gattService) {

        if (gattService == null) {

            Log.w(TAG, "No Gatt Service found");
            return;
        }

        BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(BluetoothLeService.UUID_BLE_TX);
        map.put(characteristic.getUuid(), characteristic);

        BluetoothGattCharacteristic characteristicRx = gattService.getCharacteristic(BluetoothLeService.UUID_BLE_RX);

        if (characteristicRx == null) {

            Log.w(TAG, "characteristicRx is NOT a characteristic of gattService");
            return;
        }
        mBluetoothLeService.setCharacteristicNotification(characteristicRx, true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public void onClickLEDOn(View v) {

        if (mBluetoothLeService != null) {

            byte b = 0x00;
            byte[] temp = "on".getBytes();
            byte[] data = new byte[temp.length + 1];
            data[0] = b;

            for (int i = 1; i < temp.length + 1; i++) {
                data[i] = temp[i - 1];
            }

            mBluetoothLeService.writeCustomCharacteristic(data);
        }
    }

    public void onClickLEDOff(View v) {

        if (mBluetoothLeService != null) {

            byte b = 0x00;
            byte[] temp = "off".getBytes();
            byte[] data = new byte[temp.length + 1];
            data[0] = b;

            for (int i = 1; i < temp.length + 1; i++) {
                data[i] = temp[i - 1];
            }

            mBluetoothLeService.writeCustomCharacteristic(data);
        }
    }

    public void graphData(View view) {

        final Intent intent = new Intent(this, GraphActivity.class);
        intent.putExtra(GraphActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(GraphActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(intent);
    }
}
