package com.example.water.genuinomonitor;

import java.util.HashMap;

/**
 * Created by water on 2017-03-07.
 */

public class gattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String GYRO_X_MEASUREMENT = "0000bbb1-0000-1000-8000-00805f9b34fb";
    public static String GYRO_Y_MEASUREMENT = "0000bbb2-0000-1000-8000-00805f9b34fb";
    public static String GYRO_Z_MEASUREMENT = "0000bbb3-0000-1000-8000-00805f9b34fb";

    static {
        // Sample Services.
        attributes.put("0000bbb0-0000-1000-8000-00805f9b34fb", "Gyro Measurement Service");
        // Sample Characteristics.
        attributes.put(GYRO_X_MEASUREMENT, "Gyro X Measurement");
        attributes.put(GYRO_Y_MEASUREMENT, "Gyro Y Measurement");
        attributes.put(GYRO_Z_MEASUREMENT, "Gyro Z Measurement");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
