package com.example.esp32_iot_androidapp;

import java.util.HashMap;

public class Esp32GattAttributes {

    private static HashMap<String, String> attributes = new HashMap();

    public static String HUM_CH = "00002A6F-0000-1000-8000-00805F9B34FB";
    public static String TEMP_CH = "00002A6E-0000-1000-8000-00805F9B34FB";
    public static String SLRAD_CH = "00002A77-0000-1000-8000-00805F9B34FB";
    public static String IOT_SERVICE = "0000181A-0000-1000-8000-00805F9B34FB";
    public static String CCCD = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put(IOT_SERVICE, "IoTService");
        attributes.put(CCCD, "ClientCharacteristicConfigurationDescriptor");
        attributes.put(HUM_CH, "HumidityCharacteristic");
        attributes.put(TEMP_CH, "TemperatureCharacteristic");
        attributes.put(SLRAD_CH, "SolarRadiationCharacteristic");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

}
