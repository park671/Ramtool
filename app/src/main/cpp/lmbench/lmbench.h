//
// Created by parkyu on 2023/9/7.
//

#ifndef RAMTOOL_LMBENCH_H
#define RAMTOOL_LMBENCH_H

#ifdef __cplusplus
extern "C" {
#endif

int entrance(int ac, char **av);
double getMemoryLatency();
double getL1CacheLatency();
char * getLatencys();

#ifdef __cplusplus
}
#endif

#endif //RAMTOOL_LMBENCH_H
