package com.hongyun.viservice;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.hongyun.viservice.AndroidMultiPartEntity.ProgressListener;
import com.vincent.videocompressor.VideoCompress;

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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@SuppressLint("Registered")
public class UploadActivity1 extends Activity {
    private ProgressDialog pDialog;
    private String filePath = null;
    private String device_id;
    long totalSize = 0;

    private ProgressDialog progressBarDialog;

    private SessionHandler session;

    private EditText videoID;
    private EditText carNumber;
    private EditText techName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        session = new SessionHandler(getApplicationContext());
        HashMap<String, String> device = session.getDeviceDetails();
        device_id = device.get(SessionHandler.KEY_DEVICEID);



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
                Intent intent= new Intent(UploadActivity1.this,MainActivity.class);
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

                File sourceFile = new File(filePath);

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
                            : getString(R.string.response_message) + " " + response.optString("message") + "\n\n") ;
//                              getString(R.string.response_fileurl) + " " + response.optString("file_path");


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
        File folder = null;
        folder = new File(Environment.getExternalStorageDirectory() + "/VIService");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        final File tempFile = new File(folder.getAbsolutePath() + "/" + mediaFileName + "compressed.mp4");

        VideoCompress.compressVideoMedium(path, tempFile.getAbsolutePath(), new VideoCompress.CompressListener() {
            ProgressDialog progressDialog;
            @Override
            public void onStart() {
                progressDialog = new ProgressDialog(UploadActivity1.this);
                progressDialog.setMessage("Compressing a video...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setProgress(0);
                progressDialog.setMax(100);
                progressDialog.setSecondaryProgress(0);
                progressDialog.setIndeterminate(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
            }

            @Override
            public void onSuccess() {
                progressDialog.dismiss();

                new UploadFileToServer().execute();
            }

            @Override
            public void onFail() {
                progressDialog.dismiss();

                Toast.makeText(UploadActivity1.this, "Compression not done, Please try again.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProgress(float percent) {
                progressDialog.setProgress(Float.valueOf(percent).intValue());

            }
        });
    }
}