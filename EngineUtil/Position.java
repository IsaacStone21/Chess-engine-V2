package EngineUtil;


public class Position {
    private boolean whiteToMove;
    private Move lastMove;
    private Position lastPosition;

     long whitePawns;
    long whiteKnights;
    long whiteBishops;
    long whiteRooks;
    long whiteQueens;
    long whiteKing;

    long blackPawns;
    long blackKnights;
    long blackBishops;
    long blackRooks;
    long blackQueens;
    long blackKing;

    long whitePieces;
    long blackPieces;
    long occupied;

    // sets values inside to false if a king or rook moves
    private boolean[] castlingCheck = new boolean[4];


    public Position() {
        whiteToMove = true;


        whitePawns = 0x00FF000000000000L;
        whiteKnights = 0x4200000000000000L;
        whiteBishops = 0x2400000000000000L;
        whiteRooks = 0x8100000000000000L;
        whiteQueens = 0x0800000000000000L;
        whiteKing = 0x1000000000000000L;

        whitePieces = whitePawns | whiteKnights | whiteBishops |  whiteRooks | whiteQueens | whiteKing;

        blackPawns = 0x000000000000FF00L;
        blackKnights = 0x0000000000000042L;
        blackBishops = 0x0000000000000024L;
        blackRooks = 0x0000000000000081L;
        blackQueens = 0x0000000000000008L;
        blackKing = 0x0000000000000010L;

        blackPieces = blackPawns | blackKnights | blackBishops | blackRooks | blackQueens | blackKing;

        occupied = whitePieces | blackPieces;


        for(int i = 0; i < 4; i++) {
            castlingCheck[i] = true; 
        }

        lastMove = null;
    }

    public Position(Position newPosition) {
        this.whitePawns = newPosition.whitePawns;
        this.whiteBishops = newPosition.whiteBishops;
        this.whiteKnights = newPosition.whiteKnights;
        this.whiteRooks = newPosition.whiteRooks;
        this.whiteQueens = newPosition.whiteQueens;
        this.whiteKing = newPosition.whiteKing;

        this.blackPawns = newPosition.blackPawns;
        this.blackBishops = newPosition.blackBishops;
        this.blackKnights = newPosition.blackKnights;
        this.blackRooks = newPosition.blackRooks;
        this.blackQueens = newPosition.blackQueens;
        this.blackKing = newPosition.blackKing;

        this.whiteToMove = newPosition.whiteToMove;
        this.castlingCheck = newPosition.castlingCheck.clone();
    }


    public boolean isWhiteToMove() {
        return whiteToMove;
    }

    public void switchTurns() {
        whiteToMove = !whiteToMove;
    }


   public static long bit(int index) {
    return 1L << index;
   }

   public long getWhitePieces() {
    whitePieces = whitePawns | whiteKnights | whiteBishops |  whiteRooks | whiteQueens | whiteKing;
    return whitePieces;
   }

   public long getBlackPieces() {
    blackPieces = blackPawns | blackKnights | blackBishops |  blackRooks | blackQueens | blackKing;
    return blackPieces;
   }

   public long getOccupiedSquares() {
    occupied = getWhitePieces() | getBlackPieces();
    return occupied;
   }


//    public void playMove(Move acceptedMove) {
//     lastMove = acceptedMove;
//     lastPosition = new Position(this);
//     int startIndex = acceptedMove.startSquare;
//     int targetIndex = acceptedMove.targetSquare;

//     if(acceptedMove.isEnPessant) {
//         int offset = startIndex > targetIndex ? 8 : -8;

//         edit(targetIndex + offset, Piece.emptyTile);
//     }

//     edit(targetIndex, getPieceAtIndex(startIndex));
//     edit(startIndex, Piece.emptyTile);

//     if(acceptedMove.isCastling) {
//         boolean isWhite = startIndex == 60;
//         Piece rook = isWhite ? new Piece(Piece.white, Piece.rook) : new Piece(Piece.black, Piece.rook);

//         edit((startIndex + targetIndex) / 2, rook);

//         boolean kingside = targetIndex == 6 || targetIndex == 62;
//         int rookIndex = isWhite ? (kingside ? 63 : 56) : (kingside ? 7 : 0);

//         edit(rookIndex, Piece.emptyTile);
//     }

//     if(acceptedMove.isPromotion) {
//         edit(acceptedMove.targetSquare, new Piece(acceptedMove.targetSquare <= 7 ? Piece.white : Piece.black, Piece.queen));
//     }

//     updateCastlingCheck(acceptedMove);
//     switchTurns();
//    }

//     public void undoMove() {
//         Position previous = lastPosition;

//         this.castlingCheck = previous.castlingCheck.clone();
//         this.lastMove = previous.lastMove;
//         this.pieces = previous.pieces.clone();
//         this.whiteToMove = previous.whiteToMove;
//         this.lastPosition = previous.lastPosition;
//     }

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
