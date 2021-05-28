#include <jni.h>
#include <string>
#include "mylog.h"

extern "C"
{
#include <H264ToJPEG.h>

}
#define JNIREG_CLASS "com/lwl/mediacodectest/H264ToJPEGActivity"//指定要注册的类


extern "C"
JNIEXPORT jbyteArray JNICALL
decode(JNIEnv *env, jobject thiz, jbyteArray buff) {

    unsigned char *result;
    int resultLenth = 0;
    jsize buffSize = env->GetArrayLength(buff);
    jbyte *buffjbyte = env->GetByteArrayElements(buff, NULL);

    int ret = decodeData(reinterpret_cast<unsigned char *>(buffjbyte), buffSize, &result,
                         &resultLenth);

    env->ReleaseByteArrayElements(buff, buffjbyte, 0);

    if (ret < 0) {
        return env->NewByteArray(0);
    }
    jbyteArray array = env->NewByteArray(resultLenth);
    try {
        env->SetByteArrayRegion(array, 0, resultLenth, reinterpret_cast<jbyte *>(result));
        free(result);
        return array;
    } catch (...) {
        return env->NewByteArray(0);
    }

}

/**
 * 方法签名
 */
static JNINativeMethod gMethods[] = {
        {"decode", "([B)[B", (void *) decode},
};

/**
 * 注册方法
 * @param env
 * @param className
 * @param gMethods
 * @param numMethods
 * @return
 */
int registerNativeMethod(JNIEnv *env, const char *className, JNINativeMethod *gMethods,
                         int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;

}

/*
* System.loadLibrary("lib")时调用
* 如果成功返回JNI版本, 失败返回-1
*/
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = NULL;
    jint result = -1;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return -1;
    }
    assert(env != NULL);


    if (!registerNativeMethod(env, JNIREG_CLASS, gMethods,
                              sizeof(gMethods) / sizeof(gMethods[0]))) {//注册
        return -1;
    }
    //成功
    result = JNI_VERSION_1_6;
    return result;
}

