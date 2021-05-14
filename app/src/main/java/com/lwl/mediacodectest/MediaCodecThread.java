package com.lwl.mediacodectest;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;

public class MediaCodecThread extends Thread {
    String TAG = "MediaCodecThread";

    //解码器
    private MediaCodecUtil util;
    //文件路径
    private String path;
    //文件读取完成标识
    private boolean isFinish = false;
    //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 1024;
    //一般H264帧大小不超过200k,如果解码失败可以尝试增大这个值
//    private static int FRAME_MAX_LEN = 500 * 1024;
    //根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 1000 / 25;
    int iFrameIndex = -1;
    int spsFrameIndex = -1;
    int sppFrameIndex = -1;

    /**
     * 初始化解码器
     *
     * @param util 解码Util
     * @param path 文件路径
     */
    public MediaCodecThread(MediaCodecUtil util, String path) {
        this.util = util;
        this.path = path;
    }

//    /**
//     * 寻找指定buffer中h264头的开始位置
//     *
//     * @param data   数据
//     * @param offset 偏移量
//     * @param max    需要检测的最大值
//     * @return h264头的开始位置 ,-1表示未发现
//     */
//    private int findHead(byte[] data, int offset, int max) {
//        int i;
//        for (i = offset; i <= max; i++) {
//            //发现帧头
//            if (isHead(data, i))
//                break;
//        }
//        //检测到最大值，未发现帧头
//        if (i == max) {
//            i = -1;
//        }
//        return i;
//    }

    private void findHead(byte[] data, int offset, int max) {

        for (; offset <= max; offset++) {
            //发现帧头
            if (iFrameIndex == -1 && isHead(data, offset)) {
                iFrameIndex = offset;
            }
            if (spsFrameIndex == -1 && isSpsHead(data, offset)) {
                spsFrameIndex = offset;
            }
            if (sppFrameIndex == -1 && isSppHead(data, offset)) {
                sppFrameIndex = offset;
            }

            if (iFrameIndex != -1 && spsFrameIndex != -1 && sppFrameIndex != -1) {
                break;
            }
        }
    }


    /**
     * 判断是否是I帧/P帧头:
     * 00 00 00 01 65    (I帧)
     * 00 00 00 01 61 / 41   (P帧)
     *
     * @param data
     * @param offset
     * @return 是否是帧头
     */
    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[3] == 0x01 && isVideoFrameHeadType(data[offset + 4])) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && isVideoFrameHeadType(data[offset + 3])) {
            result = true;
        }
        return result;
    }

    /**
     * I帧或者P帧
     */
    private boolean isVideoFrameHeadType(byte head) {
        return head == (byte) 0x65 || head == (byte) 0x61 || head == (byte) 0x41;
    }

    private boolean isSpsHead(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[3] == 0x01 && data[offset + 4] == (byte) 0x67) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && data[offset + 3] == (byte) 0x67) {
            result = true;
        }
        return result;
    }

    private boolean isSppHead(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[3] == 0x01 && data[offset + 4] == (byte) 0x68) {
            result = true;
        }
        // 00 00 01 x
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && data[offset + 3] == (byte) 0x68) {
            result = true;
        }
        return result;
    }
    @Override
    public void run() {
        super.run();
        File file = new File(path);
        int fileSize = (int) file.length();
        //判断文件是否存在
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                //保存完整数据帧
                byte[] frame = new byte[fileSize*2];
                //当前帧长度
                int frameLen = 0;
                //每次从文件读取的数据
                byte[] readData = new byte[10 * 1024];
                //开始时间
                long startTime = System.currentTimeMillis();
                //循环读取数据
                while (!isFinish) {
                    if (fis.available() > 0) {
                        int readLen = fis.read(readData);
                        Log.d(TAG, "readLen" + readLen);
                        //当前长度小于文件大小
                        //将readData拷贝到frame
                        System.arraycopy(readData, 0, frame, frameLen, readLen);
                        //修改frameLen
                        frameLen += readLen;
                        //寻找第一个帧头
                        if (frameLen >= fileSize) {
                            //视频解码
                            Log.d(TAG, "编码文件读完 totalRead "+frameLen);
                            onFrame(frame, 0, frameLen);
                            //文件读取结束
                            isFinish = true;
                            break;
                        }

                    } else {
                        //文件读取结束
                        isFinish = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "File not found");
        }
    }

//
//    @Override
//    public void run() {
//        super.run();
//        File file = new File(path);
//        int fileSize = (int) file.length();
//        //判断文件是否存在
//        if (file.exists()) {
//            try {
//                FileInputStream fis = new FileInputStream(file);
//                //保存完整数据帧
//                byte[] frame = new byte[fileSize];
//                //当前帧长度
//                int frameLen = 0;
//                //每次从文件读取的数据
//                byte[] readData = new byte[10 * 1024];
//                //开始时间
//                long startTime = System.currentTimeMillis();
//                //循环读取数据
//                while (!isFinish) {
//                    if (fis.available() > 0) {
//                        int readLen = fis.read(readData);
//                        Log.d(TAG, "readLen" + readLen);
//                        //当前长度小于文件大小
//                        //将readData拷贝到frame
//                        System.arraycopy(readData, 0, frame, frameLen, readLen);
//                        //修改frameLen
//                        frameLen += readLen;
//                        //寻找第一个帧头
//                        findHead(frame, 0, frameLen);
//
//                        Log.d(TAG, "i帧位置 " + iFrameIndex + " spp " + spsFrameIndex + " spp " + sppFrameIndex + " totalRead  " + frameLen);
//                        if (frameLen >= fileSize) {
//                            byte[] sps = new byte[sppFrameIndex - spsFrameIndex];
//                            byte[] spp = new byte[iFrameIndex - sppFrameIndex];
//                            System.arraycopy(frame, spsFrameIndex, sps,0,sps.length);
//                            System.arraycopy(frame, sppFrameIndex, spp,0,spp.length);
//                            util.initDecoder(sps, spp);
//                            util.onFrame(frame, iFrameIndex, fileSize - iFrameIndex);
//                            isFinish = true;
//                                break;
//                        }
////                        while (headFirstIndex >= 0 && isHead(frame, headFirstIndex)) {
////                            Log.d(TAG, "找到i帧头 " + frameLen + " file size" + fileSize);
////
////                            //读长度 等于文件长度 ,表示 文件已读完
////                            if (frameLen >= fileSize) {
////                                //视频解码
////                                Log.d(TAG, "编码文件读完");
////                                onFrame(frame, headFirstIndex, fileSize - headFirstIndex);
////                                //文件读取结束
////                                isFinish = true;
////                                break;
////                            } else {
////                                //找不到第二个帧头
////                                headFirstIndex = -1;
////                            }
////                        }
//
//                    } else {
//                        //文件读取结束
//                        isFinish = true;
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } else {
//            Log.e(TAG, "File not found");
//        }
//    }

    //视频解码
    private void onFrame(byte[] frame, int offset, int length) {
        if (util != null) {
            try {
                util.onFrame(frame, offset, length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "mediaCodecUtil is NULL");
        }
    }

    //修眠
    private void sleepThread(long startTime, long endTime) {
        //根据读文件和解码耗时，计算需要休眠的时间
        long time = PRE_FRAME_TIME - (endTime - startTime);
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //手动终止读取文件，结束线程
    public void stopThread() {
        isFinish = true;
    }
}