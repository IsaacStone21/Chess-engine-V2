# Chess Engine V2

A chess engine written from scratch in Java, with a Swing GUI for playing against it, and firmware for a
physical self-moving chessboard driven by a Teensy 4.1.

The physical board is a work in progress. The engine and GUI can be played today.

## Repository layout

```
EngineUtil/     The engine: board representation, move generation, search, evaluation, opening book
GUI/            Swing front end for playing against the engine; piece images live in GUI/PNGs/
Testing/        Match.java, which plays two versions of the engine against each other
Firmware/       PlatformIO project for the Teensy 4.1 that moves pieces on the physical board
```

A C++17 port of the engine lives on the `cpp-engine` branch. It is kept node-for-node identical to the Java
engine: same perft counts, and the same best move, score and node count at a fixed depth.

## The engine

**Board and move generation**
- Bitboard position representation, with magic bitboards for sliding-piece attacks
- Strictly legal move generation using pin masks and check detection (no pseudo-legal filtering)
- Moves packed into a `short`
- Zobrist hashing, with incremental make/unmake

**Search**
- Negamax with alpha-beta pruning and iterative deepening, limited by think time (3 seconds per move by default)
- Quiescence search on captures
- Transposition table
- Move ordering: transposition table move, then captures by MVV-LVA, then killer moves and the history heuristic
- Null-move pruning and late move reductions
- Draw detection: threefold repetition and the fifty-move rule
- A built-in opening book

**Evaluation**
- Material plus piece-square tables
- Separate middlegame and endgame tables for pawns and kings, blended by game phase
- A mop-up term that drives the enemy king to the edge in won endgames

## Running the GUI

Requires a JDK (developed on Java 27; any recent JDK with `switch` expressions should work).

Run these from the repository root. The piece images are loaded by a path relative to the working directory.

```sh
javac -d out GUI/*.java EngineUtil/*.java
java -cp out GUI.Main
```

## Testing engine changes

`Testing/Match.java` plays one commit of the engine against another (or against your uncommitted working
tree) and reports the score. It compiles each version into a temp folder and loads them with separate class
loaders, so there is no build step:

```sh
java Testing/Match.java HEAD                  # working tree vs. the last commit
java Testing/Match.java 97a4ffe --time 200    # working tree vs. 97a4ffe, 200 ms per move
java Testing/Match.java HEAD~2 HEAD           # two commits against each other
```

Options: `--games N`, `--time MS` (default 100), `--depth N`, `--threads N`. Each opening in its list is
played twice, once with each version as white. It works with any commit from `97a4ffe` onward.

## The physical board (work in progress)

The plan is a Raspberry Pi that runs the engine and keeps the official game state, and a Teensy 4.1 that
reads the board and moves the pieces. They talk over the serial protocol in
[Firmware/Protocol.md](Firmware/Protocol.md).

Hardware the firmware drives:
- **Gantry:** a CoreXY belt system with two stepper motors (2 mm pitch belt, 16-tooth pulleys) and X/Y limit
  switches for homing
- **Electromagnet:** on an H-bridge, so its polarity can be flipped to pick up white or black pieces
- **Sensors:** one analog sensor per square, read through multiplexers, which classify each square as white,
  black or empty

Firmware libraries are in `Firmware/lib/` (`Stepper`, `Gantry`, `Electromagnet`, `Multiplexor`).

To build and flash, install [PlatformIO](https://platformio.org/) and run from `Firmware/`:

```sh
pio run -t upload
```

Still to do: square offsets need calibrating, homing is disabled in `Gantry::initialize`, captured pieces have
nowhere to go yet, and the serial command loop in `main.cpp` hasn't been written.
