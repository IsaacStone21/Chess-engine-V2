//host test executable for the C++ engine.
//
//  chess_tests                              run every test; exits non-zero if any fails
//  chess_tests --bench [depth] [--tt-bits N]  nodes per second on the shared fixed-depth suite
//
//With --tt-bits 14 the PC build uses the Teensy's table size, so its node counts must equal the Teensy's `bench`.

#include <inttypes.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <string>
#include <vector>

#include "bench.h"
#include "engine.h"
#include "magic.h"
#include "movegen.h"
#include "platform.h"
#include "position.h"

#ifndef CHESS_TEST_DATA_DIR
#define CHESS_TEST_DATA_DIR "tests/data"
#endif

static int failures = 0;
static int checks = 0;

//prints the first few failures in full and only counts the rest, so one broken table doesn't flood the output
static void fail(const char* format, ...) __attribute__((format(printf, 1, 2)));
static void fail(const char* format, ...) {
    failures++;
    if(failures <= 40) {
        va_list args;
        va_start(args, format);
        printf("  FAIL: ");
        vprintf(format, args);
        printf("\n");
        va_end(args);
    }
}

#define CHECK(condition, ...) do { checks++; if(!(condition)) fail(__VA_ARGS__); } while(0)

static MoveGenerator moveGenerator;

static const char* rookBackendName() {
#if ENGINE_ROOK_BACKEND == ENGINE_ROOK_HYPERBOLA
    return "hyperbola";
#elif ENGINE_ROOK_BACKEND == ENGINE_ROOK_FLASH
    return "flash table";
#else
    return "computed table";
#endif
}

//---- perft -----------------------------------------------------------------------------------------------------

struct PerftCase {
    const char* name;
    const char* fen;
    int maxDepth;
    uint64_t expected[6];
};

