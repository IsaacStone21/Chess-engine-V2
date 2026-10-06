#include "position.h"

#include <string.h>

//castlingKeepMask[square] = the rights that survive a move touching that square; moving a king or rook
//off its start square (or capturing a rook on it) loses the matching rights
static int castlingKeepMask[64];

//Zobrist hashing: every piece on every square, every combination of castling rights, every en passant file and
//the side to move gets a random 64-bit key, and a position's hash is the XOR of the keys of everything in it.
//XOR undoes itself, so a move updates the hash by XORing out what changed and XORing in what replaced it,
//and two positions with the same hash are, for practical purposes, the same position

//pieceKeys[color offset + piece type][square], with white's offset 0 and black's 7 (piece types are 1-6)
static uint64_t pieceKeys[14][64];
static uint64_t castlingKeys[16];
static uint64_t enPassantKeys[8];
static uint64_t blackToMoveKey;

//the Java engine draws its keys from java.util.SplittableRandom(0x2C1D_E7A3_9B44_F00DL), whose nextLong() is
//SplitMix64; reproducing it exactly makes every hash, and so every table hit, the same as in Java
static uint64_t zobristState;

static uint64_t nextZobristKey() {
    uint64_t z = (zobristState += 0x9E3779B97F4A7C15ULL);
    z = (z ^ (z >> 30)) * 0xBF58476D1CE4E5B9ULL;
    z = (z ^ (z >> 27)) * 0x94D049BB133111EBULL;
    return z ^ (z >> 31);
}

void Position::initTables() {
    for(int square = 0; square < 64; square++) {
        castlingKeepMask[square] = 0b1111;
    }
    castlingKeepMask[0] = ~0b0001;
    castlingKeepMask[7] = ~0b0010;
    castlingKeepMask[4] = ~0b0011;
    castlingKeepMask[56] = ~0b0100;
    castlingKeepMask[63] = ~0b1000;
    castlingKeepMask[60] = ~0b1100;

    //same order as Java, including the unused piece type 0 rows, so every key lines up
    zobristState = 0x2C1DE7A39B44F00DULL;
    for(int keys = 0; keys < 14; keys++) {
        for(int square = 0; square < 64; square++) {
            pieceKeys[keys][square] = nextZobristKey();
        }
    }
    for(int rights = 0; rights < 16; rights++) {
        castlingKeys[rights] = nextZobristKey();
    }
    for(int file = 0; file < 8; file++) {
        enPassantKeys[file] = nextZobristKey();
    }
    blackToMoveKey = nextZobristKey();
}

Position::Position() {
    setStartPosition();
}

void Position::clear() {
    whitePawns = whiteKnights = whiteBishops = whiteRooks = whiteQueens = whiteKing = 0;
    blackPawns = blackKnights = blackBishops = blackRooks = blackQueens = blackKing = 0;
    whitePieces = blackPieces = occupied = 0;

    whiteToMove = true;
    castlingRights = 0b1111;
    enPassantSquare = -1;
    halfmoveClock = 0;
    ply = 0;
    hash = 0;

    memset(moveStack, 0, sizeof(moveStack));
    memset(undoStack, 0, sizeof(undoStack));
    memset(hashHistory, 0, sizeof(hashHistory));
}

void Position::setStartPosition() {
    clear();

    whitePawns = 0x00FF000000000000ULL;
    whiteKnights = 0x4200000000000000ULL;
    whiteBishops = 0x2400000000000000ULL;
    whiteRooks = 0x8100000000000000ULL;
    whiteQueens = 0x0800000000000000ULL;
    whiteKing = 0x1000000000000000ULL;

    blackPawns = 0x000000000000FF00ULL;
    blackKnights = 0x0000000000000042ULL;
    blackBishops = 0x0000000000000024ULL;
    blackRooks = 0x0000000000000081ULL;
    blackQueens = 0x0000000000000008ULL;
    blackKing = 0x0000000000000010ULL;

    getOccupiedSquares();
    hash = computeHash();
}

