package com.example.water.genuinomonitor;

/**
 * Created by water on 2017-03-14.
 */

public class Genuino101 {
    private BLE mBLE = new BLE();
    private Gyroscope mGyro = new Gyroscope();
    private Accelerometer mAccelerometer = new Accelerometer();
    private float positionX, positionY, positionZ;

    public Genuino101() {
        initPosition();
    }

    public Gyroscope getGyroscope() {
        return mGyro;
    }

    public Accelerometer getAccelerometer() {
        return mAccelerometer;
    }

    public BLE getBLE() {
        return mBLE;
    }

    public void initPosition() {
        setPotision(0,0,0);
    }

    public void setPotision(float x, float y, float z) {
        positionX = x;
        positionY = y;
        positionZ = z;
    }
    public float getPositionX() {
        return positionX;
    }
    public float getPositionY() {
        return positionY;
    }
    public float getPositionZ() {
        return positionZ;
    }
}
