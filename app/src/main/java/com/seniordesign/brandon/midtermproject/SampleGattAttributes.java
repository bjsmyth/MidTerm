package com.seniordesign.brandon.midtermproject;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String FOLLOWER_SERVICE = "19b10000-e8f2-537e-4f6c-d104768a1214";

    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public static String EXAMPLE_POTENTIOMETER = "19b10001-e8f2-537e-4f6c-e104768a1215";
    public static String EXAMPLE_LIGHTSENSOR = "19b10001-e8f2-537e-4f6c-e104768a1216";
    public static String CONTROL_CHARACTERISTIC = "19b10001-e8f2-537e-4f6c-d104768a1215";


    static {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
        attributes.put(FOLLOWER_SERVICE, "Line Follower Service");
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
        attributes.put(EXAMPLE_POTENTIOMETER, "Potentiometer");
        attributes.put(EXAMPLE_LIGHTSENSOR, "Light Sensor");
        attributes.put(CONTROL_CHARACTERISTIC, "Motor Control");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
