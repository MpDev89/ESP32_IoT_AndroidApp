package com.example.esp32_iot_androidapp;

import java.util.HashMap;

public class Esp32GattAttributes {

    private static HashMap<String, String> attributes = new HashMap();

    public static String HUM_CH = "9ec13766-fec8-46c4-a280-6b0f9346dea0";
    public static String TEMP_CH = "4ac8a682-9736-4e5d-932b-e9b31405049c";
    public static String SLRAD_CH = "e7924032-959e-4493-bd5c-0df9f1d225a3";
    public static String IOT_SERVICE = "906311b0-4d56-11eb-8404-0800200c9a66";
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
