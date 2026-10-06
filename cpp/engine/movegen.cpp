#include "movegen.h"

#include "magic.h"

//Up, down, left, right, UL, UR, DL, DR
static const int directionOffsets[8] = {-8, 8, -1, 1, -9, -7, 7, 9};

// UUR, URR, DRR, DDR, DDL, DLL, ULL, UUL
static const int knightOffsets[8] = {-15, -6, 10, 17, 15, 6, -10, -17};
static uint64_t knightAttackTable[64];

//contains the number of squares to the edge in each direction
//first number is the index
//second is the direction: Up, down, left, right, UL, UR, DL, DR
static int numSquaresToEdge[64][8];

static uint64_t kingAttackTable[64];

//attacks[square] = squares a pawn of that color on `square` attacks; used both to generate pawn
//captures elsewhere and, via the symmetry trick in isSquareAttacked, to detect check
static uint64_t whitePawnAttackTable[64];
static uint64_t blackPawnAttackTable[64];

//between[a][b] = squares strictly between a and b if they share a rank, file or diagonal, otherwise 0
static uint64_t between[64][64];

static constexpr uint16_t whiteKingsideCastle = Move::castling(60, 62);
static constexpr uint16_t whiteQueensideCastle = Move::castling(60, 58);
static constexpr uint16_t blackKingsideCastle = Move::castling(4, 6);
static constexpr uint16_t blackQueensideCastle = Move::castling(4, 2);

static void generateSquaresToEdge() {
    for(int index = 0; index < 64; index++) {
        int row = (index - (index % 8)) / 8;
        int col = index - 8 * row;

        int numSquaresNorth = row;
        int numSquaresSouth = 7 - row;
        int numSquaresEast = col;
        int numSquaresWest = 7 - col;

        numSquaresToEdge[index][0] = numSquaresNorth;
        numSquaresToEdge[index][1] = numSquaresSouth;
        numSquaresToEdge[index][2] = numSquaresEast;
        numSquaresToEdge[index][3] = numSquaresWest;
        numSquaresToEdge[index][4] = numSquaresNorth < numSquaresEast ? numSquaresNorth : numSquaresEast;
        numSquaresToEdge[index][5] = numSquaresNorth < numSquaresWest ? numSquaresNorth : numSquaresWest;
        numSquaresToEdge[index][6] = numSquaresSouth < numSquaresEast ? numSquaresSouth : numSquaresEast;
        numSquaresToEdge[index][7] = numSquaresSouth < numSquaresWest ? numSquaresSouth : numSquaresWest;
    }
}

//checks if the move doesnt wrap around the board
static bool wrapsBoard(int startingIndex, int targetIndex) {
    int startCol = startingIndex % 8;
    int attackedCol = targetIndex % 8;
    int difference = attackedCol - startCol;
    return difference > 2 || difference < -2;
}

static void generateKnightTable() {
    for(int i = 0; i < 64; i++) {
        uint64_t attackableSquares = 0;

        for(int j = 0; j < 8; j++) {
            int targetIndex = i + knightOffsets[j];
            if(targetIndex < 64 && targetIndex >= 0 && !wrapsBoard(i, targetIndex)) {
                attackableSquares |= squareBit(targetIndex);
            }
        }

        knightAttackTable[i] = attackableSquares;
    }
}

static void generateKingTable() {
    for(int i = 0; i < 64; i++) {
        uint64_t attackableSquares = 0;

        for(int dir = 0; dir < 8; dir++) {
            if(numSquaresToEdge[i][dir] > 0) {
                attackableSquares |= squareBit(i + directionOffsets[dir]);
            }
        }

        kingAttackTable[i] = attackableSquares;
    }
}

