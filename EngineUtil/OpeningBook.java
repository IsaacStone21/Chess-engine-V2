package EngineUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

//well known opening lines for the engine to play from instead of searching, so games start like real games rather
//than the same few search-chosen moves every time. The lines are merged into a tree of moves from the starting
//position; the engine follows the tree along the moves already played and, while the game is still in it, picks
//one of the branches at random
public final class OpeningBook {
    //moves are written as start and target squares, e.g. "e2e4", and castling as the king's move, e.g. "e1g1".
    //Lines that share their first moves share those branches of the tree, so a position reached by more lines is
    //played more often: the main lines get several entries and the rarer openings just one
    private static final String[] lines = {
        //1.e4 e5
        "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6 e1g1 f8e7 f1e1 b7b5 a4b3 d7d6 c2c3 e8g8",     //Ruy Lopez, Closed
        "e2e4 e7e5 g1f3 b8c6 f1b5 g8f6 e1g1 f6e4 d2d4 e4d6 b5c6 d7c6 d4e5 d6f5 d1d8 e8d8",     //Ruy Lopez, Berlin
        "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5c6 d7c6 e1g1 f7f6",                                   //Ruy Lopez, Exchange
        "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6 d2d3 d7d6 e1g1 e8g8",                         //Italian Game
        "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 d2d3 f8e7 e1g1 e8g8",                                   //Two Knights, 4.d3
        "e2e4 e7e5 g1f3 b8c6 f1c4 g8f6 f3g5 d7d5 e4d5 c6a5 c4b5 c7c6 d5c6 b7c6 b5e2 h7h6",     //Two Knights, 4.Ng5
        "e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 g8f6 d4c6 b7c6 e4e5 d8e7 d1e2 f6d5 c2c4 c8a6",     //Scotch, Mieses
        "e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 f8c5 d4b3 c5b6 a2a4 a7a6",                         //Scotch, 4...Bc5
        "e2e4 e7e5 g1f3 g8f6 f3e5 d7d6 e5f3 f6e4 d2d4 d6d5 f1d3 b8c6 e1g1 f8e7",               //Petrov Defense
        "e2e4 e7e5 g1f3 b8c6 b1c3 g8f6 f1b5 f8b4 e1g1 e8g8 d2d3 d7d6",                         //Four Knights
        "e2e4 e7e5 g1f3 d7d6 d2d4 g8f6 b1c3 b8d7 f1c4 f8e7 e1g1 e8g8",                         //Philidor Defense
        "e2e4 e7e5 b1c3 g8f6 f1c4 b8c6 d2d3 f8b4 g1e2 d7d5",                                   //Vienna Game
        "e2e4 e7e5 f2f4 e5f4 g1f3 g7g5 h2h4 g5g4 f3e5 g8f6",                                   //King's Gambit Accepted
        "e2e4 e7e5 f2f4 f8c5 g1f3 d7d6 c2c3 g8f6",                                             //King's Gambit Declined

        //1.e4 c5
        "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6 c1e3 e7e5 d4b3 c8e6 f2f3 f8e7",     //Sicilian, Najdorf
        "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 g7g6 c1e3 f8g7 f2f3 e8g8 d1d2 b8c6",     //Sicilian, Dragon
        "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 b8c6 c1g5 e7e6 d1d2 a7a6 e1c1",          //Sicilian, Richter-Rauzer
        "e2e4 c7c5 g1f3 b8c6 d2d4 c5d4 f3d4 g8f6 b1c3 e7e5 d4b5 d7d6 c1g5 a7a6 b5a3 b7b5",     //Sicilian, Sveshnikov
        "e2e4 c7c5 g1f3 e7e6 d2d4 c5d4 f3d4 b8c6 b1c3 d8c7 f1e2 a7a6 e1g1 g8f6",               //Sicilian, Taimanov
        "e2e4 c7c5 g1f3 e7e6 d2d4 c5d4 f3d4 a7a6 f1d3 g8f6 e1g1 d8c7",                         //Sicilian, Kan
        "e2e4 c7c5 g1f3 b8c6 f1b5 g7g6 e1g1 f8g7 f1e1 e7e5",                                   //Sicilian, Rossolimo
        "e2e4 c7c5 g1f3 d7d6 f1b5 c8d7 b5d7 d8d7 e1g1 b8c6 c2c3 g8f6",                         //Sicilian, Moscow
        "e2e4 c7c5 c2c3 g8f6 e4e5 f6d5 d2d4 c5d4 g1f3 b8c6 c3d4 d7d6",                         //Sicilian, Alapin
        "e2e4 c7c5 b1c3 b8c6 g2g3 g7g6 f1g2 f8g7 d2d3 d7d6",                                   //Closed Sicilian

        //1.e4, other replies
        "e2e4 e7e6 d2d4 d7d5 b1c3 g8f6 c1g5 f8e7 e4e5 f6d7 g5e7 d8e7 f2f4 e8g8",               //French, Classical
        "e2e4 e7e6 d2d4 d7d5 b1c3 f8b4 e4e5 c7c5 a2a3 b4c3 b2c3 g8e7",                         //French, Winawer
        "e2e4 e7e6 d2d4 d7d5 b1d2 g8f6 e4e5 f6d7 f1d3 c7c5 c2c3 b8c6",                         //French, Tarrasch
        "e2e4 e7e6 d2d4 d7d5 e4e5 c7c5 c2c3 b8c6 g1f3 d8b6 a2a3 c5c4",                         //French, Advance
        "e2e4 e7e6 d2d4 d7d5 e4d5 e6d5 g1f3 g8f6 f1d3 f8d6 e1g1 e8g8",                         //French, Exchange
        "e2e4 c7c6 d2d4 d7d5 b1c3 d5e4 c3e4 c8f5 e4g3 f5g6 h2h4 h7h6 g1f3 b8d7",               //Caro-Kann, Classical
        "e2e4 c7c6 d2d4 d7d5 e4e5 c8f5 g1f3 e7e6 f1e2 c6c5 e1g1 b8c6",                         //Caro-Kann, Advance
        "e2e4 c7c6 d2d4 d7d5 e4d5 c6d5 f1d3 b8c6 c2c3 g8f6 c1f4 c8g4",                         //Caro-Kann, Exchange
        "e2e4 c7c6 b1c3 d7d5 g1f3 c8g4 h2h3 g4f3 d1f3 e7e6",                                   //Caro-Kann, Two Knights
        "e2e4 d7d5 e4d5 d8d5 b1c3 d5a5 d2d4 g8f6 g1f3 c8f5 f1c4 e7e6",                         //Scandinavian, 3...Qa5
        "e2e4 d7d5 e4d5 g8f6 d2d4 f6d5 g1f3 g7g6 f1e2 f8g7 e1g1 e8g8",                         //Scandinavian, 2...Nf6
        "e2e4 d7d6 d2d4 g8f6 b1c3 g7g6 g1f3 f8g7 f1e2 e8g8 e1g1 c7c6",                         //Pirc Defense
        "e2e4 g7g6 d2d4 f8g7 b1c3 d7d6 c1e3 a7a6 d1d2 b7b5",                                   //Modern Defense
        "e2e4 g8f6 e4e5 f6d5 d2d4 d7d6 g1f3 c8g4 f1e2 e7e6 e1g1 f8e7",                         //Alekhine Defense

        //1.d4
        "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7 e2e3 e8g8 g1f3 h7h6 g5h4 b7b6",               //Queen's Gambit Declined
        "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c4d5 e6d5 c1g5 c7c6 e2e3 f8e7 f1d3 b8d7",               //QGD, Exchange
        "d2d4 d7d5 c2c4 d5c4 g1f3 g8f6 e2e3 e7e6 f1c4 c7c5 e1g1 a7a6",                         //Queen's Gambit Accepted
        "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 d5c4 a2a4 c8f5 e2e3 e7e6 f1c4 f8b4",               //Slav Defense
        "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 e7e6 e2e3 b8d7 f1d3 d5c4 d3c4 b7b5",               //Semi-Slav, Meran
        "d2d4 d7d5 g1f3 g8f6 c1f4 e7e6 e2e3 c7c5 c2c3 b8c6 b1d2 f8d6",                         //London System
        "d2d4 g8f6 c1f4 g7g6 e2e3 f8g7 g1f3 e8g8 f1e2 d7d6 h2h3 b8d7",                         //London vs. King's Indian
        "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6 g1f3 e8g8 f1e2 e7e5 e1g1 b8c6 d4d5 c6e7",     //King's Indian Defense
        "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 e2e3 e8g8 f1d3 d7d5 g1f3 c7c5 e1g1 b8c6",               //Nimzo-Indian, Rubinstein
        "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 d1c2 e8g8 a2a3 b4c3 c2c3 b7b6",                         //Nimzo-Indian, Classical
        "d2d4 g8f6 c2c4 e7e6 g1f3 b7b6 g2g3 c8b7 f1g2 f8e7 e1g1 e8g8 b1c3 f6e4",               //Queen's Indian Defense
        "d2d4 g8f6 c2c4 e7e6 g2g3 d7d5 f1g2 f8e7 g1f3 e8g8 e1g1 d5c4",                         //Catalan
        "d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 c4d5 f6d5 e2e4 d5c3 b2c3 f8g7 f1c4 c7c5 g1e2 b8c6",     //Grunfeld, Exchange
        "d2d4 g8f6 c2c4 c7c5 d4d5 e7e6 b1c3 e6d5 c4d5 d7d6 e2e4 g7g6 g1f3 f8g7",               //Modern Benoni
        "d2d4 g8f6 c2c4 c7c5 d4d5 b7b5 c4b5 a7a6 b5a6 g7g6 b1c3 c8a6",                         //Benko Gambit
        "d2d4 f7f5 g2g3 g8f6 f1g2 g7g6 g1f3 f8g7 e1g1 e8g8 c2c4 d7d6",                         //Dutch, Leningrad

        //flank openings
        "c2c4 e7e5 b1c3 g8f6 g2g3 d7d5 c4d5 f6d5 f1g2 d5b6 g1f3 b8c6",                         //English, Reversed Sicilian
        "c2c4 c7c5 g1f3 b8c6 b1c3 g7g6 g2g3 f8g7 f1g2 g8f6 e1g1 e8g8",                         //English, Symmetrical
        "c2c4 g8f6 b1c3 e7e5 g1f3 b8c6 g2g3 d7d5 c4d5 f6d5 f1g2 d5b6",                         //English, Four Knights
        "g1f3 d7d5 g2g3 g8f6 f1g2 e7e6 e1g1 f8e7 d2d3 e8g8 b1d2 c7c5",                         //Reti Opening
        "g1f3 d7d5 g2g3 c7c5 f1g2 b8c6 e1g1 e7e5 d2d3 g8f6",                                   //King's Indian Attack
        "g1f3 g8f6 c2c4 g7g6 b1c3 d7d5 c4d5 f6d5 d1a4 c8d7",                                   //Reti, Grunfeld setup
        "b2b3 e7e5 c1b2 b8c6 e2e3 d7d5",                                                       //Larsen's Opening
        "f2f4 d7d5 g1f3 g8f6 e2e3 g7g6",                                                       //Bird's Opening
        "g2g3 d7d5 f1g2 g8f6 g1f3 c7c6",                                                       //King's Fianchetto
    };

