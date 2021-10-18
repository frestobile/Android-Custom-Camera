package com.hongyun.viservice;

import java.util.Date;


public class Device {
    String deviceid;
    Date sessionExpiryDate;

    public void setDeviceID(String device) {
        this.deviceid = device;
    }


    public void setSessionExpiryDate(Date sessionExpiryDate) {
        this.sessionExpiryDate = sessionExpiryDate;
    }

    public String getDeviceID() {
        return deviceid;
    }

    public Date getSessionExpiryDate() {
        return sessionExpiryDate;
    }
}