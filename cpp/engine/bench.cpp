#include "bench.h"

const BenchPosition benchSuite[] = {
    {"start", ""},
    {"ruy_lopez", "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6"},
    {"najdorf", "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6"},
    {"kings_gambit", "e2e4 e7e5 f2f4 e5f4 g1f3 g7g5"},
    {"en_passant", "e2e4 g8f6 e4e5 d7d5"},
    {"scholar_mate", "e2e4 e7e5 f1c4 b8c6 d1h5 g8f6"},
    {"kid_middlegame", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 b8d7 c1e3 e7e5 f1d3 f6g4 d1d2 e5d4 f3d4 g4e3"
            " f2e3 e8g8 e1c1 d7e5 c1b1 c8g4 d3e2 g4e2 d2e2 d8h4 h2h3 a8d8 b2b4 c7c5"},
    {"italian_middlegame", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d4 e5d4 e4e5 d7d5 e5f6 d5c4 f6g7 h8g8 c3d4"
            " c6d4 c1h6 d4f3 d1f3 d8h4 h6e3 c5e3 f3e3 h4e7"},
    {"qgd_endgame", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7 g5f6 e7f6 c4d5 e6d5 e2e3 b8c6 d1b3 c6e7 f1d3 e8g8"
            " g1f3 d8d6 e3e4 d5e4 c3e4 d6c6 e1g1 c8e6 e4f6 g7f6 d3b5 e6b3 b5c6 b3d5 c6d5 e7d5 a1e1 h7h5 e1e4 g8g7"
            " e4e1 a8d8 a2a4 f6f5 f3e5 g7f6 a4a5 d5f4 e5f3 f8e8 e1d1 d8d5 g2g3 f4e2 g1g2 e8d8 d1e1 e2d4 f3d4 d5d4"
            " e1c1 d8d7 c1b1 d4a4 b2b4 d7d4 f1c1 a4b4 b1b4 d4b4 c1c7 b4b5 g2f3 f6e6 h2h4 b5a5"},
    {"kid_rook_endgame", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 b8d7 c1e3 e7e5 f1d3 f6g4 d1d2 e5d4 f3d4"
            " g4e3 f2e3 e8g8 e1c1 d7e5 c1b1 c8g4 d3e2 g4e2 d2e2 d8h4 h2h3 a8d8 b2b4 c7c5 b4c5 d6c5 d4b5 a7a6 b5c7"
            " h4h5 d1d8 f8d8 e2f1 e5f3 f1f3 h5f3 g2f3 g7c3 c7d5 c3g7 b1c2 g6g5 f3f4 g5f4 e3f4 b7b5 c2d3 b5b4 f4f5"
            " h7h5 h1g1 f7f6 e4e5 g8h7 e5f6 g7f6 g1g6 f6d4 g6a6 h5h4 a6c6 h7g7 c6c7 g7g8 c7b7 d8a8 d5e7 g8f7 e7c6"
            " f7f6 c6d4 c5d4 d3e4 a8a2 b7b6 f6f7 b6b4 a2e2 e4d4 f7f6 b4b5 e2h2 d4e4 f6g5 e4e5 h2e2 e5d6 e2e3 c4c5"
            " g5f6 c5c6 e3d3 b5d5 d3a3 c6c7 a3a8 d6d7 f6g5 c7c8q a8c8 d7c8 g5f4"},
    {"italian_mop_up", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d4 e5d4 e4e5 d7d5 e5f6 d5c4 f6g7 h8g8 c3d4 c6d4"
            " c1h6 d4f3 d1f3 d8h4 h6e3 c5e3 f3e3 h4e7 b1a3 e7e3 f2e3 g8g7 a3b5 f7f5 e1g1 g7e7 a1c1 a7a6 b5d4 b7b5"
            " c1e1 e7e5 d4f3 e5d5 f3g5 d5d2 f1f2 d2f2 g1f2 h7h6 g5f3 e8f7 f2g3 c8b7 g3f4 b7f3 g2f3 f7e6 e3e4 f5e4"
            " f4e4 e6f6 e4f4 a8d8 e1e2 d8d4 f4e3 f6e5 b2b4 h6h5 e2e1 d4d3 e3f2 e5d4 e1e2 d4c3 a2a3 c3b3 e2e7 b3a3"
            " h2h4 d3d4 e7c7 d4h4 c7a7 c4c3 a7a6 a3b2 f2g3 h4b4 f3f4 c3c2 a6c6 b4c4 c6b6 b5b4 b6f6 c2c1q g3h4 c4f4"
            " f6f4 c1f4 h4h5 b4b3 h5g6 b2c3"},
    {"qgd_late_endgame", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7 g5f6 e7f6 c4d5 e6d5 e2e3 b8c6 d1b3 c6e7 f1d3"
            " e8g8 g1f3 d8d6 e3e4 d5e4 c3e4 d6c6 e1g1 c8e6 e4f6 g7f6 d3b5 e6b3 b5c6 b3d5 c6d5 e7d5 a1e1 h7h5 e1e4"
            " g8g7 e4e1 a8d8 a2a4 f6f5 f3e5 g7f6 a4a5 d5f4 e5f3 f8e8 e1d1 d8d5 g2g3 f4e2 g1g2 e8d8 d1e1 e2d4 f3d4"
            " d5d4 e1c1 d8d7 c1b1 d4a4 b2b4 d7d4 f1c1 a4b4 b1b4 d4b4 c1c7 b4b5 g2f3 f6e6 h2h4 b5a5 c7b7 a5a3 f3f4"
            " a3a2 f4e3 a2a4 f2f4 a4a3 e3d4 a3a4 d4c5 a4a6 b7c7 a6a2 c7c6 e6e7 c6h6 a7a5 c5d5 a2g2 h6h5 e7f6 h5h6"
            " f6e7 h6a6 g2g3 d5e5 g3g4 h4h5 f7f6 a6f6 g4f4 h5h6 f4h4 f6a6 f5f4 e5e4 a5a4 h6h7 f4f3 e4f3 e7f7 a6a4"
            " h4h3 f3e4 h3h4 e4d5 h4h5 d5c6 h5h6 c6d7 f7g7 a4d4 g7h7"},
};

