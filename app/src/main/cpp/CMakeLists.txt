cmake_minimum_required(VERSION 3.22.1)

project("ramtool")

set(ANDROID_STL "none")
enable_language(ASM)
set(CMAKE_ASM_SOURCE_FILE_EXTENSIONS s)

file(GLOB_RECURSE LOGGER_SRC "logger/*.cpp")
file(GLOB_RECURSE ARM_OPT_SRC "opt/*.S")
file(GLOB_RECURSE ARM_STREAM_SRC "stream/*.cpp")
file(GLOB_RECURSE LM_BENCH_SRC "lmbench/*.cpp" "lmbench/*.c")

add_library(ramtool
        SHARED
        ramtool.cpp
        ${LOGGER_SRC}
        ${ARM_OPT_SRC}
        ${ARM_STREAM_SRC}
        ${LM_BENCH_SRC}
        )

target_link_libraries(ramtool log android)
