//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <stdint.h>
#include <android/log.h>
#include "inst.h"

#define PMULL_TAG "pmull"

extern "C" int testPmull() {
    uint64_t a = 0x123456789abcdef0;
    uint64_t b = 0xfedcba9876543210;
    uint64_t result;

    asm volatile (
            "pmull %0.1q, %1.1d, %2.1d \n\t" // 使用 PMULL 指令执行 64 位多项式乘法
            : "=w" (result)                   // 输出操作数
            : "w" (a), "w" (b)                // 输入操作数
            );

    __android_log_print(ANDROID_LOG_DEBUG, PMULL_TAG, "PMULL supported! Result: 0x%016lx\n", result);

    return 0;
}
