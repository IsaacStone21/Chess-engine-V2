package EngineUtil;

import java.util.ArrayList;
import java.util.List;


public class MoveGenerator {
    //Up, down, left, right, UL, UR, DL, DR
    int[] directionOffsets = {-8, 8, -1, 1, -9, -7, 7, 9};

    // UUR, URR, DRR, DDR, DDL, DLL, ULL, UUL
    int[] knightOffsets = {-15, -6, 10, 17, 15, 6, -10, -17};
    long[] knightAttackTable = new long[64];

    //contains the number of squares to the edge in each direction
    //first number is the index
    //second is the direction: Up, down, left, right, UL, UR, DL, DR
    int[][] numSquaresToEdge = new int[64][8];

    long[] kingAttackTable = new long[64];

    //attacks[square] = squares a pawn of that color on `square` attacks; used both to generate pawn
    //captures elsewhere and, via the symmetry trick in isSquareAttacked, to detect check
    long[] whitePawnAttackTable = new long[64];
    long[] blackPawnAttackTable = new long[64];


    private static final short whiteKingsideCastle = Move.castling(60, 62);
    private static final short whiteQueensideCastle = Move.castling(60, 58);
    private static final short blackKingsideCastle = Move.castling(4, 6);
    private static final short blackQueensideCastle = Move.castling(4, 2);

    //the most legal moves any position has is 218; pseudo-legal lists can be a bit longer, so this leaves
    //generous headroom while still costing only 1KB per buffer
    public static final int maxMoves = 512;

    //buffer for the single-shot helpers (getLegalMove, getNumLegalMoves); searches should pass their own per-depth buffers
    private final short[] scratchMoves = new short[maxMoves];

    List<Long> times = new ArrayList<>();

    //between[a][b] = squares strictly between a and b if they share a rank, file or diagonal, otherwise 0
    long[][] between = new long[64][64];

    //check and pin state for the side to move, filled in by computeCheckAndPins at the start of generateMoves
    private int kingSquare;
    //squares a non-king move must land on: everything when not in check, the checker plus the squares
    //between it and the king in single check, nothing in double check
    private long checkMask;
    //our pieces pinned to our king, and for each pinned piece the squares it may still move to
    private long pinned;
    private final long[] pinRay = new long[64];


    public MoveGenerator() {
        generateSquaresToEdge();
        generateKnightTable();
        generateKingTable();
        generatePawnAttackTables();
        generateBetweenTable();
    }

    private void generateBetweenTable() {
        for(int from = 0; from < 64; from++) {
            for(int dir = 0; dir < 8; dir++) {
                long ray = 0L;
                int square = from;

                for(int step = 0; step < numSquaresToEdge[from][dir]; step++) {
                    square += directionOffsets[dir];
                    //everything passed so far, excluding both ends
                    between[from][square] = ray;
                    ray |= Position.bit(square);
                }
            }
        }
    }

    private void generateSquaresToEdge() {
         for (int index = 0; index < 64; index++) {
            int row = (index - (index%8)) / 8;
            int col = index - 8*row;

            int numSquaresNorth = row;
            int numSquaresSouth = 7 - row;
            int numSquaresEast = col;
            int numSquaresWest = 7 - col;

            numSquaresToEdge[index][0] = numSquaresNorth;
            numSquaresToEdge[index][1] = numSquaresSouth;
            numSquaresToEdge[index][2] = numSquaresEast;
            numSquaresToEdge[index][3] = numSquaresWest;
            numSquaresToEdge[index][4] = Math.min(numSquaresNorth, numSquaresEast);
            numSquaresToEdge[index][5] = Math.min(numSquaresNorth, numSquaresWest);
            numSquaresToEdge[index][6] = Math.min(numSquaresSouth, numSquaresEast);
            numSquaresToEdge[index][7] = Math.min(numSquaresSouth, numSquaresWest);
        }
    }

    private void generateKnightTable() {
        for(int i = 0; i < 64; i++) {
            long attackableSquares = 0x0000000000000000L;

            for(int j = 0; j < 8; j++) {
                int targetIndex = i + knightOffsets[j];
                if(targetIndex < 64 && targetIndex >= 0 && !wrapsBoard(i, targetIndex)) {
                    attackableSquares |= Position.bit(targetIndex);
                }
            }

            knightAttackTable[i] = attackableSquares;
        }
    }

