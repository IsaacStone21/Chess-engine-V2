package EngineUtil;

import java.util.ArrayList;
import java.util.List;


public class MoveGenerator {
    //Up, down, left, right, UL, UR, DL, DR
    int[] directionOffsets = {-8, 8, -1, 1, -9, -7, 7, 9};

    // UUR, URR, DRR, DDR, DDL, DLL, ULL, UUL
    int[] knightOffsets = {-15, -6, 10, 17, 15, 6, -10, -17};

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

    //checks if the move doesnt wrap around the board
    private boolean wrapsBoard(int startingIndex, int targetIndex) {


        int startRow = (startingIndex - (startingIndex % 8)) / 8;
        int startCol = startingIndex - 8 * startRow;

        int attackedRow = (targetIndex - (targetIndex % 8)) / 8;
        int attackedCol = targetIndex - 8 * attackedRow;

        return Math.abs(attackedCol - startCol) > 2;
    }

    private List<Move> generateKnightMoves(Position position) {
        //optimize later with precomputed knight moves
        List<Move> moves = new ArrayList<>();
        long knights = position.isWhiteToMove() ? position.whiteKnights : position.blackKnights;
        long friendlyPieces = position.isWhiteToMove() ? position.getWhitePieces() : position.getBlackPieces();

        while(knights != 0) {
            int knightIndex = Long.numberOfTrailingZeros(knights);

            for(int i = 0; i < 8; i++) {
                int targetIndex = knightIndex + knightOffsets[i];

                if(targetIndex >= 64 || targetIndex < 0 || wrapsBoard(knightIndex, targetIndex)) {
                    continue;
                }

                if ((friendlyPieces & Position.bit(targetIndex)) != 0) {
                    continue;
                }

                moves.add(new Move(knightIndex, targetIndex ));
            }

            knights &= ~Position.bit(knightIndex);
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
