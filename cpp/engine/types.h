#pragma once

#include <stdint.h>

#include "config.h"

//square index 0 = a8, 7 = h8, 56 = a1, 63 = h1

//piece types and colors; a piece on the board is color | type, and 0 is an empty square
namespace Piece {
constexpr int white = 8;
constexpr int black = 16;

constexpr int empty = 0;
constexpr int bishop = 1;
constexpr int pawn = 2;
constexpr int rook = 3;
constexpr int knight = 4;
constexpr int queen = 5;
constexpr int king = 6;

constexpr int typeOf(int piece) { return piece & 7; }
constexpr bool isWhite(int piece) { return (piece & white) != 0; }
}

//Java's Position.bit
inline uint64_t squareBit(int index) {
    return 1ULL << index;
}

//index of the lowest set bit. Java's numberOfTrailingZeros(0) is 64 but __builtin_ctzll(0) is undefined, so
//callers must never pass 0; every call site sits inside a `!= 0` check
inline int lsb(uint64_t bits) {
    return __builtin_ctzll(bits);
}

inline int popcount(uint64_t bits) {
    return __builtin_popcountll(bits);
}
