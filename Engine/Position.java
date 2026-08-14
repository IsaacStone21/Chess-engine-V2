package Engine;


public class Position {
    private boolean whiteToMove;
    private Piece[] pieces = new Piece[64];
    private Move lastMove;


    // sets values inside to false if a king or rook moves
    private boolean[] castlingCheck = new boolean[4];


    public Position() {
        pieces = FENUtil.FENtoPosition(FENUtil.startFEN);
        whiteToMove = true;

        for(int i = 0; i < 4; i++) {
            castlingCheck[i] = true; 
        }

        lastMove = null;
    }

    public Position(Position newPosition) {
        this.whiteToMove = newPosition.whiteToMove;
        this.lastMove = newPosition.lastMove;
        this.castlingCheck = newPosition.castlingCheck.clone();
        this.pieces = newPosition.pieces.clone();
    }


    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void switchTurns() {
        whiteToMove = !whiteToMove;
    }

    // public int getNumMoves() {
    //     return numMoves;
    // }

//    public void updateBoard(String FEN) {
//     pieces = FENUtil.FENtoPosition(FEN);
//    }

   public Piece getPieceAtIndex(int index) {
    return pieces[index];
   }

   public void edit(int index, Piece piece) {
    pieces[index] = piece;
   }

   public Piece[] getPosition() {
    return pieces;
   }

   public void playMove(Move acceptedMove) {
    lastMove = acceptedMove;
    int startIndex = acceptedMove.startSquare;
    int targetIndex = acceptedMove.targetSquare;

    if(acceptedMove.isEnPessant) {
        int offset = startIndex > targetIndex ? 8 : -8;

        edit(targetIndex + offset, Piece.emptyTile);
    }

    edit(targetIndex, getPieceAtIndex(startIndex));
    edit(startIndex, Piece.emptyTile);

    if(acceptedMove.isCastling) {
        boolean isWhite = startIndex == 60;
        Piece rook = isWhite ? new Piece(Piece.white, Piece.rook) : new Piece(Piece.black, Piece.rook);

        edit((startIndex + targetIndex) / 2, rook);

        boolean kingside = targetIndex == 6 || targetIndex == 62;
        int rookIndex = isWhite ? (kingside ? 63 : 56) : (kingside ? 7 : 0);

        edit(rookIndex, Piece.emptyTile);
    }

    if(acceptedMove.isPromotion) {
        edit(acceptedMove.targetSquare, new Piece(acceptedMove.targetSquare <= 7 ? Piece.white : Piece.black, Piece.queen));
    }

    updateCastlingCheck(acceptedMove);
    switchTurns();
   }

   public Move getLastMove() {
    return lastMove;
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
}
