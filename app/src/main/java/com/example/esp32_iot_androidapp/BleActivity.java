package com.example.esp32_iot_androidapp;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
    public static int counter_notify = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.sentimentalise.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.sentimentalise.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.sentimentalise.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.sentimentalise.ACTION_DATA_AVAILABLE";
    public final static String ACTION_SEND_DATA = "com.example.sentimentalise.ACTION_SEND_DATA";
    public final static String ACTION_NOTIFY_ENABLED = "com.example.sentimentalise.ACTION_NOTIFY_ENABLED";
    public final static String EXTRA_DATA = "com.example.sentimentalise.EXTRA_DATA";

    public final static UUID UUID_IOT_HUM_CH = UUID.fromString(Esp32GattAttributes.HUM_CH);
    public final static UUID UUID_IOT_TEMP_CH = UUID.fromString(Esp32GattAttributes.TEMP_CH);
    public final static UUID UUID_IOT_SLRAD_CH = UUID.fromString(Esp32GattAttributes.SLRAD_CH);
    public static final UUID CCCD_UUID = UUID.fromString(Esp32GattAttributes.CCCD);
    private final Queue<Runnable> bleOperationQueue = new LinkedList<>();
    private boolean isExecutingBleOperation = false;
    /**
     * BroadcastReceiver that listens for bonding (pairing) state changes.
     * This is necessary to handle the pairing process with a BLE device.
     */
    private final BroadcastReceiver mBondingReceiver = new BroadcastReceiver() {
        /**
         * Handles the reception of an intent. In this case, it checks for bonding state changes.
         * @param context The Context in which the receiver is running.
         * @param intent The Intent being received.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.i(TAG, "Bonding successful. Discovering services.");
                    @SuppressLint("MissingPermission")
                    boolean result = mBluetoothGatt.discoverServices();
                    if (!result) {
                        Log.w(TAG, "discoverServices failed to start");
                    }
                    unregisterReceiver(mBondingReceiver);
                } else if (state == BluetoothDevice.BOND_NONE) {
                    Log.i(TAG, "Bonding failed or removed.");
                    unregisterReceiver(mBondingReceiver);
                }
            }
        }
    };


    /**
     * Adds a BLE operation to the queue.
     * @param operation The operation to be executed.
     */
    private synchronized void enqueueBleOperation(Runnable operation) {
        bleOperationQueue.add(operation);
        if (!isExecutingBleOperation) {
            executeNextBleOperation();
        }
    }

    /**
     * Executes the next BLE operation in the queue.
     */
    private synchronized void executeNextBleOperation() {
        if (isExecutingBleOperation || bleOperationQueue.isEmpty()) {
            return;
        }
        isExecutingBleOperation = true;
        Runnable operation = bleOperationQueue.poll();
        if (operation != null) {
            operation.run();
        }
    }
    /**
     * Signals that the current BLE operation has finished and the next one can be executed.
     * This should be called from the GATT callbacks.
     */
    private synchronized void signalBleOperationCompleted() {
        isExecutingBleOperation = false;
        executeNextBleOperation();
    }



    /**
     * Implements callback methods for GATT events that the app cares about.
     * These events include connection changes, services discovered, and characteristic interactions.
     */
    public final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote GATT server.
         * @param gatt GATT client
         * @param status Status of the connect or disconnect operation.
         * @param newState The new connection state.
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                BleConnectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                // Check the bond state of the device
                @SuppressLint("MissingPermission")
                int bondState = device.getBondState();
                if (bondState == BluetoothDevice.BOND_NONE) {
                    // The device is not bonded, so we need to initiate the bonding process.
                    Log.i(TAG, "Device not bonded. Starting pairing process...");
                    final IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
                    registerReceiver(mBondingReceiver, intentFilter);
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

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                BleConnectionState = STATE_DISCONNECTED;
                counter_notify = 0;
                disconnect();
                close();
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         * @param gatt GATT client
         * @param status GATT_SUCCESS if the remote device has been explored successfully.
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "GATT services discovered successfully.");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         * @param gatt GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the remote device.
         * @param status GATT_SUCCESS if the read operation was completed successfully.
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         * @param gatt GATT client invoked {@link BluetoothGatt#writeCharacteristic}
         * @param characteristic Characteristic that was written to the remote device.
         * @param status The result of the write operation.
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_SEND_DATA);
            }
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         * @param gatt GATT client
         * @param characteristic Characteristic that has changed.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                counter_notify ++;
                if (counter_notify == 3){
                    broadcastUpdate(ACTION_NOTIFY_ENABLED);
                }
                Log.d(TAG, "Descriptor write successful for " + descriptor.getUuid());
            } else {
                Log.w(TAG, "Descriptor write failed with status: " + status);
            }
            // IMPORTANT: Signal that the operation is complete to unblock the queue
            signalBleOperationCompleted();
        }
    };

    /**
     * Sends a broadcast to other components of the application.
     * @param action The action to be broadcasted.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Sends a broadcast with data from a BLE characteristic.
     * @param action The action to be broadcasted.
     * @param characteristic The characteristic containing the data.
     */
    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X", byteChar));
            if (UUID_IOT_HUM_CH.equals(characteristic.getUuid())) {
                intent.putExtra(EXTRA_DATA, "HUM_" + stringBuilder.toString());
            } else if (UUID_IOT_TEMP_CH.equals(characteristic.getUuid())) {
                intent.putExtra(EXTRA_DATA, "TEMP_" + stringBuilder.toString());
            } else if (UUID_IOT_SLRAD_CH.equals(characteristic.getUuid())) {
                intent.putExtra(EXTRA_DATA, "SLRAD_" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    private final IBinder mBinder = new LocalBinder();
    /**
     * Binder for the service, allowing clients to get a reference to the service.
     */
    public class LocalBinder extends Binder {
        /**
         * Returns the instance of BleActivity.
         * @return The BleActivity instance.
         */
        public BleActivity getService() {
            return BleActivity.this;
        }
    }

    /**
     * Called when a client binds to the service.
     * @param intent The Intent that was used to bind to this service.
     * @return Return an IBinder through which clients can call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Called when all clients have unbound from a particular interface published by the service.
     * @param intent The Intent that was used to bind to this service.
     * @return Return true if you would like to have the service's onRebind method later called when new clients bind to it.
     */
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /**
     * APIs for clients
     *
     * @return
     */
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
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                BleConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
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
    @SuppressLint("MissingPermission")
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
    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
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
    @SuppressLint("MissingPermission")
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic, String data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        byte[] dataArray = data.getBytes();
        characteristic.setValue(dataArray);
        return mBluetoothGatt.writeCharacteristic(characteristic);
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
        if (descriptor != null) {
            byte[] value;
            if (enabled) {
                int properties = characteristic.getProperties();
                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
                } else {
                    return; // Characteristic does not support notifications or indications
                }
            } else {
                value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
            }
            // Enqueue the write operation
            enqueueBleOperation(() -> {
                descriptor.setValue(value);
                mBluetoothGatt.writeDescriptor(descriptor);
            });
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
