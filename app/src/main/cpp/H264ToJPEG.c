//
//
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
#include "H264ToJPEG.h"
#include"time.h"


#define MAX_SCALE_SIZE 200
#define TAG "h264ToJpg"
#define FFMPEG_TAG "ffmpeg"

#define ALOGI(_TAG, FORMAT, ...) __android_log_vprint(ANDROID_LOG_INFO,_TAG, FORMAT, ##__VA_ARGS__);
#define ALOGE(_TAG, FORMAT, ...) __android_log_vprint(ANDROID_LOG_ERROR, _TAG, FORMAT, ##__VA_ARGS__);
#define ALOGW(_TAG, FORMAT, ...) __android_log_vprint(ANDROID_LOG_WARN, _TAG, FORMAT, ##__VA_ARGS__);

/**
 * ffmpeg 日志输出
 * @param ptr
 * @param level
 * @param format
 * @param args
 */
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


int getMin(int i, int j) {
    if (i > j) {
        return j;
    } else {
        return i;
    }
}

/**
 * 缩放
 * @param avFrame
 * @param outAvF
 * @return
 */
int scaleYuv(AVFrame *avFrame, AVFrame **outAvF) {
    //初始化图片宽高
    int sourceWidth = avFrame->width;
    int sourceHeight = avFrame->height;
    //宽高比
    float rate = (float) sourceWidth / sourceHeight;
    int scaledWidth = getMin(sourceWidth, MAX_SCALE_SIZE);
    int scaledHeight = (scaledWidth / rate);

    AVFrame *outAvFrame;
    outAvFrame = av_frame_alloc();
    outAvFrame->width = scaledWidth;
    outAvFrame->height = scaledHeight;
    outAvFrame->format = AV_PIX_FMT_YUV420P;
//    outAvFrame->format =  avFrame->format;

    //分配outAvFrame内存 , 转换成jpg后再释放
    avpicture_alloc((AVPicture *) outAvFrame, outAvFrame->format, scaledWidth, scaledHeight);

    LOGD(TAG, "原尺寸 w %d  w %d  转换尺寸 w %d  h %d", sourceWidth, sourceHeight, scaledWidth,
         scaledHeight);
    struct SwsContext *imgConvertCtx = sws_getContext(sourceWidth,
                                                      sourceHeight,
                                                      avFrame->format,
                                                      scaledWidth,
                                                      scaledHeight,
                                                      outAvFrame->format,
                                                      SWS_POINT,
                                                      NULL,
                                                      NULL,
                                                      NULL);
    if (imgConvertCtx == NULL) {
        LOGE(TAG, "SwsContext 初始化失败");
        return -1;
    }

    sws_scale(imgConvertCtx,
              avFrame->data,
              avFrame->linesize,
              0,
              sourceHeight,
              outAvFrame->data,
              outAvFrame->linesize);

    *outAvF = outAvFrame;
    sws_freeContext(imgConvertCtx);
    LOGV(TAG, "sws_scale complete");
}


