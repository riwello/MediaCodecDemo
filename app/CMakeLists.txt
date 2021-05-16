# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Declares and names the project.


# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        ijkffmpegCmd

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/H264ToJPEG.c
        src/main/cpp/IjkffmpegCmd_jni.cpp)


# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

#ijk的ffmpeg
add_library(ijkffmpeg
        SHARED
        IMPORTED)
set_target_properties(
        ijkffmpeg
        PROPERTIES IMPORTED_LOCATION
        # Android 4、0 以后， cmake 引用的动态库不能放在jniLibs里，需要换个路径
        ${CMAKE_SOURCE_DIR}/src/main/extra/${CMAKE_ANDROID_ARCH_ABI}/libijkffmpeg.so
)

# FfmpegAndroid的ffmpeg
#add_library(ffmpeg
#        SHARED
#        IMPORTED)
#set_target_properties(
#        ffmpeg
#        PROPERTIES IMPORTED_LOCATION
#        ${CMAKE_SOURCE_DIR}/src/main/extra/${CMAKE_ANDROID_ARCH_ABI}/libffmpeg.so
#)

include_directories(src/main/cpp)

include_directories(src/main/cpp/include)

if (${CMAKE_ANDROID_ARCH_ABI} MATCHES "armeabi-v7a")
    include_directories(src/main/cpp/include/armeabi-v7a)

elseif (${CMAKE_ANDROID_ARCH_ABI} MATCHES "arm64-v8a")
    include_directories(src/main/cpp/include/arm64-v8a)

endif ()


find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        ijkffmpegCmd
        ijkffmpeg
#        ffmpeg
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Os -Wall -s")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Os -Wall -s")