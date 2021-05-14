package com.lwl.mediacodectest;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DecoderActivity extends AppCompatActivity {

    ImageView surfaceView;
    ImageView iv_media_codec;

    private MediaCodecUtil codecUtil;
    private MediaCodecThread thread;
    private String path = "/sdcard/AKASO/amba/thumb_1620899691638.h264";
//    private String path = "/sdcard/Download/NORM_0016.MP4.thumb";
    private SurfaceHolder holder;

    private String TAG = "DecoderActivity";

    static {
        System.loadLibrary("ijkffmpegCmd");
    }

    public native byte[] decode(byte[] buff);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);
        surfaceView = (ImageView) findViewById(R.id.surface_view);
        iv_media_codec = (ImageView) findViewById(R.id.iv_media_codec);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDecoder();
//                startMediaCodecDecoder();

            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        }

    }

    private void startDecoder() {
        try {
            File file = new File(path);
            int fileLength = (int) file.length();
            byte[] buff = new byte[fileLength];
            FileInputStream fileInputStream = new FileInputStream(path);
            int read = fileInputStream.read(buff);
            Log.d(TAG, "file length " + fileLength + "   read length " + read);
            byte[] decode = decode(buff);

            Log.d(TAG, "decode complete" + decode.length + "   " + (decode.length / 1024 / 1024) + " mb  Thread " + Thread.currentThread().getName());
            Bitmap VideoBit = Bitmap.createBitmap(200, 112, Bitmap.Config.RGB_565);

            VideoBit.copyPixelsFromBuffer(ByteBuffer.wrap(decode));
            surfaceView.setImageBitmap(VideoBit);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //初始化播放相关
    private void startMediaCodecDecoder() {
        codecUtil = new MediaCodecUtil(this);
        codecUtil.startCodec();

        codecUtil.setOnImageCallBack(new MediaCodecUtil.OnImageCallBack() {
            @Override
            public void onImage(Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iv_media_codec.setImageBitmap(bitmap);
                    }
                });

            }
        });

        thread = new MediaCodecThread(codecUtil, path);

        thread.start();

    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.e("Main", "onPause");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("Main", "onDestroy");
    }

    public void onclick(View view) {
        startMediaCodecDecoder();

    }
}
