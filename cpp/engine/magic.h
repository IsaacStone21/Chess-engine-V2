#pragma once

#include <stdint.h>

#include "types.h"

//"Fancy" magic bitboards for sliding piece attacks. For each square, the relevant blockers
//(occupied & mask) are hashed with a multiply-and-shift into an index into a precomputed attack table.
//"Fancy" means every square gets its own variable-sized slice of one shared table (sized 2^bitsInMask),
//instead of a fixed worst-case 4096/512 entries per square.
//Square indexing matches the rest of the engine: index = row * 8 + col, row 0 being the top of the board.
namespace Magic {

constexpr int rookTableSize = 102400;
constexpr int bishopTableSize = 5248;

extern const uint64_t rookMagics[64];
extern const uint64_t bishopMagics[64];

//filled by init()
extern uint64_t rookMasks[64];
extern uint64_t bishopMasks[64];
extern int rookShifts[64];
extern int bishopShifts[64];
extern int rookOffsets[64];
extern int bishopOffsets[64];
extern uint64_t bishopTable[bishopTableSize];
extern uint64_t rookTable[rookTableSize];

//fills the masks, shifts, offsets and attack tables
void init();

//attacks found by walking the rays square by square; only used to build tables and in tests
uint64_t slowRookAttacks(int square, uint64_t occupied);
uint64_t slowBishopAttacks(int square, uint64_t occupied);

inline int rookIndex(int square, uint64_t occupied) {
    uint64_t blockers = occupied & rookMasks[square];
    return rookOffsets[square] + (int) ((blockers * rookMagics[square]) >> rookShifts[square]);
}

inline int bishopIndex(int square, uint64_t occupied) {
    uint64_t blockers = occupied & bishopMasks[square];
    return bishopOffsets[square] + (int) ((blockers * bishopMagics[square]) >> bishopShifts[square]);
}

inline uint64_t rookAttacks(int square, uint64_t occupied) {
    return rookTable[rookIndex(square, occupied)];
}

inline uint64_t bishopAttacks(int square, uint64_t occupied) {
    return bishopTable[bishopIndex(square, occupied)];
}

inline uint64_t queenAttacks(int square, uint64_t occupied) {
    return rookAttacks(square, occupied) | bishopAttacks(square, occupied);
}

}