static void generatePawnAttackTables() {
    for(int i = 0; i < 64; i++) {
        uint64_t whiteAttacks = 0;
        uint64_t blackAttacks = 0;

        //white pawns attack UL/UR (dir 4/5); black pawns attack DL/DR (dir 6/7)
        if(numSquaresToEdge[i][4] > 0) whiteAttacks |= squareBit(i + directionOffsets[4]);
        if(numSquaresToEdge[i][5] > 0) whiteAttacks |= squareBit(i + directionOffsets[5]);
        if(numSquaresToEdge[i][6] > 0) blackAttacks |= squareBit(i + directionOffsets[6]);
        if(numSquaresToEdge[i][7] > 0) blackAttacks |= squareBit(i + directionOffsets[7]);

        whitePawnAttackTable[i] = whiteAttacks;
        blackPawnAttackTable[i] = blackAttacks;
    }
}

static void generateBetweenTable() {
    for(int from = 0; from < 64; from++) {
        for(int to = 0; to < 64; to++) {
            between[from][to] = 0;
        }
        for(int dir = 0; dir < 8; dir++) {
            uint64_t ray = 0;
            int square = from;

            for(int step = 0; step < numSquaresToEdge[from][dir]; step++) {
                square += directionOffsets[dir];
                //everything passed so far, excluding both ends
                between[from][square] = ray;
                ray |= squareBit(square);
            }
        }
    }
}

void MoveGenerator::initTables() {
    generateSquaresToEdge();
    generateKnightTable();
    generateKingTable();
    generatePawnAttackTables();
    generateBetweenTable();
}

//sliding attacks via magic bitboard lookup (see magic.h); `occupied` blockers are included in the result
static uint64_t getSlidingAttacks(int square, uint64_t occupied, bool orthogonal, bool diagonal) {
    uint64_t attacks = 0;

    if(orthogonal) {
        attacks |= Magic::rookAttacks(square, occupied);
    }

    if(diagonal) {
        attacks |= Magic::bishopAttacks(square, occupied);
    }

    return attacks;
}

//bitboard of every `byWhite` piece attacking `square`, using `occupied` as the blockers. Works by placing
//each attacker type on `square` and checking whether its attack pattern hits an actual enemy piece of that
//type - attacks are symmetric, so this is equivalent to asking which enemy pieces attack `square` directly.
//Taking occupancy as a parameter lets king moves be tested with the king itself lifted off the board.
uint64_t MoveGenerator::attackersOf(const Position& position, int square, bool byWhite, uint64_t occupied) const {
    uint64_t enemyQueens = byWhite ? position.whiteQueens : position.blackQueens;
    uint64_t enemyRooks = (byWhite ? position.whiteRooks : position.blackRooks) | enemyQueens;
    uint64_t enemyBishops = (byWhite ? position.whiteBishops : position.blackBishops) | enemyQueens;

    //a pawn of `byWhite`'s color attacks `square` iff `square` shows up in the opposite-colored
    //pawn attack table centered on `square` itself (the symmetry trick mentioned above)
    uint64_t pawnAttackersFromSquare = byWhite ? blackPawnAttackTable[square] : whitePawnAttackTable[square];

    return (pawnAttackersFromSquare & (byWhite ? position.whitePawns : position.blackPawns))
         | (knightAttackTable[square] & (byWhite ? position.whiteKnights : position.blackKnights))
         | (kingAttackTable[square] & (byWhite ? position.whiteKing : position.blackKing))
         | (Magic::rookAttacks(square, occupied) & enemyRooks)
         | (Magic::bishopAttacks(square, occupied) & enemyBishops);
}

bool MoveGenerator::isSquareAttacked(Position& position, int square, bool byWhite) const {
    return isSquareAttacked(position, square, byWhite, position.getOccupiedSquares());
}

