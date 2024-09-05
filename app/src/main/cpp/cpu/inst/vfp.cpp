//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <android/log.h>
#include "inst.h"

#define VFP_TAG "vfp"

extern "C" int testVfp() {
    double a = 1.0f, b = 2.0f, result;

    asm volatile (
            "fadd %d0, %d1, %d2\n\t"   // AArch64 浮点加法指令 (64位双精度)
            : "=w" (result)            // 输出约束，使用 AArch64 的 64 位浮点寄存器
            : "w" (a), "w" (b)         // 输入约束
            );

    __android_log_print(ANDROID_LOG_DEBUG, VFP_TAG, "VFP supported! Result of 1.0 + 2.0 = %f\n", result);

    return 0;
}
