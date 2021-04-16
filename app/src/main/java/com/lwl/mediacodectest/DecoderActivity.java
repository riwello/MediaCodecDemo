package com.lwl.mediacodectest;

import android.Manifest;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class DecoderActivity extends AppCompatActivity {

    SurfaceView surfaceView;

    private MediaCodecUtil codecUtil;
    private MediaCodecThread thread;
    private String path = "/sdcard/Download/NORM_0016.MP4.thumb";
    private SurfaceHolder holder;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                thread.start();
            }
        });

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        initSurface();

    }

    //初始化播放相关
    private void initSurface() {
        codecUtil = new MediaCodecUtil(2624,1488);
        codecUtil.startCodec();
        thread = new MediaCodecThread(codecUtil, path);

    }


}
