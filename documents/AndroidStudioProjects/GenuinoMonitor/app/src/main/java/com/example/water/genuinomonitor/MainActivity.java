package com.example.water.genuinomonitor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {

    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBLEScanner;
    private BLEService mBLEService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mGyroXCharacteristic;
    private BluetoothGattCharacteristic mGyroYCharacteristic;
    private BluetoothGattCharacteristic mGyroZCharacteristic;
    private BluetoothGattCharacteristic mAcclXCharacteristic;
    private BluetoothGattCharacteristic mAcclYCharacteristic;
    private BluetoothGattCharacteristic mAcclZCharacteristic;
    private BluetoothGattCharacteristic mPosXCharacteristic;
    private BluetoothGattCharacteristic mPosYCharacteristic;
    private BluetoothGattCharacteristic mPosZCharacteristic;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private Genuino101 mGenuino;

    private boolean mScanning;
    private boolean mScanned;
    private boolean mfindGenuino;
    private TextView mDeviceName;
    private TextView mDeviceAddress;
    private TextView mDeviceStatus;
    private TextView mGyroGx;
    private TextView mGyroGy;
    private TextView mGyroGz;
    private TextView mAcclAx;
    private TextView mAcclAy;
    private TextView mAcclAz;
    private TextView mPosX;
    private TextView mPosY;
    private TextView mPosZ;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private final String DEFAULT_BLE_ADDRESS = "98:4F:EE:10:7F:E5";

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private String mServiceAddress;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGenuino = new Genuino101();
        mHandler = new Handler();
        mBLEService = new BLEService();

        BackThread thread = new BackThread();
        thread.setDaemon(true);
        thread.start();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mBLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        // Checks if Bluetooth LE Scanner is available.
        if (mBLEScanner == null) {
            Toast.makeText(this, R.string.ble_scanner_not_find, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDeviceName = (TextView) findViewById(R.id.deviceName);
        mDeviceAddress  = (TextView) findViewById(R.id.deviceAddress);
        mDeviceStatus = (TextView) findViewById(R.id.deviceStatus);
        mGyroGx  = (TextView) findViewById(R.id.gyroGx);
        mGyroGy  = (TextView) findViewById(R.id.gyroGy);
        mGyroGz  = (TextView) findViewById(R.id.gyroGz);
        mAcclAx  = (TextView) findViewById(R.id.acclAx);
        mAcclAy  = (TextView) findViewById(R.id.acclAy);
        mAcclAz  = (TextView) findViewById(R.id.acclAz);
        mPosX = (TextView) findViewById(R.id.posX);
        mPosY = (TextView) findViewById(R.id.posY);
        mPosZ = (TextView) findViewById(R.id.posZ);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent gattServiceIntent = new Intent(this, BLEService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(mGenuino.getBLE().getConnectState()) {
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            if(mfindGenuino) {
                menu.findItem(R.id.menu_scan).setVisible(false);
                menu.findItem(R.id.menu_connect).setVisible(true);
                menu.findItem(R.id.menu_disconnect).setVisible(false);
                menu.findItem(R.id.menu_stop).setVisible(true);
                menu.findItem(R.id.menu_refresh).setActionView(null);
            } else {
                if (mScanning) {
                    menu.findItem(R.id.menu_scan).setVisible(false);
                    menu.findItem(R.id.menu_connect).setVisible(false);
                    menu.findItem(R.id.menu_disconnect).setVisible(false);
                    menu.findItem(R.id.menu_stop).setVisible(true);
                    menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
                } else {
                    menu.findItem(R.id.menu_scan).setVisible(true);
                    menu.findItem(R.id.menu_connect).setVisible(false);
                    menu.findItem(R.id.menu_disconnect).setVisible(false);
                    menu.findItem(R.id.menu_stop).setVisible(false);
                    menu.findItem(R.id.menu_refresh).setActionView(null);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanBLEDevice(true);
                break;
            case R.id.menu_stop:
                mBLEService.disconnect();
                scanBLEDevice(false);
                break;
            case R.id.menu_connect:
                mBLEService.connect(mServiceAddress);
                break;
            case R.id.menu_disconnect:
                mBLEService.disconnect();
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (mBLEService != null) {
            final boolean result = mBLEService.connect(mServiceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        scanBLEDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanBLEDevice(false);
    }

    private void scanBLEDevice(final boolean enable) {

        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        filters = new ArrayList<ScanFilter>();
        ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(DEFAULT_BLE_ADDRESS).build();
        filters.add(filter);

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBLEScanner.startScan(filters, settings, mScanCallback);
            updateConnectionState(R.string.ble_scanning);
            invalidateOptionsMenu();
        } else {
            mScanning = false;
            mfindGenuino = false;
            mBLEScanner.stopScan(mScanCallback);
            updateConnectionState(R.string.ble_stopscan);
            mDeviceName.setText("No Device");
            mDeviceAddress.setText("No Device");
            mServiceAddress = null;
            invalidateOptionsMenu();
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

        private void processResult(final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(!mfindGenuino) {
                        mGenuino.getBLE().setName(result.getDevice().getName());
                        mDeviceName.setText(mGenuino.getBLE().getName());
                        mGenuino.getBLE().setAddress(result.getDevice().getAddress());
                        mServiceAddress = mGenuino.getBLE().getAddress();
                        mDeviceAddress.setText(mServiceAddress);
                        updateConnectionState(R.string.ble_scan_finish);
                        mfindGenuino = true;
                        invalidateOptionsMenu();
                    }
                }
            });
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service){
            mBLEService = ((BLEService.LocalBinder) service).getService();
            if (!mBLEService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBLEService.connect(mServiceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0){
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
            if (BLEService.ACTION_GATT_CONNECTED.equals(action)) {
                mGenuino.getBLE().setConnect();
                updateConnectionState(R.string.ble_connected);
                invalidateOptionsMenu();
            } else if (BLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mGenuino.getBLE().setDisconnect();
                updateConnectionState(R.string.ble_disconnected);
                clearUI();
                invalidateOptionsMenu();
            } else if (BLEService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Select Gyro services and characteristics on the user interface.
                selectGyroGattServices(mBLEService.getSupportedGattServices());
            } else if (BLEService.ACTION_DATA_AVAILABLE.equals(action)) {
                float gx = mGenuino.getGyroscope().getGx();
                float gy = mGenuino.getGyroscope().getGy();
                float gz = mGenuino.getGyroscope().getGz();
                float ax = mGenuino.getAccelerometer().getAx();
                float ay = mGenuino.getAccelerometer().getAy();
                float az = mGenuino.getAccelerometer().getAz();
                float px = mGenuino.getPositionX();
                float py = mGenuino.getPositionY();
                float pz = mGenuino.getPositionZ();

                if(intent.getStringExtra(BLEService.GYRO_X_DATA) != null) {
                    gx = Float.parseFloat(intent.getStringExtra(BLEService.GYRO_X_DATA));
                }
                if(intent.getStringExtra(BLEService.GYRO_Y_DATA) != null) {
                    gy = Float.parseFloat(intent.getStringExtra(BLEService.GYRO_Y_DATA));
                }
                if(intent.getStringExtra(BLEService.GYRO_Z_DATA) != null) {
                    gz = Float.parseFloat(intent.getStringExtra(BLEService.GYRO_Z_DATA));
                }
                if(intent.getStringExtra(BLEService.ACCL_X_DATA) != null) {
                    ax = Float.parseFloat(intent.getStringExtra(BLEService.ACCL_X_DATA));
                }
                if(intent.getStringExtra(BLEService.ACCL_Y_DATA) != null) {
                    ay = Float.parseFloat(intent.getStringExtra(BLEService.ACCL_Y_DATA));
                }
                if(intent.getStringExtra(BLEService.ACCL_Z_DATA) != null) {
                    az = Float.parseFloat(intent.getStringExtra(BLEService.ACCL_Z_DATA));
                }
                if(intent.getStringExtra(BLEService.POS_X_DATA) != null) {
                    px = Float.parseFloat(intent.getStringExtra(BLEService.POS_X_DATA));
                }
                if(intent.getStringExtra(BLEService.POS_Y_DATA) != null) {
                    py = Float.parseFloat(intent.getStringExtra(BLEService.POS_Y_DATA));
                }
                if(intent.getStringExtra(BLEService.POS_Z_DATA) != null) {
                    pz = Float.parseFloat(intent.getStringExtra(BLEService.POS_Z_DATA));
                }
                mGenuino.getGyroscope().updateData(gx, gy, gz);
                mGenuino.getAccelerometer().updateData(ax, ay, az);
                mGenuino.setPotision(px, py, pz);
                displayData(mGenuino.getGyroscope().getData());
                displayData(mGenuino.getAccelerometer().getData());
                mPosX.setText(String.valueOf(mGenuino.getPositionX()));
                mPosY.setText(String.valueOf(mGenuino.getPositionY()));
                mPosZ.setText(String.valueOf(mGenuino.getPositionZ()));
            }
        }
    };

    private void clearUI() {
        mGyroGx.setText(R.string.no_data);
        mGyroGy.setText(R.string.no_data);
        mGyroGz.setText(R.string.no_data);
        mAcclAx.setText(R.string.no_data);
        mAcclAy.setText(R.string.no_data);
        mAcclAz.setText(R.string.no_data);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceStatus.setText(resourceId);
            }
        });
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void selectGyroGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.ble_unknown_service);
        String unknownCharaString = getResources().getString(R.string.ble_unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, gattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                if(uuid.equals(BLEService.UUID_GYRO_X_MEASUREMENT.toString())) {
                    mGyroXCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_GYRO_Y_MEASUREMENT.toString())) {
                    mGyroYCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_GYRO_Z_MEASUREMENT.toString())) {
                    mGyroZCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_ACCL_X_MEASUREMENT.toString())) {
                    mAcclXCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_ACCL_Y_MEASUREMENT.toString())) {
                    mAcclYCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_ACCL_Z_MEASUREMENT.toString())) {
                    mAcclZCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_POS_X_CALCULATION.toString())) {
                    mPosXCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_POS_Y_CALCULATION.toString())) {
                    mPosYCharacteristic = gattCharacteristic;
                }
                else if(uuid.equals(BLEService.UUID_POS_Z_CALCULATION.toString())) {
                    mPosZCharacteristic = gattCharacteristic;
                }
                currentCharaData.put(LIST_NAME, gattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }

            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private void displayData(Gyroscope gyro) {
        mGyroGx.setText(String.valueOf(gyro.getGx()));
        mGyroGy.setText(String.valueOf(gyro.getGy()));
        mGyroGz.setText(String.valueOf(gyro.getGz()));
    }

    private void displayData(Accelerometer accl) {
        mAcclAx.setText(String.valueOf(accl.getAx()));
        mAcclAy.setText(String.valueOf(accl.getAy()));
        mAcclAz.setText(String.valueOf(accl.getAz()));
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BLEService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    class BackThread extends Thread {
        public void run() {
            while(true) {
                updateData(mGyroXCharacteristic);
                updateData(mGyroYCharacteristic);
                updateData(mGyroZCharacteristic);
                updateData(mAcclXCharacteristic);
                updateData(mAcclYCharacteristic);
                updateData(mAcclZCharacteristic);
                updateData(mPosXCharacteristic);
                updateData(mPosYCharacteristic);
                updateData(mPosZCharacteristic);
            }
        }

        void updateData(BluetoothGattCharacteristic characteristic) {
            if (characteristic != null) {
                final int charaProp = characteristic.getProperties();

                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBLEService.setCharacteristicNotification(mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                    }
                    mBLEService.readCharacteristic(characteristic);
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifyCharacteristic = characteristic;
                    mBLEService.setCharacteristicNotification(characteristic, true);
                }
            }
        }
    }
}