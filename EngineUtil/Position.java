package EngineUtil;


public class Position {
    private boolean whiteToMove;

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

    //castling rights as bits: 0 = black queenside, 1 = black kingside, 2 = white queenside, 3 = white kingside
    private int castlingRights = 0b1111;

    //castlingKeepMask[square] = the rights that survive a move touching that square; moving a king or rook
    //off its start square (or capturing a rook on it) loses the matching rights
    private static final int[] castlingKeepMask = new int[64];

    // square a pawn can capture onto en passant this turn, or -1 if none
    private int enPassantSquare = -1;

    //undo stack: moveStack[i] is the i-th move played and undoStack[i] packs what that move destroyed:
    //  bits 0-2   captured piece type (Piece.empty if nothing was captured)
    //  bits 3-6   castling rights before the move
    //  bits 7-13  en passant square before the move, plus one (so -1 stores as 0)
    private short[] moveStack = new short[256];
    private int[] undoStack = new int[256];
    private int ply = 0;

    static {
        java.util.Arrays.fill(castlingKeepMask, 0b1111);
        castlingKeepMask[0] = ~0b0001;
        castlingKeepMask[7] = ~0b0010;
        castlingKeepMask[4] = ~0b0011;
        castlingKeepMask[56] = ~0b0100;
        castlingKeepMask[63] = ~0b1000;
        castlingKeepMask[60] = ~0b1100;
    }


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

   //returns the type of the given color's piece on this square, or Piece.empty
   private int pieceTypeAt(int index, boolean white) {
    long mask = bit(index);

    if(white) {
        if((whitePawns & mask) != 0) return Piece.pawn;
        if((whiteKnights & mask) != 0) return Piece.knight;
        if((whiteBishops & mask) != 0) return Piece.bishop;
        if((whiteRooks & mask) != 0) return Piece.rook;
        if((whiteQueens & mask) != 0) return Piece.queen;
        if((whiteKing & mask) != 0) return Piece.king;
    } else {
        if((blackPawns & mask) != 0) return Piece.pawn;
        if((blackKnights & mask) != 0) return Piece.knight;
        if((blackBishops & mask) != 0) return Piece.bishop;
        if((blackRooks & mask) != 0) return Piece.rook;
        if((blackQueens & mask) != 0) return Piece.queen;
        if((blackKing & mask) != 0) return Piece.king;
    }

    return Piece.empty;
   }

   //flips the given bits on one piece bitboard; used for both adding and removing, so make and unmake share it
   private void toggle(boolean white, int type, long mask) {
    if(white) {
        switch(type) {
            case Piece.pawn -> whitePawns ^= mask;
            case Piece.knight -> whiteKnights ^= mask;
            case Piece.bishop -> whiteBishops ^= mask;
            case Piece.rook -> whiteRooks ^= mask;
            case Piece.queen -> whiteQueens ^= mask;
            case Piece.king -> whiteKing ^= mask;
        }
    } else {
        switch(type) {
            case Piece.pawn -> blackPawns ^= mask;
            case Piece.knight -> blackKnights ^= mask;
            case Piece.bishop -> blackBishops ^= mask;
            case Piece.rook -> blackRooks ^= mask;
            case Piece.queen -> blackQueens ^= mask;
            case Piece.king -> blackKing ^= mask;
        }
    }
   }

   //the en passant victim sits one rank behind the target square, on the mover's side
   private static int capturedSquare(short move, boolean white) {
    int target = Move.targetSquare(move);
    if(!Move.isEnPessant(move)) {
        return target;
    }
    return white ? target + 8 : target - 8;
   }

   //bitboard with the rook's start and target squares for a castling move by the king to kingTarget
   private static long castlingRookMask(int kingStart, int kingTarget) {
    boolean kingside = kingTarget > kingStart;
    int rookStart = kingside ? kingStart + 3 : kingStart - 4;
    int rookTarget = (kingStart + kingTarget) / 2;
    return bit(rookStart) | bit(rookTarget);
   }

   public void playMove(short move) {
    int startIndex = Move.startSquare(move);
    int targetIndex = Move.targetSquare(move);
    boolean white = whiteToMove;

    int movingType = pieceTypeAt(startIndex, white);
    int captureIndex = capturedSquare(move, white);
    int capturedType = pieceTypeAt(captureIndex, !white);

    if(ply == moveStack.length) {
        moveStack = java.util.Arrays.copyOf(moveStack, ply * 2);
        undoStack = java.util.Arrays.copyOf(undoStack, ply * 2);
    }
    moveStack[ply] = move;
    undoStack[ply] = capturedType | (castlingRights << 3) | ((enPassantSquare + 1) << 7);
    ply++;

    if(capturedType != Piece.empty) {
        toggle(!white, capturedType, bit(captureIndex));
    }

    toggle(white, movingType, bit(startIndex));
    toggle(white, Move.isPromotion(move) ? Move.promotionPiece(move) : movingType, bit(targetIndex));

    if(Move.isCastling(move)) {
        toggle(white, Piece.rook, castlingRookMask(startIndex, targetIndex));
    }

    castlingRights &= castlingKeepMask[startIndex] & castlingKeepMask[targetIndex];

    //a double push leaves the skipped-over square open to en passant for exactly one turn
    enPassantSquare = (movingType == Piece.pawn && Math.abs(targetIndex - startIndex) == 16)
            ? (startIndex + targetIndex) / 2
            : -1;

    whiteToMove = !white;
   }

   public void undoMove() {
    if(ply == 0) {
        return;
    }

    ply--;
    short move = moveStack[ply];
    int undoInfo = undoStack[ply];

    whiteToMove = !whiteToMove;
    boolean white = whiteToMove;

    int startIndex = Move.startSquare(move);
    int targetIndex = Move.targetSquare(move);

    int placedType = pieceTypeAt(targetIndex, white);
    toggle(white, placedType, bit(targetIndex));
    toggle(white, Move.isPromotion(move) ? Piece.pawn : placedType, bit(startIndex));

    if(Move.isCastling(move)) {
        toggle(white, Piece.rook, castlingRookMask(startIndex, targetIndex));
    }

    int capturedType = undoInfo & 0b111;
    if(capturedType != Piece.empty) {
        toggle(!white, capturedType, bit(capturedSquare(move, white)));
    }

    castlingRights = (undoInfo >>> 3) & 0b1111;
    enPassantSquare = ((undoInfo >>> 7) & 0b1111111) - 1;
   }

   public int getEnPassantSquare() {
    return enPassantSquare;
   }

   //number of moves played that can still be undone
   public int getPly() {
    return ply;
   }

   public short getLastMove() {
    return ply == 0 ? Move.none : moveStack[ply - 1];
   }

   public boolean canCastle(int index) {
    return ((castlingRights >>> index) & 1) != 0;
   }
}
