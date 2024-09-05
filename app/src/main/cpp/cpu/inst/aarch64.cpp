//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <android/log.h>
#include "inst.h"

#define AARCH64_TAG "aarch64"

extern "C" int testAarch64() {
    uint64_t result;

    asm volatile (
            "movz x0, #0x90AB, lsl #48\n\t"   // 将高 16 位加载到 x0
            "movk x0, #0xCDEF, lsl #32\n\t"   // 加载下一个 16 位，移位 32 位
            "movk x0, #0x1234, lsl #16\n\t"   // 加载下一个 16 位，移位 16 位
            "movk x0, #0x5678\n\t"            // 加载最低的 16 位
            "mov %0, x0\n\t"                  // 将 x0 的值移动到 result 变量
            : "=r" (result)                   // 输出约束
            :                                 // 无输入操作数
            : "x0"                            // 使用的寄存器
            );

    __android_log_print(ANDROID_LOG_DEBUG, AARCH64_TAG, "AArch64 supported! Result: 0x%016lx\n",
                        result);

    return 0;
}
