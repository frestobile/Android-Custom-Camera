package com.hongyun.viservice;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;

import com.hongyun.viservice.DcCam.Config;

import java.io.File;


/**
 * Created by sotsys016-2 on 13/8/16 in com.cnc3camera.
 */
public class PhotoVideoRedirectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photovideo_redirect);


        init();
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                Intent intent= new Intent(PhotoVideoRedirectActivity.this,UploadActivity.class);
                intent.putExtra("filePath",getIntent().getStringExtra(Config.KeyName.FILEPATH));
                startActivity(intent);
            }
        });
        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder alertDialog= new AlertDialog.Builder(PhotoVideoRedirectActivity.this);
                alertDialog.setTitle("Delete");
                alertDialog.setMessage("Are You Sure Want To Delete Video?");
                alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = new File(getIntent().getStringExtra(Config.KeyName.FILEPATH));
                        if (file.exists()) {
                            file.delete();
                        }
                        finish();
                        Intent intent= new Intent(PhotoVideoRedirectActivity.this,MainActivity.class);

                        startActivity(intent);
                    }
                });
                AlertDialog dialog= alertDialog.create();
                dialog.show();

            }
        });
    }
    VideoView videoView;
    private void init() {

        videoView = (VideoView) findViewById(R.id.vidShow);



            videoView.setVisibility(View.VISIBLE);
            MediaController mediaController = new MediaController(videoView.getContext());
                mediaController.setMediaPlayer(videoView);
                mediaController.setAnchorView(mediaController);
        videoView.setMediaController(mediaController);
            try {
//                videoView.setMediaController(new MediaController(this));
                videoView.setVideoURI(Uri.parse(getIntent().getStringExtra(Config.KeyName.FILEPATH)));
                videoView.seekTo(1);
            } catch (Exception e){
                e.printStackTrace();
            }
            videoView.requestFocus();
            //videoView.setZOrderOnTop(true);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {

//                    videoView.start();
                }
            });
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
//                    videoView.start();
                }
            });












    }

    @Override
    public void onBackPressed() {
        if (videoView.isPlaying()) {
            videoView.pause();
        }
        super.onBackPressed();
    }
}
