//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <arm_neon.h>
#include <android/log.h>
#include "inst.h"

#define NEON_TAG "neon"

extern "C" int testNeon() {
    float32x4_t a = {1.0, 2.0, 3.0, 4.0};  // 使用 NEON 数据类型定义向量
    float32x4_t b = {4.0, 3.0, 2.0, 1.0};
    float32x4_t result;

    asm volatile (
            "fadd v0.4s, %1.4s, %2.4s\n\t"  // 使用 fadd 进行 NEON 向量浮点加法
            "mov %0.16b, v0.16b\n\t"        // 将结果存储在 result 中
            : "=w" (result)                 // 输出约束
            : "w" (a), "w" (b)              // 输入约束
            : "v0"                          // 被使用的 NEON 寄存器
            );

    // 打印结果到 Android 日志
    __android_log_print(ANDROID_LOG_DEBUG, NEON_TAG, "NEON supported! Result of vector addition:");
    for (int i = 0; i < 4; i++) {
        __android_log_print(ANDROID_LOG_DEBUG, NEON_TAG, "%f ", result[i]);
    }
    __android_log_print(ANDROID_LOG_DEBUG, NEON_TAG, "\n");

    return 0;
}

