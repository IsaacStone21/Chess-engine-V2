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


    private final Move whiteKingsideCastle = new Move(60, 62, false, true, false);
    private final Move whiteQueensideCastle = new Move(60, 58, false, true, false);
    private final Move blackKingsideCastle = new Move(4, 6, false, true, false);
    private final Move blackQueensideCastle = new Move(4, 2, false, true, false);

    List<Long> times = new ArrayList<>();


    public MoveGenerator() {
        generateSquaresToEdge();
        generateKnightTable();
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

    private List<Move> generatePawnMoves(Position position) {
        List<Move> moves = new ArrayList<>();
        boolean whiteToPlay = position.isWhiteToMove();
        long occupiedSquares = position.getOccupiedSquares();

        if(whiteToPlay) {
            long pawns = position.whitePawns;
            long enemyPieces =position.getBlackPieces();


            long singlePush = (pawns >>> 8) & ~occupiedSquares;
            //the hexadecimal ensure that the pawns are moving to the correct rank
            long doublePush = (singlePush >>> 8) & ~occupiedSquares & 0x00000000FF000000L;

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
            long doublePush = (singlePush << 8) & ~occupiedSquares & 0x000000FF00000000L;

            //the hexadecimal ensure the H file isn't being attacked because that would be immpossible to be attacking left and attacking the H file
            long leftAttack = ((pawns & ~0x0101010101010101L) << 9) & enemyPieces;

            //same concept as left attack but with the A file
            long rightAttack = ((pawns & ~0x8080808080808080L) << 7) & enemyPieces;

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


    public int getNumLegalMoves(Position currentPosition) {
        return generateMoves(currentPosition).size();
    }


    public List<Move> generateMoves(Position currentPosition) {
    List<Move> moves = new ArrayList<>();
    long startTime = System.nanoTime();

    long finishTime = System.nanoTime();

    times.add(finishTime - startTime);
    long sum = 0;

    for(int i = 0; i < times.size(); i++) {
        sum += times.get(i);
    }

    long avg = sum / times.size();

    //System.out.println("Average time to compute: " + avg + " nanoseconds");
    //System.out.println("Num legal Moves: " + moves.size());

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
