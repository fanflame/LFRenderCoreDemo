cmake_minimum_required(VERSION 3.22.1)

project("ffmpegutil")

add_library( # Sets the name of the library.
        ffmpegutil
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        native-lib.cpp)
include_directories(./include
        ./include/libavcodec
        ./include/libavdevice
        ./include/libavfilter
        ./include/libavformat
        ./include/libavutil
        ./include/libswresample
        ./include/libswscale
        )

find_library( # Sets the name of the path variable.
        log-lib
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

target_link_libraries( # Specifies the target library.
        test

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})