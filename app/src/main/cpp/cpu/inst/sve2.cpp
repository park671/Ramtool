//
// Created by Park Yu on 2024/9/3.
//
#include <stdio.h>
#include <stdlib.h>
#include <android/log.h>
#include "inst.h"

#define SVE2_TAG "sve_2"

extern "C" int testSve2() {
    int8_t a[16] = {1, 2, 3, 4, 5, 6, 7, 8, -1, -2, -3, -4, -5, -6, -7, -8};
    int8_t b[16] = {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
    int8_t c[16];

    __asm__ volatile (
            "ld1b {z0.b}, p0/z, %[a]\n\t"
            "ld1b {z1.b}, p0/z, %[b]\n\t"
            "sqrdmlah z2.s, z0.s, z1.s\n\t"
            "st1b {z2.b}, p0, %[c]\n\t"
            : [c] "=m"(c)
    : [a] "m"(a), [b] "m"(b)
    : "z0", "z1", "z2"
    );

    // 输出结果
    __android_log_print(ANDROID_LOG_DEBUG, SVE2_TAG, "Result array c:\n");
    for (int i = 0; i < 16; i++) {
        __android_log_print(ANDROID_LOG_DEBUG, SVE2_TAG, "%d ", c[i]);
    }
    __android_log_print(ANDROID_LOG_DEBUG, SVE2_TAG, "\n");

    return 0;
}
