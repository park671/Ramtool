// © 2020 Erik Rigtorp <erik@rigtorp.se>
// SPDX-License-Identifier: MIT

// Measure inter-core one-way data latency
//
// Build:
// g++ -O3 -DNDEBUG c2clat.cpp -o c2clat -pthread
//
// Plot results using gnuplot:
// $ c2clat -p | gnuplot -p

#include <sched.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

#include <atomic>
#include <chrono>
#include <iomanip>
#include <iostream>
#include <map>
#include <thread>
#include <vector>
#include <android/log.h>
#include <jni.h>

#define C2CLAT_TAG "c2clat"

void pinThread(int cpu) {
    cpu_set_t set;
    CPU_ZERO(&set);
    CPU_SET(cpu, &set);
    if (sched_setaffinity(0, sizeof(set), &set) == -1) {
        perror("sched_setaffinity");
        exit(1);
    }
}

int *test(int *cpuCount, int mode) {
    int nsamples = 1000;
    bool use_write = false;
    bool preheat = false;
    const char *name = NULL;

    preheat = true;
    use_write = mode == 1;

    cpu_set_t set;
    CPU_ZERO(&set);
    if (sched_getaffinity(0, sizeof(set), &set) == -1) {
        perror("sched_getaffinity");
        exit(1);
    }

    // enumerate available CPUs
    std::vector<int> cpus;
    for (int i = 0; i < CPU_SETSIZE; ++i) {
        if (CPU_ISSET(i, &set)) {
            cpus.push_back(i);
        }
    }

    std::map<std::pair<int, int>, std::chrono::nanoseconds> data;

    for (size_t i = 0; i < cpus.size(); ++i) {
        for (size_t j = i + 1; j < cpus.size(); ++j) {

            alignas(64) std::atomic<int> seq1 = {-1};
            alignas(64) std::atomic<int> seq2 = {-1};

            auto t = std::thread([&] {
                pinThread(cpus[i]);

                if (preheat) {
                    auto init = std::chrono::steady_clock::now();
                    while (1) {
                        auto now = std::chrono::steady_clock::now();
                        if ((now - init).count() >= 200000000)
                            break;
                    }
                }

                for (int m = 0; m < nsamples; ++m) {
                    if (!use_write) {
                        for (int n = 0; n < 100; ++n) {
                            while (seq1.load(std::memory_order_acquire) != n);
                            seq2.store(n, std::memory_order_release);
                        }
                    } else {
                        while (seq2.load(std::memory_order_acquire) != 0);
                        seq2.store(1, std::memory_order_release);
                        for (int n = 0; n < 100; ++n) {
                            int cmp;
                            do {
                                cmp = 2 * n;
                            } while (!seq1.compare_exchange_strong(cmp, cmp + 1));
                        }
                    }
                }
            });

            std::chrono::nanoseconds rtt = std::chrono::nanoseconds::max();

            pinThread(cpus[j]);

            if (preheat) {
                auto init = std::chrono::steady_clock::now();
                while (1) {
                    auto now = std::chrono::steady_clock::now();
                    if ((now - init).count() >= 200000000)
                        break;
                }
            }

            for (int m = 0; m < nsamples; ++m) {
                seq1 = seq2 = -1;
                if (!use_write) {
                    auto ts1 = std::chrono::steady_clock::now();
                    for (int n = 0; n < 100; ++n) {
                        seq1.store(n, std::memory_order_release);
                        while (seq2.load(std::memory_order_acquire) != n);
                    }
                    auto ts2 = std::chrono::steady_clock::now();
                    rtt = std::min(rtt, ts2 - ts1);
                } else {
                    // wait for the other thread to be ready
                    seq2.store(0, std::memory_order_release);
                    while (seq2.load(std::memory_order_acquire) == 0);
                    seq2.store(-1, std::memory_order_release);
                    auto ts1 = std::chrono::steady_clock::now();
                    for (int n = 0; n < 100; ++n) {
                        int cmp;
                        do {
                            cmp = 2 * n - 1;
                        } while (!seq1.compare_exchange_strong(cmp, cmp + 1));
                    }
                    // wait for the other thread to see the last value
                    while (seq1.load(std::memory_order_acquire) != 199);
                    auto ts2 = std::chrono::steady_clock::now();
                    rtt = std::min(rtt, ts2 - ts1);
                }
            }

            t.join();

            data[{i, j}] = rtt / 2 / 100;
            data[{j, i}] = rtt / 2 / 100;
        }
    }

    __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "CPU");
    for (size_t i = 0; i < cpus.size(); ++i) {
        size_t c0 = i;
        int cpuIndex = cpus[c0];
        __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "\t%zu:%d", i, cpuIndex);
    }
    *cpuCount = cpus.size();
    int *result = (int *) malloc(sizeof(int) * (cpus.size() * cpus.size()));
    __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "---");
    for (size_t i = 0; i < cpus.size(); ++i) {
        size_t c0 = i;
        __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "\t%d", cpus[c0]);
        for (size_t j = 0; j < cpus.size(); ++j) {
            size_t c1 = j;
            __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "\t%d", data[{c0, c1}].count());
            result[c0 * (cpus.size()) + c1] = data[{c0, c1}].count();
        }
        __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "---");
    }

    return result;
}

jobjectArray convertCArrayToJava(JNIEnv *env, int *c_array, int rows, int cols) {
    // Find the integer array class
    jclass intArrayClass = env->FindClass("[I");

    // Create the outer Java 2D array
    jobjectArray javaArray = env->NewObjectArray(rows, intArrayClass, nullptr);

    // Populate the Java 2D array with the C++ 2D array data
    for (int i = 0; i < rows; ++i) {
        jintArray row = env->NewIntArray(cols);
        env->SetIntArrayRegion(row, 0, cols, &c_array[i * cols]);
        env->SetObjectArrayElement(javaArray, i, row);
        env->DeleteLocalRef(row); // Clean up local reference
    }

    return javaArray;
}


extern "C" jobjectArray testCore2CoreLat(JNIEnv *env, jint mode) {
    __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "start, mode=%d", mode);
    int cpus = 0;
    int *result = test(&cpus, mode);
    __android_log_print(ANDROID_LOG_DEBUG, C2CLAT_TAG, "finish, cpus=%d", cpus);
    jobjectArray jResult = convertCArrayToJava(env, result, cpus, cpus);
    return jResult;
}
