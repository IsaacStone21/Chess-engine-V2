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

    // square a pawn can capture onto en passant this turn, or -1 if none
    private int enPassantSquare = -1;


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

    // Copies every field needed to fully restore this position later, including the undo chain
    // itself (lastMove/lastPosition) so playMove/undoMove can be nested arbitrarily deep (e.g. a
    // search making several moves in a row before unwinding them one at a time).
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
        this.enPassantSquare = newPosition.enPassantSquare;

        this.lastMove = newPosition.lastMove;
        this.lastPosition = newPosition.lastPosition;
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

   //reconstructs a Piece object for the given square by checking each bitboard in turn
   public Piece getPieceAtIndex(int index) {
    long mask = bit(index);

    if((whitePawns & mask) != 0) return new Piece(Piece.white, Piece.pawn);
    if((whiteKnights & mask) != 0) return new Piece(Piece.white, Piece.knight);
    if((whiteBishops & mask) != 0) return new Piece(Piece.white, Piece.bishop);
    if((whiteRooks & mask) != 0) return new Piece(Piece.white, Piece.rook);
    if((whiteQueens & mask) != 0) return new Piece(Piece.white, Piece.queen);
    if((whiteKing & mask) != 0) return new Piece(Piece.white, Piece.king);

    if((blackPawns & mask) != 0) return new Piece(Piece.black, Piece.pawn);
    if((blackKnights & mask) != 0) return new Piece(Piece.black, Piece.knight);
    if((blackBishops & mask) != 0) return new Piece(Piece.black, Piece.bishop);
    if((blackRooks & mask) != 0) return new Piece(Piece.black, Piece.rook);
    if((blackQueens & mask) != 0) return new Piece(Piece.black, Piece.queen);
    if((blackKing & mask) != 0) return new Piece(Piece.black, Piece.king);

    return Piece.emptyTile;
   }

   //clears every bitboard's bit at this square - safe to call on an empty square
   private void clearSquare(int index) {
    long mask = ~bit(index);

    whitePawns &= mask;
    whiteKnights &= mask;
    whiteBishops &= mask;
    whiteRooks &= mask;
    whiteQueens &= mask;
    whiteKing &= mask;

    blackPawns &= mask;
    blackKnights &= mask;
    blackBishops &= mask;
    blackRooks &= mask;
    blackQueens &= mask;
    blackKing &= mask;
   }

   private void setSquare(int index, boolean white, int type) {
    long mask = bit(index);

    if(white) {
        switch(type) {
            case Piece.pawn -> whitePawns |= mask;
            case Piece.knight -> whiteKnights |= mask;
            case Piece.bishop -> whiteBishops |= mask;
            case Piece.rook -> whiteRooks |= mask;
            case Piece.queen -> whiteQueens |= mask;
            case Piece.king -> whiteKing |= mask;
        }
    } else {
        switch(type) {
            case Piece.pawn -> blackPawns |= mask;
            case Piece.knight -> blackKnights |= mask;
            case Piece.bishop -> blackBishops |= mask;
            case Piece.rook -> blackRooks |= mask;
            case Piece.queen -> blackQueens |= mask;
            case Piece.king -> blackKing |= mask;
        }
    }
   }

   public void playMove(Move acceptedMove) {
    //snapshot everything needed to undo, including the older chain link, before mutating this position
    Position snapshot = new Position(this);

    int startIndex = acceptedMove.startSquare;
    int targetIndex = acceptedMove.targetSquare;
    boolean white = whiteToMove;

    Piece movingPiece = getPieceAtIndex(startIndex);
    int movingType = movingPiece.ID % 8;

    if(acceptedMove.isEnPessant) {
        //the captured pawn sits one rank behind the target square, not on the target square itself
        int capturedPawnIndex = startIndex > targetIndex ? targetIndex + 8 : targetIndex - 8;
        clearSquare(capturedPawnIndex);
    }

    clearSquare(startIndex);
    clearSquare(targetIndex);
    setSquare(targetIndex, white, movingType);

    if(acceptedMove.isCastling) {
        boolean kingside = targetIndex == 6 || targetIndex == 62;
        int rookStart = white ? (kingside ? 63 : 56) : (kingside ? 7 : 0);
        int rookTarget = (startIndex + targetIndex) / 2;

        clearSquare(rookStart);
        setSquare(rookTarget, white, Piece.rook);
    }

    if(acceptedMove.isPromotion) {
        clearSquare(targetIndex);
        setSquare(targetIndex, white, Piece.queen);
    }

    //a double push leaves the skipped-over square open to en passant for exactly one turn
    enPassantSquare = (movingType == Piece.pawn && Math.abs(targetIndex - startIndex) == 16)
            ? (startIndex + targetIndex) / 2
            : -1;

    updateCastlingCheck(acceptedMove);
    switchTurns();

    lastMove = acceptedMove;
    lastPosition = snapshot;
   }

   public void undoMove() {
    Position previous = lastPosition;

    if(previous == null) {
        return;
    }

    this.whitePawns = previous.whitePawns;
    this.whiteKnights = previous.whiteKnights;
    this.whiteBishops = previous.whiteBishops;
    this.whiteRooks = previous.whiteRooks;
    this.whiteQueens = previous.whiteQueens;
    this.whiteKing = previous.whiteKing;

    this.blackPawns = previous.blackPawns;
    this.blackKnights = previous.blackKnights;
    this.blackBishops = previous.blackBishops;
    this.blackRooks = previous.blackRooks;
    this.blackQueens = previous.blackQueens;
    this.blackKing = previous.blackKing;

    this.whiteToMove = previous.whiteToMove;
    this.castlingCheck = previous.castlingCheck.clone();
    this.enPassantSquare = previous.enPassantSquare;

    this.lastMove = previous.lastMove;
    this.lastPosition = previous.lastPosition;
   }

   public int getEnPassantSquare() {
    return enPassantSquare;
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
    castlingCheck[3] = !(!castlingCheck[3] || whiteKingInvolved || whiteKingsideRookIndex == startIndex || whiteKingsideRookIndex == targetIndex);
   }

   public boolean canCastle(int index) {
    return castlingCheck[index];
   }
}