bool Position::fromFEN(const char* fen, Position& out) {
    out.clear();
    const char* c = fen;
    while(*c == ' ') c++;

    //piece placement, rank 8 first, which is exactly square order 0..63
    int square = 0;
    for(; *c != '\0' && *c != ' '; c++) {
        if(*c == '/') {
            if(square % 8 != 0) return false;
            continue;
        }
        if(*c >= '1' && *c <= '8') {
            square += *c - '0';
            continue;
        }
        if(square >= 64) return false;

        uint64_t mask = squareBit(square++);
        switch(*c) {
            case 'P': out.whitePawns |= mask; break;
            case 'N': out.whiteKnights |= mask; break;
            case 'B': out.whiteBishops |= mask; break;
            case 'R': out.whiteRooks |= mask; break;
            case 'Q': out.whiteQueens |= mask; break;
            case 'K': out.whiteKing |= mask; break;
            case 'p': out.blackPawns |= mask; break;
            case 'n': out.blackKnights |= mask; break;
            case 'b': out.blackBishops |= mask; break;
            case 'r': out.blackRooks |= mask; break;
            case 'q': out.blackQueens |= mask; break;
            case 'k': out.blackKing |= mask; break;
            default: return false;
        }
    }
    if(square != 64) return false;

    while(*c == ' ') c++;
    if(*c == 'w') {
        out.whiteToMove = true;
    } else if(*c == 'b') {
        out.whiteToMove = false;
    } else {
        return false;
    }
    c++;

    while(*c == ' ') c++;
    out.castlingRights = 0;
    for(; *c != '\0' && *c != ' '; c++) {
        switch(*c) {
            case 'K': out.castlingRights |= 0b1000; break;
            case 'Q': out.castlingRights |= 0b0100; break;
            case 'k': out.castlingRights |= 0b0010; break;
            case 'q': out.castlingRights |= 0b0001; break;
            case '-': break;
            default: return false;
        }
    }

    while(*c == ' ') c++;
    if(*c == '-') {
        c++;
    } else if(*c >= 'a' && *c <= 'h' && c[1] >= '1' && c[1] <= '8') {
        out.enPassantSquare = (c[0] - 'a') + 8 * ('8' - c[1]);
        c += 2;
    } else if(*c != '\0') {
        return false;
    }

    //the halfmove clock is optional; the fullmove number isn't needed
    while(*c == ' ') c++;
    int halfmoves = 0;
    while(*c >= '0' && *c <= '9') {
        halfmoves = halfmoves * 10 + (*c++ - '0');
        if(halfmoves > 100000) return false;
    }
    out.halfmoveClock = halfmoves;

    out.getOccupiedSquares();
    out.hash = out.computeHash();
    return true;
}

//XOR of the keys for one piece type standing on every square in `pieces`
static uint64_t pieceHash(uint64_t pieces, bool white, int type) {
    const uint64_t* keys = pieceKeys[(white ? 0 : 7) + type];
    uint64_t key = 0;
    while(pieces != 0) {
        key ^= keys[lsb(pieces)];
        pieces &= pieces - 1;
    }
    return key;
}

//builds the hash from nothing; playMove keeps it up to date incrementally, so this only runs when a
//position is created (and in tests, to check the incremental updates)
uint64_t Position::computeHash() const {
    uint64_t key = pieceHash(whitePawns, true, Piece::pawn) ^ pieceHash(whiteKnights, true, Piece::knight)
            ^ pieceHash(whiteBishops, true, Piece::bishop) ^ pieceHash(whiteRooks, true, Piece::rook)
            ^ pieceHash(whiteQueens, true, Piece::queen) ^ pieceHash(whiteKing, true, Piece::king)
            ^ pieceHash(blackPawns, false, Piece::pawn) ^ pieceHash(blackKnights, false, Piece::knight)
            ^ pieceHash(blackBishops, false, Piece::bishop) ^ pieceHash(blackRooks, false, Piece::rook)
            ^ pieceHash(blackQueens, false, Piece::queen) ^ pieceHash(blackKing, false, Piece::king);

    key ^= castlingKeys[castlingRights];
    if(enPassantSquare != -1) {
        key ^= enPassantKeys[enPassantSquare & 7];
    }
    if(!whiteToMove) {
        key ^= blackToMoveKey;
    }
    return key;
}

//true if this exact position, with the same side to move, already came up earlier in the game or the
//search line leading here. Only positions since the last capture or pawn move can match, and only every
//other one has the same side to move
bool Position::isRepetition() const {
    int earliest = ply - halfmoveClock > 0 ? ply - halfmoveClock : 0;
    for(int index = ply - 2; index >= earliest; index -= 2) {
        if(hashHistory[index] == hash) {
            return true;
        }
    }
    return false;
}

