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

#if ENGINE_ROOK_BACKEND == ENGINE_ROOK_COMPUTED
extern uint64_t rookTable[rookTableSize];
#endif

//the rook table precomputed by cpp/tools/gen_rook_table into generated/rook_attacks.h (in flash on the Teensy)
extern const uint64_t generatedRookAttacks[rookTableSize];

//fills the masks, shifts and offsets, and the attack tables the configured backend needs
void init();

//fills `table` (rookTableSize entries) with every rook attack set; needs init() first. The engine, the header
//generator and the tests all build the table through this one function
void buildRookTable(uint64_t* table);

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

//GCC has no bit reversal builtin, but the Cortex-M7 has an rbit instruction for 32 bits
inline uint32_t reverseBits32(uint32_t x) {
#if defined(__arm__) && defined(__ARM_ARCH) && __ARM_ARCH >= 7
    uint32_t reversed;
    __asm__("rbit %0, %1" : "=r"(reversed) : "r"(x));
    return reversed;
#else
    x = ((x >> 1) & 0x55555555u) | ((x & 0x55555555u) << 1);
    x = ((x >> 2) & 0x33333333u) | ((x & 0x33333333u) << 2);
    x = ((x >> 4) & 0x0F0F0F0Fu) | ((x & 0x0F0F0F0Fu) << 4);
    return __builtin_bswap32(x);
#endif
}

//reversing each half and swapping them reverses all 64 bits
inline uint64_t reverseBits64(uint64_t x) {
    return ((uint64_t) reverseBits32((uint32_t) x) << 32) | reverseBits32((uint32_t) (x >> 32));
}

//hyperbola quintessence along one line through `square` (lineMask excludes the square itself): subtracting the
//slider from the blockers ripples a carry up to the first blocker, and doing the same on the reversed board
//finds the first blocker in the other direction
inline uint64_t lineAttacks(int square, uint64_t occupied, uint64_t lineMask) {
    uint64_t slider = squareBit(square);
    uint64_t forward = occupied & lineMask;
    uint64_t reverse = reverseBits64(forward);
    forward -= slider;
    reverse -= reverseBits64(slider);
    return (forward ^ reverseBits64(reverse)) & lineMask;
}

inline uint64_t rookAttacksHyperbola(int square, uint64_t occupied) {
    uint64_t file = (0x0101010101010101ULL << (square & 7)) ^ squareBit(square);
    uint64_t rank = (0xFFULL << (square & 56)) ^ squareBit(square);
    return lineAttacks(square, occupied, file) | lineAttacks(square, occupied, rank);
}

inline uint64_t rookAttacks(int square, uint64_t occupied) {
#if ENGINE_ROOK_BACKEND == ENGINE_ROOK_HYPERBOLA
    return rookAttacksHyperbola(square, occupied);
#elif ENGINE_ROOK_BACKEND == ENGINE_ROOK_FLASH
    return generatedRookAttacks[rookIndex(square, occupied)];
#else
    return rookTable[rookIndex(square, occupied)];
#endif
}

inline uint64_t bishopAttacks(int square, uint64_t occupied) {
    return bishopTable[bishopIndex(square, occupied)];
}

inline uint64_t queenAttacks(int square, uint64_t occupied) {
    return rookAttacks(square, occupied) | bishopAttacks(square, occupied);
}

}