//same as attackersOf(...) != 0, but bails out at the first attacker found
bool MoveGenerator::isSquareAttacked(const Position& position, int square, bool byWhite, uint64_t occupied) const {
    uint64_t enemyQueens = byWhite ? position.whiteQueens : position.blackQueens;
    uint64_t enemyRooks = (byWhite ? position.whiteRooks : position.blackRooks) | enemyQueens;
    uint64_t enemyBishops = (byWhite ? position.whiteBishops : position.blackBishops) | enemyQueens;

    uint64_t pawnAttackersFromSquare = byWhite ? blackPawnAttackTable[square] : whitePawnAttackTable[square];
    if((pawnAttackersFromSquare & (byWhite ? position.whitePawns : position.blackPawns)) != 0) return true;

    if((knightAttackTable[square] & (byWhite ? position.whiteKnights : position.blackKnights)) != 0) return true;
    if((kingAttackTable[square] & (byWhite ? position.whiteKing : position.blackKing)) != 0) return true;
    if((Magic::rookAttacks(square, occupied) & enemyRooks) != 0) return true;
    if((Magic::bishopAttacks(square, occupied) & enemyBishops) != 0) return true;

    return false;
}

bool MoveGenerator::inCheck(Position& position) const {
    bool white = position.isWhiteToMove();
    uint64_t king = white ? position.whiteKing : position.blackKing;

    if(king == 0) {
        return false;
    }

    int square = lsb(king);
    return isSquareAttacked(position, square, !white);
}

//fills kingSquare, checkMask, pinned and pinRay for the side to move
void MoveGenerator::computeCheckAndPins(Position& position) {
    bool white = position.isWhiteToMove();
    uint64_t king = white ? position.whiteKing : position.blackKing;

    pinned = 0;

    //positions without a king (only used for testing) have nothing to protect
    if(king == 0) {
        kingSquare = -1;
        checkMask = ~0ULL;
        return;
    }

    kingSquare = lsb(king);
    uint64_t occupied = position.getOccupiedSquares();
    uint64_t friendlyPieces = white ? position.getWhitePieces() : position.getBlackPieces();
    uint64_t enemyPieces = white ? position.getBlackPieces() : position.getWhitePieces();

    uint64_t checkers = attackersOf(position, kingSquare, !white, occupied);

    if(checkers == 0) {
        checkMask = ~0ULL;
    } else if(popcount(checkers) == 1) {
        //capture the checker or step into the line between it and the king (empty for knights and pawns)
        checkMask = checkers | between[kingSquare][lsb(checkers)];
    } else {
        checkMask = 0;
    }

    uint64_t enemyQueens = white ? position.blackQueens : position.whiteQueens;
    uint64_t enemyRooks = (white ? position.blackRooks : position.whiteRooks) | enemyQueens;
    uint64_t enemyBishops = (white ? position.blackBishops : position.whiteBishops) | enemyQueens;

    //looking out from the king with only enemy pieces as blockers sees straight through our own pieces,
    //so this finds every enemy slider lined up with the king
    uint64_t snipers = (Magic::rookAttacks(kingSquare, enemyPieces) & enemyRooks)
                     | (Magic::bishopAttacks(kingSquare, enemyPieces) & enemyBishops);

    while(snipers != 0) {
        int sniper = lsb(snipers);
        uint64_t blockers = between[kingSquare][sniper] & occupied;

        //exactly one piece in the way and it's ours: it's pinned and may only move along the line
        if(popcount(blockers) == 1 && (blockers & friendlyPieces) != 0) {
            pinned |= blockers;
            pinRay[lsb(blockers)] = between[kingSquare][sniper] | squareBit(sniper);
        }

        snipers &= snipers - 1;
    }
}

//every generator below writes into `moves` starting at `count` and returns the new count. Apart from the
//king, they only produce moves that land in checkMask and stay on the piece's pinRay if it's pinned

int MoveGenerator::generateSlidingMoves(Position& position, uint64_t pieces, bool orthogonal, bool diagonal, uint16_t* moves, int count) {
    uint64_t friendlyPieces = position.isWhiteToMove() ? position.getWhitePieces() : position.getBlackPieces();
    uint64_t occupied = position.getOccupiedSquares();

    while(pieces != 0) {
        int startSquare = lsb(pieces);

        uint64_t attackableSquares = getSlidingAttacks(startSquare, occupied, orthogonal, diagonal) & ~friendlyPieces & checkMask;
        if((pinned & squareBit(startSquare)) != 0) {
            attackableSquares &= pinRay[startSquare];
        }

        while(attackableSquares != 0) {
            moves[count++] = Move::create(startSquare, lsb(attackableSquares));
            attackableSquares &= attackableSquares - 1;
        }

        pieces &= pieces - 1;
    }

    return count;
}

