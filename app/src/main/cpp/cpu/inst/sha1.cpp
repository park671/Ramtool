//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <stdint.h>
#include <android/log.h>
#include "inst.h"

#define SHA1_TAG "sha1"

extern "C" int testSha1() {
    __android_log_print(ANDROID_LOG_DEBUG, SHA1_TAG, "\n");
    return 0;
}
