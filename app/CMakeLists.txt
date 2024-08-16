cmake_minimum_required(VERSION 3.4.1)
project(app)

add_library(termux SHARED src/main/jni/termux.c)

target_link_libraries(termux)
