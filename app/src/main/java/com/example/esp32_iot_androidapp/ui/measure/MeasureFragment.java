package com.example.esp32_iot_androidapp.ui.measure;

import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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
    TextView tv_temp_value, tv_hum_value, tv_lum_value, tv_status_conn;
    ImageView iv_slrad;
    private GattUpdateReceiver mGattUpdateReceiver;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MeasureViewModel measureViewModel =
                new ViewModelProvider(this).get(MeasureViewModel.class);

        binding = FragmentMeasureBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        tv_temp_value = root.findViewById(R.id.TV_temperature_value);
        tv_hum_value = root.findViewById(R.id.TV_humidity_value);
        tv_lum_value = root.findViewById(R.id.TV_luminosity_value);
        tv_status_conn = root.findViewById(R.id.TV_status_result);
        iv_slrad = root.findViewById(R.id.IV_luminosity);

        mGattUpdateReceiver = new GattUpdateReceiver(this);

        if (MainActivity.BleDeviceAddress.equals("NA")) {
            tv_status_conn.setText("Connect to device first!");
            tv_temp_value.setText("");
            tv_hum_value.setText("");
            tv_lum_value.setText("");
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
        tv_status_conn.setText("Device Connected");
    }

    @Override
    public void onGattDisconnected() {
        tv_status_conn.setText("Device Disconnected");
    }

    @Override
    public void onGattServicesDiscovered() {
        //no action
    }

    @Override
    public void onDataAvailable(String value) {
        tv_status_conn.setText("Device Connected");
        if (value != null) {
            if(value.contains("TEMP")){
                int index_data = value.indexOf('_');
                String data = value.substring(index_data + 1);
                int temp_data = SettingData.convertBleData(data);
                tv_temp_value.setText(String.valueOf(temp_data)+ "°C");

            }else if(value.contains("HUM")) {
                int index_data = value.indexOf('_');
                String data = value.substring(index_data + 1);
                int hum_data = SettingData.convertBleData(data);
                tv_hum_value.setText(String.valueOf(hum_data)+ "%");

            }else if(value.contains("SLRAD")) {
                int index_data = value.indexOf('_');
                String data = value.substring(index_data + 1);
                int lum_data = SettingData.convertBleData(data);
                if(lum_data == 0){
                    tv_lum_value.setText("ON");
                    iv_slrad.setImageResource(R.drawable.bulbon);
                }else{
                    tv_lum_value.setText("OFF");
                    iv_slrad.setImageResource(R.drawable.bulboff);
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
        intentFilter.addAction(BleActivity.ACTION_SEND_DATA);
        intentFilter.addAction(BleActivity.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


}