//true once this position has come up for the third time, which ends the game in a draw. The search treats
//the first repeat as a draw already (isRepetition), since a side that can repeat once can usually repeat again
bool Position::isThreefoldRepetition() const {
    int occurrences = 1;
    int earliest = ply - halfmoveClock > 0 ? ply - halfmoveClock : 0;
    for(int index = ply - 2; index >= earliest; index -= 2) {
        if(hashHistory[index] == hash && ++occurrences == 3) {
            return true;
        }
    }
    return false;
}

//every light square; a8 (index 0) is light, and the colors alternate along each rank and between ranks
static const uint64_t lightSquares = 0xAA55AA55AA55AA55ULL;

//true when neither side could checkmate however badly the other played: bare kings, a single bishop or
//knight, or nothing but bishops that all stand on the same color of square
bool Position::isInsufficientMaterial() const {
    if((whitePawns | blackPawns | whiteRooks | blackRooks | whiteQueens | blackQueens) != 0) {
        return false;
    }

    uint64_t minors = whiteKnights | blackKnights | whiteBishops | blackBishops;
    if(popcount(minors) <= 1) {
        return true;
    }

    //same-colored bishops can never cover the squares of the other color that a mating net needs
    if((whiteKnights | blackKnights) == 0) {
        uint64_t bishops = whiteBishops | blackBishops;
        return (bishops & lightSquares) == 0 || (bishops & ~lightSquares) == 0;
    }
    return false;
}

int Position::getPieceAtIndex(int index) const {
    int type = pieceTypeAt(index, true);
    if(type != Piece::empty) {
        return Piece::white | type;
    }
    type = pieceTypeAt(index, false);
    if(type != Piece::empty) {
        return Piece::black | type;
    }
    return Piece::empty;
}

//flips the given bits on one piece bitboard; used for both adding and removing, so make and unmake share it.
//It also XORs the matching keys into the hash, which keeps the hash in step with every piece change
void Position::toggle(bool white, int type, uint64_t mask) {
    const uint64_t* keys = pieceKeys[(white ? 0 : 7) + type];
    for(uint64_t bits = mask; bits != 0; bits &= bits - 1) {
        hash ^= keys[lsb(bits)];
    }

    if(white) {
        switch(type) {
            case Piece::pawn: whitePawns ^= mask; break;
            case Piece::knight: whiteKnights ^= mask; break;
            case Piece::bishop: whiteBishops ^= mask; break;
            case Piece::rook: whiteRooks ^= mask; break;
            case Piece::queen: whiteQueens ^= mask; break;
            case Piece::king: whiteKing ^= mask; break;
        }
    } else {
        switch(type) {
            case Piece::pawn: blackPawns ^= mask; break;
            case Piece::knight: blackKnights ^= mask; break;
            case Piece::bishop: blackBishops ^= mask; break;
            case Piece::rook: blackRooks ^= mask; break;
            case Piece::queen: blackQueens ^= mask; break;
            case Piece::king: blackKing ^= mask; break;
        }
    }
}

//the en passant victim sits one rank behind the target square, on the mover's side
static int capturedSquare(uint16_t move, bool white) {
    int target = Move::targetSquare(move);
    if(!Move::isEnPessant(move)) {
        return target;
    }
    return white ? target + 8 : target - 8;
}

//bitboard with the rook's start and target squares for a castling move by the king to kingTarget
static uint64_t castlingRookMask(int kingStart, int kingTarget) {
    bool kingside = kingTarget > kingStart;
    int rookStart = kingside ? kingStart + 3 : kingStart - 4;
    int rookTarget = (kingStart + kingTarget) / 2;
    return squareBit(rookStart) | squareBit(rookTarget);
}

//records what a move is about to destroy. Java grows its arrays instead; here a game that fills the fixed history
//loses its oldest half, which can then no longer be undone. Everything after stays undoable and the search keeps
//working, and repetitions are unaffected unless the halfmove clock reaches back past the dropped moves
void Position::pushHistory(uint16_t move, int capturedType) {
    if(ply == historyCapacity) {
        constexpr int keep = historyCapacity / 2;
        memmove(moveStack, moveStack + keep, keep * sizeof(moveStack[0]));
        memmove(undoStack, undoStack + keep, keep * sizeof(undoStack[0]));
        memmove(hashHistory, hashHistory + keep, keep * sizeof(hashHistory[0]));
        ply = keep;
    }

    moveStack[ply] = move;
    undoStack[ply] = (uint32_t) capturedType | ((uint32_t) castlingRights << 3)
            | ((uint32_t) (enPassantSquare + 1) << 7) | ((uint32_t) halfmoveClock << 14);
    hashHistory[ply] = hash;
    ply++;
}