const int benchSuiteSize = sizeof(benchSuite) / sizeof(benchSuite[0]);

//0 = a8, 63 = h1; -1 if the text isn't a square
static int parseSquare(const char* text) {
    if(text[0] < 'a' || text[0] > 'h' || text[1] < '1' || text[1] > '8') {
        return -1;
    }
    return (text[0] - 'a') + 8 * ('8' - text[1]);
}

bool playCoordinateMove(Position& position, const char* text, int length, MoveGenerator& moveGenerator) {
    if(length != 4 && length != 5) {
        return false;
    }

    int start = parseSquare(text);
    int target = parseSquare(text + 2);
    if(start < 0 || target < 0) {
        return false;
    }

    uint16_t move;
    if(length == 5) {
        int piece;
        switch(text[4]) {
            case 'q': piece = Piece::queen; break;
            case 'r': piece = Piece::rook; break;
            case 'b': piece = Piece::bishop; break;
            case 'n': piece = Piece::knight; break;
            default: return false;
        }
        move = moveGenerator.getLegalMove(start, target, piece, position);
    } else {
        move = moveGenerator.getLegalMove(start, target, position);
    }

    if(move == Move::none) {
        return false;
    }
    position.playMove(move);
    return true;
}

bool playCoordinateMoves(Position& position, const char* moves, MoveGenerator& moveGenerator) {
    position.setStartPosition();

    const char* c = moves;
    while(*c != '\0') {
        while(*c == ' ') c++;
        const char* start = c;
        while(*c != '\0' && *c != ' ') c++;
        if(c > start && !playCoordinateMove(position, start, (int) (c - start), moveGenerator)) {
            return false;
        }
    }
    return true;
}

void moveToCoordinates(uint16_t move, char* out) {
    if(move == Move::none) {
        out[0] = 'n'; out[1] = 'o'; out[2] = 'n'; out[3] = 'e'; out[4] = '\0';
        return;
    }

    int start = Move::startSquare(move);
    int target = Move::targetSquare(move);
    out[0] = (char) ('a' + (start & 7));
    out[1] = (char) ('8' - (start >> 3));
    out[2] = (char) ('a' + (target & 7));
    out[3] = (char) ('8' - (target >> 3));
    out[4] = '\0';

    if(Move::isPromotion(move)) {
        int piece = Move::promotionPiece(move);
        out[4] = piece == Piece::queen ? 'q' : piece == Piece::rook ? 'r' : piece == Piece::bishop ? 'b' : 'n';
        out[5] = '\0';
    }
}
