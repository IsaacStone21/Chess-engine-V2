package Testing;

import java.lang.reflect.Field;
import java.util.Locale;

import EngineUtil.Engine;
import EngineUtil.Move;
import EngineUtil.MoveGenerator;
import EngineUtil.Piece;
import EngineUtil.Position;

//prints what the Java engine does on a fixed suite of positions, so the C++ port (cpp/) can be checked against it
//move for move and node for node. Run it from the repository root, with no build step:
//
//  java Testing/EngineReference.java > cpp/tests/data/java_reference.jsonl    reference data for the C++ tests
//  java Testing/EngineReference.java --bench [depth]                           nodes per second at a fixed depth
//
//Each line is one JSON object: the late move reduction table ("lmr"), the Zobrist hash of every suite position
//("zobrist"), and a fixed-depth search of every suite position at depths 1 to maxDepth ("search"). Positions are
//the start position plus coordinate moves, so no FEN support is needed; the C++ suite in cpp/engine/bench.cpp
//must list the same ids and moves, which the C++ test checks against the "moves" field.
//Only EngineUtil classes are loaded, never anything from GUI.
public class EngineReference {
    private static final String[][] suite = {
        {"start", ""},
        {"ruy_lopez", "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6"},
        {"najdorf", "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6"},
        {"kings_gambit", "e2e4 e7e5 f2f4 e5f4 g1f3 g7g5"},
        {"en_passant", "e2e4 g8f6 e4e5 d7d5"},
        {"scholar_mate", "e2e4 e7e5 f1c4 b8c6 d1h5 g8f6"},
        {"kid_middlegame", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 b8d7 c1e3 e7e5 f1d3 f6g4 d1d2 e5d4 f3d4 g4e3"
                + " f2e3 e8g8 e1c1 d7e5 c1b1 c8g4 d3e2 g4e2 d2e2 d8h4 h2h3 a8d8 b2b4 c7c5"},
        {"italian_middlegame", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d4 e5d4 e4e5 d7d5 e5f6 d5c4 f6g7 h8g8 c3d4"
                + " c6d4 c1h6 d4f3 d1f3 d8h4 h6e3 c5e3 f3e3 h4e7"},
        {"qgd_endgame", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7 g5f6 e7f6 c4d5 e6d5 e2e3 b8c6 d1b3 c6e7 f1d3 e8g8"
                + " g1f3 d8d6 e3e4 d5e4 c3e4 d6c6 e1g1 c8e6 e4f6 g7f6 d3b5 e6b3 b5c6 b3d5 c6d5 e7d5 a1e1 h7h5 e1e4 g8g7"
                + " e4e1 a8d8 a2a4 f6f5 f3e5 g7f6 a4a5 d5f4 e5f3 f8e8 e1d1 d8d5 g2g3 f4e2 g1g2 e8d8 d1e1 e2d4 f3d4 d5d4"
                + " e1c1 d8d7 c1b1 d4a4 b2b4 d7d4 f1c1 a4b4 b1b4 d4b4 c1c7 b4b5 g2f3 f6e6 h2h4 b5a5"},
        {"kid_rook_endgame", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 b8d7 c1e3 e7e5 f1d3 f6g4 d1d2 e5d4 f3d4"
                + " g4e3 f2e3 e8g8 e1c1 d7e5 c1b1 c8g4 d3e2 g4e2 d2e2 d8h4 h2h3 a8d8 b2b4 c7c5 b4c5 d6c5 d4b5 a7a6 b5c7"
                + " h4h5 d1d8 f8d8 e2f1 e5f3 f1f3 h5f3 g2f3 g7c3 c7d5 c3g7 b1c2 g6g5 f3f4 g5f4 e3f4 b7b5 c2d3 b5b4 f4f5"
                + " h7h5 h1g1 f7f6 e4e5 g8h7 e5f6 g7f6 g1g6 f6d4 g6a6 h5h4 a6c6 h7g7 c6c7 g7g8 c7b7 d8a8 d5e7 g8f7 e7c6"
                + " f7f6 c6d4 c5d4 d3e4 a8a2 b7b6 f6f7 b6b4 a2e2 e4d4 f7f6 b4b5 e2h2 d4e4 f6g5 e4e5 h2e2 e5d6 e2e3 c4c5"
                + " g5f6 c5c6 e3d3 b5d5 d3a3 c6c7 a3a8 d6d7 f6g5 c7c8q a8c8 d7c8 g5f4"},
        {"italian_mop_up", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d4 e5d4 e4e5 d7d5 e5f6 d5c4 f6g7 h8g8 c3d4 c6d4"
                + " c1h6 d4f3 d1f3 d8h4 h6e3 c5e3 f3e3 h4e7 b1a3 e7e3 f2e3 g8g7 a3b5 f7f5 e1g1 g7e7 a1c1 a7a6 b5d4 b7b5"
                + " c1e1 e7e5 d4f3 e5d5 f3g5 d5d2 f1f2 d2f2 g1f2 h7h6 g5f3 e8f7 f2g3 c8b7 g3f4 b7f3 g2f3 f7e6 e3e4 f5e4"
                + " f4e4 e6f6 e4f4 a8d8 e1e2 d8d4 f4e3 f6e5 b2b4 h6h5 e2e1 d4d3 e3f2 e5d4 e1e2 d4c3 a2a3 c3b3 e2e7 b3a3"
                + " h2h4 d3d4 e7c7 d4h4 c7a7 c4c3 a7a6 a3b2 f2g3 h4b4 f3f4 c3c2 a6c6 b4c4 c6b6 b5b4 b6f6 c2c1q g3h4 c4f4"
                + " f6f4 c1f4 h4h5 b4b3 h5g6 b2c3"},
        {"qgd_late_endgame", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7 g5f6 e7f6 c4d5 e6d5 e2e3 b8c6 d1b3 c6e7 f1d3"
                + " e8g8 g1f3 d8d6 e3e4 d5e4 c3e4 d6c6 e1g1 c8e6 e4f6 g7f6 d3b5 e6b3 b5c6 b3d5 c6d5 e7d5 a1e1 h7h5 e1e4"
                + " g8g7 e4e1 a8d8 a2a4 f6f5 f3e5 g7f6 a4a5 d5f4 e5f3 f8e8 e1d1 d8d5 g2g3 f4e2 g1g2 e8d8 d1e1 e2d4 f3d4"
                + " d5d4 e1c1 d8d7 c1b1 d4a4 b2b4 d7d4 f1c1 a4b4 b1b4 d4b4 c1c7 b4b5 g2f3 f6e6 h2h4 b5a5 c7b7 a5a3 f3f4"
                + " a3a2 f4e3 a2a4 f2f4 a4a3 e3d4 a3a4 d4c5 a4a6 b7c7 a6a2 c7c6 e6e7 c6h6 a7a5 c5d5 a2g2 h6h5 e7f6 h5h6"
                + " f6e7 h6a6 g2g3 d5e5 g3g4 h4h5 f7f6 a6f6 g4f4 h5h6 f4h4 f6a6 f5f4 e5e4 a5a4 h6h7 f4f3 e4f3 e7f7 a6a4"
                + " h4h3 f3e4 h3h4 e4d5 h4h5 d5c6 h5h6 c6d7 f7g7 a4d4 g7h7"},
    };