    //checks if the move doesnt wrap around the board
    private boolean wrapsBoard(int startingIndex, int targetIndex) {


        int startRow = (startingIndex - (startingIndex % 8)) / 8;
        int startCol = startingIndex - 8 * startRow;

        int attackedRow = (targetIndex - (targetIndex % 8)) / 8;
        int attackedCol = targetIndex - 8 * attackedRow;

        return Math.abs(attackedCol - startCol) > 2;
    }

    private void generateKingTable() {
        for(int i = 0; i < 64; i++) {
            long attackableSquares = 0L;

            for(int dir = 0; dir < 8; dir++) {
                if(numSquaresToEdge[i][dir] > 0) {
                    attackableSquares |= Position.bit(i + directionOffsets[dir]);
                }
            }

            kingAttackTable[i] = attackableSquares;
        }
    }

    private void generatePawnAttackTables() {
        for(int i = 0; i < 64; i++) {
            long whiteAttacks = 0L;
            long blackAttacks = 0L;

            //white pawns attack UL/UR (dir 4/5); black pawns attack DL/DR (dir 6/7)
            if(numSquaresToEdge[i][4] > 0) whiteAttacks |= Position.bit(i + directionOffsets[4]);
            if(numSquaresToEdge[i][5] > 0) whiteAttacks |= Position.bit(i + directionOffsets[5]);
            if(numSquaresToEdge[i][6] > 0) blackAttacks |= Position.bit(i + directionOffsets[6]);
            if(numSquaresToEdge[i][7] > 0) blackAttacks |= Position.bit(i + directionOffsets[7]);

            whitePawnAttackTable[i] = whiteAttacks;
            blackPawnAttackTable[i] = blackAttacks;
        }
    }

    //sliding attacks via magic bitboard lookup (see MagicBitboards); `occupied` blockers are included in the result
    private long getSlidingAttacks(int square, long occupied, boolean orthogonal, boolean diagonal) {
        long attacks = 0L;

        if (orthogonal) {
            attacks |= MagicBitboards.rookAttacks(square, occupied);
        }

        if (diagonal) {
            attacks |= MagicBitboards.bishopAttacks(square, occupied);
        }

        return attacks;
    }

    //bitboard of every `byWhite` piece attacking `square`, using `occupied` as the blockers. Works by placing
    //each attacker type on `square` and checking whether its attack pattern hits an actual enemy piece of that
    //type - attacks are symmetric, so this is equivalent to asking which enemy pieces attack `square` directly.
    //Taking occupancy as a parameter lets king moves be tested with the king itself lifted off the board.
    public long attackersOf(Position position, int square, boolean byWhite, long occupied) {
        long enemyQueens = byWhite ? position.whiteQueens : position.blackQueens;
        long enemyRooks = (byWhite ? position.whiteRooks : position.blackRooks) | enemyQueens;
        long enemyBishops = (byWhite ? position.whiteBishops : position.blackBishops) | enemyQueens;

        //a pawn of `byWhite`'s color attacks `square` iff `square` shows up in the opposite-colored
        //pawn attack table centered on `square` itself (the symmetry trick mentioned above)
        long pawnAttackersFromSquare = byWhite ? blackPawnAttackTable[square] : whitePawnAttackTable[square];

        return (pawnAttackersFromSquare & (byWhite ? position.whitePawns : position.blackPawns))
             | (knightAttackTable[square] & (byWhite ? position.whiteKnights : position.blackKnights))
             | (kingAttackTable[square] & (byWhite ? position.whiteKing : position.blackKing))
             | (MagicBitboards.rookAttacks(square, occupied) & enemyRooks)
             | (MagicBitboards.bishopAttacks(square, occupied) & enemyBishops);
    }

    public boolean isSquareAttacked(Position position, int square, boolean byWhite) {
        return isSquareAttacked(position, square, byWhite, position.getOccupiedSquares());
    }