void Position::playMove(uint16_t move) {
    int startIndex = Move::startSquare(move);
    int targetIndex = Move::targetSquare(move);
    bool white = whiteToMove;

    int movingType = pieceTypeAt(startIndex, white);
    int captureIndex = capturedSquare(move, white);
    int capturedType = pieceTypeAt(captureIndex, !white);

    pushHistory(move, capturedType);

    //castling rights and en passant are about to change; their old keys come out here and the new ones go in below
    hash ^= castlingKeys[castlingRights];
    if(enPassantSquare != -1) {
        hash ^= enPassantKeys[enPassantSquare & 7];
    }

    if(capturedType != Piece::empty) {
        toggle(!white, capturedType, squareBit(captureIndex));
    }

    toggle(white, movingType, squareBit(startIndex));
    toggle(white, Move::isPromotion(move) ? Move::promotionPiece(move) : movingType, squareBit(targetIndex));

    if(Move::isCastling(move)) {
        toggle(white, Piece::rook, castlingRookMask(startIndex, targetIndex));
    }

    castlingRights &= castlingKeepMask[startIndex] & castlingKeepMask[targetIndex];

    //a double push leaves the skipped-over square open to en passant for exactly one turn
    int distance = targetIndex - startIndex;
    enPassantSquare = (movingType == Piece::pawn && (distance == 16 || distance == -16))
            ? (startIndex + targetIndex) / 2
            : -1;

    halfmoveClock = (movingType == Piece::pawn || capturedType != Piece::empty) ? 0 : halfmoveClock + 1;

    hash ^= castlingKeys[castlingRights];
    if(enPassantSquare != -1) {
        hash ^= enPassantKeys[enPassantSquare & 7];
    }
    hash ^= blackToMoveKey;

    whiteToMove = !white;
}

void Position::undoMove() {
    if(ply == 0) {
        return;
    }

    ply--;
    uint16_t move = moveStack[ply];
    uint32_t undoInfo = undoStack[ply];

    whiteToMove = !whiteToMove;
    bool white = whiteToMove;

    int startIndex = Move::startSquare(move);
    int targetIndex = Move::targetSquare(move);

    int placedType = pieceTypeAt(targetIndex, white);
    toggle(white, placedType, squareBit(targetIndex));
    toggle(white, Move::isPromotion(move) ? Piece::pawn : placedType, squareBit(startIndex));

    if(Move::isCastling(move)) {
        toggle(white, Piece::rook, castlingRookMask(startIndex, targetIndex));
    }

    int capturedType = undoInfo & 0b111;
    if(capturedType != Piece::empty) {
        toggle(!white, capturedType, squareBit(capturedSquare(move, white)));
    }

    castlingRights = (undoInfo >> 3) & 0b1111;
    enPassantSquare = (int) ((undoInfo >> 7) & 0b1111111) - 1;
    halfmoveClock = (int) (undoInfo >> 14);

    //the toggles above already undid the piece keys, but restoring the saved hash also covers castling,
    //en passant and the side to move in one step
    hash = hashHistory[ply];
}

//passes the turn without moving, for the search's null-move pruning. It goes on the undo stack as Move::none
//and must be taken back with undoNullMove, not undoMove. The halfmove clock restarts at zero so isRepetition
//never looks back past the pass: a position from before it didn't arise in a real game line
void Position::playNullMove() {
    pushHistory(Move::none, Piece::empty);

    if(enPassantSquare != -1) {
        hash ^= enPassantKeys[enPassantSquare & 7];
        enPassantSquare = -1;
    }
    hash ^= blackToMoveKey;

    halfmoveClock = 0;
    whiteToMove = !whiteToMove;
}

void Position::undoNullMove() {
    ply--;
    uint32_t undoInfo = undoStack[ply];

    whiteToMove = !whiteToMove;
    enPassantSquare = (int) ((undoInfo >> 7) & 0b1111111) - 1;
    halfmoveClock = (int) (undoInfo >> 14);
    hash = hashHistory[ply];
}