    private static final int maxReferenceDepth = 8;
    private static final int defaultBenchDepth = 8;

    private static final MoveGenerator moveGenerator = new MoveGenerator();

    public static void main(String[] args) throws Exception {
        if(args.length > 0 && args[0].equals("--bench")) {
            bench(args.length > 1 ? Integer.parseInt(args[1]) : defaultBenchDepth);
            return;
        }

        printLateMoveReductions();

        for(String[] entry : suite) {
            Position position = play(entry[1]);
            System.out.printf("{\"type\":\"zobrist\",\"id\":\"%s\",\"hash\":\"%016x\"}%n", entry[0], position.getHash());
        }

        for(String[] entry : suite) {
            for(int depth = 1; depth <= maxReferenceDepth; depth++) {
                Position position = play(entry[1]);
                short move = Engine.findBestMoveFixedDepth(position, depth);
                System.out.printf("{\"type\":\"search\",\"id\":\"%s\",\"moves\":\"%s\",\"depth\":%d,\"move\":%d,\"uci\":\"%s\","
                        + "\"score\":%d,\"completed\":%d,\"nodes\":%d}%n",
                        entry[0], entry[1], depth, move, moveName(move), Engine.getLastScore(), Engine.getLastDepth(),
                        Engine.getNodes());
            }
        }
    }

