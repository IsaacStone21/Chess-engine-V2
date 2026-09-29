package EngineUtil;

import java.awt.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Engine {
    //A negative number indicate the position is favorable for black 

    private static Map<Integer, Integer> pieceToEvalMap = new HashMap<>();
    private static MoveGenerator moveGenerator;
    private static Board board;

    static {
        pieceToEvalMap.put(Piece.white | Piece.pawn, 1);
        pieceToEvalMap.put(Piece.white | Piece.bishop, 3);
        pieceToEvalMap.put(Piece.white | Piece.knight, 3);
        pieceToEvalMap.put(Piece.white | Piece.rook, 5);
        pieceToEvalMap.put(Piece.white | Piece.queen, 9);
        pieceToEvalMap.put(Piece.white | Piece.king, 10000);

        pieceToEvalMap.put(Piece.black | Piece.pawn, -1);
        pieceToEvalMap.put(Piece.black | Piece.bishop, -3);
        pieceToEvalMap.put(Piece.black | Piece.knight, -3);
        pieceToEvalMap.put(Piece.black | Piece.rook, -5);
        pieceToEvalMap.put(Piece.black | Piece.queen, -9);
        pieceToEvalMap.put(Piece.black | Piece.king, -10000);

        pieceToEvalMap.put(Piece.empty, 0);

        moveGenerator = new MoveGenerator();
        board = Board.createBoard();
    }

    public static void initiateEngine(){
        new Engine();
    }



    public static double getPositionEval(Position position) {
        double eval = 0;

        for (int index = 0; index < 64; index++) {
            eval += pieceToEvalMap.get(position.getPieceAtIndex(index).ID);
        }

        return eval;
    } 


    //one move buffer per remaining depth, so the recursion never allocates
    private static short[][] moveBuffers = new short[0][];

    public static long getNumPossiblePositions(int depth) {
        if(moveBuffers.length < depth) {
            moveBuffers = new short[depth][MoveGenerator.maxMoves];
        }

        return countPositions(board.getPosition(), depth);
    }

    private static long countPositions(Position position, int depth) {
        if(depth == 0) {
            return 1;
        }

        short[] moves = moveBuffers[depth - 1];
        long numMoves = moveGenerator.generateMoves(position, moves);
        long numPositions = 0;

        for(int index = 0; index < numMoves; index++) {
            position.playMove(moves[index]);
            numPositions += countPositions(position, depth - 1);
            position.undoMove();
        }

        return numPositions;
    }
    
}