int MoveGenerator::generateKnightMoves(Position& position, uint16_t* moves, int count) {
    //a pinned knight can never stay on its pin line, so it has no moves at all
    uint64_t knights = (position.isWhiteToMove() ? position.whiteKnights : position.blackKnights) & ~pinned;
    uint64_t friendlyPieces = position.isWhiteToMove() ? position.getWhitePieces() : position.getBlackPieces();

    while(knights != 0) {
        int knightIndex = lsb(knights);

        uint64_t attackableSquares = knightAttackTable[knightIndex] & ~friendlyPieces & checkMask;

        while(attackableSquares != 0) {
            moves[count++] = Move::create(knightIndex, lsb(attackableSquares));
            attackableSquares &= attackableSquares - 1;
        }

        knights &= knights - 1;
    }

    return count;
}

int MoveGenerator::generateKingMoves(Position& position, uint16_t* moves, int count) {
    bool white = position.isWhiteToMove();
    uint64_t king = white ? position.whiteKing : position.blackKing;
    uint64_t friendlyPieces = white ? position.getWhitePieces() : position.getBlackPieces();

    if(king == 0) {
        return count;
    }

    int kingIndex = lsb(king);
    uint64_t attackableSquares = kingAttackTable[kingIndex] & ~friendlyPieces;

    //the king is lifted off the board for the attack test; otherwise stepping straight away from a
    //slider looks safe because the king's own square still blocks the slider's line
    uint64_t occupiedWithoutKing = position.getOccupiedSquares() & ~king;

    while(attackableSquares != 0) {
        int targetSquare = lsb(attackableSquares);
        if(!isSquareAttacked(position, targetSquare, !white, occupiedWithoutKing)) {
            moves[count++] = Move::create(kingIndex, targetSquare);
        }
        attackableSquares &= attackableSquares - 1;
    }

    return generateCastlingMoves(position, moves, count);
}

//castling rights indices: 0 = black queenside, 1 = black kingside, 2 = white queenside, 3 = white kingside
int MoveGenerator::generateCastlingMoves(Position& position, uint16_t* moves, int count) {
    bool white = position.isWhiteToMove();
    uint64_t occupied = position.getOccupiedSquares();
    bool attackedByWhite = !white;

    if(white) {
        bool squaresEmptyKingside = (occupied & (squareBit(61) | squareBit(62))) == 0;
        if(position.canCastle(3) && squaresEmptyKingside
                && !isSquareAttacked(position, 60, attackedByWhite)
                && !isSquareAttacked(position, 61, attackedByWhite)
                && !isSquareAttacked(position, 62, attackedByWhite)) {
            moves[count++] = whiteKingsideCastle;
        }

        bool squaresEmptyQueenside = (occupied & (squareBit(57) | squareBit(58) | squareBit(59))) == 0;
        if(position.canCastle(2) && squaresEmptyQueenside
                && !isSquareAttacked(position, 60, attackedByWhite)
                && !isSquareAttacked(position, 59, attackedByWhite)
                && !isSquareAttacked(position, 58, attackedByWhite)) {
            moves[count++] = whiteQueensideCastle;
        }
    } else {
        bool squaresEmptyKingside = (occupied & (squareBit(5) | squareBit(6))) == 0;
        if(position.canCastle(1) && squaresEmptyKingside
                && !isSquareAttacked(position, 4, attackedByWhite)
                && !isSquareAttacked(position, 5, attackedByWhite)
                && !isSquareAttacked(position, 6, attackedByWhite)) {
            moves[count++] = blackKingsideCastle;
        }

        bool squaresEmptyQueenside = (occupied & (squareBit(1) | squareBit(2) | squareBit(3))) == 0;
        if(position.canCastle(0) && squaresEmptyQueenside
                && !isSquareAttacked(position, 4, attackedByWhite)
                && !isSquareAttacked(position, 3, attackedByWhite)
                && !isSquareAttacked(position, 2, attackedByWhite)) {
            moves[count++] = blackQueensideCastle;
        }
    }

    return count;
}

