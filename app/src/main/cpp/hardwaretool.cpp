#include <jni.h>
#include "logger/global.h"
#include "string.h"
#include "stdio.h"
#include "stdlib.h"
#include "limits"
#include "stream/arm_stream.h"
#include "lmbench/lmbench.h"
#include "cpu/lat/c2clat.h"

#include "cpu/inst/inst.h"
#include "cpu/load/load.h"

long long *array[10240];
int index = 0;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_park_hardware_NativeBridge_allocHeapMemory(JNIEnv *env, jclass clazz, jint size_in_mb) {
    int size_in_b = 1024 * 1024 * size_in_mb; // 8
    array[index] = (long long *) malloc(size_in_b);
    if (array[index] != nullptr) {
        memset(array[index], 0, size_in_b);
        for (int i = 0; i < size_in_b / 8; i++) {
            array[index][i] = rand();
        }
        index++;
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_release(JNIEnv *env, jclass clazz) {
    for (int i = 0; i < index; i++) {
        free(array[i]);
    }
    index = 0;
}

jstring charToJstring(JNIEnv *env, const char *pat) {
    jclass strClass = (*env).FindClass("java/lang/String");
    jmethodID ctorID = (*env).GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    jbyteArray bytes = (*env).NewByteArray(strlen(pat));
    (*env).SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte *) pat);
    jstring encoding = (*env).NewStringUTF("utf-8");
    return (jstring) (*env).NewObject(strClass, ctorID, bytes, encoding);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_park_hardware_NativeBridge_testLatency(JNIEnv *env, jclass clazz) {
    int count = 3;
    char *params[] = {"128", "256", "2048"};
    entrance(count, params);
    return charToJstring(env, getLatencys());
}
extern "C"
JNIEXPORT jstring JNICALL
Java_com_park_hardware_NativeBridge_testBandWidth(JNIEnv *env, jclass clazz) {
    return charToJstring(env, startBenchmark());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_park_hardware_NativeBridge_getCurrentLatency(JNIEnv *env, jclass clazz) {
    return charToJstring(env, getLatencys());
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testCore2CoreLatency(JNIEnv *env, jclass clazz, jint mode) {
    jobjectArray result = testCore2CoreLat(env, mode);
    // Find the Java class
    jclass nativeBridgeClass = env->FindClass("com/park/hardware/NativeBridge");

    // Find the Java method ID
    jmethodID methodID = env->GetStaticMethodID(nativeBridgeClass, "onCore2CoreLatencyFinish",
                                                "([[I)V");

    // Call the Java method
    env->CallStaticVoidMethod(nativeBridgeClass, methodID, result);

    // Clean up local references
    env->DeleteLocalRef(result);
    env->DeleteLocalRef(nativeBridgeClass);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testSve(JNIEnv *env, jclass clazz) {
    testSve();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testSve2(JNIEnv *env, jclass clazz) {
    testSve2();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testAarch64(JNIEnv *env, jclass clazz) {
    testAarch64();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testVfp(JNIEnv *env, jclass clazz) {
    testVfp();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testNeon(JNIEnv *env, jclass clazz) {
    testNeon();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testAsimd(JNIEnv *env, jclass clazz) {
    testAsimd();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testAes(JNIEnv *env, jclass clazz) {
    testAes();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testSha1(JNIEnv *env, jclass clazz) {
    testSha1();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testSha2(JNIEnv *env, jclass clazz) {
    testSha2();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testPmull(JNIEnv *env, jclass clazz) {
    testPmull();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_testCrc32(JNIEnv *env, jclass clazz) {
    testCrc32();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_park_hardware_NativeBridge_occupyCpu(JNIEnv *env, jclass clazz, jint occupy_duration) {
    occupy_cpu_with_load(occupy_duration);
}