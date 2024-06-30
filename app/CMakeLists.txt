cmake_minimum_required(VERSION 3.4.1)

add_library(termux
             SHARED
             src/main/cpp/termux.c)

target_link_libraries(termux)

#add_library(nexec
#             SHARED
#             src/main/cpp/exec.c)

