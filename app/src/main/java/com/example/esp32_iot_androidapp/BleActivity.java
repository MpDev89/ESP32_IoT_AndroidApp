package com.example.esp32_iot_androidapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.UUID;


public class BleActivity extends Service {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager mBluetoothManager;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    public int BleConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice device;

    private final static String TAG = BleActivity.class.getSimpleName();

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.sentimentalise.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.sentimentalise.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.sentimentalise.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.sentimentalise.ACTION_DATA_AVAILABLE";
    public final static String ACTION_SEND_DATA = "com.example.sentimentalise.ACTION_SEND_DATA";
    public final static String EXTRA_DATA = "com.example.sentimentalise.EXTRA_DATA";

    public final static UUID UUID_IOT_HUM_CH = UUID.fromString(Esp32GattAttributes.HUM_CH);
    public final static UUID UUID_IOT_TEMP_CH = UUID.fromString(Esp32GattAttributes.TEMP_CH);
    public final static UUID UUID_IOT_SLRAD_CH = UUID.fromString(Esp32GattAttributes.SLRAD_CH);


    private final Handler bleHandler = new Handler();


    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                BleConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                // Check the bond state of the device
                @SuppressLint("MissingPermission")
                int bondState = device.getBondState();
                if (bondState == BluetoothDevice.BOND_NONE) {
                    // The device is not bonded, so we need to initiate the bonding process.
                    Log.i(TAG, "Device not bonded. Starting pairing process...");
                    @SuppressLint("MissingPermission")
                    boolean bondingInitiated = device.createBond();
                    if (!bondingInitiated) {
                        Log.e(TAG, "Failed to start pairing process.");
                    }

                }else if (bondState == BluetoothDevice.BOND_BONDED) {
                        // The device is already bonded, so we can discover services immediately.
                        Log.i(TAG, "Device is already bonded. Discovering services.");
                        @SuppressLint("MissingPermission")
                        boolean result = gatt.discoverServices();
                        if (!result) {
                            Log.w(TAG, "discoverServices failed to start");
                        }
                } else if (bondState == BluetoothDevice.BOND_BONDING) {
                    // Bonding is in progress.
                    Log.i(TAG, "Bonding is in progress.");
                }
//                Log.i(TAG, "Connected to GATT server.");
//                // Attempts to discover services after successful connection.
//                Log.i(TAG, "Attempting to start service discovery:" +mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                BleConnectionState = STATE_DISCONNECTED;
                disconnect();
                close();
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
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_SEND_DATA);
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

        if (UUID_IOT_HUM_CH.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X", byteChar));
                intent.putExtra(EXTRA_DATA, "HUM_" + stringBuilder.toString());
            }
        }else if (UUID_IOT_TEMP_CH.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X", byteChar));
                intent.putExtra(EXTRA_DATA, "TEMP_" + stringBuilder.toString());
            }
        }else if (UUID_IOT_SLRAD_CH.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X", byteChar));
                intent.putExtra(EXTRA_DATA, "SLRAD_" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public BleActivity getService() {
            return BleActivity.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /** method for clients
     * @return*/
    public BluetoothLeScanner getBLeScanner() {
        return bluetoothLeScanner;
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
        bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
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
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return false;
            }else {
                if (mBluetoothGatt.connect()) {
                    BleConnectionState = STATE_CONNECTING;
                    return true;
                } else {
                    return false;
                }
            }
        }
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        Log.d(TAG, "Trying to create a new connection.");
        BleConnectionState = STATE_CONNECTING;
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }else {
            mBluetoothGatt.disconnect();
        }


    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }else {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to write.
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        byte[] dataArray = data.getBytes();
        characteristic.setValue(dataArray);
        @SuppressLint("MissingPermission") boolean status = mBluetoothGatt.writeCharacteristic(characteristic);
        return status;
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (UUID_IOT_HUM_CH.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(Esp32GattAttributes.HUM_CH));
            if (descriptor != null){
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }

        }else if (UUID_IOT_TEMP_CH.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(Esp32GattAttributes.TEMP_CH));
            if (descriptor != null){
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
            }
        }
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

}
