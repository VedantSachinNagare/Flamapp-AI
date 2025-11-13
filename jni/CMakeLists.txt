cmake_minimum_required(VERSION 3.22.1)
project(flappnative LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(OPENCV_DIR_HINT $ENV{OpenCV_DIR})
if (NOT OPENCV_DIR_HINT)
    message(WARNING "OpenCV_DIR environment variable not set. Configure the OpenCV path before building.")
endif ()

find_package(OpenCV REQUIRED core imgproc)

add_library(flappnative SHARED
    native-lib.cpp
    opencv_processor.cpp
)

target_include_directories(flappnative
    PRIVATE
        ${OpenCV_INCLUDE_DIRS}
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(flappnative
    PRIVATE
        ${OpenCV_LIBS}
        log
        android
        GLESv2
)