    //same as attackersOf(...) != 0, but bails out at the first attacker found
    public boolean isSquareAttacked(Position position, int square, boolean byWhite, long occupied) {
        long enemyQueens = byWhite ? position.whiteQueens : position.blackQueens;
        long enemyRooks = (byWhite ? position.whiteRooks : position.blackRooks) | enemyQueens;
        long enemyBishops = (byWhite ? position.whiteBishops : position.blackBishops) | enemyQueens;

        long pawnAttackersFromSquare = byWhite ? blackPawnAttackTable[square] : whitePawnAttackTable[square];
        if((pawnAttackersFromSquare & (byWhite ? position.whitePawns : position.blackPawns)) != 0) return true;

        if((knightAttackTable[square] & (byWhite ? position.whiteKnights : position.blackKnights)) != 0) return true;
        if((kingAttackTable[square] & (byWhite ? position.whiteKing : position.blackKing)) != 0) return true;
        if((MagicBitboards.rookAttacks(square, occupied) & enemyRooks) != 0) return true;
        if((MagicBitboards.bishopAttacks(square, occupied) & enemyBishops) != 0) return true;

        return false;
    }

    public boolean inCheck(Position position) {
        boolean white = position.isWhiteToMove();
        long king = white ? position.whiteKing : position.blackKing;

        if(king == 0) {
            return false;
        }

        int kingSquare = Long.numberOfTrailingZeros(king);
        return isSquareAttacked(position, kingSquare, !white);
    }

    //fills kingSquare, checkMask, pinned and pinRay for the side to move
    private void computeCheckAndPins(Position position) {
        boolean white = position.isWhiteToMove();
        long king = white ? position.whiteKing : position.blackKing;

        pinned = 0L;

        //positions without a king (only used for testing) have nothing to protect
        if(king == 0) {
            kingSquare = -1;
            checkMask = ~0L;
            return;
        }

        kingSquare = Long.numberOfTrailingZeros(king);
        long occupied = position.getOccupiedSquares();
        long friendlyPieces = white ? position.getWhitePieces() : position.getBlackPieces();
        long enemyPieces = white ? position.getBlackPieces() : position.getWhitePieces();

        long checkers = attackersOf(position, kingSquare, !white, occupied);

        if(checkers == 0) {
            checkMask = ~0L;
        } else if(Long.bitCount(checkers) == 1) {
            //capture the checker or step into the line between it and the king (empty for knights and pawns)
            checkMask = checkers | between[kingSquare][Long.numberOfTrailingZeros(checkers)];
        } else {
            checkMask = 0L;
        }

        long enemyQueens = white ? position.blackQueens : position.whiteQueens;
        long enemyRooks = (white ? position.blackRooks : position.whiteRooks) | enemyQueens;
        long enemyBishops = (white ? position.blackBishops : position.whiteBishops) | enemyQueens;

        //looking out from the king with only enemy pieces as blockers sees straight through our own pieces,
        //so this finds every enemy slider lined up with the king
        long snipers = (MagicBitboards.rookAttacks(kingSquare, enemyPieces) & enemyRooks)
                     | (MagicBitboards.bishopAttacks(kingSquare, enemyPieces) & enemyBishops);

        while(snipers != 0) {
            int sniper = Long.numberOfTrailingZeros(snipers);
            long blockers = between[kingSquare][sniper] & occupied;

            //exactly one piece in the way and it's ours: it's pinned and may only move along the line
            if(Long.bitCount(blockers) == 1 && (blockers & friendlyPieces) != 0) {
                pinned |= blockers;
                pinRay[Long.numberOfTrailingZeros(blockers)] = between[kingSquare][sniper] | Position.bit(sniper);
            }

            snipers &= snipers - 1;
        }
    }

    //every generator below writes into `moves` starting at `count` and returns the new count. Apart from the
    //king, they only produce moves that land in checkMask and stay on the piece's pinRay if it's pinned

