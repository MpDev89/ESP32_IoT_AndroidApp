package com.example.esp32_iot_androidapp.ui.connect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.esp32_iot_androidapp.BleActivity;
import com.example.esp32_iot_androidapp.GattUpdateReceiver;
import com.example.esp32_iot_androidapp.MainActivity;
import com.example.esp32_iot_androidapp.databinding.FragmentConnectBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment for scanning and connecting to BLE devices.
 */
public class ConnectFragment extends Fragment implements GattUpdateReceiver.GattUpdateListener {

    private static final String TAG = ConnectFragment.class.getSimpleName();
    private FragmentConnectBinding binding;
    List<ScanFilter> filters;
    ScanSettings settings;
    private final Handler scanBleHandler = new Handler();
    private static final long SCAN_PERIOD = 10000;
    BluetoothLeScanner bluetoothLeScanner;
    public BleActivity mBluetoothActivity;
    private ArrayList<String> deviceList;
    private ArrayAdapter<String> adapter;
    private GattUpdateReceiver mGattUpdateReceiver;


    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.S)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ConnectViewModel connectViewModel =
                new ViewModelProvider(this).get(ConnectViewModel.class);

        binding = FragmentConnectBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mGattUpdateReceiver = new GattUpdateReceiver(this);

        /* Build ScanSetting */
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        filters = new ArrayList<ScanFilter>();

        deviceList = new ArrayList<>();
        adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, deviceList);
        binding.lvBleDevicesList.setAdapter(adapter);

        Intent gattServiceIntent = new Intent(getActivity(), BleActivity.class);
        requireActivity().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        // Handle click
        binding.lvBleDevicesList.setOnItemClickListener((parent, view, position, id) -> {
            if(mBluetoothActivity.BleConnectionState == BleActivity.STATE_DISCONNECTED) {
                String selectedDevice = deviceList.get(position);
                Log.d(TAG, "Clicked: " + selectedDevice);
                String address = (selectedDevice.substring(selectedDevice.indexOf("-") + 1)).trim();
                Log.d(TAG, "address: " + address);
                MainActivity.BleDeviceAddress = address;
                ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN);
                scanLeDevice(false);
                if (mBluetoothActivity.connect(MainActivity.BleDeviceAddress)) {
                    MainActivity.BleDeviceName = selectedDevice.substring(0, selectedDevice.indexOf("-"));
                    binding.tvScannerInfo.setText("Connecting to " + MainActivity.BleDeviceName);
                }
            }else{
                binding.tvScannerInfo.setText("Device already connected to " + MainActivity.BleDeviceName);
            }
        });

        binding.btBleDisconn.setOnClickListener(v -> {
            if(mBluetoothActivity.BleConnectionState == BleActivity.STATE_CONNECTED){
                mBluetoothActivity.disconnect();
            }

        });


        return root;
    }

    /**
     * Called when the view previously created by onCreateView() has been detached from the fragment.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //no action binding shall be kept
    }

    /**
     * Starts or stops scanning for BLE devices.
     *
     * @param enable True to start scanning, false to stop.
     */
    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (bluetoothLeScanner == null) return;
        BluetoothManager bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return;
        }
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            if (binding != null) {
                binding.tvScannerInfo.setText("Please enable Bluetooth");
                if (deviceList != null && adapter != null) {
                    deviceList.clear();
                    adapter.notifyDataSetChanged();
                    MainActivity.BleDeviceAddress = "NA";
                    MainActivity.BleDeviceName = "NA";
                }
            }
        }else if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanBleHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    /**
     * Callback for BLE scan results.
     */
    private final ScanCallback leScanCallback = new ScanCallback() {

        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType The type of callback.
         * @param result A ScanResult object, which contains the advertising data.
         */
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            String deviceName = result.getDevice().getName();
            String deviceAddress = result.getDevice().getAddress(); // MAC address
            String deviceInfo = (deviceName != null ? deviceName : "Unknown") + " - " + deviceAddress;
            if (!deviceList.contains(deviceInfo)) {
                deviceList.add(deviceInfo);
                adapter.notifyDataSetChanged();
            }
        }

        /**
         * Callback when batch results are delivered.
         *
         * @param results List of scan results that are previously scanned.
         */
        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "BLE// onBatchScanResults");
            for (ScanResult sr : results) {
                Log.d(TAG, "ScanResult - Results" + sr.toString());
            }
        }

        /**
         * Callback when scan could not be started.
         *
         * @param errorCode Error code (one of SCAN_FAILED_*).
         */
        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE// onScanFailed");
            Log.e(TAG, "Scan Failed - Error Code: " + errorCode);
        }

    };

    /**
     * Manages the connection to the BleActivity service.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        /**
         * Called when a connection to the Service has been established.
         *
         * @param componentName The concrete component name of the service that has been connected.
         * @param service The IBinder of the Service's communication channel.
         */
        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BleActivity.LocalBinder binder = (BleActivity.LocalBinder) service;
            mBluetoothActivity = binder.getService();
            if (!mBluetoothActivity.initialize()) {
                Log.e(TAG, "ServiceConnection - Unable to initialize Bluetooth");
                requireActivity().finish();
            } else {
                bluetoothLeScanner = mBluetoothActivity.getBLeScanner();
                scanLeDevice(true);
            }
        }

        /**
         * Called when a connection to the Service has been lost.
         *
         * @param componentName The concrete component name of the service whose connection has been lost.
         */
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothActivity = null;
        }
    };

    /**
     * Called when the fragment is visible to the user and actively running.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(requireActivity(), mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (mBluetoothActivity != null) {
            if(!MainActivity.BleDeviceAddress.equals("NA")) {
                if (mBluetoothActivity.BleConnectionState == BleActivity.STATE_DISCONNECTED) {
                    binding.tvScannerInfo.setText("Disconnected");
                    MainActivity.BleDeviceAddress = "NA";
                    MainActivity.BleDeviceName = "NA";
                } else if (mBluetoothActivity.BleConnectionState == BleActivity.STATE_CONNECTED) {
                    if(MainActivity.NotifyEnabled) {
                        binding.tvScannerInfo.setText("Connected to " + MainActivity.BleDeviceName + " and notification enabled!");
                    }else{
                        binding.tvScannerInfo.setText("Connected to " + MainActivity.BleDeviceName + " and waiting for notification enabling...!");
                    }
                } else {
                    binding.tvScannerInfo.setText("Click on device to connect!");
                }
            }
            bluetoothLeScanner = mBluetoothActivity.getBLeScanner();
            scanLeDevice(true);
        }
        Log.d(TAG, "onResume");
    }

    /**
     * Called when the Fragment is no longer resumed.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(mGattUpdateReceiver);
        scanLeDevice(false);
    }

    /**
     * Callback for GATT connection event.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onGattConnected() {
        binding.tvScannerInfo.setText("Connected to "+MainActivity.BleDeviceName+"and waiting for notification enabling...");
    }

    /**
     * Callback for GATT disconnection event.
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onGattDisconnected() {
        MainActivity.NotifyEnabled = false;
        binding.tvScannerInfo.setText("Disconnected!");
    }

    /**
     * Callback for GATT services discovered event.
     */
    @Override
    public void onGattServicesDiscovered() {
        displayGattServices(mBluetoothActivity.getSupportedGattServices());
    }

    /**
     * Callback for when data is available from a characteristic.
     * @param data The data received.
     */
    @Override
    public void onDataAvailable(String data) {
        // no action
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onNotificationsEnabled() {
        binding.tvScannerInfo.setText("Connected to "+MainActivity.BleDeviceName+" and notification enabled");
        MainActivity.NotifyEnabled = true;
    }

    /**
     * Creates an IntentFilter for GATT update actions.
     *
     * @return The configured IntentFilter.
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleActivity.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleActivity.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleActivity.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleActivity.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleActivity.ACTION_NOTIFY_ENABLED);
        return intentFilter;
    }

    /**
     * Iterates through supported GATT Services/Characteristics to enable notifications.
     *
     * @param gattServices List of discovered GATT services.
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Get UUIDs for the characteristics we are interested in
        String humUuid = BleActivity.UUID_IOT_HUM_CH.toString();
        String tempUuid = BleActivity.UUID_IOT_TEMP_CH.toString();
        String slradUuid = BleActivity.UUID_IOT_SLRAD_CH.toString();

        // Loops through available GATT Services
        for (BluetoothGattService gattService : gattServices) {
            // Loops through available Characteristics
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                String charUuid = gattCharacteristic.getUuid().toString();

                // Check if this is one of the target characteristics
                if (charUuid.equalsIgnoreCase(humUuid) ||
                        charUuid.equalsIgnoreCase(tempUuid) ||
                        charUuid.equalsIgnoreCase(slradUuid)) {

                    // Enable notifications if the characteristic supports it
                    if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        Log.d(TAG, "Enabling notification for characteristic: " + charUuid);
                        mBluetoothActivity.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
        }
    }
}
