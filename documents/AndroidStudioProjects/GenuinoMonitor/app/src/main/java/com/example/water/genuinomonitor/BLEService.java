package com.example.water.genuinomonitor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;

/**
 * Created by water on 2017-03-07.
 */

public class BLEService extends Service {

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;

    private final IBinder mBinder = new LocalBinder();

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String GYRO_X_DATA =
            "com.example.bluetooth.le.GYRO_X_DATA";
    public final static String GYRO_Y_DATA =
            "com.example.bluetooth.le.GYRO_Y_DATA";
    public final static String GYRO_Z_DATA =
            "com.example.bluetooth.le.GYRO_Z_DATA";
    public final static String ACCL_X_DATA =
            "com.example.bluetooth.le.ACCL_X_DATA";
    public final static String ACCL_Y_DATA =
            "com.example.bluetooth.le.ACCL_Y_DATA";
    public final static String ACCL_Z_DATA =
            "com.example.bluetooth.le.ACCL_Z_DATA";
    public final static String POS_X_DATA =
            "com.example.bluetooth.le.POS_X_DATA";
    public final static String POS_Y_DATA =
            "com.example.bluetooth.le.POS_Y_DATA";
    public final static String POS_Z_DATA =
            "com.example.bluetooth.le.POS_Z_DATA";
    public final static String SPEED_X_DATA =
            "com.example.bluetooth.le.SPEED_X_DATA";
    public final static String SPEED_Y_DATA =
            "com.example.bluetooth.le.SPEED_Y_DATA";
    public final static String SPEED_Z_DATA =
            "com.example.bluetooth.le.SPEED_Z_DATA";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_GYRO_X_MEASUREMENT =
            UUID.fromString(gattAttributes.GYRO_X_MEASUREMENT);
    public final static UUID UUID_GYRO_Y_MEASUREMENT =
            UUID.fromString(gattAttributes.GYRO_Y_MEASUREMENT);
    public final static UUID UUID_GYRO_Z_MEASUREMENT =
            UUID.fromString(gattAttributes.GYRO_Z_MEASUREMENT);
    public final static UUID UUID_ACCL_X_MEASUREMENT =
            UUID.fromString(gattAttributes.ACCL_X_MEASUREMENT);
    public final static UUID UUID_ACCL_Y_MEASUREMENT =
            UUID.fromString(gattAttributes.ACCL_Y_MEASUREMENT);
    public final static UUID UUID_ACCL_Z_MEASUREMENT =
            UUID.fromString(gattAttributes.ACCL_Z_MEASUREMENT);
    public final static UUID UUID_POS_X_CALCULATION =
            UUID.fromString(gattAttributes.POS_X_CALCULATION);
    public final static UUID UUID_POS_Y_CALCULATION =
            UUID.fromString(gattAttributes.POS_Y_CALCULATION);
    public final static UUID UUID_POS_Z_CALCULATION =
            UUID.fromString(gattAttributes.POS_Z_CALCULATION);
    public final static UUID UUID_SPEED_X_CALCULATION =
            UUID.fromString(gattAttributes.SPEED_X_CALCULATION);
    public final static UUID UUID_SPEED_Y_CALCULATION =
            UUID.fromString(gattAttributes.SPEED_Y_CALCULATION);
    public final static UUID UUID_SPEED_Z_CALCULATION =
            UUID.fromString(gattAttributes.SPEED_Z_CALCULATION);

    public class LocalBinder extends Binder {
        BLEService getService() {
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent){
        return mBinder;
    }

    @Override
    public void onCreate(){
    }

    @Override
    public void onDestroy() {
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {

        final Intent intent = new Intent(action);
        int format = -1;

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_GYRO_X_MEASUREMENT.equals(characteristic.getUuid())) {
            intent.putExtra(GYRO_X_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_GYRO_Y_MEASUREMENT.equals(characteristic.getUuid())) {
            intent.putExtra(GYRO_Y_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_GYRO_Z_MEASUREMENT.equals(characteristic.getUuid())) {
            intent.putExtra(GYRO_Z_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if (UUID_ACCL_X_MEASUREMENT.equals(characteristic.getUuid())) {
            intent.putExtra(ACCL_X_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_ACCL_Y_MEASUREMENT.equals(characteristic.getUuid())) {
            intent.putExtra(ACCL_Y_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_ACCL_Z_MEASUREMENT.equals(characteristic.getUuid())) {
            intent.putExtra(ACCL_Z_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if (UUID_POS_X_CALCULATION.equals(characteristic.getUuid())) {
            intent.putExtra(POS_X_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_POS_Y_CALCULATION.equals(characteristic.getUuid())) {
            intent.putExtra(POS_Y_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_POS_Z_CALCULATION.equals(characteristic.getUuid())) {
            intent.putExtra(POS_Z_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if (UUID_SPEED_X_CALCULATION.equals(characteristic.getUuid())) {
            intent.putExtra(SPEED_X_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_SPEED_Y_CALCULATION.equals(characteristic.getUuid())) {
            intent.putExtra(SPEED_Y_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else if(UUID_SPEED_Z_CALCULATION.equals(characteristic.getUuid())) {
            intent.putExtra(SPEED_Z_DATA, String.valueOf(changeFloatByteOrder(characteristic.getValue(),ByteOrder.LITTLE_ENDIAN)));
        }
        else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public static float changeFloatByteOrder(byte[] v, ByteOrder order) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.put(v).flip();
        return b.order(order).getFloat();
    }

    public static int changeIntByteOrder(byte[] v, ByteOrder order) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.put(v).flip();
        return b.order(order).getInt();
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }
}
