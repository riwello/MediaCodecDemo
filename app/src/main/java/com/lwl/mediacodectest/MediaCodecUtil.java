package com.lwl.mediacodectest;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Created by ZhangHao on 2016/8/5.
 * 用于硬件解码(MediaCodec)H264的工具类
 */
public class MediaCodecUtil {
    //自定义的log打印，可以无视
    Logger logger = Logger.getLogger("MyMediaCodec");

    private String TAG = "MediaCodecUtil";
    //解码后显示的surface及其宽高
    private int width = 200, height = 200;
    //解码器
    private MediaCodec mCodec;
    private boolean isFirst = true;
    // 需要解码的类型
    private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video
    private final static int TIME_INTERNAL = 5;
    private MediaFormat mediaFormat;


    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;

    /**
     * 初始化解码器
     *
     * @param width  surface宽
     * @param height surface高
     */
    public MediaCodecUtil(int width, int height) {
//        logger.d("MediaCodecUtil() called with: " + "holder = [" + holder + "], " +
//                "width = [" + width + "], height = [" + height + "]");
        this.width = width;
        this.height = height;
    }


    public void startCodec() {
        if (isFirst) {
            //第一次打开则初始化解码器
            initDecoder();
        }
    }

    private void initDecoder() {
        try {
            //根据需要解码的类型创建解码器
            mCodec = MediaCodec.createDecoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //初始化MediaFormat
        mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
                width, height);
        //配置MediaFormat以及需要显示的surface
        mCodec.configure(mediaFormat, null, null, 0);
        //开始解码
        mCodec.start();
        isFirst = false;
    }

    int mCount = 0;


    public boolean onFrame(byte[] buf, int offset, int length) {
        // 获取输入buffer index
        ByteBuffer[] inputBuffers = mCodec.getInputBuffers();
        //-1表示一直等待；0表示不等待；其他大于0的参数表示等待毫秒数
        int inputBufferIndex = mCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            //清空buffer
            inputBuffer.clear();
            //put需要解码的数据
            inputBuffer.put(buf, offset, length);
            //解码
            mCodec.queueInputBuffer(inputBufferIndex, 0, length, mCount * TIME_INTERNAL, 0);
            mCount++;

        } else {
            return false;
        }
        // 获取输出buffer index
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 100);
        if (outputBufferIndex>0){
            Log.d(TAG, "onFrame outputBufferIndex " + outputBufferIndex);
            //循环解码，直到数据全部解码完成
//        while (outputBufferIndex >= 0) {
            //logger.d("outputBufferIndex = " + outputBufferIndex);
            //true : 将解码的数据显示到surface上
//            mCodec.releaseOutputBuffer(outputBufferIndex, true);
//            outputBufferIndex = mCodec.dequeueOutputBuffer(bufferInfo, 0);
            Image image = mCodec.getOutputImage(outputBufferIndex);
            String fileName = "/sdcard/Download/" + String.format("frame_1.jpg");
            compressToJpeg(fileName, image);
        }else {
            Log.e(TAG, "outputBufferIndex = " + outputBufferIndex);

        }

        return true;
    }

    private void compressToJpeg(String fileName, Image image) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(fileName);
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to create output file " + fileName, ioe);
        }
        Rect rect = image.getCropRect();
        YuvImage yuvImage = new YuvImage(getDataFromImage(image, COLOR_FormatNV21), ImageFormat.NV21, rect.width(), rect.height(), null);
        yuvImage.compressToJpeg(rect, 100, outStream);
    }

    private byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            Log.v(TAG, "pixelStride " + pixelStride);
            Log.v(TAG, "rowStride " + rowStride);
            Log.v(TAG, "width " + width);
            Log.v(TAG, "height " + height);
            Log.v(TAG, "buffer size " + buffer.remaining());
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    /**
     * 停止解码，释放解码器
     */
    public void stopCodec() {

        try {
            mCodec.stop();
            mCodec.release();
            mCodec = null;
            isFirst = true;
        } catch (Exception e) {
            e.printStackTrace();
            mCodec = null;
        }
    }
}