#pragma once

#include <stdint.h>

#include "types.h"

//moves are plain uint16_t values; this only holds the helpers that pack and read them
struct Move {
    //bit layout (bit 15 unused):
    //  0-5   start square
    //  6-11  target square
    //  12    promotion flag
    //  13    castling flag        | when the promotion flag is set, bits 13-14 instead hold
    //  14    en pessant flag      | the promotion piece: 00 knight, 01 bishop, 10 rook, 11 queen
    //a promotion can never be a castle or en pessant, so the two meanings of bits 13-14 never collide
    static constexpr int squareMask = 0b111111;
    static constexpr int targetShift = 6;
    static constexpr int promotionFlag = 1 << 12;
    static constexpr int castlingFlag = 1 << 13;
    static constexpr int enPessantFlag = 1 << 14;
    static constexpr int promotionPieceShift = 13;

    //a8 -> a8 can never be a real move, so 0 stands in for "no move"
    static constexpr uint16_t none = 0;

    static constexpr uint16_t create(int startIndex, int targetIndex) {
        return (uint16_t) (startIndex | (targetIndex << targetShift));
    }

    static constexpr uint16_t castling(int startIndex, int targetIndex) {
        return (uint16_t) (create(startIndex, targetIndex) | castlingFlag);
    }

    static constexpr uint16_t enPessant(int startIndex, int targetIndex) {
        return (uint16_t) (create(startIndex, targetIndex) | enPessantFlag);
    }

    static constexpr uint16_t promotion(int startIndex, int targetIndex, int pieceType) {
        return (uint16_t) (create(startIndex, targetIndex) | promotionFlag | (promotionCode(pieceType) << promotionPieceShift));
    }

    //only pawn promotion pieces are ever passed in; Java throws for anything else
    static constexpr int promotionCode(int pieceType) {
        switch(pieceType) {
            case Piece::knight: return 0;
            case Piece::bishop: return 1;
            case Piece::rook: return 2;
            default: return 3;
        }
    }

    static int startSquare(uint16_t move) {
        return move & squareMask;
    }

    static int targetSquare(uint16_t move) {
        return (move >> targetShift) & squareMask;
    }

    static bool isPromotion(uint16_t move) {
        return (move & promotionFlag) != 0;
    }

    //returns the piece type the pawn promotes to, or Piece::empty if this move isn't a promotion
    static int promotionPiece(uint16_t move) {
        static constexpr int promotionCodeToPiece[4] = {Piece::knight, Piece::bishop, Piece::rook, Piece::queen};
        if(!isPromotion(move)) {
            return Piece::empty;
        }
        return promotionCodeToPiece[(move >> promotionPieceShift) & 0b11];
    }

    static bool isCastling(uint16_t move) {
        return (move & (promotionFlag | castlingFlag)) == castlingFlag;
    }

    static bool isEnPessant(uint16_t move) {
        return (move & (promotionFlag | enPessantFlag)) == enPessantFlag;
    }
};

//the pieces a pawn can promote to, strongest first
constexpr int promotionPieces[4] = {Piece::queen, Piece::rook, Piece::bishop, Piece::knight};