    private int generateSlidingMoves(Position position, long pieces, boolean orthogonal, boolean diagonal, short[] moves, int count) {
        long friendlyPieces = position.isWhiteToMove() ? position.getWhitePieces() : position.getBlackPieces();
        long occupied = position.getOccupiedSquares();

        while (pieces != 0) {
            int startSquare = Long.numberOfTrailingZeros(pieces);

            long attackableSquares = getSlidingAttacks(startSquare, occupied, orthogonal, diagonal) & ~friendlyPieces & checkMask;
            if((pinned & Position.bit(startSquare)) != 0) {
                attackableSquares &= pinRay[startSquare];
            }

            while (attackableSquares != 0) {
                moves[count++] = Move.create(startSquare, Long.numberOfTrailingZeros(attackableSquares));
                attackableSquares &= attackableSquares - 1;
            }

            pieces &= pieces - 1;
        }

        return count;
    }

    private int generateKnightMoves(Position position, short[] moves, int count) {
        //a pinned knight can never stay on its pin line, so it has no moves at all
        long knights = (position.isWhiteToMove() ? position.whiteKnights : position.blackKnights) & ~pinned;
        long friendlyPieces = position.isWhiteToMove() ? position.getWhitePieces() : position.getBlackPieces();

        while(knights != 0) {
            int knightIndex = Long.numberOfTrailingZeros(knights);

            long attackableSquares = knightAttackTable[knightIndex] & ~friendlyPieces & checkMask;

            while(attackableSquares != 0) {
                moves[count++] = Move.create(knightIndex, Long.numberOfTrailingZeros(attackableSquares));
                attackableSquares &= attackableSquares - 1;
            }

            knights &= knights - 1;
        }

        return count;
    }

    private int generateKingMoves(Position position, short[] moves, int count) {
        boolean white = position.isWhiteToMove();
        long king = white ? position.whiteKing : position.blackKing;
        long friendlyPieces = white ? position.getWhitePieces() : position.getBlackPieces();

        if(king == 0) {
            return count;
        }

        int kingIndex = Long.numberOfTrailingZeros(king);
        long attackableSquares = kingAttackTable[kingIndex] & ~friendlyPieces;

        //the king is lifted off the board for the attack test; otherwise stepping straight away from a
        //slider looks safe because the king's own square still blocks the slider's line
        long occupiedWithoutKing = position.getOccupiedSquares() & ~king;

        while(attackableSquares != 0) {
            int targetSquare = Long.numberOfTrailingZeros(attackableSquares);
            if(!isSquareAttacked(position, targetSquare, !white, occupiedWithoutKing)) {
                moves[count++] = Move.create(kingIndex, targetSquare);
            }
            attackableSquares &= attackableSquares - 1;
        }

        return generateCastlingMoves(position, moves, count);
    }

    //castling rights indices: 0 = black queenside, 1 = black kingside, 2 = white queenside, 3 = white kingside
    private int generateCastlingMoves(Position position, short[] moves, int count) {
        boolean white = position.isWhiteToMove();
        long occupied = position.getOccupiedSquares();
        boolean attackedByWhite = !white;

        if(white) {
            boolean squaresEmptyKingside = (occupied & (Position.bit(61) | Position.bit(62))) == 0;
            if(position.canCastle(3) && squaresEmptyKingside
                    && !isSquareAttacked(position, 60, attackedByWhite)
                    && !isSquareAttacked(position, 61, attackedByWhite)
                    && !isSquareAttacked(position, 62, attackedByWhite)) {
                moves[count++] = whiteKingsideCastle;
            }

            boolean squaresEmptyQueenside = (occupied & (Position.bit(57) | Position.bit(58) | Position.bit(59))) == 0;
            if(position.canCastle(2) && squaresEmptyQueenside
                    && !isSquareAttacked(position, 60, attackedByWhite)
                    && !isSquareAttacked(position, 59, attackedByWhite)
                    && !isSquareAttacked(position, 58, attackedByWhite)) {
                moves[count++] = whiteQueensideCastle;
            }
        } else {
            boolean squaresEmptyKingside = (occupied & (Position.bit(5) | Position.bit(6))) == 0;
            if(position.canCastle(1) && squaresEmptyKingside
                    && !isSquareAttacked(position, 4, attackedByWhite)
                    && !isSquareAttacked(position, 5, attackedByWhite)
                    && !isSquareAttacked(position, 6, attackedByWhite)) {
                moves[count++] = blackKingsideCastle;
            }

            boolean squaresEmptyQueenside = (occupied & (Position.bit(1) | Position.bit(2) | Position.bit(3))) == 0;
            if(position.canCastle(0) && squaresEmptyQueenside
                    && !isSquareAttacked(position, 4, attackedByWhite)
                    && !isSquareAttacked(position, 3, attackedByWhite)
                    && !isSquareAttacked(position, 2, attackedByWhite)) {
                moves[count++] = blackQueensideCastle;
            }
        }

        return count;
    }

