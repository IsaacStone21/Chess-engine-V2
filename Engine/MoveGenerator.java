package Engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        board = Board.createBoard();
    }

    public class Move{
        public int startSquare;
        public int targetSquare;

        public Move(int startIndex, int targetIndex) {
            startSquare = startIndex;
            targetSquare = targetIndex;
        }

        @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Move other = (Move) obj;

        return startSquare == other.startSquare &&
               targetSquare == other.targetSquare;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startSquare, targetSquare);
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

    private List<Move> generateSlidingMoves(int startingIndex, Piece piece) {
        List<Move> moves = new ArrayList<>();
    
        int startDirIndex = piece.isType(Piece.bishop) ? 4 : 0;
        int endDirIndex = piece.isType(Piece.rook) ? 4 : 8;
        boolean isWhite = piece.isWhite();

        for(int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            for (int dist = 1; dist <= numSquaresToEdge[startingIndex][directionIndex]; dist++) {
                int targetSquare = startingIndex + (dist * directionOffsets[directionIndex]);

                Piece targetPiece = board.square[targetSquare];
                boolean targetIsEmpty = targetPiece.ID == Piece.emptyTile.ID;

                System.out.println("Target Square Index: " + targetSquare + " contains piece: " + targetPiece.ID);
                
                if (isWhite == targetPiece.isWhite() && !targetIsEmpty) {
                    System.out.println("Failed Color Check");
                    break;
                }

                moves.add(new Move(startingIndex, targetSquare));

            if (isWhite != targetPiece.isWhite() && !targetIsEmpty) {
                    break;
            }
            }
        }
        System.out.println("Sliding moves length: " + moves.size());

        for(int i = 0; i < moves.size(); i++) {
            System.out.println("Start Square: " + moves.get(i).startSquare + " Target Square: " + moves.get(i).targetSquare);

        }

        return moves;
    }

    public List<Move> generatePawnMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();

        boolean isWhitePawn = board.square[startingIndex].isWhite();

        int row = (startingIndex - (startingIndex % 8)) / 8;
        boolean onStartSquare = (isWhitePawn && row == 6) || (!isWhitePawn && row == 1);

        int directionIndex = isWhitePawn ? directionOffsets[0] : directionOffsets[1];

        if (board.square[startingIndex + directionIndex].ID == 0) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex));

          if (onStartSquare && board.square[startingIndex + 2*directionIndex].ID == 0) {
            moves.add(new Move(startingIndex, startingIndex + 2*directionIndex));
          }

        }

        Piece attackedPiece = board.square[startingIndex + directionIndex - 1];
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex - 1));
        }

        attackedPiece = board.square[startingIndex + directionIndex + 1];
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex + 1));
        }

        System.out.println("Num Pawn Moves" + moves.size());

        return moves;
    }


    //TODO: Generate knight, king, castlings, en pessant, and make sure king isn't in check
    public List<Move> generateMoves() {
        List<Move> moves = new ArrayList<>();
   
        for (int index = 0; index < 64; index++) {
              
            Piece piece = board.square[index];
              
            if (piece == Piece.emptyTile) {
                System.out.println("Index " + index + " is empty");
                continue;
            }

            if (piece.isWhite() != board.whiteToMove) {
                System.out.println("Index " + index + " is wrong color");
                continue;
            }

            System.out.println("Attacking piece index: " + index + " contains piece: " + piece.ID);

            if(piece.isSlidingPiece()) {
                System.out.println("Index " + index + " is sliding piece");
                moves.addAll(generateSlidingMoves(index, piece));
            }

            if(piece.isType(Piece.pawn)) {
                moves.addAll(generatePawnMoves(index));
            }
        }
       
        return moves;
    }

    public boolean isLegalMove(int startingIndex, int targetIndex) {

        if (startingIndex == targetIndex) return false;
            List<Move> legalMoves = generateMoves();
            Move requestedMove = new Move(startingIndex, targetIndex);



            boolean isLegalMove = legalMoves.contains(requestedMove);
            System.out.println("Legal Move: " + isLegalMove);

            return isLegalMove;
    }
}
