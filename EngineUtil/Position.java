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

    //plies since the last capture or pawn move, for the fifty-move rule; only positions inside this window
    //can repeat, since captures and pawn moves can never be undone
    private int halfmoveClock = 0;

    //undo stack: moveStack[i] is the i-th move played and undoStack[i] packs what that move destroyed:
    //  bits 0-2   captured piece type (Piece.empty if nothing was captured)
    //  bits 3-6   castling rights before the move
    //  bits 7-13  en passant square before the move, plus one (so -1 stores as 0)
    //  bits 14-31 halfmove clock before the move
    //hashHistory[i] is the hash of the position the i-th move was played from, which is what repetitions are found in
    private short[] moveStack = new short[256];
    private int[] undoStack = new int[256];
    private long[] hashHistory = new long[256];
    private int ply = 0;

    //Zobrist hashing: every piece on every square, every combination of castling rights, every en passant file and
    //the side to move gets a random 64-bit key, and a position's hash is the XOR of the keys of everything in it.
    //XOR undoes itself, so a move updates the hash by XORing out what changed and XORing in what replaced it,
    //and two positions with the same hash are, for practical purposes, the same position
    private long hash;

    //pieceKeys[color offset + piece type][square], with white's offset 0 and black's 7 (piece types are 1-6)
    private static final long[][] pieceKeys = new long[14][64];
    private static final long[] castlingKeys = new long[16];
    private static final long[] enPassantKeys = new long[8];
    private static final long blackToMoveKey;

    static {
        java.util.Arrays.fill(castlingKeepMask, 0b1111);
        castlingKeepMask[0] = ~0b0001;
        castlingKeepMask[7] = ~0b0010;
        castlingKeepMask[4] = ~0b0011;
        castlingKeepMask[56] = ~0b0100;
        castlingKeepMask[63] = ~0b1000;
        castlingKeepMask[60] = ~0b1100;

        //a fixed seed, so a position hashes the same every run
        java.util.SplittableRandom random = new java.util.SplittableRandom(0x2C1D_E7A3_9B44_F00DL);
        for(long[] keys : pieceKeys) {
            for(int square = 0; square < 64; square++) {
                keys[square] = random.nextLong();
            }
        }
        for(int rights = 0; rights < 16; rights++) {
            castlingKeys[rights] = random.nextLong();
        }
        for(int file = 0; file < 8; file++) {
            enPassantKeys[file] = random.nextLong();
        }
        blackToMoveKey = random.nextLong();
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

        hash = computeHash();
    }

    //independent copy, so the engine can play moves on it from another thread without touching the original
    public Position(Position other) {
        whiteToMove = other.whiteToMove;

        whitePawns = other.whitePawns;
        whiteKnights = other.whiteKnights;
        whiteBishops = other.whiteBishops;
        whiteRooks = other.whiteRooks;
        whiteQueens = other.whiteQueens;
        whiteKing = other.whiteKing;

        blackPawns = other.blackPawns;
        blackKnights = other.blackKnights;
        blackBishops = other.blackBishops;
        blackRooks = other.blackRooks;
        blackQueens = other.blackQueens;
        blackKing = other.blackKing;

        whitePieces = other.whitePieces;
        blackPieces = other.blackPieces;
        occupied = other.occupied;

        castlingRights = other.castlingRights;
        enPassantSquare = other.enPassantSquare;
        halfmoveClock = other.halfmoveClock;

        moveStack = other.moveStack.clone();
        undoStack = other.undoStack.clone();
        hashHistory = other.hashHistory.clone();
        ply = other.ply;
        hash = other.hash;
    }

    //builds the hash from nothing; playMove keeps it up to date incrementally, so this only runs when a
    //position is created (and in tests, to check the incremental updates)
    long computeHash() {
        long key = pieceHash(whitePawns, true, Piece.pawn) ^ pieceHash(whiteKnights, true, Piece.knight)
                ^ pieceHash(whiteBishops, true, Piece.bishop) ^ pieceHash(whiteRooks, true, Piece.rook)
                ^ pieceHash(whiteQueens, true, Piece.queen) ^ pieceHash(whiteKing, true, Piece.king)
                ^ pieceHash(blackPawns, false, Piece.pawn) ^ pieceHash(blackKnights, false, Piece.knight)
                ^ pieceHash(blackBishops, false, Piece.bishop) ^ pieceHash(blackRooks, false, Piece.rook)
                ^ pieceHash(blackQueens, false, Piece.queen) ^ pieceHash(blackKing, false, Piece.king);

        key ^= castlingKeys[castlingRights];
        if(enPassantSquare != -1) {
            key ^= enPassantKeys[enPassantSquare & 7];
        }
        if(!whiteToMove) {
            key ^= blackToMoveKey;
        }
        return key;
    }

    //XOR of the keys for one piece type standing on every square in `pieces`
    private static long pieceHash(long pieces, boolean white, int type) {
        long[] keys = pieceKeys[(white ? 0 : 7) + type];
        long key = 0;
        while(pieces != 0) {
            key ^= keys[Long.numberOfTrailingZeros(pieces)];
            pieces &= pieces - 1;
        }
        return key;
    }

    public long getHash() {
        return hash;
    }

    public int getHalfmoveClock() {
        return halfmoveClock;
    }

    //true if this exact position, with the same side to move, already came up earlier in the game or the
    //search line leading here. Only positions since the last capture or pawn move can match, and only every
    //other one has the same side to move
    public boolean isRepetition() {
        int earliest = Math.max(0, ply - halfmoveClock);
        for(int index = ply - 2; index >= earliest; index -= 2) {
            if(hashHistory[index] == hash) {
                return true;
            }
        }
        return false;
    }

    //true once this position has come up for the third time, which ends the game in a draw. The search treats
    //the first repeat as a draw already (isRepetition), since a side that can repeat once can usually repeat again
    public boolean isThreefoldRepetition() {
        int occurrences = 1;
        int earliest = Math.max(0, ply - halfmoveClock);
        for(int index = ply - 2; index >= earliest; index -= 2) {
            if(hashHistory[index] == hash && ++occurrences == 3) {
                return true;
            }
        }
        return false;
    }

    //every light square; a8 (index 0) is light, and the colors alternate along each rank and between ranks
    private static final long lightSquares = 0xAA55AA55AA55AA55L;

    //true when neither side could checkmate however badly the other played: bare kings, a single bishop or
    //knight, or nothing but bishops that all stand on the same color of square
    public boolean isInsufficientMaterial() {
        if((whitePawns | blackPawns | whiteRooks | blackRooks | whiteQueens | blackQueens) != 0) {
            return false;
        }

        long minors = whiteKnights | blackKnights | whiteBishops | blackBishops;
        if(Long.bitCount(minors) <= 1) {
            return true;
        }

        //same-colored bishops can never cover the squares of the other color that a mating net needs
        if((whiteKnights | blackKnights) == 0) {
            long bishops = whiteBishops | blackBishops;
            return (bishops & lightSquares) == 0 || (bishops & ~lightSquares) == 0;
        }
        return false;
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
   int pieceTypeAt(int index, boolean white) {
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

   //flips the given bits on one piece bitboard; used for both adding and removing, so make and unmake share it.
   //It also XORs the matching keys into the hash, which keeps the hash in step with every piece change
   private void toggle(boolean white, int type, long mask) {
    long[] keys = pieceKeys[(white ? 0 : 7) + type];
    for(long bits = mask; bits != 0; bits &= bits - 1) {
        hash ^= keys[Long.numberOfTrailingZeros(bits)];
    }

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
        hashHistory = java.util.Arrays.copyOf(hashHistory, ply * 2);
    }
    moveStack[ply] = move;
    undoStack[ply] = capturedType | (castlingRights << 3) | ((enPassantSquare + 1) << 7) | (halfmoveClock << 14);
    hashHistory[ply] = hash;
    ply++;

    //castling rights and en passant are about to change; their old keys come out here and the new ones go in below
    hash ^= castlingKeys[castlingRights];
    if(enPassantSquare != -1) {
        hash ^= enPassantKeys[enPassantSquare & 7];
    }

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

    halfmoveClock = (movingType == Piece.pawn || capturedType != Piece.empty) ? 0 : halfmoveClock + 1;

    hash ^= castlingKeys[castlingRights];
    if(enPassantSquare != -1) {
        hash ^= enPassantKeys[enPassantSquare & 7];
    }
    hash ^= blackToMoveKey;

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
    halfmoveClock = undoInfo >>> 14;

    //the toggles above already undid the piece keys, but restoring the saved hash also covers castling,
    //en passant and the side to move in one step
    hash = hashHistory[ply];
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
