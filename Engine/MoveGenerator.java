package Engine;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    //Up, down, left, right, UL, UR, DL, DR
    int[] directionOffsets = {-8, 8, -1, 1, -9, -7, 7, 9};

    //contains the number of squares to the edge in each direction
    //first number is the index
    //second is the direction: Up, down, left, right, UL, UR, DL, DR
    int[][] numSquaresToEdge = new int[64][8];

    Board board;

   
    public MoveGenerator() {
        generateSquaresToEdge();
        board = new Board();
    }

    public class Move{
        public int startSquare;
        public int targetSquare;

        public Move(int startIndex, int targetIndex) {
            startSquare = startIndex;
            targetSquare = targetIndex;
        }
    }

    public void generateSquaresToEdge() {
         for (int index = 0; index < 64; index++) {
            int row = (index - (index%8)) / 8;
            int col = index - 8*row;

            int numSquaresNorth = row;
            int numSquaresSouth = 7 - row;
            int numSquaresEast = col;
            int numSquaresWest = 7 - col;

            numSquaresToEdge[index] = new int[]{numSquaresNorth, numSquaresSouth, numSquaresEast, numSquaresWest,
                 Math.min(numSquaresNorth, numSquaresEast), Math.min(numSquaresNorth, numSquaresWest), 
                 Math.min(numSquaresSouth, numSquaresEast), Math.min(numSquaresSouth, numSquaresWest)};
        }
    }

    private List<Move> generateSlidingMoves(int startingIndex, Piece piece) {
        int startDirIndex = piece.isType(piece.ID, Piece.bishop) ? 4 : 0;
        int endDirIndex = piece.isType(piece.ID, Piece.rook) ? 4 : 8;
        boolean isWhite = piece.isWhite();
        List<Move> moves = new ArrayList<>();

        for(int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            for (int dist = 0; dist < numSquaresToEdge[startingIndex][directionIndex]; dist++) {
                int targetSquare = startingIndex + (dist * directionOffsets[directionIndex]);
                Piece targetPiece = board.square[targetSquare];

                if (isWhite == targetPiece.isWhite()) {
                    break;
                }

                moves.add(new Move(startingIndex, targetSquare));

            if (isWhite != targetPiece.isWhite()) {
                    break;
            }
            }
        }
        System.out.println("here");
         if (moves.isEmpty()) System.out.println(("No legal Moves"));
        return moves;
    }


    public List<Move> generateMoves() {
        List<Move> moves = new ArrayList<>();
        Piece[] pieces = new Piece[64];


        for (int i = 0; i < 64; i++) {
            
            Piece piece = board.square[i];
            pieces[i] = piece;
           
            if (piece == null) continue;
            if (piece.isWhite() != board.whiteToMove) continue;

            if(piece.isSlidingPiece()) {
                moves.addAll(generateSlidingMoves(i, piece));
            }
        }

        System.out.println(FENUtil.positionToFEN(pieces));
       
        return moves;
    }

    public boolean isLegalMove(int startingIndex, int targetIndex) {

        // System.out.println("Requested start index: " + startingIndex);
        // System.out.println("Requested target index: " + targetIndex);
        if (startingIndex == targetIndex) return false;
        List<Move> legalMoves = generateMoves();
        Move requestedMove = new Move(startingIndex, targetIndex);

        boolean isLegalMove = legalMoves.contains(requestedMove);
        System.out.println("Legal Move: " + isLegalMove);

        return isLegalMove;
    }
}
