package com.example.esp32_iot_androidapp.ui.measure;

import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.esp32_iot_androidapp.BleActivity;
import com.example.esp32_iot_androidapp.GattUpdateReceiver;
import com.example.esp32_iot_androidapp.MainActivity;
import com.example.esp32_iot_androidapp.R;
import com.example.esp32_iot_androidapp.SettingData;
import com.example.esp32_iot_androidapp.databinding.FragmentMeasureBinding;

public class MeasureFragment extends Fragment implements GattUpdateReceiver.GattUpdateListener {

    private FragmentMeasureBinding binding;
    private GattUpdateReceiver mGattUpdateReceiver;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MeasureViewModel measureViewModel =
                new ViewModelProvider(this).get(MeasureViewModel.class);

        binding = FragmentMeasureBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        mGattUpdateReceiver = new GattUpdateReceiver(this);

        if (MainActivity.BleDeviceAddress.equals("NA")) {
            binding.tvStatusResult.setText("Connect to device first!");
            binding.tvTemperatureValue.setText("");
            binding.tvHumidityValue.setText("");
            binding.tvLuminosityValue.setText("");
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    
    @Override
    public void onGattConnected() {
        binding.tvStatusResult.setText("Device Connected");
    }

    @Override
    public void onGattDisconnected() {
        binding.tvStatusResult.setText("Device Disconnected");
    }

    @Override
    public void onGattServicesDiscovered() {
        //no action
    }
    @Override
    public void onNotificationsEnabled(){
        //no action
    }


    @Override
    public void onDataAvailable(String value) {
        binding.tvStatusResult.setText("Device Connected");
        if (value != null) {
            if(value.contains("TEMP")){
                int index_data = value.indexOf('_');
                String data = value.substring(index_data + 1);
                int temp_data = SettingData.convertBleData(data);
                binding.tvTemperatureValue.setText(String.valueOf(temp_data)+ "°C");

            }else if(value.contains("HUM")) {
                int index_data = value.indexOf('_');
                String data = value.substring(index_data + 1);
                int hum_data = SettingData.convertBleData(data);
                binding.tvHumidityValue.setText(String.valueOf(hum_data)+ "%");

            }else if(value.contains("SLRAD")) {
                int index_data = value.indexOf('_');
                String data = value.substring(index_data + 1);
                int lum_data = SettingData.convertBleData(data);
                if(lum_data == 0){
                    binding.tvLuminosityValue.setText("ON");
                    binding.ivLuminosity.setImageResource(R.drawable.bulbon);
                }else{
                    binding.tvLuminosityValue.setText("OFF");
                    binding.ivLuminosity.setImageResource(R.drawable.bulboff);
                }

            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        ContextCompat.registerReceiver(requireActivity(), mGattUpdateReceiver, makeGattUpdateIntentFilter(), ContextCompat.RECEIVER_NOT_EXPORTED);
        System.out.println("onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        requireActivity().unregisterReceiver(mGattUpdateReceiver);
        System.out.println("onPause");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println("onDestroy");
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleActivity.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleActivity.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleActivity.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleActivity.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleActivity.ACTION_NOTIFY_ENABLED);
        return intentFilter;
    }


}