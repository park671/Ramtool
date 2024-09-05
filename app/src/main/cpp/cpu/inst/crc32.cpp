//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <stdint.h>
#include <android/log.h>
#include "inst.h"

#define CRC32_TAG "crc32"

extern "C" int testCrc32() {
    uint32_t crc = 0xFFFFFFFF;
    uint32_t data = 0x12345678;

    asm volatile (
            "crc32w %w0, %w0, %w1 \n\t" // 使用 CRC32 指令计算 CRC
            : "+r" (crc)               // 输出操作数，同时作为输入
            : "r" (data)               // 输入操作数
            );

    __android_log_print(ANDROID_LOG_DEBUG, CRC32_TAG, "CRC32 supported! CRC value: 0x%08x\n", crc);

    return 0;
}
