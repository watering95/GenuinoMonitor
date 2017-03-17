package com.example.water.genuinomonitor;

/**
 * Created by water on 2017-03-14.
 */

public class Gyroscope {
    private float mGx, mGy, mGz;

    public Gyroscope() {
        updateData(0,0,0);
    }

    public void updateData(float x, float y, float z) {
        mGx = x;
        mGy = y;
        mGz = z;
    }
    public Gyroscope getData() {
        return this;
    }
    public float getGx() {
        return mGx;
    }
    public float getGy() {
        return mGy;
    }
    public float getGz() {
        return mGz;
    }
}