int encodeAsJPEG(AVFrame *pFrame, unsigned char **rData, int *rLeng) {
    int width = pFrame->width;
    int height = pFrame->height;
    LOGV(TAG, "width %d %d", width, height);
//    char out_file[100] = "/sdcard/AKASO/amba/out.jpeg";
    AVCodecContext *pCodeCtx = NULL;
    // 分配AVFormatContext对象

    AVFormatContext *pFormatCtx = avformat_alloc_context();
    // 设置输出文件格式
    pFormatCtx->oformat = av_guess_format("mjpeg", NULL, NULL);
    if (pFormatCtx->oformat == NULL) {
        LOGE(TAG, "Couldn't av_guess_format oformat");
        return -1;
    }

    // 创建并初始化输出AVIOContext
    if (avio_open_dyn_buf(&pFormatCtx->pb) < 0) {
        LOGE(TAG, "Couldn't open output file.");
        return -1;
    }

//    // 创建并初始化输出AVIOContext  输出文件
//    if (avio_open(&pFormatCtx->pb, &out_file, AVIO_FLAG_READ_WRITE) < 0) {
//        LOGD(TAG,"Couldn't open output file.");
//        return -1;
//    }


    // 构建一个新stream
    AVStream *pAVStream = avformat_new_stream(pFormatCtx, 0);
    if (pAVStream == NULL) {
        return -1;
    }
    AVCodecParameters *parameters = pAVStream->codecpar;
    parameters->codec_id = pFormatCtx->oformat->video_codec;
    parameters->codec_type = AVMEDIA_TYPE_VIDEO;
    parameters->format = AV_PIX_FMT_YUVJ420P;
    parameters->width = pFrame->width;
    parameters->height = pFrame->height;

    AVCodec *pCodec = avcodec_find_encoder(pAVStream->codecpar->codec_id);

    if (!pCodec) {
        LOGE(TAG, "Could not find encoder");
        return -1;
    }

    //打印输出相关信息
    av_dump_format(pFormatCtx, 0, NULL, 1);

    pCodeCtx = avcodec_alloc_context3(pCodec);
    if (!pCodeCtx) {
        LOGE(TAG, "Could not allocate video codec context");
        return -1;
    }

    if ((avcodec_parameters_to_context(pCodeCtx, pAVStream->codecpar)) < 0) {
        LOGE(TAG, "Failed to copy  codec parameters to decoder context");
        return -1;
    }

    pCodeCtx->time_base = (AVRational) {1, 25};
    int ret_avcodec_open2 = avcodec_open2(pCodeCtx, pCodec, NULL);
    if (ret_avcodec_open2 < 0) {
        LOGE(TAG, "Could not open codec.%d", ret_avcodec_open2);
        return -1;
    }

    int ret = avformat_write_header(pFormatCtx, NULL);
    if (ret < 0) {
        LOGE(TAG, "write_header fail %d", ret);
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
        LOGE(TAG, "Could not avcodec_send_frame.");
        return -1;
    }

    // 得到编码后数据
    ret = avcodec_receive_packet(pCodeCtx, &pkt);
    if (ret < 0) {
        LOGE(TAG, "Could not avcodec_receive_packet");
        return -1;
    }

    ret = av_write_frame(pFormatCtx, &pkt);

    if (ret < 0) {
        LOGE(TAG, "Could not av_write_frame");
        return -1;
    }

    av_packet_unref(&pkt);
    av_packet_free(&pkt);

    //输出到内存
    uint8_t *outBuffer;
    *rLeng = avio_get_dyn_buf(pFormatCtx->pb, &outBuffer);
    *rData = outBuffer;

    LOGV(TAG, "JPG转换完成");
    avcodec_close(pCodeCtx);
    avformat_free_context(pFormatCtx);
    av_frame_free(&pFrame);
    avio_close_dyn_buf(pFormatCtx->pb, &outBuffer);
    return 0;
}


int decodeData(unsigned char *data, int len, unsigned char **rData, int *rLen) {

    av_log_set_level(AV_LOG_VERBOSE);
    av_log_set_callback(log_callback);

    AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (codec == NULL) {
        LOGE(TAG, "找不到H264解码器");
        return -1;
    }
    AVCodecContext *codecCtx = avcodec_alloc_context3(codec);
    if (codecCtx == NULL) {
        LOGD(TAG, "codecCtx 初始化失败");
        return -1;
    }

    int ret = avcodec_open2(codecCtx, codec, NULL);
    if (ret != 0) {
        LOGD(TAG, "open codec failed :%d", ret);
        return -1;
    }
    //开始解码
    AVPacket *packet = av_packet_alloc();

    AVFrame *pFrame = av_frame_alloc();

    av_init_packet(packet);
    packet->data = data;
    packet->size = len;
    avcodec_flush_buffers(codecCtx);

    int rec = -1;
    int send = -1;

    send = avcodec_send_packet(codecCtx, packet);
    if (send != 0) {
        LOGE(TAG, "send_packet failed ", send);
        return -1;
    }

    do {
        avcodec_send_packet(codecCtx, NULL);

        rec = avcodec_receive_frame(codecCtx, pFrame);
        LOGE(TAG, "receive_frame %d", rec);
    } while (rec != 0);

    struct AVFrame *avFrame;
    scaleYuv(pFrame, &avFrame);
    av_packet_unref(packet);
    av_packet_free(&packet);
    av_frame_free(&pFrame);
    avcodec_free_context(&codecCtx);
    pFrame = NULL;
    codec = NULL;
    codecCtx = NULL;
    return encodeAsJPEG(avFrame, rData, rLen);

}