    //one position in the tree; children[i] is the position after moves[i]
    private static final class Node {
        final List<Short> moves = new ArrayList<>();
        final List<Node> children = new ArrayList<>();
        //how many book lines pass through this position, which is how likely it is to be picked
        int weight;

        Node child(short move) {
            int index = moves.indexOf(move);
            return index < 0 ? null : children.get(index);
        }
    }

    //the starting position
    private static final Node root = new Node();

    static {
        //moves are stored exactly as the move generator encodes them (castling and en passant flags included),
        //so they compare equal to the moves the game actually played
        MoveGenerator moveGenerator = new MoveGenerator();

        for(String line : lines) {
            Position position = new Position();
            Node node = root;

            for(String text : line.trim().split("\\s+")) {
                short move = moveGenerator.getLegalMove(square(text.substring(0, 2)), square(text.substring(2, 4)), position);

                //a typo in a line shouldn't stop the engine from playing; the line is just cut short there
                if(move == Move.none) {
                    System.err.println("Opening book: " + text + " is illegal in \"" + line + "\"; the line stops there");
                    break;
                }

                Node child = node.child(move);
                if(child == null) {
                    child = new Node();
                    node.moves.add(move);
                    node.children.add(child);
                }
                child.weight++;

                position.playMove(move);
                node = child;
            }
        }
    }

    private OpeningBook() {}

    //a book move for this position, or Move.none once the game has left the book. Only the moves played so far
    //matter, so a game that reaches a book position through a different move order counts as out of book
    public static short getMove(Position position) {
        Node node = root;
        for(int index = 0; index < position.getPly() && node != null; index++) {
            node = node.child(position.getMove(index));
        }

        if(node == null || node.children.isEmpty()) {
            return Move.none;
        }

        int totalWeight = 0;
        for(Node child : node.children) {
            totalWeight += child.weight;
        }

        //lands in branch i with probability weight[i] / totalWeight
        int pick = ThreadLocalRandom.current().nextInt(totalWeight);
        for(int index = 0; index < node.children.size(); index++) {
            pick -= node.children.get(index).weight;
            if(pick < 0) {
                return node.moves.get(index);
            }
        }
        return Move.none;
    }

    //square indices start at a8, so ranks count down as the index goes up
    private static int square(String name) {
        return (7 - (name.charAt(1) - '1')) * 8 + (name.charAt(0) - 'a');
    }
}
