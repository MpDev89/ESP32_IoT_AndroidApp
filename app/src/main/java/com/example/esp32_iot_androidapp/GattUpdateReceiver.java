package com.example.esp32_iot_androidapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GattUpdateReceiver extends BroadcastReceiver {

    private GattUpdateListener listener;

    public GattUpdateReceiver(GattUpdateListener listener) {
        this.listener = listener;
    }

    public interface GattUpdateListener {
        void onGattConnected();
        void onGattDisconnected();
        void onGattServicesDiscovered();
        void onNotificationsEnabled();
        void onDataAvailable(String data);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (BleActivity.ACTION_GATT_CONNECTED.equals(action)) {
            if (listener != null) {
                listener.onGattConnected();
            }
        } else if (BleActivity.ACTION_GATT_DISCONNECTED.equals(action)) {
            if (listener != null) {
                listener.onGattDisconnected();
            }
        } else if (BleActivity.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
             if (listener != null) {
                listener.onGattServicesDiscovered();
            }
        } else if (BleActivity.ACTION_DATA_AVAILABLE.equals(action)) {
            if (listener != null) {
                String value = intent.getStringExtra(BleActivity.EXTRA_DATA);
                listener.onDataAvailable(value);
            }
        } else if (BleActivity.ACTION_NOTIFY_ENABLED.equals(action)) {
            if (listener != null) {
                listener.onNotificationsEnabled();
            }
        }
    }
}
