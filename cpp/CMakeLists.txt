cmake_minimum_required(VERSION 3.16)
project(chess_engine CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
    set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
endif()

# the engine uses neither exceptions nor RTTI
add_compile_options(-Wall -Wextra -fno-exceptions -fno-rtti)

# no MinGW runtime DLLs to hunt for when running the tests
if(MINGW)
    add_link_options(-static)
endif()

add_library(engine STATIC
    engine/magic.cpp
    engine/position.cpp
    engine/movegen.cpp
    engine/engine.cpp
    engine/bench.cpp
    platform/platform_host.cpp
)
target_include_directories(engine PUBLIC engine platform)

add_executable(chess_tests tests/main.cpp)
target_link_libraries(chess_tests PRIVATE engine)
target_compile_definitions(chess_tests PRIVATE CHESS_TEST_DATA_DIR="${CMAKE_CURRENT_SOURCE_DIR}/tests/data")

enable_testing()
add_test(NAME chess_tests COMMAND chess_tests)
