package com.hongyun.viservice;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Date;
import java.util.HashMap;


public class SessionHandler {
    private static final String PREF_NAME = "DeviceSession";
    public static final String KEY_DEVICEID = "id";
    public static final String KEY_DEVICEPASS = "password";
    public static final String ID = "userid";

    private static final String KEY_EXPIRES = "expires";
    private static final String KEY_EMPTY = "";
    private Context mContext;
    private SharedPreferences.Editor mEditor;
    private SharedPreferences mPreferences;

    public SessionHandler(Context mContext) {
        this.mContext = mContext;
        mPreferences = mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.mEditor = mPreferences.edit();
    }

    /**
     * Logs in the user by saving user details and setting session
     *
     * @param deviceid
     *
     */
    public void loginDevice(String deviceid) {
        mEditor.putString(KEY_DEVICEID, deviceid);
        Date date = new Date();

        //Set user session for next 1 days
        long millis = date.getTime() + (1 * 60 * 60 * 1000);
        mEditor.putLong(KEY_EXPIRES, millis);
        mEditor.commit();
    }

    public void savedvalue(String id,String pass) {
        mEditor.putString(ID, id);
        mEditor.putString(KEY_DEVICEPASS, pass);
        mEditor.commit();
    }
        public String getValue(String id)
        {
            return mPreferences.getString(id,"");
        }
    /**
     * Checks whether user is logged in
     *
     * @return
     */
    public boolean isLoggedIn() {
        Date currentDate = new Date();

        long millis = mPreferences.getLong(KEY_EXPIRES, 0);

        /* If shared preferences does not have a value
         then user is not logged in
         */
        if (millis == 0) {
            return false;
        }
        Date expiryDate = new Date(millis);

        /* Check if session is expired by comparing
        current date and Session expiry date
        */
        return currentDate.before(expiryDate);
    }

    /**
     * Get stored session data
     * */
    public HashMap<String, String> getDeviceDetails(){
        HashMap<String, String> device = new HashMap<String, String>();
        // device id
        device.put(KEY_DEVICEID, mPreferences.getString(KEY_DEVICEID, null));

        return device;
    }

    /**
     * Fetches and returns user details
     *
     * @return user details
     */
//    public Device getDeviceDetails() {
//        //Check if user is logged in first
//        if (!isLoggedIn()) {
//            return null;
//        }
//        Device device = new Device();
//        device.setDeviceID(mPreferences.getString(KEY_DEVICEID, KEY_EMPTY));
//        device.setSessionExpiryDate(new Date(mPreferences.getLong(KEY_EXPIRES, 0)));
//
//        return device;
//    }

    /**
     * Logs out user by clearing the session
     */
    public void logoutDevice(){
        mEditor.clear();
        mEditor.commit();
    }

}