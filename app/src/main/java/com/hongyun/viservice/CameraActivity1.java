package com.hongyun.viservice;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vincent.videocompressor.VideoCompress;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Policy;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity1 extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Handler customHandler = new Handler();
    int flag = 0;
    private File tempFile = null;
    private Camera.PictureCallback jpegCallback;
    int MAX_VIDEO_SIZE_UPLOAD = 500; //MB
    boolean isrunning=false;
    TextView textView;
    int minute =0, seconds = 0, hour = 0;
    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (myOrientationEventListener != null)
                myOrientationEventListener.enable();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }

    private File folder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);


        initControls();

        identifyOrientationEvents();


// screen and CPU will stay awake during this section

        //create a folder to get image
        folder = new File(Environment.getExternalStorageDirectory() + "/VIService");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        //capture image on callback
        captureImageCallback();
        //
        if (camera != null) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                imgFlashOnOff.setVisibility(View.GONE);
            }
        }




    }



    private void cancelSaveVideoTaskIfNeed() {
        if (saveVideoTask != null && saveVideoTask.getStatus() == AsyncTask.Status.RUNNING) {
            saveVideoTask.cancel(true);
        }
    }



    public String saveToSDCard(byte[] data, int rotation) throws IOException {
        String imagePath = "";
        try {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, options);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            int reqHeight = metrics.heightPixels;
            int reqWidth = metrics.widthPixels;

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            if (rotation != 0) {
                Matrix mat = new Matrix();
                mat.postRotate(rotation);
                Bitmap bitmap1 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
                if (bitmap != bitmap1)
                {
                    bitmap.recycle();
                }
                imagePath = getSavePhotoLocal(bitmap1);
                if (bitmap1 != null) {
                    bitmap1.recycle();
                }
            } else {
                imagePath = getSavePhotoLocal(bitmap);
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imagePath;
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }

    private String getSavePhotoLocal(Bitmap bitmap) {
        String path = "";
        try {
            OutputStream output;
            File file = new File(folder.getAbsolutePath(), "wc" + System.currentTimeMillis() + ".jpg");
            try {
                output = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                output.flush();
                output.close();
                path = file.getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    private void captureImageCallback() {

        surfaceHolder = imgSurface.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {

                refreshCamera();



            }
        };
    }

    private class SaveVideoTask extends AsyncTask<Void, Integer, Void> {

        File thumbFilename;

        ProgressDialog progressDialog = null;

        @Override
        protected void onPreExecute() {
//            super.onPreExecute();
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

//            progressDialog = new ProgressDialog(CameraActivity.this);
//            progressDialog.setMessage("Processing a video...");
//            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//            progressDialog.setIndeterminate(false);
//            progressDialog.setCancelable(false);
////            progressDialog.setProgress(0);
//            progressDialog.setMax(100);
//            progressDialog.setCanceledOnTouchOutside(false);
//            progressDialog.show();
            imgCapture.setOnTouchListener(null);
            textCounter.setVisibility(View.GONE);
            imgSwipeCamera.setVisibility(View.VISIBLE);
            imgFlashOnOff.setVisibility(View.VISIBLE);

        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                try {

                    myOrientationEventListener.enable();

                    customHandler.removeCallbacksAndMessages(null);

                    mediaRecorder.stop();
                    releaseMediaRecorder();

                    tempFile = new File(folder.getAbsolutePath() + "/" + mediaFileName + ".mp4");
//                    thumbFilename = new File(folder.getAbsolutePath(), "t_" + mediaFileName + ".jpeg");
//                    generateVideoThmb(tempFile.getPath(), thumbFilename);


                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();

            }
            return null;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
//            super.onProgressUpdate(values);
//            progressDialog.setProgress(values[0]);

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (progressDialog != null) {
                if (progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }
            }
            if (tempFile != null) {
                onVideoSendDialog(tempFile.getAbsolutePath());
            }
        }
    }

    private int mPhotoAngle = 90;

    private void identifyOrientationEvents() {

        myOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int iAngle) {

                final int iLookup[] = {0, 0, 0, 90, 90, 90, 90, 90, 90, 180, 180, 180, 180, 180, 180, 270, 270, 270, 270, 270, 270, 0, 0, 0}; // 15-degree increments
                if (iAngle != ORIENTATION_UNKNOWN) {

                    int iNewOrientation = iLookup[iAngle / 15];
                    if (iOrientation != iNewOrientation) {
                        iOrientation = iNewOrientation;
                        if (iOrientation == 0) {
                            mOrientation = 90;
                        } else if (iOrientation == 270) {
                            mOrientation = 0;
                        } else if (iOrientation == 90) {
                            mOrientation = 180;
                        }

                    }
                    mPhotoAngle = normalize(iAngle);
                }
            }
        };

        if (myOrientationEventListener.canDetectOrientation()) {
            myOrientationEventListener.enable();
        }

    }

    private MediaRecorder mediaRecorder;
    private SurfaceView imgSurface;
    private ImageView imgCapture;
    private ImageView imgFlashOnOff;
    private ImageView imgSwipeCamera;
    private TextView textCounter;

    private void initControls() {

        mediaRecorder = new MediaRecorder();
//        mediaRecorder.setOrientationHint(90);
        imgSurface = (SurfaceView) findViewById(R.id.imgSurface);
        textCounter = (TextView) findViewById(R.id.textCounter);
        imgCapture = (ImageView) findViewById(R.id.imgCapture);
        imgFlashOnOff = (ImageView) findViewById(R.id.imgFlashOnOff);
        imgSwipeCamera = (ImageView) findViewById(R.id.imgChangeCamera);
        textCounter.setVisibility(View.GONE);


        imgSwipeCamera.setOnClickListener(this);
        activeCameraCapture();

        imgFlashOnOff.setOnClickListener(this);


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.imgFlashOnOff:
                flashToggle();
                break;
            case R.id.imgChangeCamera:
                camera.stopPreview();
                camera.release();
                if (flag == 0) {
                    imgFlashOnOff.setVisibility(View.GONE);
                    flag = 1;
                } else {
                    imgFlashOnOff.setVisibility(View.VISIBLE);
                    flag = 0;
                }
                surfaceCreated(surfaceHolder);
                break;
            default:
                break;
        }
    }

    private void flashToggle() {

        if (flashType == 1) {

            flashType = 2;
        } else if (flashType == 2) {

            flashType = 3;
        } else if (flashType == 3) {

            flashType = 2;
        }
        refreshCamera();
    }



    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = new MediaRecorder();
        }
    }


    public void refreshCamera() {

        if (surfaceHolder.getSurface() == null) {
            return;
        }
        try {
            camera.stopPreview();
            Camera.Parameters param = camera.getParameters();

            if (flag == 0) {
                if (flashType == 1) {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                    imgFlashOnOff.setImageResource(R.drawable.ic_flash_auto);
                } else if (flashType == 2) {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                    Camera.Parameters params = null;
                    if (camera != null) {
                        params = camera.getParameters();

                        if (params != null) {
                            List<String> supportedFlashModes = params.getSupportedFlashModes();

                            if (supportedFlashModes != null) {
                                if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                                    param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                    param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                                }
                            }
                        }
                    }
                    imgFlashOnOff.setImageResource(R.drawable.ic_flash_on);
                } else if (flashType == 3) {
                    param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    imgFlashOnOff.setImageResource(R.drawable.ic_flash_off);
                }
            }

//            param.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
//            param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            refrechCameraPriview(param);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refrechCameraPriview(Camera.Parameters param) {
        try {
            camera.setParameters(param);
            setCameraDisplayOrientation(0);

            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCameraDisplayOrientation(int cameraId) {

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        if (Build.MODEL.equalsIgnoreCase("Nexus 6") && flag == 1) {
            rotation = Surface.ROTATION_180;
        }
        int degrees = 0;
        switch (rotation) {

            case Surface.ROTATION_0:

                degrees = 0;
                break;

            case Surface.ROTATION_90:

                degrees = 90;
                break;

            case Surface.ROTATION_180:

                degrees = 180;
                break;

            case Surface.ROTATION_270:

                degrees = 270;
                break;

        }

        int result;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {

            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror

        } else {
            result = (info.orientation - degrees + 360) % 360;

        }

        camera.setDisplayOrientation(result);

    }

    //------------------SURFACE CREATED FIRST TIME--------------------//

    int flashType = 2;

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        try {
            if (flag == 0) {
                camera = Camera.open(0);
            } else {
                camera = Camera.open(1);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }

        try {
            Camera.Parameters param;
            param = camera.getParameters();
            List<Camera.Size> sizes = param.getSupportedPreviewSizes();
            //get diff to get perfact preview sizes
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            int height = displaymetrics.heightPixels;
            int width = displaymetrics.widthPixels;
            long diff = (height * 1000 / width);
            long cdistance = Integer.MAX_VALUE;
            int idx = 0;
            for (int i = 0; i < sizes.size(); i++) {
                long value = (long) (sizes.get(i).width * 1000) / sizes.get(i).height;
                if (value > diff && value < cdistance) {
                    idx = i;
                    cdistance = value;
                }
                Log.e(CameraActivity1.class.getSimpleName(), "width=" + sizes.get(i).width + " height=" + sizes.get(i).height);
            }
            Log.e(CameraActivity1.class.getSimpleName(), "INDEX:  " + idx);
            Camera.Size cs = sizes.get(idx);
            Camera.Size optimalSize = getOptimalPreviewSize(sizes, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
            param.setPreviewSize(optimalSize.width, optimalSize.height);
            param.setVideoStabilization(true);
//            param.setFocusAreas(Lists.newArrayList(new Camera.Area(focusRect, 1000)));
//            param.setFocusAreas(Li);
//            param.set("");
            Log.e("WHHATSAPP", "INDEX111:  "+param.flatten());

            Log.e("WHHATSAPP", "INDEX111:  " +optimalSize.width +optimalSize.height);
            param.setJpegQuality(100);
            camera.setParameters(param);
            setCameraDisplayOrientation(0);

            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();

            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
//                camera.sur
                }
            });
            if (flashType == 1) {
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                param.setFlashMode(Camera.Parameters.FOCUS_MODE_AUTO);
                imgFlashOnOff.setImageResource(R.drawable.ic_flash_auto);

            } else if (flashType == 2) {

                param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                Camera.Parameters params = null;
                if (camera != null) {
                    params = camera.getParameters();

                    if (params != null) {
                        List<String> supportedFlashModes = params.getSupportedFlashModes();

                        if (supportedFlashModes != null) {
                            if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                                param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                            } else if (supportedFlashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
                                param.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                            }
                        }
                    }
                }
                imgFlashOnOff.setImageResource(R.drawable.ic_flash_on);

            } else if (flashType == 3) {
                param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                imgFlashOnOff.setImageResource(R.drawable.ic_flash_off);
            }


        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        try {
            camera.stopPreview();
            camera.release();
            camera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        refreshCamera();
//        Camera.Parameters p = camera.getParameters();
//        if (p.getMaxNumMeteringAreas() > 0) {
//            this.meteringAreaSupported = true;
//        }
    }

    //------------------SURFACE OVERRIDE METHIDS END--------------------//

    private long timeInMilliseconds = 0L, startTime = SystemClock.uptimeMillis(), updatedTime = 0L, timeSwapBuff = 0L;
    private Runnable updateTimerThread = new Runnable() {

        public void run() {

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            int hrs = mins / 60;

            secs = secs % 60;
//            textCounter.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
//            customHandler.postDelayed(this, 0);


            if(mins==7)
            {
                isrunning=false;
                scaleDownAnimation();

//                cancelSaveVideoTaskIfNeed();
//                saveVideoTask = new SaveVideoTask();
//                saveVideoTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);


                customHandler.removeCallbacksAndMessages(null);

                mediaRecorder.stop();
                releaseMediaRecorder();

                tempFile = new File(folder.getAbsolutePath() + "/" + mediaFileName + ".mp4");
//                    thumbFilename = new File(folder.getAbsolutePath(), "t_" + mediaFileName + ".jpeg");
//                    generateVideoThmb(tempFile.getPath(), thumbFilename);

//                            onVideoSendDialog(tempFile.getAbsolutePath());
                Intent mIntent = new Intent(CameraActivity1.this, PhotoVideoRedirectActivity.class);
                mIntent.putExtra("PATH", tempFile.getAbsolutePath().toString());
//                                mIntent.putExtra("THUMB", thumbPath.toString());
                mIntent.putExtra("WHO", "Video");
                startActivity(mIntent);
                finish();
            }
            else {
                if (mins >= 5) {
                    textCounter.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    textCounter.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
                    customHandler.postDelayed(this, 0);
                } else {
//                    textCounter.setTextColor(android.R.color.white);
                    textCounter.setText(String.format("%02d", mins) + ":" + String.format("%02d", secs));
                    customHandler.postDelayed(this, 0);
                }
            }

        }


    };

    private void scaleUpAnimation() {
//        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(imgCapture, "scaleX", 2f);
//        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(imgCapture, "scaleY", 2f);
//        scaleDownX.setDuration(100);
//        scaleDownY.setDuration(100);
//        AnimatorSet scaleDown = new AnimatorSet();
//        scaleDown.play(scaleDownX).with(scaleDownY);
//
//        scaleDownX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//                View p = (View) imgCapture.getParent();
//                p.invalidate();
//            }
//        });
//        scaleDown.start();
        imgCapture.setImageResource(R.drawable.ic_stop_black_24dp);
    }

    private void scaleDownAnimation() {
//        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(imgCapture, "scaleX", 1f);
//        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(imgCapture, "scaleY", 1f);
//        scaleDownX.setDuration(100);
//        scaleDownY.setDuration(100);
//        AnimatorSet scaleDown = new AnimatorSet();
//        scaleDown.play(scaleDownX).with(scaleDownY);
//
//        scaleDownX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            @Override
//            public void onAnimationUpdate(ValueAnimator valueAnimator) {
//
//                View p = (View) imgCapture.getParent();
//                p.invalidate();
//            }
//        });
//        scaleDown.start();
        imgCapture.setImageResource(R.drawable.ic_capture);

    }

    @Override
    protected void onPause() {
        super.onPause();

        try {

            if (customHandler != null)
                customHandler.removeCallbacksAndMessages(null);

            releaseMediaRecorder();       // if you are using MediaRecorder, release it first

            if (myOrientationEventListener != null)
                myOrientationEventListener.enable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private SaveVideoTask saveVideoTask = null;

    private void activeCameraCapture() {
        if (imgCapture != null) {
            imgCapture.setAlpha(1.0f);
//
            imgCapture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!isrunning) {
//                        if (isSpaceAvailable()) {
//                        captureImage();
                        isrunning = true;
                        try {
                            if (prepareMediaRecorder()) {
                                myOrientationEventListener.disable();
                                mediaRecorder.start();
                                startTime = SystemClock.uptimeMillis();
                                customHandler.postDelayed(updateTimerThread, 0);
                            } else {
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        textCounter.setVisibility(View.VISIBLE);
                        imgSwipeCamera.setVisibility(View.GONE);
                        imgFlashOnOff.setVisibility(View.GONE);
                        scaleUpAnimation();
//
                    }
                    else {
                        isrunning=false;
                        scaleDownAnimation();

//                        cancelSaveVideoTaskIfNeed();
//                        saveVideoTask = new SaveVideoTask();
//                        saveVideoTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                        try {
                            textCounter.setVisibility(View.GONE);
                            imgSwipeCamera.setVisibility(View.VISIBLE);
                            imgFlashOnOff.setVisibility(View.VISIBLE);
                            myOrientationEventListener.enable();

                            customHandler.removeCallbacksAndMessages(null);

                            mediaRecorder.stop();
                            releaseMediaRecorder();

                            tempFile = new File(folder.getAbsolutePath() + "/" + mediaFileName + ".mp4");
//                    thumbFilename = new File(folder.getAbsolutePath(), "t_" + mediaFileName + ".jpeg");
//                    generateVideoThmb(tempFile.getPath(), thumbFilename);

//                            onVideoSendDialog(tempFile.getAbsolutePath());
                            Intent mIntent = new Intent(CameraActivity1.this, PhotoVideoRedirectActivity.class);
                            mIntent.putExtra("PATH", tempFile.getAbsolutePath().toString());
//                                mIntent.putExtra("THUMB", thumbPath.toString());
                            mIntent.putExtra("WHO", "Video");
                            startActivity(mIntent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

    }

    public void onVideoSendDialog(final String videopath) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (videopath != null) {
                    File fileVideo = new File(videopath);

                    final File tempFile = new File(folder.getAbsolutePath() + "/" + mediaFileName + "compressed.mp4");

                    VideoCompress.compressVideoMedium(videopath, tempFile.getAbsolutePath(), new VideoCompress.CompressListener() {
                        ProgressDialog progressDialog;
                        @Override
                        public void onStart() {
                            progressDialog = new ProgressDialog(CameraActivity1.this);
                            progressDialog.setMessage("Compressing a video...");
                            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                            progressDialog.setCancelable(false);
                            progressDialog.setProgress(0);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.show();
                        }

                        @Override
                        public void onSuccess() {
                            progressDialog.dismiss();

                            Intent mIntent = new Intent(CameraActivity1.this, PhotoVideoRedirectActivity.class);
                            mIntent.putExtra("PATH", tempFile.getAbsolutePath().toString());
//                                mIntent.putExtra("THUMB", thumbPath.toString());
                            mIntent.putExtra("WHO", "Video");
                            startActivity(mIntent);
                            finish();
                        }

                        @Override
                        public void onFail() {
                            progressDialog.dismiss();

                            Toast.makeText(CameraActivity1.this, "Compression not done, Please try again.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onProgress(float percent) {
                            progressDialog.setProgress(Float.valueOf(percent).intValue());

                        }
                    });


                    //SendVideoDialog sendVideoDialog = SendVideoDialog.newInstance(videopath, thumbPath, name, phoneNuber);
                    // sendVideoDialog.show(getSupportFragmentManager(), "SendVideoDialog");

                }
            }
        });
    }

    private void inActiveCameraCapture() {
        if (imgCapture != null) {
            imgCapture.setAlpha(0.5f);
            imgCapture.setOnClickListener(null);
        }
    }

    //--------------------------CHECK FOR MEMORY -----------------------------//

    public int getFreeSpacePercantage() {
        int percantage = (int) (freeMemory() * 100 / totalMemory());
        int modValue = percantage % 5;
        return percantage - modValue;
    }

    public double totalMemory() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = (double) stat.getBlockCount() * (double) stat.getBlockSize();
        return sdAvailSize / 1073741824;
    }

    public double freeMemory() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double sdAvailSize = (double) stat.getAvailableBlocks() * (double) stat.getBlockSize();
        return sdAvailSize / 1073741824;
    }

    public boolean isSpaceAvailable() {
        if (getFreeSpacePercantage() >= 1) {
            return true;
        } else {
            return false;
        }
    }
    //-------------------END METHODS OF CHECK MEMORY--------------------------//


    private String mediaFileName = null;

    @SuppressLint("SimpleDateFormat")
    protected boolean prepareMediaRecorder() throws IOException {

        mediaRecorder = new MediaRecorder(); // Works well
        camera.stopPreview();
        camera.unlock();

        mediaRecorder.setCamera(camera);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        if (flag == 1) {
            mediaRecorder.setProfile(CamcorderProfile.get(1, CamcorderProfile.QUALITY_HIGH_SPEED_HIGH));
        } else {
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH_SPEED_HIGH));
        }
        mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());

