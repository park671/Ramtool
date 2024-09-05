//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <stdint.h>
#include <android/log.h>
#include "inst.h"

#define SHA2_TAG "sha2"

extern "C" int testSha2() {
    __android_log_print(ANDROID_LOG_DEBUG, SHA2_TAG, "\n");

    return 0;
}
