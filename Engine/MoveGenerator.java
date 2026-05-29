package Engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MoveGenerator {
    //Up, down, left, right, UL, UR, DL, DR
    int[] directionOffsets = {-8, 8, -1, 1, -9, -7, 7, 9};

    // UUR, URR, DRR, DDR, DDL, DLL, ULL, UUL
    int[] knightOffsets = {-15, -6, 10, 17, 15, 6, -10, -17};

    //contains the number of squares to the edge in each direction
    //first number is the index
    //second is the direction: Up, down, left, right, UL, UR, DL, DR
    int[][] numSquaresToEdge = new int[64][8];

    Board board;
    private final Move whiteKingsideCastle = new Move(60, 62);
    private final Move whiteQueensideCastle = new Move(60, 58);
    private final Move blackKingsideCastle = new Move(4, 6);
    private final Move blackQueensideCastle = new Move(4, 2);


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
    private boolean wrapsBoard(Move move) {
        int startingIndex = move.startSquare;
        int targetIndex = move.targetSquare;

        int startRow = (startingIndex - (startingIndex % 8)) / 8;
        int startCol = startingIndex - 8 * startRow;

        int attackedRow = (targetIndex - (targetIndex % 8)) / 8;
        int attackedCol = targetIndex - 8 * attackedRow;

        return Math.abs(attackedCol - startCol) > 2;
    }

    private List<Move> generateSlidingMoves(int startingIndex, Piece piece) {
        List<Move> moves = new ArrayList<>();
    
        int startDirIndex = piece.isType(Piece.bishop) ? 4 : 0;
        int endDirIndex = piece.isType(Piece.rook) ? 4 : 8;
        boolean isWhite = piece.isWhite();

        for(int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            for (int dist = 1; dist <= numSquaresToEdge[startingIndex][directionIndex]; dist++) {
                int targetSquare = startingIndex + (dist * directionOffsets[directionIndex]);

                Piece targetPiece = board.getPieceAtIndex(targetSquare);
                boolean targetIsEmpty = targetPiece.ID == Piece.emptyTile.ID;

                
                if (isWhite == targetPiece.isWhite() && !targetIsEmpty) {
                    break;
                }

                moves.add(new Move(startingIndex, targetSquare));

            if (isWhite != targetPiece.isWhite() && !targetIsEmpty) {
                    break;
            }
            }
        }

        return moves;
    }

    private List<Move> generatePawnMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();

        boolean isWhitePawn = board.getPieceAtIndex(startingIndex).isWhite();

        int row = (startingIndex - (startingIndex % 8)) / 8;
        boolean onStartSquare = (isWhitePawn && row == 6) || (!isWhitePawn && row == 1);

        int directionIndex = isWhitePawn ? directionOffsets[0] : directionOffsets[1];

        if (board.getPieceAtIndex(startingIndex + directionIndex).ID == 0) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex));

          if (onStartSquare && board.getPieceAtIndex(startingIndex + 2*directionIndex).ID == 0) {
            moves.add(new Move(startingIndex, startingIndex + 2*directionIndex));
          }

        }

        Piece attackedPiece = board.getPieceAtIndex(startingIndex + directionIndex - 1);
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex - 1));
        }

        attackedPiece = board.getPieceAtIndex(startingIndex + directionIndex + 1);
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex + 1));
        }


        return moves;
    }

    private List<Move> generateKingMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();

        boolean isWhiteKing = board.getPieceAtIndex(startingIndex).isWhite();

        for(int i = 0; i < 8; i++) {
            int attackedIndex = startingIndex + directionOffsets[i];

            Move newMove = new Move(startingIndex, attackedIndex);

            if(attackedIndex > 63 || attackedIndex < 0 || wrapsBoard(newMove)) {
                continue;
            }

            Piece attackedPiece = board.getPieceAtIndex(attackedIndex);

            if (attackedPiece.ID != 0 && attackedPiece.isWhite() == isWhiteKing) continue;

            moves.add(newMove);
        }


        return moves;
    }

    private List<Move> generateKnightMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();

        Piece attackingPiece = board.getPieceAtIndex(startingIndex);
        boolean isWhiteKnight = attackingPiece.isWhite();

        for(int i = 0; i < 8; i++) {
            int attackedIndex = startingIndex + knightOffsets[i];

            Move newMove = new Move(startingIndex, attackedIndex);

            if(attackedIndex > 63 || attackedIndex < 0 || wrapsBoard(newMove)) {
                continue;
            }

            Piece attackedPiece = board.getPieceAtIndex(attackedIndex);

          
            if(attackedPiece.ID != 0 && attackedPiece.isWhite() == isWhiteKnight) {
                continue;
            }

            moves.add(newMove);

        }

        return moves;
    }

    private List<Move> generateCastlingMoves() {

        List<Move> moves = new ArrayList<>();
        boolean whiteToMove = board.isWhiteToMove();

        //checks if the king and rook involved have moved at anytime during the game
        boolean canCastleKingside = whiteToMove ? board.canCastle(3) : board.canCastle(1);

        boolean canCastleQueenSide = whiteToMove ? board.canCastle(2) : board.canCastle(0);
        

        if(!(canCastleKingside || canCastleQueenSide)) {
            return moves;
        }


        int row = whiteToMove ? 7 : 0;

        //check if the king is castling while in check or through check
        //checks if it castles into check later
        canCastleKingside = canCastleKingside && !enemyCanSeeSquare(8*row + 4) && !enemyCanSeeSquare(8*row + 5);


        canCastleQueenSide = canCastleKingside && !enemyCanSeeSquare(8*row + 4) && !enemyCanSeeSquare(8*row + 3);

        //checks if there is pieces in the path of the rook and king
        canCastleKingside = canCastleKingside && (board.getPieceAtIndex(8*row + 5).ID == 0) && (board.getPieceAtIndex(8*row + 6).ID == 0);
        canCastleQueenSide = canCastleQueenSide && (board.getPieceAtIndex(8*row + 1).ID == 0) && (board.getPieceAtIndex(8*row + 2).ID == 0) && (board.getPieceAtIndex(8*row + 3).ID == 0);


        if(canCastleKingside) {
          moves.add(whiteToMove ? whiteKingsideCastle : blackKingsideCastle);
        }

        if (canCastleQueenSide) {
            moves.add(whiteToMove ? whiteQueensideCastle : blackQueensideCastle);
        }

        return moves;

    }

    private boolean enemyCanSeeSquare(int index) {
        boolean canSeeSquare = false;

        //switch board color to check if enemy pieces can see king
        board.switchTurns();

        List<Move> moves = generateMoves(false);

        for(int i = 0; i < moves.size() && !canSeeSquare; i++) {
            canSeeSquare = moves.get(i).targetSquare == index;
        }


        //switch board color back
        board.switchTurns();

        return canSeeSquare;
    }

    private boolean inCheck(Move desiredMove) {

        boolean inCheck = false;
        int startingIndex = desiredMove.startSquare;  
        int targetIndex = desiredMove.targetSquare;
        int kingIndex = 0;

        Piece capturedPiece = board.getPieceAtIndex(targetIndex);
 
        //hypothetical move
        board.edit(targetIndex, board.getPieceAtIndex(startingIndex));
        board.edit(startingIndex, Piece.emptyTile);


        for(int i = 0; i < 64; i++) {
            if(board.getPieceAtIndex(i).isType(Piece.king) && board.getPieceAtIndex(i).isWhite() == board.isWhiteToMove()){
                kingIndex = i;
                break;
            }
        }

        inCheck = enemyCanSeeSquare(kingIndex);

        //undo hypothetical move
        board.edit(startingIndex, board.getPieceAtIndex(targetIndex));
        board.edit(targetIndex, capturedPiece);


        return inCheck;
    }


    //TODO: Generate en pessant
    public List<Move> generateMoves() {
      return generateMoves(true);
    }

    private List<Move> generateMoves(boolean withCheckLogic) {
          List<Move> moves = new ArrayList<>();
   
        for (int index = 0; index < 64; index++) {
              
            Piece piece = board.getPieceAtIndex(index);
              
            if (piece == Piece.emptyTile) {
                continue;
            }

            if (piece.isWhite() != board.isWhiteToMove()) {
                continue;
            }

            if(piece.isSlidingPiece()) {
                moves.addAll(generateSlidingMoves(index, piece));
            }

            if(piece.isType(Piece.pawn)) {
                moves.addAll(generatePawnMoves(index));
            }

            if(piece.isType(Piece.knight)) {
                moves.addAll(generateKnightMoves(index));
            }

            if(piece.isType(Piece.king)){ 
                moves.addAll(generateKingMoves(index));
            }
        }
        
        if(withCheckLogic) {

            moves.addAll(generateCastlingMoves());


            int numMovesRemoved = 0;

           for (int i = moves.size() - 1; i >= 0; i--) {

            if(inCheck(moves.get(i))) {
                numMovesRemoved++;
                moves.remove(i);
            }
           }

            System.out.println("Total Legal Moves: " + moves.size());
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
