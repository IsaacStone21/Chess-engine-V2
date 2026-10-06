#pragma once

#include <stdint.h>

#include "position.h"
#include "types.h"

//strictly legal move generation. The attack tables are shared by every generator and filled once by initTables();
//each MoveGenerator object only holds the check and pin state of the position it last generated for
class MoveGenerator {
public:
    //part of initEngine()
    static void initTables();

    //fills `moves` (at least maxMoves long) with every legal move and returns how many there are
    int generateMoves(Position& currentPosition, uint16_t* moves);

    int getNumLegalMoves(Position& currentPosition);

    //returns the first legal move between the two squares, or Move::none; for a promotion this is the
    //queen promotion because promotions are generated strongest piece first
    uint16_t getLegalMove(int startingIndex, int targetIndex, Position& currentPosition);

    //promotionPiece is ignored for moves that aren't promotions
    uint16_t getLegalMove(int startingIndex, int targetIndex, int promotionPiece, Position& currentPosition);

    uint64_t attackersOf(const Position& position, int square, bool byWhite, uint64_t occupied) const;
    bool isSquareAttacked(Position& position, int square, bool byWhite) const;
    bool isSquareAttacked(const Position& position, int square, bool byWhite, uint64_t occupied) const;
    bool inCheck(Position& position) const;

private:
    //check and pin state for the side to move, filled in by computeCheckAndPins at the start of generateMoves
    int kingSquare = -1;
    //squares a non-king move must land on: everything when not in check, the checker plus the squares
    //between it and the king in single check, nothing in double check
    uint64_t checkMask = 0;
    //our pieces pinned to our king, and for each pinned piece the squares it may still move to
    uint64_t pinned = 0;
    uint64_t pinRay[64] = {};

    //buffer for the single-shot helpers (getLegalMove, getNumLegalMoves); searches pass their own per-ply buffers
    uint16_t scratchMoves[maxMoves];

    void computeCheckAndPins(Position& position);
    int generateSlidingMoves(Position& position, uint64_t pieces, bool orthogonal, bool diagonal, uint16_t* moves, int count);
    int generateKnightMoves(Position& position, uint16_t* moves, int count);
    int generateKingMoves(Position& position, uint16_t* moves, int count);
    int generateCastlingMoves(Position& position, uint16_t* moves, int count);
    int generatePawnMoves(Position& position, uint16_t* moves, int count);
    int addPawnMoves(uint64_t targets, int offset, uint64_t promotionRank, uint16_t* moves, int count);
    int generateEnPassantMoves(Position& position, uint16_t* moves, int count);
};
