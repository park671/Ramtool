//
// Created by Park Yu on 2024/9/4.
//

#ifndef HARDWARE_TOOL_INST_H
#define HARDWARE_TOOL_INST_H

extern "C" int testAarch64();
extern "C" int testVfp();

extern "C" int testNeon();
extern "C" int testAsimd();
extern "C" int testSve();
extern "C" int testSve2();

extern "C" int testAes();
extern "C" int testSha1();
extern "C" int testSha2();
extern "C" int testPmull();
extern "C" int testCrc32();

#endif //HARDWARE_TOOL_INST_H
