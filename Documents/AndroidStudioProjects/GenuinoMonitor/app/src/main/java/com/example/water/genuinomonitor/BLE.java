package com.example.water.genuinomonitor;

/**
 * Created by water on 2017-03-14.
 */

public class BLE {
     private String mName = null;
     private String mAddress = null;
     private boolean mIsConnected = false;

     public BLE() {
     }

     public void setAddress(String str) {
          mAddress = str;
     }
     public void setName(String str) {
          mName = str;
     }
     public String getAddress() {
          return mAddress;
     }
     public String getName() {
          return mName;
     }
     public void setConnect() {
          mIsConnected = true;
     }
     public void setDisconnect() {
          mIsConnected = false;
     }
     public boolean getConnectState() {
          return mIsConnected;
     }
}


