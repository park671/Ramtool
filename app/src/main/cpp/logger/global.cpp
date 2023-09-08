//
// Created by parkyu on 2023/5/11.
//
#include <android/log.h>
#include "global.h"

void logd(const char *tag, char *msg) {
    __android_log_print(ANDROID_LOG_DEBUG, tag, "%s", msg);
}

void loge(char *tag, char *msg) {
    __android_log_print(ANDROID_LOG_ERROR, tag, "%s", msg);
}