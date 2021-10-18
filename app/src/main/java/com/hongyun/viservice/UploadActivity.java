package com.hongyun.viservice;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.hongyun.viservice.AndroidMultiPartEntity.ProgressListener;
import com.vincent.videocompressor.VideoCompress;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

@SuppressLint("Registered")
public class UploadActivity extends Activity {
    private ProgressDialog pDialog;
    private String filePath = null;
    private String device_id;
    long totalSize = 0;

    private ProgressDialog progressBarDialog;

    private SessionHandler session;

    private EditText videoID;
    private EditText carNumber;
    private EditText techName;
    private FFmpeg ffmpeg;
    private static final String TAG = "comress";
    private ProgressDialog progressDialog;
    long videoLengthInMillis;
    private String compressed_file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        session = new SessionHandler(getApplicationContext());
        HashMap<String, String> device = session.getDeviceDetails();
        device_id = device.get(SessionHandler.KEY_DEVICEID);

        loadFFMpegBinary();


        videoID = findViewById(R.id.video_id);
        carNumber = findViewById(R.id.car_number);
        techName = findViewById(R.id.tech_name);

        progressBarDialog = new ProgressDialog(this);
        progressBarDialog.setTitle(getString(R.string.dialog_uploading));
        progressBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressBarDialog.setCancelable(false);
        progressBarDialog.setCanceledOnTouchOutside(false);

        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        Intent i = getIntent();

        filePath = i.getStringExtra("filePath");

        boolean isImage = i.getBooleanExtra("isImage", true);

