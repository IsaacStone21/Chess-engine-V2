#pragma once

#include <stdint.h>

#include "move.h"
#include "types.h"

//a board position plus the history needed to undo moves and spot repetitions. It never allocates: the history is a
//fixed array, so copying a Position copies all of it (about 14 KB).
//Positions hash with the Zobrist keys, so create them only after initEngine()
class Position {
public:
    uint64_t whitePawns;
    uint64_t whiteKnights;
    uint64_t whiteBishops;
    uint64_t whiteRooks;
    uint64_t whiteQueens;
    uint64_t whiteKing;

    uint64_t blackPawns;
    uint64_t blackKnights;
    uint64_t blackBishops;
    uint64_t blackRooks;
    uint64_t blackQueens;
    uint64_t blackKing;

    uint64_t whitePieces;
    uint64_t blackPieces;
    uint64_t occupied;

    //the standard start position
    Position();

    //resets this position in place to the start, without a 14 KB temporary on the stack
    void setStartPosition();

    //fills `out` from a FEN string and returns false if it can't be parsed. The move history starts empty, so
    //repetitions from before the FEN position can't be seen
    static bool fromFEN(const char* fen, Position& out);

    //fills the Zobrist keys and castling masks; part of initEngine()
    static void initTables();

    uint64_t computeHash() const;
    uint64_t getHash() const { return hash; }
    int getHalfmoveClock() const { return halfmoveClock; }

    bool isRepetition() const;
    bool isThreefoldRepetition() const;
    bool isInsufficientMaterial() const;

    bool isWhiteToMove() const { return whiteToMove; }
    void switchTurns() { whiteToMove = !whiteToMove; }

    uint64_t getWhitePieces() {
        whitePieces = whitePawns | whiteKnights | whiteBishops | whiteRooks | whiteQueens | whiteKing;
        return whitePieces;
    }

    uint64_t getBlackPieces() {
        blackPieces = blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKing;
        return blackPieces;
    }

    uint64_t getOccupiedSquares() {
        occupied = getWhitePieces() | getBlackPieces();
        return occupied;
    }

    //color | type of the piece on this square (see Piece), or Piece::empty
    int getPieceAtIndex(int index) const;

    //returns the type of the given color's piece on this square, or Piece::empty
    int pieceTypeAt(int index, bool white) const {
        uint64_t mask = squareBit(index);

        if(white) {
            if((whitePawns & mask) != 0) return Piece::pawn;
            if((whiteKnights & mask) != 0) return Piece::knight;
            if((whiteBishops & mask) != 0) return Piece::bishop;
            if((whiteRooks & mask) != 0) return Piece::rook;
            if((whiteQueens & mask) != 0) return Piece::queen;
            if((whiteKing & mask) != 0) return Piece::king;
        } else {
            if((blackPawns & mask) != 0) return Piece::pawn;
            if((blackKnights & mask) != 0) return Piece::knight;
            if((blackBishops & mask) != 0) return Piece::bishop;
            if((blackRooks & mask) != 0) return Piece::rook;
            if((blackQueens & mask) != 0) return Piece::queen;
            if((blackKing & mask) != 0) return Piece::king;
        }

        return Piece::empty;
    }

    void playMove(uint16_t move);
    void undoMove();
    void playNullMove();
    void undoNullMove();

    int getEnPassantSquare() const { return enPassantSquare; }

    //number of moves played that can still be undone
    int getPly() const { return ply; }

    //Move::none right after a null move
    uint16_t getLastMove() const { return ply == 0 ? Move::none : moveStack[ply - 1]; }

    bool canCastle(int index) const { return ((castlingRights >> index) & 1) != 0; }

private:
    bool whiteToMove;

    //castling rights as bits: 0 = black queenside, 1 = black kingside, 2 = white queenside, 3 = white kingside
    int castlingRights;

    //square a pawn can capture onto en passant this turn, or -1 if none
    int enPassantSquare;

    //plies since the last capture or pawn move, for the fifty-move rule; only positions inside this window
    //can repeat, since captures and pawn moves can never be undone
    int halfmoveClock;

    //undo stack: moveStack[i] is the i-th move played and undoStack[i] packs what that move destroyed:
    //  bits 0-2   captured piece type (Piece::empty if nothing was captured)
    //  bits 3-6   castling rights before the move
    //  bits 7-13  en passant square before the move, plus one (so -1 stores as 0)
    //  bits 14-31 halfmove clock before the move
    //hashHistory[i] is the hash of the position the i-th move was played from, which is what repetitions are found in
    uint16_t moveStack[historyCapacity];
    uint32_t undoStack[historyCapacity];
    uint64_t hashHistory[historyCapacity];
    int ply;

    uint64_t hash;

    void clear();
    void pushHistory(uint16_t move, int capturedType);
    void toggle(bool white, int type, uint64_t mask);
};