    private int generatePawnMoves(Position position, short[] moves, int count) {
        boolean white = position.isWhiteToMove();
        long emptySquares = ~position.getOccupiedSquares();

        long pawns;
        long enemyPieces;
        long singlePush, doublePush, leftAttack, rightAttack;
        long promotionRank;
        //startSquare = targetSquare + offset, so the offsets point back toward the side that's moving
        int pushOffset, leftOffset, rightOffset;

        if(white) {
            pawns = position.whitePawns;
            enemyPieces = position.getBlackPieces();

            singlePush = (pawns >>> 8) & emptySquares;
            //the hexadecimal ensure that the pawns are moving to the correct rank
            doublePush = (singlePush >>> 8) & emptySquares & 0x000000FF00000000L;

            //the hexadecimal ensure the H file isn't being attacked because that would be immpossible to be attacking left and attacking the H file
            leftAttack = ((pawns & ~0x0101010101010101L) >>> 9) & enemyPieces;

            //same concept as left attack but with the A file
            rightAttack = ((pawns & ~0x8080808080808080L) >>> 7) & enemyPieces;

            promotionRank = 0x00000000000000FFL;
            pushOffset = 8;
            leftOffset = 9;
            rightOffset = 7;
        } else {
            pawns = position.blackPawns;
            enemyPieces = position.getWhitePieces();

            singlePush = (pawns << 8) & emptySquares;
            //the hexadecimal ensure that the pawns are moving to the correct rank
            doublePush = (singlePush << 8) & emptySquares & 0x00000000FF000000L;

            //<< 9 moves one column right (+1), so pawns already in column 7 are excluded or they'd wrap to column 0
            leftAttack = ((pawns & ~0x8080808080808080L) << 9) & enemyPieces;

            //<< 7 moves one column left (-1), so pawns in column 0 are excluded or they'd wrap to column 7
            rightAttack = ((pawns & ~0x0101010101010101L) << 7) & enemyPieces;

            promotionRank = 0xFF00000000000000L;
            pushOffset = -8;
            leftOffset = -9;
            rightOffset = -7;
        }

        //checkMask is applied only now: doublePush is built from the unmasked singlePush, because a double
        //push can block a check even when the single push square wouldn't
        count = addPawnMoves(singlePush & checkMask, pushOffset, promotionRank, moves, count);
        count = addPawnMoves(doublePush & checkMask, 2 * pushOffset, promotionRank, moves, count);
        count = addPawnMoves(leftAttack & checkMask, leftOffset, promotionRank, moves, count);
        count = addPawnMoves(rightAttack & checkMask, rightOffset, promotionRank, moves, count);

        return count;
    }

    //a pawn reaching the last rank produces one move per piece it can promote to
    private int addPawnMoves(long targets, int offset, long promotionRank, short[] moves, int count) {
        while(targets != 0) {
            int targetSquare = Long.numberOfTrailingZeros(targets);
            int startSquare = targetSquare + offset;
            targets &= targets - 1;

            //a pinned pawn can still push along a file pin or capture its pinner along a diagonal pin
            if((pinned & Position.bit(startSquare)) != 0 && (pinRay[startSquare] & Position.bit(targetSquare)) == 0) {
                continue;
            }

            if((Position.bit(targetSquare) & promotionRank) != 0) {
                for(int pieceType : Move.promotionPieces) {
                    moves[count++] = Move.promotion(startSquare, targetSquare, pieceType);
                }
            } else {
                moves[count++] = Move.create(startSquare, targetSquare);
            }
        }

        return count;
    }


