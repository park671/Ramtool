//
// Created by Park Yu on 2024/9/3.
//

#include "sve_add.h"
#include <stdio.h>
#include <android/log.h>

#define SVE_TAG "sve_add"

// 声明汇编函数
extern "C" void sve2_add_arrays(const unsigned char *a, const unsigned char *b, unsigned char *c,
                                unsigned long len);

extern "C" int testSveAdd() {
    // 初始化数组 a 和 b
    unsigned char a[16] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    unsigned char b[16] = {16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
    unsigned char c[16] = {0};

    // 调用汇编函数进行数组加法
    sve2_add_arrays(a, b, c, 16);
    // 输出结果
    __android_log_print(ANDROID_LOG_DEBUG, SVE_TAG, "Result array c:\n");
    for (int i = 0; i < 16; i++) {
        __android_log_print(ANDROID_LOG_DEBUG, SVE_TAG, "%d ", c[i]);
    }
    __android_log_print(ANDROID_LOG_DEBUG, SVE_TAG, "\n");

    return 0;
}