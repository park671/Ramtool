/*
 * lat_mem_rd.c - measure memory load latency
 *
 * usage: lat_mem_rd [-P <parallelism>] [-W <warmup>] [-N <repetitions>] [-t] size-in-MB [stride ...]
 *
 * Copyright (c) 1994 Larry McVoy.  
 * Copyright (c) 2003, 2004 Carl Staelin.
 *
 * Distributed under the FSF GPL with additional restriction that results 
 * may published only if:
 * (1) the benchmark is unmodified, and
 * (2) the version in the sccsid below is included in the report.
 * Support for this development by Sun Microsystems is gratefully acknowledged.
 */
char *id = "$Id: s.lat_mem_rd.c 1.13 98/06/30 16:13:49-07:00 lm@lm.bitmover.com $\n";

#include "bench.h"
#include "../logger/global.h"

#define STRIDE  (512/sizeof(char *))
#define    LOWER    512

void loads(size_t len, size_t range, size_t stride,
           int parallel, int warmup, int repetitions);

size_t step(size_t k);

void initialize(iter_t iterations, void *cookie);

benchmp_f fpInit = stride_initialize;

char info[110], latency_result_string[2000];

double memSizeResult[500];
double latencyResult[500];
int resultIndex = 0;

char * getLatencys(){
    return latency_result_string;
}

int entrance(int ac, char **av) {
    resultIndex = 0;
    int i;
    int c;
    int parallel = 1;
    int warmup = 0;
    int repetitions = TRIES;
    size_t len;
    size_t range;
    size_t stride;
    char *usage = "[-P <parallelism>] [-W <warmup>] [-N <repetitions>] [-t] len [stride...]\n";
    while ((c = getopt(ac, av, "tP:W:N:")) != EOF) {
        switch (c) {
            case 't':
                fpInit = thrash_initialize;
                break;
            case 'P':
                parallel = atoi(optarg);
                if (parallel <= 0) lmbench_usage(ac, av, usage);
                break;
            case 'W':
                warmup = atoi(optarg);
                break;
            case 'N':
                repetitions = atoi(optarg);
                break;
            default:
                lmbench_usage(ac, av, usage);
                break;
        }
    }
    if (optind == ac) {
        lmbench_usage(ac, av, usage);
    }

    len = atoi(av[optind]);
    len *= 1024 * 1024;

    if (optind == ac - 1) {
        sprintf(info, "stride=%d\n", STRIDE);
		logd("lmbench", (char *)info);
        for (range = LOWER; range <= len; range = step(range)) {
            loads(len, range, STRIDE, parallel,
                  warmup, repetitions);
        }
    } else {
        for (i = optind + 1; i < ac; ++i) {
            stride = bytes(av[i]);
            sprintf(info, "stride=%d\n", stride);
			logd("lmbench", info);
            for (range = LOWER; range <= len; range = step(range)) {
                loads(len, range, stride, parallel,
                      warmup, repetitions);
            }
            sprintf(info, "\n");
			logd("lmbench", info);
        }
    }
    logd("lmbench", "strcat result");
    memset(latency_result_string, 0, 2000);
    for (i = 0; i < resultIndex; ++i) {
        if (i) {
            strcat(latency_result_string, "#");
        }
        sprintf(info, "%.5f:%.3f", memSizeResult[i], latencyResult[i]);
        strcat(latency_result_string, info);
    }
    resultIndex = 0;
    logd("lmbench", "strcat finish");
    return (0);
}

#define    ONE    p = (char **)*p;
#define    FIVE    ONE ONE ONE ONE ONE
#define    TEN    FIVE FIVE
#define    FIFTY    TEN TEN TEN TEN TEN
#define    HUNDRED    FIFTY FIFTY


void
benchmark_loads(iter_t iterations, void *cookie) {
    struct mem_state *state = (struct mem_state *) cookie;
    register char **p = (char **) state->p[0];
    register size_t i;
    register size_t count = state->len / (state->line * 100) + 1;

    while (iterations-- > 0) {
        for (i = 0; i < count; ++i) {
            HUNDRED;
        }
    }

    use_pointer((void *) p);
    state->p[0] = (char *) p;
}

void
loads(size_t len, size_t range, size_t stride,
      int parallel, int warmup, int repetitions) {
    double result;
    size_t count;
    struct mem_state state;

    if (range < stride) return;

    state.width = 1;
    state.len = range;
    state.maxlen = len;
    state.line = stride;
    state.pagesize = getpagesize();
    count = 100 * (state.len / (state.line * 100) + 1);

#if 0
    (*fpInit)(0, &state);
    sprintf(info, "loads: after init\n");
	logd("lmbench", info);
    (*benchmark_loads)(2, &state);
    sprintf(info, "loads: after benchmark\n");
	logd("lmbench", info);
    mem_cleanup(0, &state);
    sprintf(info, "loads: after cleanup\n");
	logd("lmbench", info);
    settime(1);
    save_n(1);
#else
    /*
     * Now walk them and time it.
     */
    benchmp(fpInit, benchmark_loads, mem_cleanup,
            100000, parallel, warmup, repetitions, &state);
#endif

    /* We want to get to nanoseconds / load. */
    save_minimum();
    result = (1000. * (double) gettime()) / (double) (count * get_n());

    memSizeResult[resultIndex] = range / (1024. * 1024.);
    latencyResult[resultIndex] = result;
    sprintf(info, "%.5f %.3f\n", memSizeResult[resultIndex], latencyResult[resultIndex]);
    resultIndex++;

	logd("lmbench", info);
}

size_t
step(size_t k) {
    if (k < 1024) {
        k = k * 2;
    } else if (k < 4 * 1024) {
        k += 1024;
    } else {
        size_t s;

        for (s = 32 * 1024; s <= k; s *= 2);
        k += s / 16;
    }
    return (k);
}