    //our pawns that can capture onto the en passant square are exactly the squares an enemy-colored
    //pawn standing there would attack (same symmetry trick as isSquareAttacked)
    private int generateEnPassantMoves(Position position, short[] moves, int count) {
        int epSquare = position.getEnPassantSquare();

        if(epSquare == -1) {
            return count;
        }

        boolean white = position.isWhiteToMove();
        long pawns = white ? position.whitePawns : position.blackPawns;
        long attackers = (white ? blackPawnAttackTable[epSquare] : whitePawnAttackTable[epSquare]) & pawns;

        //the captured pawn sits one rank behind the en passant square, on the mover's side
        int capturedSquare = white ? epSquare + 8 : epSquare - 8;

        //en passant is the only move that removes a piece from a square it doesn't land on. It gets out of
        //check if it lands between a slider and the king, or if the captured pawn is the checker itself
        //(the double-pushed pawn giving check), which checkMask alone would miss
        if(((Position.bit(epSquare) | Position.bit(capturedSquare)) & checkMask) == 0) {
            return count;
        }

        long occupied = position.getOccupiedSquares();
        long enemyQueens = white ? position.blackQueens : position.whiteQueens;
        long enemyRooks = (white ? position.blackRooks : position.whiteRooks) | enemyQueens;
        long enemyBishops = (white ? position.blackBishops : position.whiteBishops) | enemyQueens;

        while(attackers != 0) {
            int startSquare = Long.numberOfTrailingZeros(attackers);
            attackers &= attackers - 1;

            //rather than modelling pins for en passant, rebuild the occupancy after the capture and look for a
            //slider hitting the king. This covers a pinned capturing pawn, both pawns leaving a rank the king
            //shares with an enemy rook, and a diagonal opened up by removing the captured pawn
            if(kingSquare != -1) {
                long occupiedAfter = (occupied & ~Position.bit(startSquare) & ~Position.bit(capturedSquare)) | Position.bit(epSquare);
                if((MagicBitboards.rookAttacks(kingSquare, occupiedAfter) & enemyRooks) != 0
                        || (MagicBitboards.bishopAttacks(kingSquare, occupiedAfter) & enemyBishops) != 0) {
                    continue;
                }
            }

            moves[count++] = Move.enPessant(startSquare, epSquare);
        }

        return count;
    }


    public int getNumLegalMoves(Position currentPosition) {
        return generateMoves(currentPosition, scratchMoves);
    }


    //fills `moves` (at least maxMoves long) with every legal move and returns how many there are
    public int generateMoves(Position currentPosition, short[] moves) {
    boolean whiteToMove = currentPosition.isWhiteToMove();

    computeCheckAndPins(currentPosition);

    int count = generateKingMoves(currentPosition, moves, 0);

    //in double check only the king can move
    if(checkMask == 0) {
        return count;
    }

    count = generatePawnMoves(currentPosition, moves, count);
    count = generateEnPassantMoves(currentPosition, moves, count);
    count = generateKnightMoves(currentPosition, moves, count);
    count = generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteRooks : currentPosition.blackRooks, true, false, moves, count);
    count = generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteBishops : currentPosition.blackBishops, false, true, moves, count);
    count = generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteQueens : currentPosition.blackQueens, true, true, moves, count);

    return count;
    }


    //returns the first legal move between the two squares, or Move.none; for a promotion this is the
    //queen promotion because promotions are generated strongest piece first
    public short getLegalMove(int startingIndex, int targetIndex, Position currentPosition) {
        int count = generateMoves(currentPosition, scratchMoves);
        for(int i = 0; i < count; i++) {
            short move = scratchMoves[i];
            if (Move.startSquare(move) == startingIndex && Move.targetSquare(move) == targetIndex) {
                return move;
            }
        }
        return Move.none;
    }

    //promotionPiece is ignored for moves that aren't promotions
    public short getLegalMove(int startingIndex, int targetIndex, int promotionPiece, Position currentPosition) {
        int count = generateMoves(currentPosition, scratchMoves);
        for(int i = 0; i < count; i++) {
            short move = scratchMoves[i];
            if (Move.startSquare(move) == startingIndex && Move.targetSquare(move) == targetIndex
                    && (!Move.isPromotion(move) || Move.promotionPiece(move) == promotionPiece)) {
                return move;
            }
        }
        return Move.none;
    }
}
