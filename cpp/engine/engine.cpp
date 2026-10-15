#include "engine.h"

#include <math.h>
#include <stdlib.h>
#include <string.h>

#include "magic.h"
#include "platform.h"

void initEngine() {
    Magic::init();
    Position::initTables();
    MoveGenerator::initTables();
    Engine::initLateMoveReductions();
}

namespace Engine {

//scores are in centipawns; a positive number means the position is favorable for white
static constexpr int pawnValue = 100;
static constexpr int knightValue = 300;
static constexpr int bishopValue = 300;
static constexpr int rookValue = 500;
static constexpr int queenValue = 900;

//INF bounds every real score; it's a plain constant because -INT_MIN overflows
static constexpr int INF = 1000000;

static MoveGenerator moveGenerator;

//piece-square tables: a bonus (or penalty) for a piece standing on each square, written from white's side
//with rank 8 on the first row so they line up with square indices (0 = a8); black reads them through
//square ^ 56, which flips the board vertically
static const int pawnMiddlegameTable[64] = {
      0,  0,  0,  0,  0,  0,  0,  0,
     50, 50, 50, 50, 50, 50, 50, 50,
     10, 10, 20, 30, 30, 20, 10, 10,
      5,  5, 10, 25, 25, 10,  5,  5,
      0,  0,  0, 20, 20,  0,  0,  0,
      5, -5,-10,  0,  0,-10, -5,  5,
      5, 10, 10,-20,-20, 10, 10,  5,
      0,  0,  0,  0,  0,  0,  0,  0
};

//in the endgame a pawn is worth more the closer it gets to promoting
static const int pawnEndgameTable[64] = {
      0,  0,  0,  0,  0,  0,  0,  0,
     80, 80, 80, 80, 80, 80, 80, 80,
     50, 50, 50, 50, 50, 50, 50, 50,
     30, 30, 30, 30, 30, 30, 30, 30,
     20, 20, 20, 20, 20, 20, 20, 20,
     10, 10, 10, 10, 10, 10, 10, 10,
     10, 10, 10, 10, 10, 10, 10, 10,
      0,  0,  0,  0,  0,  0,  0,  0
};

static const int knightTable[64] = {
    -50,-40,-30,-30,-30,-30,-40,-50,
    -40,-20,  0,  0,  0,  0,-20,-40,
    -30,  0, 10, 15, 15, 10,  0,-30,
    -30,  5, 15, 20, 20, 15,  5,-30,
    -30,  0, 15, 20, 20, 15,  0,-30,
    -30,  5, 10, 15, 15, 10,  5,-30,
    -40,-20,  0,  5,  5,  0,-20,-40,
    -50,-40,-30,-30,-30,-30,-40,-50
};

static const int bishopTable[64] = {
    -20,-10,-10,-10,-10,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  5, 10, 10,  5,  0,-10,
    -10,  5,  5, 10, 10,  5,  5,-10,
    -10,  0, 10, 10, 10, 10,  0,-10,
    -10, 10, 10, 10, 10, 10, 10,-10,
    -10,  5,  0,  0,  0,  0,  5,-10,
    -20,-10,-10,-10,-10,-10,-10,-20
};

static const int rookTable[64] = {
      0,  0,  0,  0,  0,  0,  0,  0,
      5, 10, 10, 10, 10, 10, 10,  5,
     -5,  0,  0,  0,  0,  0,  0, -5,
     -5,  0,  0,  0,  0,  0,  0, -5,
     -5,  0,  0,  0,  0,  0,  0, -5,
     -5,  0,  0,  0,  0,  0,  0, -5,
     -5,  0,  0,  0,  0,  0,  0, -5,
      0,  0,  0,  5,  5,  0,  0,  0
};

static const int queenTable[64] = {
    -20,-10,-10, -5, -5,-10,-10,-20,
    -10,  0,  0,  0,  0,  0,  0,-10,
    -10,  0,  5,  5,  5,  5,  0,-10,
     -5,  0,  5,  5,  5,  5,  0, -5,
      0,  0,  5,  5,  5,  5,  0, -5,
    -10,  5,  5,  5,  5,  5,  0,-10,
    -10,  0,  5,  0,  0,  0,  0,-10,
    -20,-10,-10, -5, -5,-10,-10,-20
};

//with queens and rooks around the king hides behind its pawns...
static const int kingMiddlegameTable[64] = {
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -30,-40,-40,-50,-50,-40,-40,-30,
    -20,-30,-30,-40,-40,-30,-30,-20,
    -10,-20,-20,-20,-20,-20,-20,-10,
     20, 20,  0,  0,  0,  0, 20, 20,
     20, 30, 10,  0,  0, 10, 30, 20
};

//...but once they're traded off it becomes a fighting piece that belongs in the center
static const int kingEndgameTable[64] = {
    -50,-40,-30,-20,-20,-30,-40,-50,
    -30,-20,-10,  0,  0,-10,-20,-30,
    -30,-10, 20, 30, 30, 20,-10,-30,
    -30,-10, 30, 40, 40, 30,-10,-30,
    -30,-10, 30, 40, 40, 30,-10,-30,
    -30,-10, 20, 30, 30, 20,-10,-30,
    -30,-30,  0,  0,  0,  0,-30,-30,
    -50,-30,-30,-30,-30,-30,-30,-50
};

//game phase counts the non-pawn material left: 24 with every piece on the board, 0 with only kings and pawns
static constexpr int knightPhase = 1;
static constexpr int bishopPhase = 1;
static constexpr int rookPhase = 2;
static constexpr int queenPhase = 4;
static constexpr int totalPhase = 24;

//mop-up kicks in once a side is this far ahead and the board has emptied out to this phase or less
static constexpr int mopUpMaterialLead = 2 * pawnValue;
static constexpr int mopUpMaxPhase = 10;

//promotions can push the raw count past 24, so it's capped
static int gamePhase(const Position& position) {
    int phase = popcount(position.whiteKnights | position.blackKnights) * knightPhase
            + popcount(position.whiteBishops | position.blackBishops) * bishopPhase
            + popcount(position.whiteRooks | position.blackRooks) * rookPhase
            + popcount(position.whiteQueens | position.blackQueens) * queenPhase;
    return phase < totalPhase ? phase : totalPhase;
}

static int squareBonus(uint64_t pieces, const int* table, bool white) {
    int bonus = 0;
    while(pieces != 0) {
        int square = lsb(pieces);
        bonus += table[white ? square : square ^ 56];
        pieces &= pieces - 1;
    }
    return bonus;
}

//manhattan distance to the nearest of the four center squares: 0 in the center, 6 in a corner
static int distanceFromCenter(int square) {
    int file = square & 7;
    int rank = square >> 3;
    int fileDistance = 3 - file > file - 4 ? 3 - file : file - 4;
    int rankDistance = 3 - rank > rank - 4 ? 3 - rank : rank - 4;
    return fileDistance + rankDistance;
}

static int kingDistance(int squareA, int squareB) {
    return abs((squareA & 7) - (squareB & 7)) + abs((squareA >> 3) - (squareB >> 3));
}

//Java's numberOfTrailingZeros, 64 for an empty board; mopUp is the one place a missing king (test positions only)
//can reach, and this keeps its result identical to Java's there
static int kingSquareOr64(uint64_t king) {
    return king != 0 ? lsb(king) : 64;
}

//in a won endgame the mate is usually deeper than the search can see, so without help the engine just
//shuffles; rewarding it for driving the losing king to the edge and walking its own king over to help
//gives every move toward the mate a slightly better score for the search to follow
static int mopUp(const Position& position, int material, int phase) {
    if(abs(material) < mopUpMaterialLead || phase > mopUpMaxPhase) {
        return 0;
    }

    bool whiteWinning = material > 0;
    int winningKing = kingSquareOr64(whiteWinning ? position.whiteKing : position.blackKing);
    int losingKing = kingSquareOr64(whiteWinning ? position.blackKing : position.whiteKing);

    int bonus = distanceFromCenter(losingKing) * 10 + (14 - kingDistance(winningKing, losingKing)) * 4;
    bonus = bonus * (totalPhase - phase) / totalPhase;

    return whiteWinning ? bonus : -bonus;
}

//positive scores favor white; kings are left out of material since both are always on the board
int evaluate(const Position& position) {
    int white = popcount(position.whitePawns) * pawnValue
            + popcount(position.whiteKnights) * knightValue
            + popcount(position.whiteBishops) * bishopValue
            + popcount(position.whiteRooks) * rookValue
            + popcount(position.whiteQueens) * queenValue;

    int black = popcount(position.blackPawns) * pawnValue
            + popcount(position.blackKnights) * knightValue
            + popcount(position.blackBishops) * bishopValue
            + popcount(position.blackRooks) * rookValue
            + popcount(position.blackQueens) * queenValue;

    int material = white - black;
    int phase = gamePhase(position);

    int score = material;

    score += squareBonus(position.whiteKnights, knightTable, true) - squareBonus(position.blackKnights, knightTable, false);
    score += squareBonus(position.whiteBishops, bishopTable, true) - squareBonus(position.blackBishops, bishopTable, false);
    score += squareBonus(position.whiteRooks, rookTable, true) - squareBonus(position.blackRooks, rookTable, false);
    score += squareBonus(position.whiteQueens, queenTable, true) - squareBonus(position.blackQueens, queenTable, false);

    //pawns and kings want different squares as the game goes on, so blend their two tables by phase
    int middlegame = squareBonus(position.whitePawns, pawnMiddlegameTable, true) - squareBonus(position.blackPawns, pawnMiddlegameTable, false)
            + squareBonus(position.whiteKing, kingMiddlegameTable, true) - squareBonus(position.blackKing, kingMiddlegameTable, false);
    int endgame = squareBonus(position.whitePawns, pawnEndgameTable, true) - squareBonus(position.blackPawns, pawnEndgameTable, false)
            + squareBonus(position.whiteKing, kingEndgameTable, true) - squareBonus(position.blackKing, kingEndgameTable, false);
    score += (middlegame * phase + endgame * (totalPhase - phase)) / totalPhase;

    score += mopUp(position, material, phase);

    return score;
}

//negamax needs every score from the side to move's point of view
static int evaluateRelative(const Position& position) {
    int eval = evaluate(position);
    return position.isWhiteToMove() ? eval : -eval;
}


//one move buffer per ply, so the search never allocates and a child never overwrites its parent's moves
static uint16_t searchBuffers[maxPly][maxMoves];
static int32_t scoreBuffers[maxPly][maxMoves];

//check extensions stop past this ply, so a long run of checks can't blow up the search
static int extensionPlyLimit;

//iterative deepening stops here even with time left; check extensions double it, which still fits in maxPly
static constexpr int maxDepth = maxPly / 2;

//the clock is only read every this many nodes (minus one, as a bit mask), since reading it is slow
static constexpr uint64_t timeCheckMask = 2047;

static uint64_t nodes;
//Java keeps an absolute deadline; nowMicros() wraps, so this keeps the start and the allowed duration instead
static bool hasDeadline;
static uint32_t searchStart;
static uint32_t searchLimitMicros;
//set once time runs out (or stopSearch is called); every search call then unwinds immediately and its
//score is thrown away. Volatile because stopSearch may be called while a search is running
static volatile bool aborted;

//deepest search that finished and its score, for whoever wants to see how hard the engine thought
static int lastDepth;
static int lastScore;

//transposition table: what earlier searches learned about each position, keyed by its hash. The same position
//is reached through many move orders, so a stored result often saves searching it again; and even when the
//stored search was too shallow to reuse its score, its best move is the best first guess for move ordering.
//Entries are indexed by the low bits of the hash and survive between searches. Java spreads the same fields over
//parallel arrays; one 16-byte entry keeps each probe to a single cache line
struct TableEntry {
    uint64_t key;
    int32_t score;
    uint16_t move;
    int8_t depth;
    uint8_t bound;
};
static_assert(sizeof(TableEntry) == 16, "a table entry should pack into 16 bytes");

static TableEntry table[1u << tableSizeBits];
static constexpr uint32_t tableMask = (1u << tableSizeBits) - 1;

//what a stored score means: alpha-beta cuts searches short, so often the score is only known to be at
//least (a beta cutoff) or at most (no move beat alpha) the stored value. Zero marks an empty slot
static constexpr uint8_t exactScore = 1;
static constexpr uint8_t lowerBound = 2;
static constexpr uint8_t upperBound = 3;

//move ordering bands, best first: the table's move, then captures and queen promotions, then the two killers,
//then the remaining quiet moves by history score, which always stays within about ±historyMax
static constexpr int tableMoveOrderingScore = 1000000;
static constexpr int captureOrderingScore = 100000;
static constexpr int firstKillerOrderingScore = 90000;
static constexpr int secondKillerOrderingScore = 80000;

//killer moves: the last two quiet moves that caused a beta cutoff at each ply. Positions at the same ply of the
//tree are usually siblings that differ by one move, so a move that refuted one of them often refutes the next
static uint16_t killers[maxPly][2];

//history heuristic: how often each quiet move, by side, start and target square, has caused a cutoff anywhere in
//the tree. Unlike killers it isn't tied to a ply, so it orders all the quiet moves the killers don't cover
static int32_t history[2][64][64];
static constexpr int historyMax = 16384;

//null-move pruning: after passing the turn, the opponent's reply is searched this much shallower than normal
static constexpr int nullMoveReduction = 2;
static constexpr int deepNullMoveReduction = 3;
static constexpr int deepNullMoveDepth = 7;

//late move reductions: how many plies a late quiet move is searched shallower, by depth and by its index in the
//move list. Grows with both, since deep searches and moves far down a well ordered list are the safest to cut.
//Java's table is int[65][512]; no position has 256 moves and no entry exceeds 11, so bytes and 256 columns do
static constexpr int lateMoveIndex = 3;
static constexpr int lateMoveMinDepth = 3;
static uint8_t lateMoveReductions[maxDepth * 2 + 1][maxMoves];
static_assert(maxDepth * 2 + 1 == lateMoveReductionDepths, "table depth mismatch");

void initLateMoveReductions() {
    for(int depth = 1; depth < maxDepth * 2 + 1; depth++) {
        for(int index = 1; index < maxMoves; index++) {
            //same expression and evaluation order as Java, in double, truncated toward zero
            lateMoveReductions[depth][index] = (uint8_t) (int) (0.75 + log((double) depth) * log((double) index) / 2.25);
        }
    }
}

int lateMoveReduction(int depth, int index) {
    return lateMoveReductions[depth][index];
}

void clearSearchState() {
    memset(table, 0, sizeof(table));
    memset(killers, 0, sizeof(killers));
    memset(history, 0, sizeof(history));
}

static bool outOfTime() {
    if((++nodes & timeCheckMask) == 0) {
        if(hasDeadline && (uint32_t) (nowMicros() - searchStart) >= searchLimitMicros) {
            aborted = true;
        }
    }
    return aborted;
}

//the stored best move for this position, or Move::none if the table hasn't seen it
static uint16_t probeMove(uint64_t key) {
    const TableEntry& entry = table[(uint32_t) key & tableMask];
    return entry.bound != 0 && entry.key == key ? entry.move : Move::none;
}

//mate scores count plies from the root, but a table entry can be reached at any ply, so they're stored
//counting from the position itself and converted back when read
static int scoreToTable(int score, int ply) {
    if(score >= MATE - maxPly) {
        return score + ply;
    }
    if(score <= -(MATE - maxPly)) {
        return score - ply;
    }
    return score;
}

static int scoreFromTable(int score, int ply) {
    if(score >= MATE - maxPly) {
        return score - ply;
    }
    if(score <= -(MATE - maxPly)) {
        return score + ply;
    }
    return score;
}

static void store(uint64_t key, int depth, uint8_t bound, int score, uint16_t move, int ply) {
    TableEntry& entry = table[(uint32_t) key & tableMask];
    bool samePosition = entry.bound != 0 && entry.key == key;

    //a deeper result for the same position is worth more than this one; anything else gets overwritten,
    //since a newer entry is more likely to be needed again soon
    if(samePosition && entry.depth > depth) {
        return;
    }

    //a search where no move beat alpha doesn't know the best move, so keep the one already stored
    if(move == Move::none && samePosition) {
        move = entry.move;
    }

    entry.key = key;
    entry.move = move;
    entry.score = scoreToTable(score, ply);
    entry.depth = (int8_t) depth;
    entry.bound = bound;
}

//shifts moves[0..index-1] back one slot and puts moves[index] first, keeping the rest in order
static void moveToFront(uint16_t* moves, int index) {
    uint16_t move = moves[index];
    memmove(moves + 1, moves, index * sizeof(moves[0]));
    moves[0] = move;
}

//the side to move has a knight, bishop, rook or queen; with only king and pawns, zugzwang is too common for
//null-move pruning to be trusted
static bool hasPiecesBesidesPawns(const Position& position) {
    if(position.isWhiteToMove()) {
        return (position.whiteKnights | position.whiteBishops | position.whiteRooks | position.whiteQueens) != 0;
    }
    return (position.blackKnights | position.blackBishops | position.blackRooks | position.blackQueens) != 0;
}

//neither a capture nor a promotion; only these are ordered by killers and history, and only these are reduced
static bool isQuiet(Position& position, uint16_t move) {
    if(Move::isPromotion(move) || Move::isEnPessant(move)) {
        return false;
    }
    uint64_t enemies = position.isWhiteToMove() ? position.getBlackPieces() : position.getWhitePieces();
    return (enemies & squareBit(Move::targetSquare(move))) == 0;
}

//moves the entry toward ±historyMax by bonus, scaled down the closer it already is, so it can never get far past
//the limit and a move that stops working loses its score as quickly as it gained it. The largest intermediate
//value is about 16,400 * 400, far inside 32 bits, and division truncates toward zero as in Java
static void updateHistory(bool white, uint16_t move, int bonus) {
    int32_t* from = history[white ? 0 : 1][Move::startSquare(move)];
    int to = Move::targetSquare(move);
    from[to] += bonus - from[to] * abs(bonus) / historyMax;
}

//moves[index] is a quiet move that just caused a beta cutoff: it becomes this ply's first killer and gains
//history, and the quiet moves searched before it, which failed to cut, lose the same amount. Deeper cutoffs
//prune bigger subtrees, so they count for more
static void rewardQuietCutoff(Position& position, bool white, const uint16_t* moves, int index, int depth, int ply) {
    uint16_t move = moves[index];

    if(killers[ply][0] != move) {
        killers[ply][1] = killers[ply][0];
        killers[ply][0] = move;
    }

    int bonus = depth * depth < 400 ? depth * depth : 400;
    updateHistory(white, move, bonus);
    for(int earlier = 0; earlier < index; earlier++) {
        if(isQuiet(position, moves[earlier])) {
            updateHistory(white, moves[earlier], -bonus);
        }
    }
}

//indexed by piece type (bishop = 1, pawn = 2, rook = 3, knight = 4, queen = 5, king = 6); the king is 0
//because it can only capture undefended pieces, so taking with it never risks anything
static const int orderingValues[7] = {0, bishopValue, pawnValue, rookValue, knightValue, queenValue, 0};

//alpha-beta prunes the most when the best move is searched first, so likely good moves get high scores:
//the transposition table's move first, then captures by most valuable victim and least valuable attacker
//(MVV-LVA) and queen promotions, then this ply's killers, then the other quiet moves by history.
//Underpromotions go last; they're almost never best
static void scoreMoves(const Position& position, const uint16_t* moves, int32_t* scores, int numMoves, uint16_t tableMove, int ply) {
    bool white = position.isWhiteToMove();
    const int32_t (*sideHistory)[64] = history[white ? 0 : 1];

    for(int index = 0; index < numMoves; index++) {
        uint16_t move = moves[index];

        if(move == tableMove) {
            scores[index] = tableMoveOrderingScore;
            continue;
        }

        int victim = Move::isEnPessant(move) ? Piece::pawn : position.pieceTypeAt(Move::targetSquare(move), !white);
        int promotion = Move::promotionPiece(move);

        if(victim != Piece::empty || promotion == Piece::queen) {
            int score = captureOrderingScore + orderingValues[promotion];
            if(victim != Piece::empty) {
                int attacker = position.pieceTypeAt(Move::startSquare(move), white);
                score += 10 * orderingValues[victim] - orderingValues[attacker];
            }
            scores[index] = score;
        } else if(promotion != Piece::empty) {
            scores[index] = -historyMax - 1;
        } else if(move == killers[ply][0]) {
            scores[index] = firstKillerOrderingScore;
        } else if(move == killers[ply][1]) {
            scores[index] = secondKillerOrderingScore;
        } else {
            scores[index] = sideHistory[Move::startSquare(move)][Move::targetSquare(move)];
        }
    }
}

//moves the best remaining move into slot `start`; one selection sort step at a time, because most nodes
//cut off after the first move or two and fully sorting the list would be wasted work
static void pickNextMove(uint16_t* moves, int32_t* scores, int start, int numMoves) {
    int best = start;
    for(int index = start + 1; index < numMoves; index++) {
        if(scores[index] > scores[best]) {
            best = index;
        }
    }

    uint16_t move = moves[start];
    moves[start] = moves[best];
    moves[best] = move;

    int32_t score = scores[start];
    scores[start] = scores[best];
    scores[best] = score;
}

//compacts the move list down to captures and queen promotions and returns the new length;
//underpromotions are dropped since they're almost never better and would only bloat the search
static int keepCapturesAndPromotions(Position& position, uint16_t* moves, int numMoves) {
    uint64_t enemies = position.isWhiteToMove() ? position.getBlackPieces() : position.getWhitePieces();
    int kept = 0;

    for(int index = 0; index < numMoves; index++) {
        uint16_t move = moves[index];
        bool capture = (enemies & squareBit(Move::targetSquare(move))) != 0 || Move::isEnPessant(move);

        if(Move::isPromotion(move) ? Move::promotionPiece(move) == Piece::queen : capture) {
            moves[kept++] = move;
        }
    }

    return kept;
}

//stopping the search in the middle of a trade misjudges the position (QxP looks like a free pawn when the
//recapture is one ply past the horizon), so at the end of the main search this keeps playing captures
//until the position is quiet. The side to move may also decline every capture and keep the static
//eval ("stand pat"), since in a real game nobody is forced to capture
static int quiescence(Position& position, int ply, int alpha, int beta) {
    if(outOfTime()) {
        return 0;
    }

    int standPat = evaluateRelative(position);

    if(ply == maxPly) {
        return standPat;
    }
    if(standPat >= beta) {
        return beta;
    }
    if(standPat > alpha) {
        alpha = standPat;
    }

    uint16_t* moves = searchBuffers[ply];
    int32_t* scores = scoreBuffers[ply];
    int numMoves = moveGenerator.generateMoves(position, moves);

    if(numMoves == 0) {
        return moveGenerator.inCheck(position) ? -MATE + ply : 0;
    }

    numMoves = keepCapturesAndPromotions(position, moves, numMoves);
    scoreMoves(position, moves, scores, numMoves, Move::none, ply);

    for(int index = 0; index < numMoves; index++) {
        pickNextMove(moves, scores, index, numMoves);
        position.playMove(moves[index]);
        int score = -quiescence(position, ply + 1, -beta, -alpha);
        position.undoMove();

        if(score >= beta) {
            return beta;
        }
        if(score > alpha) {
            alpha = score;
        }
    }

    return alpha;
}

//returns the score of the position for the side to move, searched depth plies deep;
//alpha is what the side to move is already guaranteed, beta is the most the opponent will allow
static int negamax(Position& position, int depth, int ply, int alpha, int beta) {
    if(outOfTime()) {
        return 0;
    }

    //a position that already came up can be repeated into a draw, and the fifty-move rule ends the game, so
    //both score as draws: the engine then avoids repeating when it's winning and heads for it when losing
    if(position.isRepetition() || position.getHalfmoveClock() >= 100) {
        return 0;
    }

    if(ply == maxPly) {
        return evaluateRelative(position);
    }

    bool inCheck = moveGenerator.inCheck(position);

    //checks are forcing and are how most mates happen, so searching one ply deeper after each check lets
    //the engine see mating sequences that would otherwise be cut off at the horizon
    if(inCheck && ply < extensionPlyLimit) {
        depth++;
    }

    if(depth == 0) {
        return quiescence(position, ply, alpha, beta);
    }

    uint64_t key = position.getHash();
    const TableEntry& entry = table[(uint32_t) key & tableMask];
    uint16_t tableMove = Move::none;

    if(entry.bound != 0 && entry.key == key) {
        tableMove = entry.move;

        //the score is only reusable if it came from a search at least this deep
        if(entry.depth >= depth) {
            int score = scoreFromTable(entry.score, ply);
            uint8_t bound = entry.bound;

            if(bound == exactScore) {
                int clamped = score < beta ? score : beta;
                return clamped > alpha ? clamped : alpha;
            }
            if(bound == lowerBound && score >= beta) {
                return beta;
            }
            if(bound == upperBound && score <= alpha) {
                return alpha;
            }
        }
    }

    //null-move pruning: let the side to move pass, and search the opponent's reply with a reduced depth and a
    //window that only asks "does it still reach beta?". Passing is almost always worse than the best real move,
    //so if even passing reaches beta, the real moves would too and this node can be cut without searching them.
    //Never when in check (passing would be illegal), right after another pass (two passes cancel out), when the
    //static eval is already below beta (the pass will almost never reach it), or with only king and pawns left,
    //where zugzwang is common and passing really can be better than every legal move
    if(!inCheck && depth >= 3 && position.getLastMove() != Move::none && hasPiecesBesidesPawns(position)
            && evaluateRelative(position) >= beta) {
        int reduction = depth >= deepNullMoveDepth ? deepNullMoveReduction : nullMoveReduction;
        int nullDepth = depth - 1 - reduction > 0 ? depth - 1 - reduction : 0;

        position.playNullMove();
        int score = -negamax(position, nullDepth, ply + 1, -beta, -beta + 1);
        position.undoNullMove();

        if(aborted) {
            return 0;
        }
        if(score >= beta) {
            return beta;
        }
    }

    uint16_t* moves = searchBuffers[ply];
    int32_t* scores = scoreBuffers[ply];
    int numMoves = moveGenerator.generateMoves(position, moves);

    //the generator is strictly legal, so no moves means checkmate or stalemate
    if(numMoves == 0) {
        return inCheck ? -MATE + ply : 0;
    }

    scoreMoves(position, moves, scores, numMoves, tableMove, ply);

    bool white = position.isWhiteToMove();
    int originalAlpha = alpha;
    uint16_t bestMove = Move::none;

    for(int index = 0; index < numMoves; index++) {
        pickNextMove(moves, scores, index, numMoves);
        uint16_t move = moves[index];
        bool quiet = isQuiet(position, move);

        position.playMove(move);

        int score;
        //late move reductions: with good ordering the best move is almost always among the first few, so quiet
        //moves further down are first searched shallower, with a window that only asks "does it beat alpha?".
        //Most fail low and are dropped at a fraction of the cost; one that does beat alpha is searched again at
        //full depth, so a good move is never lost, only found a little later. Checks and moves out of check are
        //forcing, and killers have already refuted a sibling, so those are always searched at full depth
        if(quiet && index >= lateMoveIndex && depth >= lateMoveMinDepth && !inCheck
                && move != killers[ply][0] && move != killers[ply][1] && !moveGenerator.inCheck(position)) {
            int row = depth < maxDepth * 2 ? depth : maxDepth * 2;
            int reduction = lateMoveReductions[row][index];
            if(reduction > depth - 2) reduction = depth - 2;
            if(reduction < 1) reduction = 1;

            score = -negamax(position, depth - 1 - reduction, ply + 1, -alpha - 1, -alpha);
            if(score > alpha && !aborted) {
                score = -negamax(position, depth - 1, ply + 1, -beta, -alpha);
            }
        } else {
            score = -negamax(position, depth - 1, ply + 1, -beta, -alpha);
        }

        position.undoMove();

        //a stopped search's scores are meaningless, so none of them may reach the table
        if(aborted) {
            return 0;
        }

        //the opponent already has a better option earlier in the tree, so they'll never allow this position
        if(score >= beta) {
            if(quiet) {
                rewardQuietCutoff(position, white, moves, index, depth, ply);
            }
            store(key, depth, lowerBound, beta, move, ply);
            return beta;
        }
        if(score > alpha) {
            alpha = score;
            bestMove = move;
        }
    }

    store(key, depth, alpha > originalAlpha ? exactScore : upperBound, alpha, bestMove, ply);
    return alpha;
}

//the iterative deepening loop behind both findBestMove and searchFixedDepth. Each iteration reuses the previous
//one's best move as its first guess, so the shallow searches pay for themselves by making the deeper ones prune
//better
static uint16_t search(Position& position, int depthLimit) {
    nodes = 0;
    aborted = false;
    lastDepth = 0;
    lastScore = 0;

    //killers belong to plies of the last search's tree, which are different positions now; history describes
    //moves in general, so it carries over at reduced weight
    memset(killers, 0, sizeof(killers));
    for(int side = 0; side < 2; side++) {
        for(int from = 0; from < 64; from++) {
            for(int to = 0; to < 64; to++) {
                history[side][from][to] /= 2;
            }
        }
    }

    uint16_t* moves = searchBuffers[0];
    int32_t* scores = scoreBuffers[0];
    int numMoves = moveGenerator.generateMoves(position, moves);

    if(numMoves == 0) {
        return Move::none;
    }

    //sort the root once so the first iteration starts from the last search's best move, if it saw this
    //position, and then the obvious captures
    uint64_t rootKey = position.getHash();
    scoreMoves(position, moves, scores, numMoves, probeMove(rootKey), 0);
    for(int index = 0; index < numMoves; index++) {
        pickNextMove(moves, scores, index, numMoves);
    }

    uint16_t bestMove = moves[0];

    //nothing to think about
    if(numMoves == 1) {
        return bestMove;
    }

    for(int depth = 1; depth <= depthLimit; depth++) {
        extensionPlyLimit = depth * 2;

        int alpha = -INF;
        int bestIndex = -1;

        for(int index = 0; index < numMoves; index++) {
            position.playMove(moves[index]);
            int score = -negamax(position, depth - 1, 1, -INF, -alpha);
            position.undoMove();

            if(aborted) {
                break;
            }

            if(score > alpha) {
                alpha = score;
                bestIndex = index;
            }
        }

        //moves[0] is the previous iteration's best and is always searched first, so even when time ran
        //out partway through, any move that beat it at this depth is a real improvement
        if(bestIndex >= 0) {
            bestMove = moves[bestIndex];
            moveToFront(moves, bestIndex);
        }

        if(aborted) {
            break;
        }

        lastDepth = depth;
        lastScore = alpha;
        store(rootKey, depth, exactScore, alpha, bestMove, 0);

        //a forced mate was found; searching deeper can't find anything better
        if(alpha >= MATE - maxPly) {
            break;
        }
    }

    return bestMove;
}

uint16_t findBestMove(Position& position, uint32_t timeLimitMillis) {
    //nowMicros() wraps after about 71 minutes, which caps the time limit
    const uint32_t maxMillis = 0xFFFFFFFFu / 1000;
    hasDeadline = true;
    searchStart = nowMicros();
    searchLimitMicros = (timeLimitMillis < maxMillis ? timeLimitMillis : maxMillis) * 1000;
    return search(position, maxDepth);
}

SearchResult searchFixedDepth(Position& position, int depth) {
    clearSearchState();
    hasDeadline = false;
    uint16_t bestMove = search(position, depth < maxDepth ? depth : maxDepth);
    return SearchResult{bestMove, lastScore, lastDepth, nodes};
}

void stopSearch() {
    aborted = true;
}

int getLastDepth() {
    return lastDepth;
}

int getLastScore() {
    return lastScore;
}

uint64_t getNodes() {
    return nodes;
}

bool isMateScore(int score) {
    return abs(score) >= MATE - maxPly;
}

int pliesToMate(int score) {
    return MATE - abs(score);
}


static uint64_t countPositions(Position& position, int depth) {
    if(depth == 0) {
        return 1;
    }

    uint16_t* moves = searchBuffers[depth - 1];
    int numMoves = moveGenerator.generateMoves(position, moves);
    uint64_t numPositions = 0;

    for(int index = 0; index < numMoves; index++) {
        position.playMove(moves[index]);
        numPositions += countPositions(position, depth - 1);
        position.undoMove();
    }

    return numPositions;
}

uint64_t getNumPossiblePositions(Position& position, int depth) {
    if(depth < 0 || depth > maxPly) {
        return 0;
    }
    return countPositions(position, depth);
}

}
