package EngineUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Board {
    private Position position;
    private List<Move> acceptedMoves;
    private boolean playerIsWhite;
    private MoveGenerator moveGenerator;
    private Random random;

    static Board board;

    private Board (){
        position = new Position();
        acceptedMoves = new ArrayList<>();
        moveGenerator = new MoveGenerator();
        random = new Random();
    }

    public static Board createBoard() {
        if(board == null) {
            board = new Board();
        }

        return board;
    }

    public int getNumMoves() {
        return acceptedMoves.size();
    }

    public Position getPosition() {
        return position;
    }

    public void setPlayerColor(boolean playerWhite) {
        playerIsWhite = playerWhite;
    }

    public void playEngineMove() {
        if(playerIsWhite == position.isWhiteToMove()) {
            return;
        }

        var moves = moveGenerator.generateMoves(position);

        String color = position.isWhiteToMove() ? "White: " : "Black: ";

        System.out.println("Num Legal Moves for " + color + moves.size());

        Move move = moves.get(random.nextInt(moves.size()));

        logMove(move);
    }

    public void logMove(Move acceptedMove) {
        acceptedMoves.addLast(acceptedMove);
        position.playMove(acceptedMove);
    }

    public void undoMove() {
        position.undoMove();
        acceptedMoves.removeLast();
    }

    
}
