project(TeamSdk)
cmake_minimum_required(VERSION 3.8)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_STANDARD 17)
#set(CMAKE_BUILD_TYPE Debug)
# 发布版如下
#set(CMAKE_CXX_FLAGS -O3)
set(CMAKE_BUILD_TYPE Release)
#add_compile_definitions(DISABLE_PTHREAD_HOOK)
#if (NOT WIN32)
#    link_libraries(pthread m)
#endif (NOT WIN32)

#MATH(EXPR stack_size "500*1024*1024")
#set(CMAKE_EXE_LINKER_FLAGS "-Wl,--stack,${stack_size}")
#curl配置
set(CURL_PATH "D:/curl-8.7.1_7-win64-mingw")
find_path(CURL_INCLUDE_DIR NAMES curl/curl.h PATHS ${CURL_PATH}/include)

#SET (CMAKE_FIND_LIBRARY_SUFFIXES ".so" ".a")
find_library(CURL_LIBRARY NAMES "libcurl" PATHS ${CURL_PATH}/lib)
include_directories(${CURL_INCLUDE_DIR})

AUX_SOURCE_DIRECTORY(. src)
#list(REMOVE_ITEM src "./load_pthread.cpp")

ADD_EXECUTABLE(main ${src})
target_link_libraries(main ${CURL_LIBRARY})




