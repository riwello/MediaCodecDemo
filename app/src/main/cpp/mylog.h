//
// Created by Administrator on 2021/5/13.
//


#ifndef MEDIACODECTEST_MYLOG_H
#define MEDIACODECTEST_MYLOG_H

#include <jni.h>
#include <android/log.h>

#define DEFAULT_LOG_TAG "ijkffmpegCmd"
#define LOGI(TAG, ...) __android_log_print(ANDROID_LOG_INFO, DEFAULT_LOG_TAG, __VA_ARGS__)
#define LOGD(TAG, ...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(TAG, ...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGV(TAG, ...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)

#endif //MEDIACODECTEST_MYLOG_H
