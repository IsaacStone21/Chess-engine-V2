# C++ engine

A C++17 port of the Java engine in `EngineUtil/` (bitboards, fancy magic bitboards, negamax with alpha-beta,
iterative deepening, transposition table, killer/history ordering, null-move pruning, late move reductions). The port
is checked against the Java engine: same perft counts, and the same best move, score, completed depth and **node
count** at fixed depth.

```
engine/      the engine; no threads, no allocation after initEngine()
  config.h   transposition table size, move and history limits
platform/    nowMicros(), the one thing the engine needs from the OS
tests/       chess_tests: perft, Java comparison, magic table checks, benchmark
  data/      java_reference.jsonl (Java's output)
```

## Using the engine

```cpp
#include "engine.h"

initEngine();                        // once, before creating any Position
Position position;                   // start position (or Position::fromFEN(fen, position))
uint16_t move = Engine::findBestMove(position, 1000);         // think for 1 s
SearchResult result = Engine::searchFixedDepth(position, 8);  // or search exactly 8 plies
position.playMove(move);
```

Moves are `uint16_t` with the layout documented in `engine/move.h`, the same as `Move.java`. Squares go from
0 = a8 to 63 = h1. `Move::none` is 0. A `Position` holds its own 1024-ply history (about 14 KB); reset one in place
with `setStartPosition()` rather than making temporary copies. `Engine::stopSearch()` makes a running search return
its best move so far.

## Build and tests

Needs g++ (tested with MinGW-w64 GCC 16 on Windows) and CMake 3.16 or newer.

```
cmake -S cpp -B cpp/build -G Ninja
cmake --build cpp/build
ctest --test-dir cpp/build          # or run cpp/build/chess_tests directly
```

The tests:

- **magic tables**: rook and bishop lookups match ray walking on random occupancies and every relevant blocker
  subset.
- **incremental hashing and undo**: at every node of a depth-3 tree, the incremental Zobrist hash equals one
  recomputed from scratch, and undo restores the board.
- **perft**: start position to depth 5 and CPW positions 2–5 to depth 4 or 5, compared with the published counts.
- **history overflow**: playing more than 1024 plies keeps hashing, repetition detection and the search working.
- **Java reference**: the late move reduction table and the Zobrist hash of each suite position equal Java's.
  Fixed-depth searches of 12 positions at depths 1–8 give identical best move, score, completed depth and node
  count. The TT is 2^20 entries, as in Java.

## Benchmark

```
cpp/build/chess_tests --bench [depth]              # C++, default depth 8
java Testing/EngineReference.java --bench [depth]  # Java on the same positions (run from the repo root)
```

Both search the shared suite at a fixed depth and print nodes, time and nodes per second. Node counts are identical,
so the nodes-per-second figures compare like for like.

## Regenerating the Java reference data

From the repository root:

```
java Testing/EngineReference.java > cpp/tests/data/java_reference.jsonl
```

This uses the multi-file source launcher (Java 22 or newer) and loads only `EngineUtil` classes. It runs
`Engine.findBestMoveFixedDepth`, which clears the TT, killers and history before each search, so every result
depends only on its position. Regenerate the data whenever the Java search or evaluation changes. If you add or change
a suite position, edit both `Testing/EngineReference.java` and `engine/bench.cpp`; the test checks that the two
lists match.
