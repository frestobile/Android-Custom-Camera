package com.hongyun.viservice;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    private ProgressDialog pDialog;
    private EditText deviceID;
    private EditText pinPass;
    RunTimePermission runTimePermission;
    private SessionHandler session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new SessionHandler(getApplicationContext());



        setContentView(R.layout.activity_main);

        Button login = findViewById(R.id.loginbtn);
        deviceID = findViewById(R.id.deviceid);
        pinPass = findViewById(R.id.password);
        if(session.isLoggedIn()){
//            loadNewActivity();
        deviceID.setText(session.getValue(SessionHandler.ID));
        pinPass.setText(session.getValue(SessionHandler.KEY_DEVICEPASS));
        }
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AppConfig.hideSoftKeyboard(MainActivity.this);

                String device = deviceID.getText().toString().trim();
                String password = pinPass.getText().toString().trim();

                if (!device.isEmpty() && !password.isEmpty()) {

                    loginProcess(device, password);

                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(), "Please enter the credentials!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    private void loginProcess(final String device, final String password) {

        pDialog.setMessage("Logging In...");
        showDialog();

        JsonObjectRequest jsArrayRequest = new JsonObjectRequest (Request.Method.GET, AppConfig.DEVICE_LOGIN_URL + "?id=" + device + "&password=" + password, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        pDialog.dismiss();

                        try {
                            //Check if user got logged in successfully

                            if (response.getInt("state") == 200) {
                                Toast.makeText(getApplicationContext(),
                                        response.getString("msg"), Toast.LENGTH_SHORT).show();
                                session.loginDevice(device);
                                session.savedvalue(deviceID.getText().toString(),pinPass.getText().toString());
//                                deviceData.setDeviceID(device);
                                loadNewActivity();

                            }else if (response.getInt("state") == 100) {
                                Toast.makeText(getApplicationContext(),
                                        response.getString("msg"), Toast.LENGTH_SHORT).show();
                            }else if (response.getInt("state") == 300) {
                                Toast.makeText(getApplicationContext(),
                                        response.getString("msg"), Toast.LENGTH_SHORT).show();
                            }
                            else{
                                Toast.makeText(getApplicationContext(), response.getString("msg"), Toast.LENGTH_SHORT).show();

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

                        //Display error message whenever an error occurs
                        Toast.makeText(getApplicationContext(),
                                "Something Went Wrong!", Toast.LENGTH_SHORT).show();

                    }
                });

        // Access the RequestQueue through your singleton class.
        MyApplication.getInstance(this).addToRequestQueue(jsArrayRequest);

    }

    private void loadNewActivity() {

        runTimePermission = new RunTimePermission(this);
        runTimePermission.requestPermission(new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, new RunTimePermission.RunTimePermissionListener() {

            @Override
            public void permissionGranted() {
                // First we need to check availability of play services
                startActivity(new Intent(MainActivity.this, com.hongyun.viservice.DcCam.MainActivity.class));
                finish();

            }

            @Override
            public void permissionDenied() {

                finish();
            }
        });



    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(runTimePermission!=null){
            runTimePermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
}
