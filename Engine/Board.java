package Engine;



public class Board {
    public boolean whiteToMove;
    public int numMoves;
    public Piece[] square = new Piece[64];

    static Board board;

    private Board() {
        this.setPositionFromFEN(FENUtil.startFEN);
    }


    public static Board createBoard() {
        if (board == null) {
            board = new Board();
        } 
        return board;
    }


   public void updateBoard(String FEN) {
    square = FENUtil.FENtoPosition(FEN);
   }
    
    public void setPositionFromFEN(String FEN) {
        square = FENUtil.FENtoPosition(FEN);
    }

    
}
