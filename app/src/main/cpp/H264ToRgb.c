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

#define FFMPEG_TAG "FFmpegCmd"
#define ALOGI(TAG, FORMAT, ...) __android_log_vprint(ANDROID_LOG_INFO, TAG, FORMAT, ##__VA_ARGS__);
#define ALOGE(TAG, FORMAT, ...) __android_log_vprint(ANDROID_LOG_ERROR, TAG, FORMAT, ##__VA_ARGS__);
#define ALOGW(TAG, FORMAT, ...) __android_log_vprint(ANDROID_LOG_WARN, TAG, FORMAT, ##__VA_ARGS__);


int initDecodeFlag = 0;


void log_callback(void *ptr, int level, const char *format, va_list args) {
    switch (level) {
        case AV_LOG_WARNING:
            ALOGI(FFMPEG_TAG, format, args);
            break;
        case AV_LOG_INFO:
            ALOGI(FFMPEG_TAG, format, args);
            break;
        case AV_LOG_ERROR:
            ALOGE(FFMPEG_TAG, format, args);
            break;
        default:
            ALOGI(FFMPEG_TAG, format, args);
            break;
    }
}

void initDecoder() {
    av_log_set_level(AV_LOG_VERBOSE);
    av_log_set_callback(log_callback);

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

void scaleYuv(AVFrame *avFrame, AVFrame **outAvF) {


    AVFrame *outAvFrame;
    outAvFrame = av_frame_alloc();

    int width = avFrame->width;
    int height = avFrame->height;
    //宽高比
    float rate = (float) width / height;
    int scaleWidth = getMin(width, 200);
    int scaleHeight = (scaleWidth / rate);

    avpicture_alloc((AVPicture *) outAvFrame, AV_PIX_FMT_YUV420P, scaleWidth, scaleHeight);


    LOGD("尺寸 sw %d  sh %d   dW %d  dH %d", width, height, scaleWidth, scaleHeight);
    struct SwsContext *imgConvertCtx = sws_getContext(avFrame->width,
                                                      avFrame->height,
                                                      AV_PIX_FMT_YUV420P,
                                                      scaleWidth,
                                                      scaleHeight,
                                                      AV_PIX_FMT_YUV420P,
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
              outAvFrame->data,
              outAvFrame->linesize);

    outAvFrame->width = scaleWidth;
    outAvFrame->height = scaleHeight;

    *outAvF = outAvFrame;
    sws_freeContext(imgConvertCtx);
    LOGD("scale 完成");
//    unsigned char *dData = avPicture.data[0];
//
//    int dLen = avPicture.linesize[0] * scaleHeight*2;
//    LOGD("转换完成,linesize %d",avPicture.linesize[0]);
//    LOGD("转换完成,拷贝数据");
//    unsigned char *nData = malloc(sizeof(unsigned char *) * dLen);
//    memcpy(nData, dData, dLen);
//    LOGD("回收 AVPicture");
//    avpicture_free(&avPicture);

//    LOGD("rgbsize %d", dLen);
}


int SaveAsJPEG(AVFrame *pFrame, unsigned char **rData, int *rLeng) {
    int width = pFrame->width;
    int height = pFrame->height;
    LOGD("width %d %d", width, height);

//    char out_file[100] = "/sdcard/AKASO/amba/out.jpeg";

    AVCodecContext *pCodeCtx = NULL;
    // 分配AVFormatContext对象

    AVFormatContext *pFormatCtx = avformat_alloc_context();
    // 设置输出文件格式
    pFormatCtx->oformat = av_guess_format("mjpeg", NULL, NULL);
    if (pFormatCtx->oformat == NULL) {
        LOGD("Couldn't av_guess_format oformat");
        return -1;
    }

    if (avio_open_dyn_buf(&pFormatCtx->pb) < 0) {
        LOGD("Couldn't open output file.");
        return -1;
    }

    // 创建并初始化输出AVIOContext
//    if (avio_open(&pFormatCtx->pb, &out_file, AVIO_FLAG_READ_WRITE) < 0) {
//        LOGD("Couldn't open output file.");
//        return -1;
//    }


    LOGD("构建一个新stream");
    // 构建一个新stream
    AVStream *pAVStream = avformat_new_stream(pFormatCtx, 0);
    if (pAVStream == NULL) {
        return -1;
    }
    LOGD("设置该stream的信息");
    AVCodecParameters *parameters = pAVStream->codecpar;
    parameters->codec_id = pFormatCtx->oformat->video_codec;
    parameters->codec_type = AVMEDIA_TYPE_VIDEO;
    parameters->format = AV_PIX_FMT_YUVJ420P;
    parameters->width = pFrame->width;
    parameters->height = pFrame->height;

    AVCodec *pCodec = avcodec_find_encoder(pAVStream->codecpar->codec_id);

    if (!pCodec) {
        LOGD("Could not find encoder");
        return -1;
    }

    //打印输出相关信息
    av_dump_format(pFormatCtx, 0, NULL, 1);


    pCodeCtx = avcodec_alloc_context3(pCodec);
    if (!pCodeCtx) {
        LOGE( "Could not allocate video codec context");
        exit(1);
    }

    if ((avcodec_parameters_to_context(pCodeCtx, pAVStream->codecpar)) < 0) {
        LOGE("Failed to copy  codec parameters to decoder context");
        return -1;
    }

    pCodeCtx->time_base = (AVRational) {1, 25};
    int ret_avcodec_open2 = avcodec_open2(pCodeCtx, pCodec, NULL);
    if (ret_avcodec_open2 < 0) {
        LOGE("Could not open codec.%d", ret_avcodec_open2);
        return -1;
    }

    int ret = avformat_write_header(pFormatCtx, NULL);
    if (ret < 0) {
        LOGE("write_header fail %d", ret);
        return -1;
    }

    int y_size = width * height;

    //Encode
    // 给AVPacket分配足够大的空间
    AVPacket pkt;
    av_new_packet(&pkt, y_size * 3);

    // 编码数据
    ret = avcodec_send_frame(pCodeCtx, pFrame);
    if (ret < 0) {
        LOGE("Could not avcodec_send_frame.");
        return -1;
    }

    // 得到编码后数据
    ret = avcodec_receive_packet(pCodeCtx, &pkt);
    if (ret < 0) {
        LOGE("Could not avcodec_receive_packet");
        return -1;
    }

    ret = av_write_frame(pFormatCtx, &pkt);

    if (ret < 0) {
        LOGE("Could not av_write_frame");
        return -1;
    }

    av_packet_unref(&pkt);

    //输出到内存
    uint8_t *outBuffer;
    *rLeng = avio_get_dyn_buf(pFormatCtx->pb, &outBuffer);
    *rData = outBuffer;


    LOGD("trans TO jpg 完成");
    if (pAVStream) {
        avcodec_close(pCodeCtx);
    }
//    avio_close(pFormatCtx->pb);
    avio_close_dyn_buf(pFormatCtx->pb, &outBuffer);
    avformat_free_context(pFormatCtx);

    return 0;
}


int decodeData(unsigned char *data, int len, unsigned char **rData, int *rLen) {
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
            struct AVFrame *avFrame;
            scaleYuv(pFrame, &avFrame);
//            destryDecoder();

            return SaveAsJPEG(avFrame, rData, rLen);


        } else {
            //传入空包下去 ffmpeg 会有缓存
            AVPacket packet1 = {0};
            avcodec_decode_video2(codecCtx, pFrame, &got_picture, &packet1);
            if (got_picture) {
                LOGD("解码成功");
                struct AVFrame *avFrame;
                scaleYuv(pFrame, &avFrame);
//                destryDecoder();
                return SaveAsJPEG(avFrame, rData, rLen);
            } else {//解码失败了
                LOGD("解码失败了");

            }
        }
    } else {
        return -1;
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