//published values from the chess programming wiki
static const PerftCase perftCases[] = {
    {"start", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 5, {1, 20, 400, 8902, 197281, 4865609}},
    {"kiwipete", "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", 4, {1, 48, 2039, 97862, 4085603}},
    {"cpw3", "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 5, {1, 14, 191, 2812, 43238, 674624}},
    {"cpw4", "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 4, {1, 6, 264, 9467, 422333}},
    {"cpw4mirror", "r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ - 0 1", 4, {1, 6, 264, 9467, 422333}},
    {"cpw5", "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 4, {1, 44, 1486, 62379, 2103487}},
    //cpw4 with an extra white pawn on g4 (a typo of the real position that once crept into the port plan); not
    //published, but the Java engine and an independent brute-force generator agree on these
    {"cpw4g4", "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P1P1/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 4, {1, 6, 264, 9163, 407492}},
};

static void testPerft() {
    printf("perft\n");
    for(const PerftCase& test : perftCases) {
        static Position position;
        CHECK(Position::fromFEN(test.fen, position), "%s: FEN didn't parse", test.name);
        for(int depth = 1; depth <= test.maxDepth; depth++) {
            uint32_t start = nowMicros();
            uint64_t count = Engine::getNumPossiblePositions(position, depth);
            uint32_t elapsed = nowMicros() - start;
            CHECK(count == test.expected[depth], "%s depth %d: %" PRIu64 " positions, expected %" PRIu64,
                  test.name, depth, count, test.expected[depth]);
            if(depth == test.maxDepth) {
                printf("  %-9s depth %d  %10" PRIu64 "  %7.1f ms\n", test.name, depth, count, elapsed / 1000.0);
            }
        }
    }
}

//---- incremental hash and undo ---------------------------------------------------------------------------------

static bool sameBoard(const Position& a, const Position& b) {
    return a.whitePawns == b.whitePawns && a.whiteKnights == b.whiteKnights && a.whiteBishops == b.whiteBishops
        && a.whiteRooks == b.whiteRooks && a.whiteQueens == b.whiteQueens && a.whiteKing == b.whiteKing
        && a.blackPawns == b.blackPawns && a.blackKnights == b.blackKnights && a.blackBishops == b.blackBishops
        && a.blackRooks == b.blackRooks && a.blackQueens == b.blackQueens && a.blackKing == b.blackKing
        && a.isWhiteToMove() == b.isWhiteToMove() && a.getEnPassantSquare() == b.getEnPassantSquare()
        && a.getHalfmoveClock() == b.getHalfmoveClock() && a.getHash() == b.getHash();
}

static int hashMismatches = 0;

//every make, unmake, null move and null unmake keeps the hash equal to one computed from scratch
static void walkHashes(Position& position, int depth) {
    if(position.getHash() != position.computeHash()) {
        hashMismatches++;
    }
    if(depth == 0) {
        return;
    }

    uint16_t moves[maxMoves];
    int numMoves = moveGenerator.generateMoves(position, moves);
    for(int index = 0; index < numMoves; index++) {
        uint64_t before = position.getHash();
        position.playMove(moves[index]);
        walkHashes(position, depth - 1);
        position.undoMove();
        if(position.getHash() != before) {
            hashMismatches++;
        }
    }

    position.playNullMove();
    if(position.getHash() != position.computeHash()) {
        hashMismatches++;
    }
    position.undoNullMove();
}

static void testHashesAndUndo() {
    printf("incremental hashing and undo\n");
    for(const PerftCase& test : perftCases) {
        static Position position;
        static Position original;
        Position::fromFEN(test.fen, position);
        Position::fromFEN(test.fen, original);
        hashMismatches = 0;
        walkHashes(position, 3);
        CHECK(hashMismatches == 0, "%s: %d hash mismatches", test.name, hashMismatches);
        CHECK(sameBoard(position, original), "%s: board not restored after undo", test.name);
    }

    static Position fromFen;
    static Position start;
    start.setStartPosition();
    Position::fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", fromFen);
    CHECK(sameBoard(fromFen, start), "start FEN doesn't match the start position");
    CHECK(!Position::fromFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP w KQkq - 0 1", fromFen), "a 7-rank FEN parsed");
    CHECK(start.getPieceAtIndex(0) == (Piece::black | Piece::rook), "a8 isn't a black rook");
    CHECK(start.getPieceAtIndex(60) == (Piece::white | Piece::king), "e1 isn't the white king");
    CHECK(start.getPieceAtIndex(30) == Piece::empty, "g5 isn't empty");
}

//shuffling knights far past historyCapacity keeps hashing, repetition detection and the search working
static void testHistoryOverflow() {
    printf("history overflow\n");
    static Position position;
    position.setStartPosition();
    const char* shuffle[4] = {"g1f3", "g8f6", "f3g1", "f6g8"};

    bool hashesOk = true;
    bool repetitionsOk = true;
    for(int ply = 0; ply < 2 * historyCapacity + 100; ply++) {
        if(!playCoordinateMove(position, shuffle[ply % 4], 4, moveGenerator)) {
            fail("shuffle move %d illegal", ply);
            return;
        }
        hashesOk &= position.getHash() == position.computeHash();
        //every fourth ply is back at the start position, which has been seen before
        if(ply >= 3 && ply % 4 == 3) {
            repetitionsOk &= position.isRepetition();
        }
        if(position.getPly() > historyCapacity) {
            fail("ply %d exceeds the history capacity", position.getPly());
            return;
        }
    }
    CHECK(hashesOk, "hash drifted while the history wrapped");
    CHECK(repetitionsOk, "a repetition went unnoticed while the history wrapped");

    SearchResult result = Engine::searchFixedDepth(position, 4);
    CHECK(result.bestMove != Move::none, "no move found after the history wrapped");

    int remaining = position.getPly();
    for(int undo = 0; undo < remaining; undo++) {
        position.undoMove();
    }
    CHECK(position.getPly() == 0, "undo didn't empty the history");
    CHECK(position.getHash() == position.computeHash(), "hash wrong after undoing a wrapped history");
}

//---- rook attack backends --------------------------------------------------------------------------------------

static uint64_t computedRooks[Magic::rookTableSize];

//xorshift64*, so the random occupancies are the same every run
static uint64_t randomState = 0x9E3779B97F4A7C15ULL;
static uint64_t nextRandom() {
    randomState ^= randomState >> 12;
    randomState ^= randomState << 25;
    randomState ^= randomState >> 27;
    return randomState * 0x2545F4914F6CDD1DULL;
}

static void testRookBackends() {
    printf("rook attack backends (engine uses: %s)\n", rookBackendName());
    Magic::buildRookTable(computedRooks);

    int headerMismatches = 0;
    for(int index = 0; index < Magic::rookTableSize; index++) {
        if(Magic::generatedRookAttacks[index] != computedRooks[index]) {
            headerMismatches++;
        }
    }
    CHECK(headerMismatches == 0, "generated rook header differs from the computed table in %d entries "
          "(rebuild it with the regen_rook_table target)", headerMismatches);

    //every square with 100k random occupancies of varying density, plus every relevant blocker subset
    int hyperbolaMismatches = 0;
    int engineMismatches = 0;
    int slowMismatches = 0;
    for(int square = 0; square < 64; square++) {
        for(int sample = 0; sample < 100000; sample++) {
            uint64_t occupied = nextRandom();
            switch(sample % 3) {
                case 1: occupied &= nextRandom(); break;
                case 2: occupied &= nextRandom() & nextRandom(); break;
            }
            uint64_t expected = computedRooks[Magic::rookIndex(square, occupied)];
            hyperbolaMismatches += Magic::rookAttacksHyperbola(square, occupied) != expected;
            engineMismatches += Magic::rookAttacks(square, occupied) != expected;
            if(sample < 2000) {
                slowMismatches += Magic::slowRookAttacks(square, occupied) != expected;
            }
        }

        uint64_t mask = Magic::rookMasks[square];
        uint64_t subset = 0;
        do {
            uint64_t expected = Magic::slowRookAttacks(square, subset);
            hyperbolaMismatches += Magic::rookAttacksHyperbola(square, subset) != expected;
            engineMismatches += Magic::rookAttacks(square, subset) != expected;
            slowMismatches += computedRooks[Magic::rookIndex(square, subset)] != expected;
            subset = (subset - mask) & mask;
        } while(subset != 0);
    }
    CHECK(hyperbolaMismatches == 0, "hyperbola quintessence differs from the magic table %d times", hyperbolaMismatches);
    CHECK(engineMismatches == 0, "engine rookAttacks differs from the magic table %d times", engineMismatches);
    CHECK(slowMismatches == 0, "magic table differs from ray walking %d times", slowMismatches);

    int bishopMismatches = 0;
    for(int square = 0; square < 64; square++) {
        for(int sample = 0; sample < 20000; sample++) {
            uint64_t occupied = nextRandom() & nextRandom();
            bishopMismatches += Magic::bishopAttacks(square, occupied) != Magic::slowBishopAttacks(square, occupied);
        }
    }
    CHECK(bishopMismatches == 0, "bishop table differs from ray walking %d times", bishopMismatches);
}

//---- Java reference --------------------------------------------------------------------------------------------

//value of "key" in one JSON line: the text inside quotes or brackets, or up to the next , or }
static bool jsonField(const std::string& line, const char* key, std::string& out) {
    std::string pattern = std::string("\"") + key + "\":";
    size_t start = line.find(pattern);
    if(start == std::string::npos) {
        return false;
    }
    start += pattern.size();

    size_t end;
    if(line[start] == '"') {
        end = line.find('"', start + 1);
        out = line.substr(start + 1, end - start - 1);
    } else if(line[start] == '[') {
        end = line.find(']', start);
        out = line.substr(start + 1, end - start - 1);
    } else {
        end = line.find_first_of(",}", start);
        out = line.substr(start, end - start);
    }
    return end != std::string::npos;
}

static long long jsonNumber(const std::string& line, const char* key) {
    std::string value;
    if(!jsonField(line, key, value)) {
        fail("missing \"%s\" in: %.120s", key, line.c_str());
        return 0;
    }
    return strtoll(value.c_str(), nullptr, 10);
}

static std::string jsonString(const std::string& line, const char* key) {
    std::string value;
    if(!jsonField(line, key, value)) {
        fail("missing \"%s\" in: %.120s", key, line.c_str());
    }
    return value;
}

static const BenchPosition* findBenchPosition(const std::string& id) {
    for(int index = 0; index < benchSuiteSize; index++) {
        if(id == benchSuite[index].id) {
            return &benchSuite[index];
        }
    }
    return nullptr;
}

static void testJavaReference() {
    const char* path = CHESS_TEST_DATA_DIR "/java_reference.jsonl";
    printf("Java reference (%s)\n", path);

    FILE* file = fopen(path, "rb");
    if(file == nullptr) {
        fail("can't open %s", path);
        return;
    }

    std::vector<std::string> lines;
    std::string line;
    int c;
    while((c = fgetc(file)) != EOF) {
        if(c == '\n') {
            lines.push_back(line);
            line.clear();
        } else if(c != '\r') {
            line += (char) c;
        }
    }
    if(!line.empty()) {
        lines.push_back(line);
    }
    fclose(file);

    //compared in the order the plan suggests for tracking down a difference: LMR table, Zobrist keys, then searches
    int lmrRows = 0;
    int lmrMismatches = 0;
    for(const std::string& entry : lines) {
        if(jsonString(entry, "type") != "lmr") continue;
        int depth = (int) jsonNumber(entry, "depth");
        std::string values = jsonString(entry, "values");
        const char* cursor = values.c_str();
        for(int index = 0; index < maxMoves; index++) {
            char* next;
            long value = strtol(cursor, &next, 10);
            if(next == cursor) {
                fail("lmr row %d has too few values", depth);
                break;
            }
            if(Engine::lateMoveReduction(depth, index) != value) {
                if(lmrMismatches++ < 5) {
                    printf("  lmr[%d][%d] = %d, Java %ld\n", depth, index, Engine::lateMoveReduction(depth, index), value);
                }
            }
            cursor = *next == ',' ? next + 1 : next;
        }
        lmrRows++;
    }
    CHECK(lmrRows == Engine::lateMoveReductionDepths, "%d lmr rows, expected %d", lmrRows, Engine::lateMoveReductionDepths);
    CHECK(lmrMismatches == 0, "%d late move reduction entries differ from Java", lmrMismatches);

    static Position position;
    int zobristChecked = 0;
    for(const std::string& entry : lines) {
        if(jsonString(entry, "type") != "zobrist") continue;
        std::string id = jsonString(entry, "id");
        const BenchPosition* bench = findBenchPosition(id);
        if(bench == nullptr) {
            fail("Java position %s isn't in the C++ suite", id.c_str());
            continue;
        }
        CHECK(playCoordinateMoves(position, bench->moves, moveGenerator), "%s: illegal move in suite", id.c_str());
        uint64_t expected = strtoull(jsonString(entry, "hash").c_str(), nullptr, 16);
        CHECK(position.getHash() == expected, "%s: hash %016" PRIx64 ", Java %016" PRIx64, id.c_str(),
              position.getHash(), expected);
        zobristChecked++;
    }
    CHECK(zobristChecked == benchSuiteSize, "%d Zobrist hashes checked, expected %d", zobristChecked, benchSuiteSize);
    printf("  lmr table: %d rows, zobrist: %d positions\n", lmrRows, zobristChecked);

    int searches = 0;
    int searchMismatches = 0;
    uint64_t totalNodes = 0;
    for(const std::string& entry : lines) {
        if(jsonString(entry, "type") != "search") continue;
        std::string id = jsonString(entry, "id");
        const BenchPosition* bench = findBenchPosition(id);
        if(bench == nullptr) {
            fail("Java position %s isn't in the C++ suite", id.c_str());
            continue;
        }
        CHECK(jsonString(entry, "moves") == bench->moves, "%s: C++ suite moves differ from Java's", id.c_str());

        int depth = (int) jsonNumber(entry, "depth");
        playCoordinateMoves(position, bench->moves, moveGenerator);
        SearchResult result = Engine::searchFixedDepth(position, depth);
        totalNodes += result.nodes;

        int javaMove = (int) jsonNumber(entry, "move");
        int javaScore = (int) jsonNumber(entry, "score");
        int javaDepth = (int) jsonNumber(entry, "completed");
        uint64_t javaNodes = (uint64_t) jsonNumber(entry, "nodes");

        bool same = result.bestMove == javaMove && result.score == javaScore && result.depth == javaDepth
                && result.nodes == javaNodes;
        if(!same) {
            searchMismatches++;
            char move[6];
            moveToCoordinates(result.bestMove, move);
            fail("%s depth %d: C++ %s score %d depth %d nodes %" PRIu64 "; Java %s score %d depth %d nodes %" PRIu64,
                 id.c_str(), depth, move, result.score, result.depth, result.nodes,
                 jsonString(entry, "uci").c_str(), javaScore, javaDepth, javaNodes);
        }
        checks++;
        searches++;
    }
    CHECK(searches >= 8 * 8, "only %d searches in the reference data", searches);
    printf("  %d fixed-depth searches compared (%d differ), %" PRIu64 " nodes in total\n", searches, searchMismatches,
           totalNodes);
}

//---- benchmark -------------------------------------------------------------------------------------------------

static int runBench(int depth) {
    static Position position;
    printf("C++ bench: depth %d, transposition table 2^%d, rook attacks: %s\n", depth, Engine::getTableSizeBits(),
           rookBackendName());
    printf("lmr checksum %08" PRIx32 ", start hash %016" PRIx64 "\n", lateMoveReductionChecksum(), startPositionHash());

    uint64_t totalNodes = 0;
    uint64_t totalMicros = 0;
    for(int index = 0; index < benchSuiteSize; index++) {
        if(!playCoordinateMoves(position, benchSuite[index].moves, moveGenerator)) {
            printf("illegal move in suite position %s\n", benchSuite[index].id);
            return 1;
        }
        uint32_t start = nowMicros();
        SearchResult result = Engine::searchFixedDepth(position, depth);
        uint32_t elapsed = nowMicros() - start;

        char move[6];
        moveToCoordinates(result.bestMove, move);
        printf("%-20s depth %d  nodes %10" PRIu64 "  time %8.1f ms  best %-5s score %d\n", benchSuite[index].id,
               result.depth, result.nodes, elapsed / 1000.0, move, result.score);
        totalNodes += result.nodes;
        totalMicros += elapsed;
    }
    printf("C++ total: nodes %" PRIu64 "  time %.1f ms  nps %.0f\n", totalNodes, totalMicros / 1000.0,
           totalMicros > 0 ? totalNodes * 1e6 / (double) totalMicros : 0.0);
    return 0;
}

int main(int argc, char** argv) {
    initEngine();

    if(argc > 1 && strcmp(argv[1], "--bench") == 0) {
        int depth = defaultBenchDepth;
        for(int arg = 2; arg < argc; arg++) {
            if(strcmp(argv[arg], "--tt-bits") == 0 && arg + 1 < argc) {
                Engine::setTableSizeBits(atoi(argv[++arg]));
            } else {
                depth = atoi(argv[arg]);
            }
        }
        return runBench(depth);
    }

    printf("rook attacks: %s, transposition table 2^%d\n", rookBackendName(), Engine::getTableSizeBits());
    uint32_t start = nowMicros();

    testRookBackends();
    testHashesAndUndo();
    testPerft();
    testHistoryOverflow();
    testJavaReference();

    printf("\n%d checks, %d failed (%.1f s)\n", checks, failures, (nowMicros() - start) / 1e6);
    printf(failures == 0 ? "ALL TESTS PASSED\n" : "TESTS FAILED\n");
    return failures == 0 ? 0 : 1;
}
