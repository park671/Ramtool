//
// Created by Park Yu on 2024/9/4.
//
#include <stdio.h>
#include <stdint.h>
#include <android/log.h>
#include "inst.h"

#define AES_TAG "aes"

extern "C" int testAes() {
    uint8_t key[16] = {
            0x2b, 0x7e, 0x15, 0x16, 0x28, 0xae, 0xd2, 0xa6,
            0xab, 0xf7, 0xcf, 0x4f, 0x55, 0x66, 0x73, 0x4f
    };
    uint8_t input[16] = {
            0x32, 0x43, 0xf6, 0xa8, 0x88, 0x5a, 0x30, 0x8d,
            0x31, 0x31, 0x98, 0xa2, 0xe0, 0x37, 0x07, 0x34
    };
    uint8_t output[16];

    asm volatile (
            "ld1 {v0.16b}, [%1]   \n\t"  // 加载128位的明文到寄存器v0中
            "ld1 {v1.16b}, [%2]   \n\t"  // 加载128位的密钥到寄存器v1中
            "aese v0.16b, v1.16b  \n\t"  // 执行 AES 单轮加密
            "st1 {v0.16b}, [%0]   \n\t"  // 将加密后的结果存储到输出数组中
            : "=r" (output)              // 输出操作数
            : "r" (input), "r" (key)     // 输入操作数
            : "v0", "v1"                 // 使用的寄存器
            );

    __android_log_print(ANDROID_LOG_DEBUG, AES_TAG, "AES supported! Encrypted output:\n");
    for (int i = 0; i < 16; i++) {
        __android_log_print(ANDROID_LOG_DEBUG, AES_TAG, "%02x ", output[i]);
    }
    __android_log_print(ANDROID_LOG_DEBUG, AES_TAG, "\n");

    return 0;
}
