cmake_minimum_required(VERSION 3.16)
project(chess_engine CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

if(NOT CMAKE_BUILD_TYPE AND NOT CMAKE_CONFIGURATION_TYPES)
    set(CMAKE_BUILD_TYPE Release CACHE STRING "" FORCE)
endif()

# the engine has to build without exceptions or RTTI for the Teensy, so everything here does too
add_compile_options(-Wall -Wextra -fno-exceptions -fno-rtti)

# no MinGW runtime DLLs to hunt for when running the tests
if(MINGW)
    add_link_options(-static)
endif()

if(NOT EXISTS ${CMAKE_CURRENT_SOURCE_DIR}/engine/generated/rook_attacks.h)
    message(WARNING "engine/generated/rook_attacks.h is missing: build the regen_rook_table target first")
endif()

set(ENGINE_SOURCES
    engine/magic.cpp
    engine/position.cpp
    engine/movegen.cpp
    engine/engine.cpp
    engine/bench.cpp
    platform/platform_host.cpp
)

# the 100k-entry rook header only needs compiling once, whatever backend the engine uses
add_library(rook_table OBJECT engine/rook_table.cpp)
target_include_directories(rook_table PRIVATE engine)

# one engine library per rook attack backend: computed table (the default, as in Java), flash table (what the
# Teensy uses) and hyperbola quintessence. Every test runs against all three
function(add_engine_variant suffix backend)
    add_library(engine_${suffix} STATIC ${ENGINE_SOURCES} $<TARGET_OBJECTS:rook_table>)
    target_include_directories(engine_${suffix} PUBLIC engine platform)
    target_compile_definitions(engine_${suffix} PUBLIC ENGINE_ROOK_BACKEND=${backend})

    add_executable(chess_tests${ARGN} tests/main.cpp)
    target_link_libraries(chess_tests${ARGN} PRIVATE engine_${suffix})
    target_compile_definitions(chess_tests${ARGN} PRIVATE CHESS_TEST_DATA_DIR="${CMAKE_CURRENT_SOURCE_DIR}/tests/data")
endfunction()

add_engine_variant(computed 0)
add_engine_variant(flash 1 _flash)
add_engine_variant(hyperbola 2 _hyperbola)

# writes engine/generated/rook_attacks.h; magic.cpp is compiled straight in so it doesn't need the header it makes
add_executable(gen_rook_table tools/gen_rook_table.cpp engine/magic.cpp)
target_include_directories(gen_rook_table PRIVATE engine)
target_compile_definitions(gen_rook_table PRIVATE ENGINE_ROOK_BACKEND=0)

add_custom_target(regen_rook_table
    COMMAND gen_rook_table ${CMAKE_CURRENT_SOURCE_DIR}/engine/generated/rook_attacks.h
    DEPENDS gen_rook_table
    COMMENT "Regenerating engine/generated/rook_attacks.h"
)

enable_testing()
add_test(NAME chess_tests COMMAND chess_tests)
add_test(NAME chess_tests_flash COMMAND chess_tests_flash)
add_test(NAME chess_tests_hyperbola COMMAND chess_tests_hyperbola)
