#pragma once

#include <stdint.h>

//every size and placement that differs between the PC build and the Teensy 4.1 build lives here

#if defined(__IMXRT1062__) || defined(ARDUINO_TEENSY41)
#define ENGINE_TEENSY 1
#else
#define ENGINE_TEENSY 0
#endif

//where the rook attack lookups come from:
//  computed:  the fancy magic table, built in RAM at startup (what the Java engine does)
//  flash:     the same table, precomputed into a const array in flash (cpp/engine/generated/rook_attacks.h);
//             100 KB of entries would not fit in the Teensy's RAM next to everything else
//  hyperbola: no table at all; hyperbola quintessence works the attacks out with two bit reversals
#define ENGINE_ROOK_COMPUTED 0
#define ENGINE_ROOK_FLASH 1
#define ENGINE_ROOK_HYPERBOLA 2

#ifndef ENGINE_ROOK_BACKEND
#if ENGINE_TEENSY
#define ENGINE_ROOK_BACKEND ENGINE_ROOK_FLASH
#else
#define ENGINE_ROOK_BACKEND ENGINE_ROOK_COMPUTED
#endif
#endif

//transposition table entries, as a power of two. The PC build must match the Java engine (2^20) for node counts
//to be identical; the Teensy's 2^14 entries of 16 bytes fill half of RAM2 and leave the rest to the heap
#ifndef ENGINE_TT_BITS
#if ENGINE_TEENSY
#define ENGINE_TT_BITS 14
#else
#define ENGINE_TT_BITS 20
#endif
#endif

//section attributes, spelled out here because the engine can't include Arduino.h. On the Teensy 4.1 const data is
//copied into RAM1 at boot unless it's placed in .progmem, and DMAMEM puts a variable in RAM2 (OCRAM), which
//the startup code does not zero
#if ENGINE_TEENSY
#define ENGINE_FLASH __attribute__((section(".progmem")))
#define ENGINE_DMAMEM __attribute__((section(".dmabuffers"), used))
#else
#define ENGINE_FLASH
#define ENGINE_DMAMEM
#endif

//the most legal moves any position has is 218, and the generator is strictly legal
constexpr int maxMoves = 256;

//moves a Position remembers for undo and repetition checks; the oldest half is dropped once it fills up
constexpr int historyCapacity = 1024;

//deepest ply the search reaches, including check extensions
constexpr int maxPly = 64;
