package EngineUtil;

public class Board {
    private Position position;
    private boolean playerIsWhite;

    //plies the engine searches when picking its move
    private static final int searchDepth = 5;

    static Board board;

    private Board (){
        position = new Position();
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

    public void newGame() {
        position = new Position();
    }

    public void setPlayerColor(boolean playerWhite) {
        playerIsWhite = playerWhite;
    }

    public void playEngineMove() {
        if(playerIsWhite == position.isWhiteToMove()) {
            return;
        }

        short move = Engine.findBestMove(position, searchDepth);

        //no legal moves means the game is already over
        if(move == Move.none) {
            return;
        }

        logMove(move);
    }

    public void logMove(short acceptedMove) {
        position.playMove(acceptedMove);
    }

    public void undoMove() {
        position.undoMove();
    }


}
