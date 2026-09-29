package EngineUtil;

import java.util.Random;

public class Board {
    private Position position;
    private boolean playerIsWhite;
    private MoveGenerator moveGenerator;
    private Random random;
    private short[] moveBuffer;

    static Board board;

    private Board (){
        position = new Position();
        moveGenerator = new MoveGenerator();
        random = new Random();
        moveBuffer = new short[MoveGenerator.maxMoves];
    }

    public static Board createBoard() {
        if(board == null) {
            board = new Board();
        }

        return board;
    }

    //the position's undo stack doubles as the game's move history
    public int getNumMoves() {
        return position.getPly();
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

        int numMoves = moveGenerator.generateMoves(position, moveBuffer);

        String color = position.isWhiteToMove() ? "White: " : "Black: ";

        System.out.println("Num Legal Moves for " + color + numMoves);

        short move = moveBuffer[random.nextInt(numMoves)];

        logMove(move);
    }

    public void logMove(short acceptedMove) {
        position.playMove(acceptedMove);
    }

    public void undoMove() {
        position.undoMove();
    }


}
