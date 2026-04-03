package Engine;



public class Board {
    public boolean whiteToMove;
    public int numMoves;
    public Piece[] square = new Piece[64];



    public Board() {

    }

   
    
    public void setPositionFromFEN(String FEN) {
        square = FENUtil.FENtoPosition(FEN);
    }

    
}
