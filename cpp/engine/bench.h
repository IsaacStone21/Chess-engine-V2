#pragma once

#include <stdint.h>

#include "movegen.h"
#include "position.h"

//the fixed-depth suite shared by the PC tests, the PC benchmark and the Teensy's `bench` command. It must list the
//same ids and moves as Testing/EngineReference.java; the PC test checks that against the Java reference data
struct BenchPosition {
    const char* id;
    //coordinate moves from the start position, e.g. "e2e4 e7e5 g1f3"; a fifth letter picks a promotion piece
    const char* moves;
};

extern const BenchPosition benchSuite[];
extern const int benchSuiteSize;

constexpr int defaultBenchDepth = 8;

//resets `position` to the start and plays the moves; false (with `position` part way through) if one is illegal
bool playCoordinateMoves(Position& position, const char* moves, MoveGenerator& moveGenerator);

//plays one coordinate move such as "e7e8q" (exactly 4 or 5 characters starting at `text`); false if illegal
bool playCoordinateMove(Position& position, const char* text, int length, MoveGenerator& moveGenerator);

//writes the move as "e2e4" / "e7e8q" (or "none") into `out`, which needs 6 chars
void moveToCoordinates(uint16_t move, char* out);

//sum of every late move reduction table entry and the start position's hash: two numbers that show at a glance
//whether a build's floating point log and Zobrist keys match the PC build
uint32_t lateMoveReductionChecksum();
uint64_t startPositionHash();
