cmake_minimum_required(VERSION 3.22.1)

project("hardwaretool")

set(ANDROID_STL "none")
enable_language(ASM)
set(CMAKE_ASM_SOURCE_FILE_EXTENSIONS s)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -DNDEBUG")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -DNDEBUG")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfpu=neon -march=armv9-a+sve+crypto")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon -march=armv9-a+sve+crypto")


file(GLOB_RECURSE LOGGER_SRC "logger/*.cpp")
file(GLOB_RECURSE ARM_OPT_SRC "opt/*.S")
file(GLOB_RECURSE ARM_STREAM_SRC "stream/*.cpp")
file(GLOB_RECURSE LM_BENCH_SRC "lmbench/*.cpp" "lmbench/*.c")
file(GLOB_RECURSE CPU_BENCH_SRC "cpu/*.cpp" "cpu/*.c" "cpu/*.s")

add_library(hardwaretool
        SHARED
        hardwaretool.cpp
        cpu/load/load.cpp
        ${LOGGER_SRC}
        ${ARM_OPT_SRC}
        ${ARM_STREAM_SRC}
        ${LM_BENCH_SRC}
        ${CPU_BENCH_SRC}
)

target_link_libraries(hardwaretool log android)
