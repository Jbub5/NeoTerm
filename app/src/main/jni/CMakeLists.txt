cmake_minimum_required(VERSION 3.4.1)

add_library(termux SHARED termux.c)

target_link_libraries(termux)
