//
// Created by youngpark on 2023/4/27.
//

#ifndef METALMAX_MEM_OPT_H
#define METALMAX_MEM_OPT_H
#include <stddef.h>

extern "C" void* __memcpy_aarch64_simd(void* const dst, const void* src, size_t copy_amount);
extern "C" void* __memset_aarch64(void* const s, int c, size_t n);

#endif //METALMAX_MEM_OPT_H