        if (filePath != null) {
        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.error_file_path), Toast.LENGTH_LONG).show();
        }

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent= new Intent(UploadActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.btnUploadContent).setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {

                String video_serial = session.getValue(SessionHandler.KEY_DEVICEID);
                String car_number  = carNumber.getText().toString().trim();
                String tech_name  = techName.getText().toString().trim();

                if(HomeActivity.checkInternet(getApplicationContext())) {
                    if (!car_number.isEmpty() && !video_serial.isEmpty() && !tech_name.isEmpty()) {
                        check_videoID(video_serial,car_number);
                    } else {
                        // Prompt user to enter credentials
                        Toast.makeText(getApplicationContext(), "Please enter the details of the Video!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.error_no_internet), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void check_videoID(final String videoid,final String car_number) {
        pDialog.setMessage("Checking In...");
        showDialog();

        JsonObjectRequest jsArrayRequest = new JsonObjectRequest (Request.Method.GET, AppConfig.VIDEO_CHECK_URL + "?deviceID=" + device_id+"&car_number="+car_number, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject responseObj) {
                pDialog.dismiss();
                try {
                    boolean error = responseObj.getBoolean("error");
                    String message = responseObj.getString("msg");
                    if (!error) {
//                        new UploadFileToServer().execute();
                        compress_video(filePath);
                    }else{
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Something Went Wrong!"
                            , Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                pDialog.dismiss();

                Toast.makeText(getApplicationContext(),
                        "Something Went Wrong!", Toast.LENGTH_SHORT).show();

            }
        });
        MyApplication.getInstance(this).addToRequestQueue(jsArrayRequest);
    }



    @SuppressLint("StaticFieldLeak")
    private class UploadFileToServer extends AsyncTask<Void, Integer, String> {

        String video_serial = "1563661373";
        String car_number  = carNumber.getText().toString().trim();
        String tech_name  = techName.getText().toString().trim();

        @Override
        protected void onPreExecute() {
//            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            progressBarDialog.show();
            progressBarDialog.setProgress(0);
            super.onPreExecute();
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBarDialog.setProgress(progress[0]);
        }

        @Override
        protected String doInBackground(Void... params) {
            return uploadFile();
        }

        @SuppressWarnings("deprecation")
        private String uploadFile() {
            String responseString;

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(AppConfig.FILE_UPLOAD_URL);

            try {
                AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                        new ProgressListener() {
                            @Override
                            public void transferred(long num) {
                                publishProgress((int) ((num / (float) totalSize) * 100));
                            }
                        });

                File sourceFile = new File(compressed_file);

                entity.addPart("UImage", new FileBody(sourceFile));

                entity.addPart("deviceID", new StringBody(device_id));
//                entity.addPart("videoID", new StringBody(video_serial));
                entity.addPart("car_number", new StringBody(car_number));
                entity.addPart("tech_name", new StringBody(tech_name));

                totalSize = entity.getContentLength();
                httppost.setEntity(entity);

                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();

                int statusCode = response.getStatusLine().getStatusCode();
                responseString = (statusCode == 200) ? EntityUtils.toString(r_entity) : getString(R.string.error_http_status_code) + statusCode;
            } catch (IOException e) {
                responseString = e.toString();
            }

            return responseString;
        }

        @Override
        protected void onPostExecute(final String result) {
            if(progressBarDialog.isShowing())
                progressBarDialog.dismiss();
            showAlert(result);
            super.onPostExecute(result);
        }
    }

    private void showAlert(String message) {
        try {
            JSONObject response = new JSONObject(message);

            message = getString(R.string.response_car) + "" + response.optString("car_number") +
                    getString(R.string.response_tech) + " " + response.optString("tech_name") + "\n" +

                    ((response.getBoolean("error"))
                            ? getString(R.string.response_error) + " "  + response.optString("message") + "\n\n"
                            : getString(R.string.response_message) + " " + response.optString("message") + "\n\n");
//                    +
//                    getString(R.string.response_fileurl) + " " + response.optString("file_path");


            new AlertDialog.Builder(this)

                    .setMessage(message)
                    .setTitle(getString(R.string.response_from_server))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.button_upload_ok), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
//                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                            Intent intent = new Intent(getApplication(), MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            finish();
                            startActivity(intent);
                        }
                    }).show();
        } catch(JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if(progressBarDialog.isShowing()) progressBarDialog.dismiss();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if(progressBarDialog.isShowing()) progressBarDialog.dismiss();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if(progressBarDialog.isShowing()) progressBarDialog.dismiss();
        super.onStop();
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    private void compress_video(String path)
    {
        String mediaFileName = "wc_vid_" + System.currentTimeMillis();

        File folder = new File(Environment.getExternalStorageDirectory() + "/VIService");
        if (!folder.exists()) {
            folder.mkdirs();
        }
     final File new_file = new File(folder.getAbsolutePath() + "/" + mediaFileName + "compressed.mp4");

        compressed_file=new_file.getAbsolutePath();
//
//        VideoCompress.compressVideoMedium(path, tempFile.getAbsolutePath(), new VideoCompress.CompressListener() {
////            ProgressDialog progressDialog;
//            @Override
//            public void onStart() {
//                progressDialog = new ProgressDialog(UploadActivity.this);
//                progressDialog.setMessage("Compressing a video...");
////                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//                progressDialog.setCancelable(false);
//                progressDialog.setProgress(0);
//                progressDialog.setCanceledOnTouchOutside(false);
//                progressDialog.show();
//            }
//
//            @Override
//            public void onSuccess() {
//                progressDialog.dismiss();
//
//                new UploadFileToServer().execute();
//            }
//
//            @Override
//            public void onFail() {
//                progressDialog.dismiss();
//
//                Toast.makeText(UploadActivity.this, "Compression not done, Please try again.", Toast.LENGTH_SHORT).show();
//            }
//
//            @Override
//            public void onProgress(float percent) {
//                progressDialog.setProgress(Float.valueOf(percent).intValue());
//
//            }
//        });


        String complexCommand = "-y -i "+filePath+" -strict experimental -vcodec libx264 -preset ultrafast -crf 24 -acodec aac -ar 44100 -ac 2 -b 36000k -s 1280*720 -aspect 16:9 -metadata:s:v:0 rotate=0 "+compressed_file;
        String[] command = complexCommand.split(" ");
        execFFmpegBinary(command);
    }


    private void execFFmpegBinary(final String[] command) {
        try {
            progressDialog = new ProgressDialog(UploadActivity.this);
//                progressDialog.setMessage("Compressing a video...");
//                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setProgress(0);
            progressDialog.setCanceledOnTouchOutside(false);
//                progressDialog.show();
            ffmpeg.execute(command, new ExecuteBinaryResponseHandler() {
                @Override
                public void onFailure(String s) {
                    Log.d(TAG, "FAILED with output : " + s);
                    progressDialog.dismiss();
                    Toast.makeText(UploadActivity.this, "Error: "+s, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(String s) {
                    Log.d(TAG, "SUCCESS with output : " + s);
                    new UploadFileToServer().execute();
                }

                @Override
                public void onProgress(String s) {
                    Log.d(TAG, "Started command : ffmpeg " + command);

                    progressDialog.setMessage("Compressing a video..." + getProgress(s)+"%");
                    Log.d(TAG, "progress : " + s);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "Started command : ffmpeg " + command);
                    progressDialog.setMessage("Processing...");
                    progressDialog.show();
                    getVideoLength(filePath);
                }

                @Override
                public void onFinish() {
                    Log.d(TAG, "Finished command : ffmpeg " + command);

                    progressDialog.dismiss();


                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // do nothing for now
        }
    }
    private void loadFFMpegBinary() {
        try {
            if (ffmpeg == null) {
                Log.d(TAG, "ffmpeg : era nulo");
                ffmpeg = FFmpeg.getInstance(this);
            }
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onFailure() {
                    showUnsupportedExceptionDialog();
                }

                @Override
                public void onSuccess() {
                    Log.d(TAG, "ffmpeg : correct Loaded");
                }
            });
        } catch (FFmpegNotSupportedException e) {
            showUnsupportedExceptionDialog();
        } catch (Exception e) {
            Log.d(TAG, "EXception no controlada : " + e);
        }
    }

    private void showUnsupportedExceptionDialog() {
        new android.support.v7.app.AlertDialog.Builder(UploadActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Not Supported")
                .setMessage("Device Not Supported")
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .create()
                .show();

    }


    private void getVideoLength(String basePath) {
        MediaPlayer mp = MediaPlayer.create(this, Uri.parse(basePath));
        videoLengthInMillis = TimeUnit.MILLISECONDS.toMillis(mp.getDuration());
        mp.release();
        Log.d(TAG, "onStart: VideoLeng -> " + videoLengthInMillis);
    }

    Pattern pattern = Pattern.compile("time=([\\d\\w:]{8}[\\w.][\\d]+)");
    private long getProgress(String message) {
        if (message.contains("speed")) {
            Matcher matcher = pattern.matcher(message);
            matcher.find();
            String tempTime = String.valueOf(matcher.group(1));
            Log.d(TAG, "getProgress: tempTime " + tempTime);
            String[] arrayTime = tempTime.split("[:|.]");
            long currentTime =
                    TimeUnit.HOURS.toMillis(Long.parseLong(arrayTime[0]))
                            + TimeUnit.MINUTES.toMillis(Long.parseLong(arrayTime[1]))
                            + TimeUnit.SECONDS.toMillis(Long.parseLong(arrayTime[2]))
                            + Long.parseLong(arrayTime[3]);

            long percent = 100 * currentTime/videoLengthInMillis;

            Log.d(TAG, "currentTime -> " + currentTime + "s % -> " + percent);

            return percent;
        }
        return 0;
    }
}