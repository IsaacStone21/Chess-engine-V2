#pragma once

#include <stdint.h>

#include "config.h"
#include "movegen.h"
#include "position.h"

//builds every table the engine needs, in this order: magic attack tables, Zobrist keys and castling masks,
//move generator tables, late move reductions, then clears the transposition table (on the Teensy it lives in
//RAM2, which isn't zeroed at boot). Call it once before creating any Position; nothing allocates afterwards
void initEngine();

struct SearchResult {
    uint16_t bestMove;
    int score;
    //deepest iteration that finished
    int depth;
    uint64_t nodes;
};

namespace Engine {

//mate scores are MATE minus the ply the mate happens at, so faster mates score higher
constexpr int MATE = 100000;

//searches 1 ply deep, then 2, then 3... until the time runs out, and returns the best move found (Move::none if
//the game is over). The transposition table and history carry over from earlier searches
uint16_t findBestMove(Position& position, uint32_t timeLimitMillis);

//searches exactly `depth` plies (iterative deepening up to it) with no time limit, starting from an empty table,
//killers and history so the result depends only on the position. Matches Java's Engine.findBestMoveFixedDepth
SearchResult searchFixedDepth(Position& position, int depth);

//makes a running search return as soon as possible with the best move it has so far; safe to call from an
//interrupt or from serviceHook()
void stopSearch();

int getLastDepth();
int getLastScore();
uint64_t getNodes();

bool isMateScore(int score);

//plies from the searched position until the mate lands; only meaningful when isMateScore(score)
int pliesToMate(int score);

//positive scores favor white
int evaluate(const Position& position);

//perft: the number of positions exactly `depth` plies away. Shares the search's move buffers, so never call it
//during a search; depths past maxPly return 0
uint64_t getNumPossiblePositions(Position& position, int depth);

//forgets everything earlier searches learned: transposition table, killers and history
void clearSearchState();

//uses only the first 2^bits transposition table entries (at most ENGINE_TT_BITS), so the PC build can reproduce
//the Teensy's smaller table node for node. Clears the table
void setTableSizeBits(int bits);
int getTableSizeBits();

//where the transposition table lives and how big its storage is, so firmware can confirm it landed in RAM2
const void* tableStorage();
uint32_t tableStorageBytes();

//the late move reduction table, for comparing against the Java engine's
int lateMoveReduction(int depth, int index);
constexpr int lateMoveReductionDepths = maxPly + 1;

void initLateMoveReductions();

}
