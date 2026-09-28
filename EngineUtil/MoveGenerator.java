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


    private final Move whiteKingsideCastle = new Move(60, 62, false, true, false);
    private final Move whiteQueensideCastle = new Move(60, 58, false, true, false);
    private final Move blackKingsideCastle = new Move(4, 6, false, true, false);
    private final Move blackQueensideCastle = new Move(4, 2, false, true, false);

    List<Long> times = new ArrayList<>();


    public MoveGenerator() {
        generateSquaresToEdge();
        generateKnightTable();
        generateKingTable();
        generatePawnAttackTables();
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

    //true if a piece of `byWhite`'s color attacks `square` in this position. Works by placing each
    //attacker type on `square` and checking whether its (occupancy-aware, for sliders) attack pattern
    //hits an actual enemy piece of that type - attacks are symmetric, so this is equivalent to asking
    //whether any enemy piece attacks `square` directly.
    public boolean isSquareAttacked(Position position, int square, boolean byWhite) {
        long occupied = position.getOccupiedSquares();

        long enemyPawns = byWhite ? position.whitePawns : position.blackPawns;
        long enemyKnights = byWhite ? position.whiteKnights : position.blackKnights;
        long enemyKing = byWhite ? position.whiteKing : position.blackKing;
        long enemyQueens = byWhite ? position.whiteQueens : position.blackQueens;
        long enemyRooks = (byWhite ? position.whiteRooks : position.blackRooks) | enemyQueens;
        long enemyBishops = (byWhite ? position.whiteBishops : position.blackBishops) | enemyQueens;

        //a pawn of `byWhite`'s color attacks `square` iff `square` shows up in the opposite-colored
        //pawn attack table centered on `square` itself (the symmetry trick mentioned above)
        long pawnAttackersFromSquare = byWhite ? blackPawnAttackTable[square] : whitePawnAttackTable[square];
        if((pawnAttackersFromSquare & enemyPawns) != 0) return true;

        if((knightAttackTable[square] & enemyKnights) != 0) return true;
        if((kingAttackTable[square] & enemyKing) != 0) return true;
        if((getSlidingAttacks(square, occupied, true, false) & enemyRooks) != 0) return true;
        if((getSlidingAttacks(square, occupied, false, true) & enemyBishops) != 0) return true;

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

    private List<Move> generateSlidingMoves(Position position, long pieces, boolean orthogonal, boolean diagonal) {
        List<Move> moves = new ArrayList<>();
        long friendlyPieces = position.isWhiteToMove() ? position.getWhitePieces() : position.getBlackPieces();
        long occupied = position.getOccupiedSquares();

        while (pieces != 0) {
            int startSquare = Long.numberOfTrailingZeros(pieces);

            long attackableSquares = getSlidingAttacks(startSquare, occupied, orthogonal, diagonal) & ~friendlyPieces;

            while (attackableSquares != 0) {
                moves.add(new Move(startSquare, Long.numberOfTrailingZeros(attackableSquares)));
                attackableSquares &= attackableSquares - 1;
            }

            pieces &= pieces - 1;
        }

        return moves;
    }

    private List<Move> generateKnightMoves(Position position) {
        List<Move> moves = new ArrayList<>();
        long knights = position.isWhiteToMove() ? position.whiteKnights : position.blackKnights;
        long friendlyPieces = position.isWhiteToMove() ? position.getWhitePieces() : position.getBlackPieces();

        while(knights != 0) {
            int knightIndex = Long.numberOfTrailingZeros(knights);

            long attackableSquares = knightAttackTable[knightIndex] & ~friendlyPieces;

            while(attackableSquares != 0) {
                moves.add(new Move(knightIndex, Long.numberOfTrailingZeros(attackableSquares)));
                attackableSquares &= attackableSquares - 1;
            }

            knights &= knights - 1;
        }

        return moves;
    }

    private List<Move> generateKingMoves(Position position) {
        List<Move> moves = new ArrayList<>();
        boolean white = position.isWhiteToMove();
        long king = white ? position.whiteKing : position.blackKing;
        long friendlyPieces = white ? position.getWhitePieces() : position.getBlackPieces();

        if(king == 0) {
            return moves;
        }

        int kingIndex = Long.numberOfTrailingZeros(king);
        long attackableSquares = kingAttackTable[kingIndex] & ~friendlyPieces;

        while(attackableSquares != 0) {
            int targetSquare = Long.numberOfTrailingZeros(attackableSquares);
            moves.add(new Move(kingIndex, targetSquare));
            attackableSquares &= attackableSquares - 1;
        }

        moves.addAll(generateCastlingMoves(position));

        return moves;
    }

    //castlingCheck indices: 0 = black queenside, 1 = black kingside, 2 = white queenside, 3 = white kingside
    private List<Move> generateCastlingMoves(Position position) {
        List<Move> moves = new ArrayList<>();
        boolean white = position.isWhiteToMove();
        long occupied = position.getOccupiedSquares();
        boolean attackedByWhite = !white;

        if(white) {
            boolean squaresEmptyKingside = (occupied & (Position.bit(61) | Position.bit(62))) == 0;
            if(position.canCastle(3) && squaresEmptyKingside
                    && !isSquareAttacked(position, 60, attackedByWhite)
                    && !isSquareAttacked(position, 61, attackedByWhite)
                    && !isSquareAttacked(position, 62, attackedByWhite)) {
                moves.add(whiteKingsideCastle);
            }

            boolean squaresEmptyQueenside = (occupied & (Position.bit(57) | Position.bit(58) | Position.bit(59))) == 0;
            if(position.canCastle(2) && squaresEmptyQueenside
                    && !isSquareAttacked(position, 60, attackedByWhite)
                    && !isSquareAttacked(position, 59, attackedByWhite)
                    && !isSquareAttacked(position, 58, attackedByWhite)) {
                moves.add(whiteQueensideCastle);
            }
        } else {
            boolean squaresEmptyKingside = (occupied & (Position.bit(5) | Position.bit(6))) == 0;
            if(position.canCastle(1) && squaresEmptyKingside
                    && !isSquareAttacked(position, 4, attackedByWhite)
                    && !isSquareAttacked(position, 5, attackedByWhite)
                    && !isSquareAttacked(position, 6, attackedByWhite)) {
                moves.add(blackKingsideCastle);
            }

            boolean squaresEmptyQueenside = (occupied & (Position.bit(1) | Position.bit(2) | Position.bit(3))) == 0;
            if(position.canCastle(0) && squaresEmptyQueenside
                    && !isSquareAttacked(position, 4, attackedByWhite)
                    && !isSquareAttacked(position, 3, attackedByWhite)
                    && !isSquareAttacked(position, 2, attackedByWhite)) {
                moves.add(blackQueensideCastle);
            }
        }

        return moves;
    }

    private List<Move> generatePawnMoves(Position position) {
        List<Move> moves = new ArrayList<>();
        boolean whiteToPlay = position.isWhiteToMove();
        long occupiedSquares = position.getOccupiedSquares();

        if(whiteToPlay) {
            long pawns = position.whitePawns;
            long enemyPieces =position.getBlackPieces();


            long singlePush = (pawns >>> 8) & ~occupiedSquares;
            //the hexadecimal ensure that the pawns are moving to the correct rank
            long doublePush = (singlePush >>> 8) & ~occupiedSquares & 0x000000FF00000000L;

            //the hexadecimal ensure the H file isn't being attacked because that would be immpossible to be attacking left and attacking the H file
            long leftAttack = ((pawns & ~0x0101010101010101L) >>> 9) & enemyPieces;

            //same concept as left attack but with the A file
            long rightAttack = ((pawns & ~0x8080808080808080L) >>> 7) & enemyPieces;

            while(singlePush != 0) {
                int targetSquare = Long.numberOfTrailingZeros(singlePush);
                int startSquare = targetSquare + 8;
                if(targetSquare < 8) {
                    moves.add(new Move(startSquare, targetSquare, false, false, true));
                } else {
                    moves.add(new Move(startSquare, targetSquare));
                }
                singlePush &= singlePush - 1;
            }

            while(doublePush != 0) {
                int targetSquare = Long.numberOfTrailingZeros(doublePush);
                int startSquare = targetSquare + 16;
                moves.add(new Move(startSquare, targetSquare));
                doublePush &= doublePush - 1;
            }

            while(leftAttack != 0) {
                int targetSquare = Long.numberOfTrailingZeros(leftAttack);
                int startSquare = targetSquare + 9;
                if(targetSquare < 8) {
                    moves.add(new Move(startSquare, targetSquare, false, false, true));
                } else {
                    moves.add(new Move(startSquare, targetSquare));
                }
                leftAttack &= leftAttack - 1;
            }

            while(rightAttack != 0) {
                int targetSquare = Long.numberOfTrailingZeros(rightAttack);
                int startSquare = targetSquare + 7;
                if(targetSquare < 8) {
                    moves.add(new Move(startSquare, targetSquare, false, false, true));
                } else {
                    moves.add(new Move(startSquare, targetSquare));
                }
                rightAttack &= rightAttack - 1;
            }
        } else {
            long pawns = position.blackPawns;
            long enemyPieces =position.getWhitePieces();

            long singlePush = (pawns << 8) & ~occupiedSquares;
            //the hexadecimal ensure that the pawns are moving to the correct rank
            long doublePush = (singlePush << 8) & ~occupiedSquares & 0x00000000FF000000L;

            //<< 9 moves one column right (+1), so pawns already in column 7 are excluded or they'd wrap to column 0
            long leftAttack = ((pawns & ~0x8080808080808080L) << 9) & enemyPieces;

            //<< 7 moves one column left (-1), so pawns in column 0 are excluded or they'd wrap to column 7
            long rightAttack = ((pawns & ~0x0101010101010101L) << 7) & enemyPieces;

            while(singlePush != 0) {
                int targetSquare = Long.numberOfTrailingZeros(singlePush);
                int startSquare = targetSquare - 8;
                if(targetSquare >= 56) {
                    moves.add(new Move(startSquare, targetSquare, false, false, true));
                } else {
                    moves.add(new Move(startSquare, targetSquare));
                }
                singlePush &= singlePush - 1;
            }

            while(doublePush != 0) {
                int targetSquare = Long.numberOfTrailingZeros(doublePush);
                int startSquare = targetSquare - 16;
                moves.add(new Move(startSquare, targetSquare));
                doublePush &= doublePush - 1;
            }

            while(leftAttack != 0) {
                int targetSquare = Long.numberOfTrailingZeros(leftAttack);
                int startSquare = targetSquare - 9;
                if(targetSquare >= 56) {
                    moves.add(new Move(startSquare, targetSquare, false, false, true));
                } else {
                    moves.add(new Move(startSquare, targetSquare));
                }
                leftAttack &= leftAttack - 1;
            }

            while(rightAttack != 0) {
                int targetSquare = Long.numberOfTrailingZeros(rightAttack);
                int startSquare = targetSquare - 7;
                if(targetSquare >= 56) {
                    moves.add(new Move(startSquare, targetSquare, false, false, true));
                } else {
                    moves.add(new Move(startSquare, targetSquare));
                }
                rightAttack &= rightAttack - 1;
        }
      }

      return moves;
    }


    //our pawns that can capture onto the en passant square are exactly the squares an enemy-colored
    //pawn standing there would attack (same symmetry trick as isSquareAttacked)
    private List<Move> generateEnPassantMoves(Position position) {
        List<Move> moves = new ArrayList<>();
        int epSquare = position.getEnPassantSquare();

        if(epSquare == -1) {
            return moves;
        }

        boolean white = position.isWhiteToMove();
        long pawns = white ? position.whitePawns : position.blackPawns;
        long attackers = (white ? blackPawnAttackTable[epSquare] : whitePawnAttackTable[epSquare]) & pawns;

        while(attackers != 0) {
            moves.add(new Move(Long.numberOfTrailingZeros(attackers), epSquare, true, false, false));
            attackers &= attackers - 1;
        }

        return moves;
    }


    public int getNumLegalMoves(Position currentPosition) {
        return generateMoves(currentPosition).size();
    }


    public List<Move> generateMoves(Position currentPosition) {
    boolean whiteToMove = currentPosition.isWhiteToMove();

    List<Move> pseudoLegalMoves = new ArrayList<>();
    pseudoLegalMoves.addAll(generatePawnMoves(currentPosition));
    pseudoLegalMoves.addAll(generateEnPassantMoves(currentPosition));
    pseudoLegalMoves.addAll(generateKnightMoves(currentPosition));
    pseudoLegalMoves.addAll(generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteRooks : currentPosition.blackRooks, true, false));
    pseudoLegalMoves.addAll(generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteBishops : currentPosition.blackBishops, false, true));
    pseudoLegalMoves.addAll(generateSlidingMoves(currentPosition, whiteToMove ? currentPosition.whiteQueens : currentPosition.blackQueens, true, true));
    pseudoLegalMoves.addAll(generateKingMoves(currentPosition));

    //filter out any move that would leave the mover's own king in check (castling legality,
    //including "can't castle through/out of check", is already enforced in generateCastlingMoves)
    List<Move> moves = new ArrayList<>();
    for(Move move : pseudoLegalMoves) {
        currentPosition.playMove(move);

        long ownKing = whiteToMove ? currentPosition.whiteKing : currentPosition.blackKing;
        boolean movedIntoCheck = ownKing != 0 && isSquareAttacked(currentPosition, Long.numberOfTrailingZeros(ownKing), !whiteToMove);

        currentPosition.undoMove();

        if(!movedIntoCheck) {
            moves.add(move);
        }
    }

    return moves;
    }


    public Move getLegalMove(int startingIndex, int targetIndex, Position currentPosition) {
        List<Move> legalMoves = generateMoves(currentPosition);
        Move requestedMove = new Move(startingIndex, targetIndex);

        for(int i = 0; i < legalMoves.size(); i++) {
            if (requestedMove.startSquare == legalMoves.get(i).startSquare && requestedMove.targetSquare == legalMoves.get(i).targetSquare) {
                return legalMoves.get(i);
            }
        }
        return null;
    }
}
