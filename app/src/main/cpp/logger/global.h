//
// Created by parkyu on 2023/5/11.
//

#ifndef METALMAX_GLOBAL_H
#define METALMAX_GLOBAL_H

typedef unsigned char byte;

#ifdef __cplusplus
extern "C" {
#endif

void logd(const char *tag, char *msg);
void loge(char *tag, char *msg);

#ifdef __cplusplus
};
#endif

#endif //METALMAX_GLOBAL_H
