package com.hongyun.viservice;


import android.app.Activity;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class AppConfig {
    public static final String FILE_UPLOAD_URL = "http://www.viservice.eu/backend/video_upload";
    public static final String VIDEO_CHECK_URL = "http://www.viservice.eu/backend/video_check";
    public static final String DEVICE_LOGIN_URL = "http://www.viservice.eu/backend/device_login";

    public static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int CAMERA_CAPTURE_VIDEO_REQUEST_CODE = 200;

    public static final String KEY_IMAGE_STORAGE_PATH = "ImagePath";

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    public static final int BITMAP_SAMPLE_SIZE = 8;

    public static final String GLOBAL_DIRECTORY_NAME = "Uploader";

    public static final String IMAGE_EXTENSION = "jpg";
    public static final String VIDEO_EXTENSION = "mp4";

    /**
     *  Hide Keyboard
     */
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(
                activity.getCurrentFocus().getWindowToken(), 0);
    }
}