    //the engine keeps its table private; reading it rather than recomputing it here is what catches a
    //floating point difference between this JVM's Math.log and the C++ library's log
    private static void printLateMoveReductions() throws Exception {
        Field field = Engine.class.getDeclaredField("lateMoveReductions");
        field.setAccessible(true);
        int[][] table = (int[][]) field.get(null);

        //the C++ table only has 256 columns; no position has more legal moves than that
        for(int depth = 0; depth < table.length; depth++) {
            StringBuilder values = new StringBuilder();
            for(int index = 0; index < 256; index++) {
                values.append(index == 0 ? "" : ",").append(table[depth][index]);
            }
            System.out.printf("{\"type\":\"lmr\",\"depth\":%d,\"values\":[%s]}%n", depth, values);
        }
    }

    //runs the suite once to warm up the JIT, then again timed
    private static void bench(int depth) {
        for(String[] entry : suite) {
            Engine.findBestMoveFixedDepth(play(entry[1]), depth);
        }

        long totalNodes = 0;
        long totalNanos = 0;
        for(String[] entry : suite) {
            Position position = play(entry[1]);
            long start = System.nanoTime();
            Engine.findBestMoveFixedDepth(position, depth);
            long elapsed = System.nanoTime() - start;

            totalNodes += Engine.getNodes();
            totalNanos += elapsed;
            System.out.printf(Locale.ROOT, "%-20s depth %d  nodes %10d  time %8.1f ms%n", entry[0], depth, Engine.getNodes(),
                    elapsed / 1e6);
        }
        System.out.printf(Locale.ROOT, "java total: nodes %d  time %.1f ms  nps %.0f%n", totalNodes, totalNanos / 1e6,
                totalNodes / (totalNanos / 1e9));
    }

    private static Position play(String moves) {
        Position position = new Position();
        for(String text : moves.trim().split("\\s+")) {
            if(text.isEmpty()) {
                continue;
            }

            int start = square(text.substring(0, 2));
            int target = square(text.substring(2, 4));
            short move = text.length() > 4
                    ? moveGenerator.getLegalMove(start, target, promotionPiece(text.charAt(4)), position)
                    : moveGenerator.getLegalMove(start, target, position);
            if(move == Move.none) {
                throw new IllegalArgumentException("illegal move " + text + " in: " + moves);
            }
            position.playMove(move);
        }
        return position;
    }

    private static int promotionPiece(char letter) {
        switch(letter) {
            case 'q': return Piece.queen;
            case 'r': return Piece.rook;
            case 'b': return Piece.bishop;
            case 'n': return Piece.knight;
            default: throw new IllegalArgumentException("unknown promotion piece " + letter);
        }
    }

    //0 = a8, 63 = h1
    private static int square(String name) {
        return (name.charAt(0) - 'a') + 8 * ('8' - name.charAt(1));
    }

    private static String squareName(int square) {
        return "" + (char) ('a' + (square & 7)) + (char) ('8' - (square >>> 3));
    }

    private static String moveName(short move) {
        if(move == Move.none) {
            return "none";
        }
        String name = squareName(Move.startSquare(move)) + squareName(Move.targetSquare(move));
        if(Move.isPromotion(move)) {
            int piece = Move.promotionPiece(move);
            name += piece == Piece.queen ? "q" : piece == Piece.rook ? "r" : piece == Piece.bishop ? "b" : "n";
        }
        return name;
    }
}
