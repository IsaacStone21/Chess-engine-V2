package EngineUtil;

public class Board {
    private Position position;
    private boolean playerIsWhite;

    //how long the engine thinks about each move; it searches as deep as it can in that time
    private static final long thinkTimeMillis = 1000;

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

    public boolean isEngineTurn() {
        return playerIsWhite != position.isWhiteToMove();
    }

    //the engine plays moves on whatever position it's searching, so a search on another thread needs its own
    //copy; take it on the thread that owns the board, before handing it to findEngineMove
    public Position copyPosition() {
        return new Position(position);
    }

    //a move from the opening book, or Move.none once the game has left it; instant, unlike findEngineMove
    public short findBookMove(Position snapshot) {
        return OpeningBook.getMove(snapshot);
    }

    //blocks for the think time, so call it off the GUI thread; returns Move.none if the game is already over
    public short findEngineMove(Position snapshot) {
        return Engine.findBestMove(snapshot, thinkTimeMillis);
    }

    //abandons the current search, e.g. when its game has been thrown away
    public void stopEngine() {
        Engine.stopSearch();
    }

    public void logMove(short acceptedMove) {
        position.playMove(acceptedMove);
    }

    public void undoMove() {
        position.undoMove();
    }


}
