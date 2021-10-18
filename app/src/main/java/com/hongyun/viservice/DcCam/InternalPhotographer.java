package com.hongyun.viservice.DcCam;

import android.app.Activity;

interface InternalPhotographer extends Photographer {

    void initWithViewfinder(Activity activity, CameraView preview);
}
