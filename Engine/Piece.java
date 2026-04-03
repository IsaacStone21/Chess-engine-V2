package Engine;

public class Piece {
    public static final int white = 8;
    public static final int black = 16;

    public static final int pawn = 2;
    public static final int bishop = 1;
    public static final int knight = 4;
    public static final int rook = 3;
    public static final int queen = 5;
    public static final int king = 6;

    
    int ID;

    public Piece(int pieceColor, int pieceType) {
        ID = pieceColor | pieceType;
       
    }

    public boolean isType(int ID, int type) {
        return(ID%8) == type;
    }

    public boolean isWhite() {
        return (ID&8) == 8;
    }

    public boolean isSlidingPiece() {
        return (ID&1) == 1;
    }
}