int MoveGenerator::generatePawnMoves(Position& position, uint16_t* moves, int count) {
    bool white = position.isWhiteToMove();
    uint64_t emptySquares = ~position.getOccupiedSquares();

    uint64_t pawns;
    uint64_t enemyPieces;
    uint64_t singlePush, doublePush, leftAttack, rightAttack;
    uint64_t promotionRank;
    //startSquare = targetSquare + offset, so the offsets point back toward the side that's moving
    int pushOffset, leftOffset, rightOffset;

    if(white) {
        pawns = position.whitePawns;
        enemyPieces = position.getBlackPieces();

        singlePush = (pawns >> 8) & emptySquares;
        //the hexadecimal ensure that the pawns are moving to the correct rank
        doublePush = (singlePush >> 8) & emptySquares & 0x000000FF00000000ULL;

        //the hexadecimal ensure the H file isn't being attacked because that would be immpossible to be attacking left and attacking the H file
        leftAttack = ((pawns & ~0x0101010101010101ULL) >> 9) & enemyPieces;

        //same concept as left attack but with the A file
        rightAttack = ((pawns & ~0x8080808080808080ULL) >> 7) & enemyPieces;

        promotionRank = 0x00000000000000FFULL;
        pushOffset = 8;
        leftOffset = 9;
        rightOffset = 7;
    } else {
        pawns = position.blackPawns;
        enemyPieces = position.getWhitePieces();

        singlePush = (pawns << 8) & emptySquares;
        //the hexadecimal ensure that the pawns are moving to the correct rank
        doublePush = (singlePush << 8) & emptySquares & 0x00000000FF000000ULL;

        //<< 9 moves one column right (+1), so pawns already in column 7 are excluded or they'd wrap to column 0
        leftAttack = ((pawns & ~0x8080808080808080ULL) << 9) & enemyPieces;

        //<< 7 moves one column left (-1), so pawns in column 0 are excluded or they'd wrap to column 7
        rightAttack = ((pawns & ~0x0101010101010101ULL) << 7) & enemyPieces;

        promotionRank = 0xFF00000000000000ULL;
        pushOffset = -8;
        leftOffset = -9;
        rightOffset = -7;
    }

    //checkMask is applied only now: doublePush is built from the unmasked singlePush, because a double
    //push can block a check even when the single push square wouldn't
    count = addPawnMoves(singlePush & checkMask, pushOffset, promotionRank, moves, count);
    count = addPawnMoves(doublePush & checkMask, 2 * pushOffset, promotionRank, moves, count);
    count = addPawnMoves(leftAttack & checkMask, leftOffset, promotionRank, moves, count);
    count = addPawnMoves(rightAttack & checkMask, rightOffset, promotionRank, moves, count);

    return count;
}

//a pawn reaching the last rank produces one move per piece it can promote to
int MoveGenerator::addPawnMoves(uint64_t targets, int offset, uint64_t promotionRank, uint16_t* moves, int count) {
    while(targets != 0) {
        int targetSquare = lsb(targets);
        int startSquare = targetSquare + offset;
        targets &= targets - 1;

        //a pinned pawn can still push along a file pin or capture its pinner along a diagonal pin
        if((pinned & squareBit(startSquare)) != 0 && (pinRay[startSquare] & squareBit(targetSquare)) == 0) {
            continue;
        }

        if((squareBit(targetSquare) & promotionRank) != 0) {
            for(int pieceType : promotionPieces) {
                moves[count++] = Move::promotion(startSquare, targetSquare, pieceType);
            }
        } else {
            moves[count++] = Move::create(startSquare, targetSquare);
        }
    }

    return count;
}

