package GUI;

import javax.swing.ImageIcon;

public class Piece extends ImageIcon{

    boolean whitePiece;
    boolean slidingPiece;
    
    public Piece(String filename, boolean isWhite, boolean isSlidingPiece) {
        super(filename);

        whitePiece = isWhite;
        slidingPiece = isSlidingPiece;
    }   
}