//        mediaRecorder.setOrientationHint(180);

        if (Build.MODEL.equalsIgnoreCase("Nexus 6") && flag == 1) {

            if (mOrientation == 90) {
                mediaRecorder.setOrientationHint(mOrientation);
            } else if (mOrientation == 180) {
                mediaRecorder.setOrientationHint(0);
            } else {
                mediaRecorder.setOrientationHint(180);
            }

        } else if (mOrientation == 90 && flag == 1) {
            mediaRecorder.setOrientationHint(270);
        } else if (flag == 1) {
            mediaRecorder.setOrientationHint(mOrientation);
        }
        mediaFileName = "wc_vid_" + System.currentTimeMillis();
        mediaRecorder.setOutputFile(folder.getAbsolutePath() + "/" + mediaFileName + ".MP4"); // Environment.getExternalStorageDirectory()

        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

            public void onInfo(MediaRecorder mr, int what, int extra) {
                // TODO Auto-generated method stub

                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {

                    long downTime = 0;
                    long eventTime = 0;
                    float x = 0.0f;
                    float y = 0.0f;
                    int metaState = 0;
                    MotionEvent motionEvent = MotionEvent.obtain(
                            downTime,
                            eventTime,
                            MotionEvent.ACTION_UP,
                            0,
                            0,
                            metaState
                    );

                    imgCapture.dispatchTouchEvent(motionEvent);

                    Toast.makeText(CameraActivity1.this, "You reached to Maximum video size.", Toast.LENGTH_SHORT).show();
                }


            }
        });

        mediaRecorder.setMaxFileSize(0);

        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            releaseMediaRecorder();
            e.printStackTrace();
            return false;
        }
        return true;

    }

    OrientationEventListener myOrientationEventListener;
    int iOrientation = 0;
    int mOrientation = 90;

    public void generateVideoThmb(String srcFilePath, File destFile) {
        try {
            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(srcFilePath, 120);
            FileOutputStream out = new FileOutputStream(destFile);
            ThumbnailUtils.extractThumbnail(bitmap, 200, 200).compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private int normalize(int degrees) {
        if (degrees > 315 || degrees <= 45) {
            return 0;
        }

        if (degrees > 45 && degrees <= 135) {
            return 90;
        }

        if (degrees > 135 && degrees <= 225) {
            return 180;
        }

        if (degrees > 225 && degrees <= 315) {
            return 270;
        }

        throw new RuntimeException("Error....");
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;

        if (sizes==null) return null;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Find size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


}
