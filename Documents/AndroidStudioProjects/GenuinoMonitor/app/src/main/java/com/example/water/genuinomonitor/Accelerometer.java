package com.example.water.genuinomonitor;

/**
 * Created by water on 2017-03-14.
 */

public class Accelerometer {
    private float mAx, mAy, mAz;

    public Accelerometer() {
        updateData(0,0,0);
    }

    public void updateData(float x, float y, float z) {
        mAx = x;
        mAy = y;
        mAz = z;
    }

    public Accelerometer getData() {
        return this;
    }
    public float getAx() {
        return mAx;
    }
    public float getAy() {
        return mAy;
    }
    public float getAz() {
        return mAz;
    }
}
