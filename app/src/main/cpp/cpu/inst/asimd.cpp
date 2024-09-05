//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <android/log.h>
#include "inst.h"

#define ASIMD_TAG "asimd"

extern "C" int testAsimd() {
    int64_t result;

    asm volatile (
            "movz x0, #0x90AB, lsl #48\n\t"   // 将高 16 位加载到 x0
            "movk x0, #0xCDEF, lsl #32\n\t"   // 加载下一个 16 位，移位 32 位
            "movk x0, #0x1234, lsl #16\n\t"   // 加载下一个 16 位，移位 16 位
            "movk x0, #0x5678\n\t"            // 加载最低的 16 位
            "ins v0.d[1], x0\n\t"        // 使用 A-SIMD 指令将 x0 的值插入到 v0 向量寄存器的高位部分（A-SIMD 指令，NEON 不支持）
            "mov %0, v0.d[1]\n\t"        // 将 v0.d[1] 的值移动到 result 变量中
            : "=r" (result)              // 输出约束
            :
            : "x0", "v0"                 // 使用的寄存器
            );

    __android_log_print(ANDROID_LOG_DEBUG, ASIMD_TAG, "A-SIMD supported! Result: 0x%lx\n", result);

    return 0;
}
