package com.example.esp32_iot_androidapp.ui.connect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.esp32_iot_androidapp.BleActivity;
import com.example.esp32_iot_androidapp.Esp32GattAttributes;
import com.example.esp32_iot_androidapp.GattUpdateReceiver;
import com.example.esp32_iot_androidapp.MainActivity;
import com.example.esp32_iot_androidapp.databinding.FragmentConnectBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class ConnectFragment extends Fragment implements GattUpdateReceiver.GattUpdateListener {

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
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

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
                System.out.println("Clicked: " + selectedDevice);
                String address = (selectedDevice.substring(selectedDevice.indexOf("-") + 1)).trim();
                System.out.println("address: " + address);
                // 👉 Perform any action here, e.g. connect to Bluetooth device
                MainActivity.BleDeviceAddress = address;
                ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_SCAN);
                bluetoothLeScanner.stopScan(leScanCallback);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    //Start the device searching
    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            scanBleHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    bluetoothLeScanner.stopScan(leScanCallback);
                    //status.setText("Status: Device Not found");
                }
            }, SCAN_PERIOD);
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    /* Scan result for SDK >= 21 */
    private final ScanCallback leScanCallback = new ScanCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            System.out.println("BLE// onScanResult");
            super.onScanResult(callbackType, result);

            System.out.println("callbackType: "+ callbackType);
            System.out.println("result: " + result.toString());
            System.out.println("Device Name: "+ result.getDevice().getName());
            System.out.println("Signal: " + result.getRssi());

            String deviceName = result.getDevice().getName();
            String deviceAddress = result.getDevice().getAddress(); // MAC address

            String deviceInfo = (deviceName != null ? deviceName : "Unknown") + " - " + deviceAddress;

            if (!deviceList.contains(deviceInfo)) {
                deviceList.add(deviceInfo);
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            System.out.println("BLE// onBatchScanResults");
            for (ScanResult sr : results) {
                System.out.println("ScanResult - Results" + sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            System.out.println("BLE// onScanFailed");
            System.out.println("Scan Failed - Error Code: " + errorCode);
        }

    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @SuppressLint("MissingPermission")
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            BleActivity.LocalBinder binder = (BleActivity.LocalBinder) service;
            mBluetoothActivity = binder.getService();
            if (!mBluetoothActivity.initialize()) {
                System.out.println("ServiceConnection - Unable to initialize Bluetooth");
                requireActivity().finish();
            } else {
                bluetoothLeScanner = mBluetoothActivity.getBLeScanner();
                scanLeDevice(true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothActivity = null;
        }
    };


    @Override
    public void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(requireActivity(), mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
        if (mBluetoothActivity != null) {
            if(!MainActivity.BleDeviceAddress.equals("NA")) {
                if (mBluetoothActivity.BleConnectionState == BleActivity.STATE_DISCONNECTED) {
                    binding.tvScannerInfo.setText("Disconnected!");
                    MainActivity.BleDeviceAddress = "NA";
                    MainActivity.BleDeviceName = "NA";
                } else if (mBluetoothActivity.BleConnectionState == BleActivity.STATE_CONNECTED) {
                    binding.tvScannerInfo.setText("Connected to " + MainActivity.BleDeviceName + "!");
                } else {
                    binding.tvScannerInfo.setText("Click on device to connect!");
                }
            }
            bluetoothLeScanner = mBluetoothActivity.getBLeScanner();
            scanLeDevice(true);
        }
        System.out.println("onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    public void onGattConnected() {
        binding.tvScannerInfo.setText("Connected to "+MainActivity.BleDeviceName+"!");
    }

    @Override
    public void onGattDisconnected() {
        binding.tvScannerInfo.setText("Disconnected!");
    }

    @Override
    public void onGattServicesDiscovered() {
        displayGattServices(mBluetoothActivity.getSupportedGattServices());
    }

    @Override
    public void onDataAvailable(String data) {
        // Left blank as requested
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleActivity.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleActivity.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleActivity.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleActivity.ACTION_SEND_DATA);
        intentFilter.addAction(BleActivity.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, Esp32GattAttributes.lookup(uuid, "unknownServiceString"));
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
                currentCharaData.put(LIST_NAME, Esp32GattAttributes.lookup(uuid, "unknownCharaString"));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                if (currentCharaData.containsValue("HumidityCharacteristic") || currentCharaData.containsValue("TemperatureCharacteristic")
                        || currentCharaData.containsValue("SolarRadiationCharacteristic")) {
                    final int CharaProp = gattCharacteristic.getProperties();
                    if (CharaProp > 0) {
                        mBluetoothActivity.setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }
}
