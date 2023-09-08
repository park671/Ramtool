//
// Created by parkyu on 2023/9/7.
//

#include "arm_stream.h"
# include <stdio.h>
# include <unistd.h>
# include <math.h>
# include <float.h>
# include <limits.h>
# include <sys/time.h>
#include "../logger/global.h"
#include "string.h"
#include "stdlib.h"
#include "perf.h"
#include "../opt/mem_opt.h"

#ifndef STREAM_TYPE
#define STREAM_TYPE double
#endif


#define STREAM_ARRAY_SIZE 20000000
#define NTIMES 20
#define OFFSET 0

#define TEST_ITEMS 5

# ifndef MIN
# define MIN(x, y) ((x)<(y)?(x):(y))
# endif
# ifndef MAX
# define MAX(x, y) ((x)>(y)?(x):(y))
# endif

static STREAM_TYPE a[STREAM_ARRAY_SIZE + OFFSET],
        b[STREAM_ARRAY_SIZE + OFFSET],
        c[STREAM_ARRAY_SIZE + OFFSET];

static double avgtime[TEST_ITEMS] = {0},
        maxtime[TEST_ITEMS] = {0},
        mintime[TEST_ITEMS] = {FLT_MAX, FLT_MAX, FLT_MAX, FLT_MAX, FLT_MAX};

static double SINGLE_CASE_SIZE = sizeof(STREAM_TYPE) * STREAM_ARRAY_SIZE;

static STREAM_TYPE scalar = 3.0;

extern void memoryCopyLegacy();

extern void memoryCopySimd();

extern void memoryFillLegacy();

extern void memoryDaxpyLegacy();

extern void memoryDotLegacy();

static double bytes[TEST_ITEMS] = {
        2 * SINGLE_CASE_SIZE,
        2 * SINGLE_CASE_SIZE,
        SINGLE_CASE_SIZE,
        3 * SINGLE_CASE_SIZE,
        2 * SINGLE_CASE_SIZE,
};

char result[500];

char *startBenchmark() {
    memset(result, 0, 500);
    char info[200];
    double t, times[TEST_ITEMS][NTIMES];
    for (int k = 0; k < NTIMES; k++) {
        startTimestamp();
        memoryCopyLegacy();
        times[0][k] = getDuration();

        startTimestamp();
        memoryCopySimd();
        times[1][k] = getDuration();

        startTimestamp();
        memoryFillLegacy();
        times[2][k] = getDuration();

        startTimestamp();
        memoryDaxpyLegacy();
        times[3][k] = getDuration();

        startTimestamp();
        memoryDotLegacy();
        times[4][k] = getDuration();
    }

    for (int k = 1; k < NTIMES; k++) {
        for (int j = 0; j < TEST_ITEMS; j++) {
            avgtime[j] = avgtime[j] + times[j][k];
            mintime[j] = MIN(mintime[j], times[j][k]);
            maxtime[j] = MAX(maxtime[j], times[j][k]);
        }
    }
    for (int j = 0; j < TEST_ITEMS; j++) {
        if (j) {
            strcat(result, "#");
        }
        avgtime[j] -= mintime[j];
        avgtime[j] -= maxtime[j];
        avgtime[j] = avgtime[j] / (double) (NTIMES - 3);
        sprintf(info, "%.1f,%.3f,%.3f,%.3f",
                (bytes[j] / 1048.576) / mintime[j],
                avgtime[j] / 1000.0,
                mintime[j] / 1000.0,
                maxtime[j] / 1000.0);
        strcat(result, info);
        logd("native", info);
    }
    logd("result:", result);
    return result;
}

void memoryCopyLegacy() {
    for (int j = 0; j < STREAM_ARRAY_SIZE; j += 4) {
        c[j] = a[j];
        c[j + 1] = a[j + 1];
        c[j + 2] = a[j + 2];
        c[j + 3] = a[j + 3];
    }
}

void memoryCopySimd() {
    __memcpy_aarch64_simd(c, a, STREAM_ARRAY_SIZE * 8);
}

void memoryFillLegacy() {
    for (int j = 0; j < STREAM_ARRAY_SIZE; j += 4) {
        c[j] = 3.1415926535;
        c[j + 1] = 3.1415926535;
        c[j + 2] = 3.1415926535;
        c[j + 3] = 3.1415926535;
    }
}

void memoryDaxpyLegacy() {
    for (int j = 0; j < STREAM_ARRAY_SIZE; j += 8) {
        a[j] = b[j] + scalar * c[j];
        a[j + 1] = b[j + 1] + scalar * c[j + 1];
        a[j + 2] = b[j + 2] + scalar * c[j + 2];
        a[j + 3] = b[j + 3] + scalar * c[j + 3];
        a[j + 4] = b[j + 4] + scalar * c[j + 4];
        a[j + 5] = b[j + 5] + scalar * c[j + 5];
        a[j + 6] = b[j + 6] + scalar * c[j + 6];
        a[j + 7] = b[j + 7] + scalar * c[j + 7];
    }
}


static double sum = 0;

void memoryDotLegacy() {
    double sum0, sum1, sum2, sum3, sum4, sum5, sum6, sum7;
    for (int l = 0; l < 2; ++l) {
        b[l] = 1.0e0;
        sum0 = 0.0e0;
        sum1 = 0.0e0;
        sum2 = 0.0e0;
        sum3 = 0.0e0;
        sum4 = 0.0e0;
        sum5 = 0.0e0;
        sum6 = 0.0e0;
        sum7 = 0.0e0;
        for (int i = 0; i < STREAM_ARRAY_SIZE; i += 8) {
            sum0 = sum0 + a[i + 0] * b[i + 0];
            sum1 = sum1 + a[i + 1] * b[i + 1];
            sum2 = sum2 + a[i + 2] * b[i + 2];
            sum3 = sum3 + a[i + 3] * b[i + 3];
            sum4 = sum4 + a[i + 4] * b[i + 4];
            sum5 = sum5 + a[i + 5] * b[i + 5];
            sum6 = sum6 + a[i + 6] * b[i + 6];
            sum7 = sum7 + a[i + 7] * b[i + 7];
        }
    }
    sum = sum0 + sum1 + sum2 + sum3 + sum4 + sum5 + sum6 + sum7;
}