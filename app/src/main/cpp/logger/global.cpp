//
// Created by parkyu on 2023/5/11.
//
#include <android/log.h>
#include <linux/time.h>
#include <sys/time.h>
#include <time.h>
#include <stdio.h>
#include "global.h"

char *get_current_time_str() {
    static char buffer[30];
    struct timeval tv;
    gettimeofday(&tv, NULL);

    struct tm *local = localtime(&tv.tv_sec);

    snprintf(buffer, sizeof(buffer), "%04d-%02d-%02d %02d:%02d:%02d.%03d",
             local->tm_year + 1900,
             local->tm_mon + 1,
             local->tm_mday,
             local->tm_hour,
             local->tm_min,
             local->tm_sec,
             tv.tv_usec / 1000);

    return buffer;
}

void logd(const char *tag, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_print(ANDROID_LOG_DEBUG, tag, fmt, args);
    va_end(args);
}

void loge(const char *tag, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_print(ANDROID_LOG_ERROR, tag, fmt, args);
    va_end(args);
}

void logd(const char *tag, char *msg) {
    __android_log_print(ANDROID_LOG_DEBUG, tag, "%s", msg);
}

void loge(char *tag, char *msg) {
    __android_log_print(ANDROID_LOG_ERROR, tag, "%s", msg);
}