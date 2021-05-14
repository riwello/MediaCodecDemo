#include <jni.h>
#include <string>
#include "mylog.h"

extern "C"
{
#include <H264ToRgb.h>

}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_lwl_mediacodectest_DecoderActivity_decode(JNIEnv *env, jobject thiz, jbyteArray buff) {

    unsigned char *result;
    int resultLenth = 0;
    jsize buffSize = env->GetArrayLength(buff);
    jbyte *buffjbyte = env->GetByteArrayElements(buff, NULL);

    decodeData(reinterpret_cast<unsigned char *>(buffjbyte), buffSize, &result, &resultLenth);

    jbyteArray array = env->NewByteArray(resultLenth);
    try {
        env->SetByteArrayRegion(array, 0, resultLenth, reinterpret_cast<jbyte *>(result));
        free(result);
        return array;
    } catch (...) {
        return array;
    }
}