//our pawns that can capture onto the en passant square are exactly the squares an enemy-colored
//pawn standing there would attack (same symmetry trick as isSquareAttacked)
int MoveGenerator::generateEnPassantMoves(Position& position, uint16_t* moves, int count) {
    int epSquare = position.getEnPassantSquare();

    if(epSquare == -1) {
        return count;
    }

    bool white = position.isWhiteToMove();
    uint64_t pawns = white ? position.whitePawns : position.blackPawns;
    uint64_t attackers = (white ? blackPawnAttackTable[epSquare] : whitePawnAttackTable[epSquare]) & pawns;

    //the captured pawn sits one rank behind the en passant square, on the mover's side
    int capturedSquare = white ? epSquare + 8 : epSquare - 8;

    //en passant is the only move that removes a piece from a square it doesn't land on. It gets out of
    //check if it lands between a slider and the king, or if the captured pawn is the checker itself
    //(the double-pushed pawn giving check), which checkMask alone would miss
    if(((squareBit(epSquare) | squareBit(capturedSquare)) & checkMask) == 0) {
        return count;
    }

    uint64_t occupied = position.getOccupiedSquares();
    uint64_t enemyQueens = white ? position.blackQueens : position.whiteQueens;
    uint64_t enemyRooks = (white ? position.blackRooks : position.whiteRooks) | enemyQueens;
    uint64_t enemyBishops = (white ? position.blackBishops : position.whiteBishops) | enemyQueens;

    while(attackers != 0) {
        int startSquare = lsb(attackers);
        attackers &= attackers - 1;

        //rather than modelling pins for en passant, rebuild the occupancy after the capture and look for a
        //slider hitting the king. This covers a pinned capturing pawn, both pawns leaving a rank the king
        //shares with an enemy rook, and a diagonal opened up by removing the captured pawn
        if(kingSquare != -1) {
            uint64_t occupiedAfter = (occupied & ~squareBit(startSquare) & ~squareBit(capturedSquare)) | squareBit(epSquare);
            if((Magic::rookAttacks(kingSquare, occupiedAfter) & enemyRooks) != 0
                    || (Magic::bishopAttacks(kingSquare, occupiedAfter) & enemyBishops) != 0) {
                continue;
            }
        }

        moves[count++] = Move::enPessant(startSquare, epSquare);
    }

    return count;
}

int MoveGenerator::getNumLegalMoves(Position& currentPosition) {
    return generateMoves(currentPosition, scratchMoves);
}

int MoveGenerator::generateMoves(Position& currentPosition, uint16_t* moves) {
    bool whiteToMove = currentPosition.isWhiteToMove();

    computeCheckAndPins(currentPosition);

    int count = generateKingMoves(currentPosition, moves, 0);

    //in double check only the king can move
    if(checkMask == 0) {
        return count;
    }

    count = generatePawnMoves(currentPosition, moves, count);
    count = generateEnPassantMoves(currentPosition, moves, count);
    count = generateKnightMoves(currentPosition, moves, count);
    count = generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteRooks : currentPosition.blackRooks, true, false, moves, count);
    count = generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteBishops : currentPosition.blackBishops, false, true, moves, count);
    count = generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteQueens : currentPosition.blackQueens, true, true, moves, count);

    return count;
}

uint16_t MoveGenerator::getLegalMove(int startingIndex, int targetIndex, Position& currentPosition) {
    int count = generateMoves(currentPosition, scratchMoves);
    for(int i = 0; i < count; i++) {
        uint16_t move = scratchMoves[i];
        if(Move::startSquare(move) == startingIndex && Move::targetSquare(move) == targetIndex) {
            return move;
        }
    }
    return Move::none;
}

uint16_t MoveGenerator::getLegalMove(int startingIndex, int targetIndex, int promotionPiece, Position& currentPosition) {
    int count = generateMoves(currentPosition, scratchMoves);
    for(int i = 0; i < count; i++) {
        uint16_t move = scratchMoves[i];
        if(Move::startSquare(move) == startingIndex && Move::targetSquare(move) == targetIndex
                && (!Move::isPromotion(move) || Move::promotionPiece(move) == promotionPiece)) {
            return move;
        }
    }
    return Move::none;
}
