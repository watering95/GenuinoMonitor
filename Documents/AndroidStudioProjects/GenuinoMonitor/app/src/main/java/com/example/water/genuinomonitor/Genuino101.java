package com.example.water.genuinomonitor;

/**
 * Created by water on 2017-03-14.
 */

public class Genuino101 {
    private BLE mBLE = new BLE();
    private Gyroscope mGyro = new Gyroscope();
    private Accelerometer mAccelerometer = new Accelerometer();
    private int positionX, positionY, positionZ;

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

    public void setPotision(int x, int y, int z) {
        positionX = x;
        positionY = y;
        positionZ = z;
    }
    public int getPositionX() {
        return positionX;
    }
    public int getPositionY() {
        return positionY;
    }
    public int getPositionZ() {
        return positionZ;
    }
}
