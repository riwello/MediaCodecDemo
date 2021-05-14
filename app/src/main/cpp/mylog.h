//
// Created by Administrator on 2021/5/13.
//


#ifndef MEDIACODECTEST_MYLOG_H
#define MEDIACODECTEST_MYLOG_H

#include <jni.h>
#include <android/log.h>

#define LOG_TAG "ijkffmpeg"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, LOG_TAG, __VA_ARGS__)

#endif //MEDIACODECTEST_MYLOG_H
