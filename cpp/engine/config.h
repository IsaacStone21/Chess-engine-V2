#pragma once

#include <stdint.h>

//transposition table entries, as a power of two. Must match the Java engine (2^20) for node counts to be
//identical
constexpr int tableSizeBits = 20;

//the most legal moves any position has is 218, and the generator is strictly legal
constexpr int maxMoves = 256;

//moves a Position remembers for undo and repetition checks; the oldest half is dropped once it fills up
constexpr int historyCapacity = 1024;

//deepest ply the search reaches, including check extensions
constexpr int maxPly = 64;
