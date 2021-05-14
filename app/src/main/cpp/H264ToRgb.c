//
//  SNDecodeHelper.c
//  Video
//
//  Created by censt on 2021/5/12.
//  Copyright © 2021 cnest. All rights reserved.
//
#include "jni.h"
#include "android/log.h"
#include "include/libavcodec/avcodec.h"
#include "include/libswscale/swscale.h"
#include "include/libavformat/avformat.h"
#include "include/libavutil/frame.h"
#include "mylog.h"
#include "H264ToRgb.h"


AVCodec *codec = NULL;
AVCodecContext *codecCtx = NULL;

int initDecodeFlag = 0;


void initDecoder() {

    if (initDecodeFlag) {
        return;
    }
    initDecodeFlag = 1;

    av_register_all();
    avcodec_register_all();
    codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (codec == NULL) {
        LOGD("查找解码器失败");
        return;
    }

    codecCtx = avcodec_alloc_context3(codec);
//    AV_CODEC_FLAG
//    codecCtx->flags=CODEC_CAP_FRAME_THREADS;

    if (codecCtx == NULL) {
        LOGD("codecCtx error");
        return;
    }

    int ret = avcodec_open2(codecCtx, codec, NULL);

    if (ret != 0) {

        LOGD("open codec failed :%d", ret);
        return;
    }
    LOGD("打开图片解码器成功");

}

int getMin(int i, int j) {
    if (i > j) {
        return j;
    } else {
        return i;
    }
}

void imageFromAVFrame(AVFrame *avFrame, unsigned char **rData, int *rLen) {

    AVPicture avPicture = {0};
    int width = avFrame->width;
    int height = avFrame->height;
    //宽高比
    float rate =(float) width / height;
    int scaleWidth = getMin(width, 200);
    int scaleHeight = (scaleWidth / rate);

    avpicture_alloc(&avPicture, AV_PIX_FMT_RGB565LE, scaleWidth, scaleHeight);
    LOGD("尺寸 sw %d  sh %d   dW %d  dH %d", width, height, scaleWidth, scaleHeight);
    struct SwsContext *imgConvertCtx = sws_getContext(avFrame->width,
                                                      avFrame->height,
                                                      AV_PIX_FMT_YUV420P,
                                                      scaleWidth,
                                                      scaleHeight,
                                                      AV_PIX_FMT_RGB565LE,
                                                      SWS_POINT,
                                                      NULL,
                                                      NULL,
                                                      NULL);
    if (imgConvertCtx == NULL) {
        LOGD("初始化 图片转换器是失败");
        return;
    }

    sws_scale(imgConvertCtx,
              avFrame->data,
              avFrame->linesize,
              0,
              height,
              avPicture.data,
              avPicture.linesize);

    sws_freeContext(imgConvertCtx);

    unsigned char *dData = avPicture.data[0];

    int dLen = avPicture.linesize[0] * scaleHeight*2;
    LOGD("转换完成,linesize %d",avPicture.linesize[0]);
    LOGD("转换完成,拷贝数据");
    unsigned char *nData = malloc(sizeof(unsigned char *) * dLen);
    memcpy(nData, dData, dLen);
    LOGD("回收 AVPicture");
    avpicture_free(&avPicture);

    *rData = nData;
    *rLen = dLen;
    LOGD("rgbsize %d", dLen);
}


void decodeData(unsigned char *data, int len, unsigned char **rData, int *rLen) {
    initDecoder();

    //开始解码
    AVPacket packet = {0};

    AVFrame *pFrame = av_frame_alloc();
    av_init_packet(&packet);
    packet.data = data;
    packet.size = len;

    int got_picture = 0;

    avcodec_flush_buffers(codecCtx);

    int ret = avcodec_decode_video2(codecCtx, pFrame, &got_picture, &packet);


    if (ret > 0) {
//        AVPacket packet1={0};
//        ret = avcodec_decode_video2(codecCtx, pFrame, &got_picture, &packet1);
        if (got_picture) {
            LOGD("解码成功");
            //进行下一步的处理
            imageFromAVFrame(pFrame, rData, rLen);


        } else {
            //传入空包下去 ffmpeg 会有缓存
            AVPacket packet1 = {0};
            avcodec_decode_video2(codecCtx, pFrame, &got_picture, &packet1);
            if (got_picture) {
                LOGD("解码成功");
                imageFromAVFrame(pFrame, rData, rLen);
            } else {//解码失败了
                LOGD("解码失败了");

            }
        }
    } else {

        LOGD("解码失败了");
    }

    av_frame_free(&pFrame);
    pFrame = NULL;
}


void destryDecoder() {

    if (codecCtx) {
        avcodec_free_context(&codecCtx);
        codecCtx = NULL;
        codec = NULL;
    }
    initDecodeFlag = 0;
}


