package Engine;

import java.util.ArrayList;
import java.util.List;

import Engine.MoveGenerator.Move;

public class Board {
    private boolean whiteToMove;
    private Piece[] square = new Piece[64];
    public List<Move> acceptedMoves;

    // sets values inside to false if a king or rook moves
    private boolean[] castlingCheck = new boolean[4];
   

    static Board board;

    private Board() {
        this.setPositionFromFEN(FENUtil.startFEN);
        acceptedMoves = new ArrayList<>();
        whiteToMove = true;

        for(int i = 0; i < 4; i++) {
            castlingCheck[i] = true; 
        }

    }


    public static Board createBoard() {
        if (board == null) {
            board = new Board();
        } 
        return board;
    }

    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void switchTurns() {
        whiteToMove = !whiteToMove;
    }


   public void updateBoard(String FEN) {
    square = FENUtil.FENtoPosition(FEN);
   }

   public Piece getPieceAtIndex(int index) {
    return square[index];
   }

   public void edit(int index, Piece piece) {
    square[index] = piece;
   }

   public Piece[] getPosition() {
    return square;
   }

   public void logMove(Move acceptedMove) {
    acceptedMoves.add(acceptedMove);
    updateCastlingCheck(acceptedMove);
   }

   private void updateCastlingCheck(Move move) {

    if(!(castlingCheck[0] || castlingCheck[1] || castlingCheck[2] || castlingCheck[3])) {
        return;
    }

    int blackKingStartIndex = 4;
    int blackQueensideRookIndex = 0;
    int blackKingsideRookIndex = 7;

    int whiteKingStartIndex = 60;
    int whiteQueensideRookIndex = 56;
    int whiteKingsideRookIndex = 63;

    int startIndex = move.startSquare;
    int targetIndex = move.targetSquare;

    boolean blackKingInvolved = blackKingStartIndex == startIndex || blackKingStartIndex == targetIndex;
    boolean whiteKingInvolved = whiteKingStartIndex == startIndex || whiteKingStartIndex == targetIndex;

    castlingCheck[0] = !(!castlingCheck[0] || blackKingInvolved || blackQueensideRookIndex == startIndex || blackQueensideRookIndex == targetIndex);
    castlingCheck[1] = !(!castlingCheck[1] || blackKingInvolved || blackKingsideRookIndex == startIndex || blackKingsideRookIndex == targetIndex);
    castlingCheck[2] = !(!castlingCheck[2] || whiteKingInvolved || whiteQueensideRookIndex == startIndex || whiteQueensideRookIndex == targetIndex);
    castlingCheck[3] = !(!castlingCheck[3] || blackKingInvolved || whiteKingsideRookIndex == startIndex || whiteKingsideRookIndex == targetIndex);
   }

   public boolean canCastle(int index) {
    return castlingCheck[index];
   }

    
    public void setPositionFromFEN(String FEN) {
        square = FENUtil.FENtoPosition(FEN);
    }

    
}
