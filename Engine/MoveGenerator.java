package Engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MoveGenerator {
    //Up, down, left, right, UL, UR, DL, DR
    int[] directionOffsets = {-8, 8, -1, 1, -9, -7, 7, 9};

    // UUR, URR, DRR, DDR, DDL, DLL, ULL, UUL
    int[] knightOffsets = {-15, -6, 10, 17, 15, 6, -10, -17};

    //contains the number of squares to the edge in each direction
    //first number is the index
    //second is the direction: Up, down, left, right, UL, UR, DL, DR
    int[][] numSquaresToEdge = new int[64][8];

    Position acceptedPosition;
    private Random random;
    private final Move whiteKingsideCastle = new Move(60, 62, false, true, false);
    private final Move whiteQueensideCastle = new Move(60, 58, false, true, false);
    private final Move blackKingsideCastle = new Move(4, 6, false, true, false);
    private final Move blackQueensideCastle = new Move(4, 2, false, true, false);

    List<Long> times = new ArrayList<>();


    public MoveGenerator() {
        generateSquaresToEdge();
        acceptedPosition = new Position();
        random = new Random();
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

     public Move getBoardMove() {
        var moves = generateMoves();

        Move move = moves.get(random.nextInt(moves.size()));

        return move;
    }

    public int getNumLegalMoves() {
        return generateMoves().size();
    }


    private List<Move> generateSlidingMoves(int startingIndex, Piece piece) {
        List<Move> moves = new ArrayList<>();
    
        int startDirIndex = piece.isType(Piece.bishop) ? 4 : 0;
        int endDirIndex = piece.isType(Piece.rook) ? 4 : 8;
        boolean isWhite = piece.isWhite();

        for(int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            for (int dist = 1; dist <= numSquaresToEdge[startingIndex][directionIndex]; dist++) {
                int targetSquare = startingIndex + (dist * directionOffsets[directionIndex]);

                Piece targetPiece = acceptedPosition.getPieceAtIndex(targetSquare);
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

    private List<Move> generatePawnMovementMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();

        boolean isWhitePawn = acceptedPosition.getPieceAtIndex(startingIndex).isWhite();

        int row = (startingIndex - (startingIndex % 8)) / 8;
        boolean onStartSquare = (isWhitePawn && row == 6) || (!isWhitePawn && row == 1);

        int directionIndex = isWhitePawn ? directionOffsets[0] : directionOffsets[1];

        if (acceptedPosition.getPieceAtIndex(startingIndex + directionIndex).isType(Piece.empty)) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex));

          if (onStartSquare && acceptedPosition.getPieceAtIndex(startingIndex + 2*directionIndex).isType(Piece.empty)) {
            moves.add(new Move(startingIndex, startingIndex + 2*directionIndex));
          }

        }

        return moves;
    }

    private List<Move> generatePawnAttackMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();
            if(startingIndex <= 7 || startingIndex >= 56) {
                return moves;
            }
        boolean isWhitePawn = acceptedPosition.isWhiteToMove();

        int directionIndex = isWhitePawn ? directionOffsets[0] : directionOffsets[1];

        Piece attackedPiece = acceptedPosition.getPieceAtIndex(startingIndex + directionIndex - 1);
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex - 1));
        }


        attackedPiece = acceptedPosition.getPieceAtIndex(startingIndex + directionIndex + 1);
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex + 1));
        }

        return moves;
    }

    private List<Move> generateEnPessant() {
        List<Move> moves = new ArrayList<>();

        if (acceptedPosition.getNumMoves() == 0) {
            return moves;
        }

        Move lastMove = acceptedPosition.getLastMove();


        boolean whiteToMove = acceptedPosition.isWhiteToMove();

        int startIndex = lastMove.startSquare;
        int endIndex = lastMove.targetSquare;

        int row = (startIndex - (startIndex % 8)) / 8;

        int offset = (whiteToMove ? directionOffsets[1] : directionOffsets[0]);

        if (row != (whiteToMove ? 1 : 6) || startIndex + 2*offset != endIndex) {
            return moves;
        }

        boolean isCorrectPawn = acceptedPosition.getPieceAtIndex(endIndex + directionOffsets[2]).isWhite() == whiteToMove && acceptedPosition.getPieceAtIndex(endIndex + directionOffsets[2]).isType(Piece.pawn);
        if(isCorrectPawn) {
            moves.add(new Move(endIndex + directionOffsets[2], endIndex - offset, true, false, false));
            System.out.println("en pessant available");
        }

            isCorrectPawn = acceptedPosition.getPieceAtIndex(endIndex + directionOffsets[3]).isWhite() == whiteToMove && acceptedPosition.getPieceAtIndex(endIndex + directionOffsets[3]).isType(Piece.pawn);        

        if(isCorrectPawn) {
            moves.add(new Move(endIndex + directionOffsets[3], endIndex - offset, true, false, false));
            System.out.println("en pessant available");
        }
        return moves;
    } 

    private List<Move> generateKingMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();

        boolean isWhiteKing = acceptedPosition.getPieceAtIndex(startingIndex).isWhite();

        for(int i = 0; i < 8; i++) {
            int attackedIndex = startingIndex + directionOffsets[i];

            Move newMove = new Move(startingIndex, attackedIndex);

            if(attackedIndex > 63 || attackedIndex < 0 || wrapsBoard(newMove)) {
                continue;
            }

            Piece attackedPiece = acceptedPosition.getPieceAtIndex(attackedIndex);

            if (attackedPiece.ID != 0 && attackedPiece.isWhite() == isWhiteKing) continue;

            moves.add(newMove);
        }


        return moves;
    }

    private List<Move> generateKnightMoves(int startingIndex) {
        List<Move> moves = new ArrayList<>();

        Piece attackingPiece = acceptedPosition.getPieceAtIndex(startingIndex);
        boolean isWhiteKnight = attackingPiece.isWhite();

        for(int i = 0; i < 8; i++) {
            int attackedIndex = startingIndex + knightOffsets[i];

            Move newMove = new Move(startingIndex, attackedIndex);

            if(attackedIndex > 63 || attackedIndex < 0 || wrapsBoard(newMove)) {
                continue;
            }

            Piece attackedPiece = acceptedPosition.getPieceAtIndex(attackedIndex);

          
            if(attackedPiece.ID != 0 && attackedPiece.isWhite() == isWhiteKnight) {
                continue;
            }

            moves.add(newMove);

        }

        return moves;
    }

    private List<Move> generateCastlingMoves() {

        List<Move> moves = new ArrayList<>();
        boolean whiteToMove = acceptedPosition.isWhiteToMove();

        //checks if the king and rook involved have moved at anytime during the game
        boolean canCastleKingside = whiteToMove ? acceptedPosition.canCastle(3) : acceptedPosition.canCastle(1);

        boolean canCastleQueenSide = whiteToMove ? acceptedPosition.canCastle(2) : acceptedPosition.canCastle(0);
        

        if(!(canCastleKingside || canCastleQueenSide)) {
            return moves;
        }


        int row = whiteToMove ? 7 : 0;

        //check if the king is castling while in check or through check
        //checks if it castles into check later
        canCastleKingside = canCastleKingside && !enemyCanSeeSquare(8*row + 4) && !enemyCanSeeSquare(8*row + 5);


        canCastleQueenSide = canCastleQueenSide && !enemyCanSeeSquare(8*row + 4) && !enemyCanSeeSquare(8*row + 3);

        //checks if there is pieces in the path of the rook and king
        canCastleKingside = canCastleKingside && (acceptedPosition.getPieceAtIndex(8*row + 5).isType(Piece.empty)) && (acceptedPosition.getPieceAtIndex(8*row + 6).isType(Piece.empty));
        canCastleQueenSide = canCastleQueenSide && (acceptedPosition.getPieceAtIndex(8*row + 1).isType(Piece.empty)) && (acceptedPosition.getPieceAtIndex(8*row + 2).isType(Piece.empty)) && (acceptedPosition.getPieceAtIndex(8*row + 3).isType(Piece.empty));


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
        Position savedPosition = new Position(acceptedPosition);
        
        acceptedPosition.switchTurns();

        List<Move> moves = generateMoves(false);

        for(int i = 0; i < moves.size() && !canSeeSquare; i++) {
            canSeeSquare = moves.get(i).targetSquare == index;
        }


        acceptedPosition = savedPosition;        

        return canSeeSquare;
    }

    private boolean inCheck(Move desiredMove) {

        boolean inCheck = false;
        int startingIndex = desiredMove.startSquare;  
        int targetIndex = desiredMove.targetSquare;
        int kingIndex = 0;

        Position savedPosition = new Position(acceptedPosition);
 
        //hypothetical move
        acceptedPosition.playMove(new Move(startingIndex, targetIndex));

        // board.edit(targetIndex, board.getPieceAtIndex(startingIndex));
        // board.edit(startingIndex, Piece.emptyTile);


        for(int i = 0; i < 64; i++) {
            if(acceptedPosition.getPieceAtIndex(i).isType(Piece.king) && acceptedPosition.getPieceAtIndex(i).isWhite() == savedPosition.isWhiteToMove()){
                kingIndex = i;
                break;
            }
        }
        acceptedPosition.switchTurns();

        inCheck = enemyCanSeeSquare(kingIndex);

        //undo hypothetical move
        // board.edit(startingIndex, board.getPieceAtIndex(targetIndex));
        // board.edit(targetIndex, capturedPiece);


        return inCheck;
    }

    public boolean inCheck() {
        boolean inCheck = false;
        int kingIndex = 0;

        for(int i = 0; i < 64; i++) {
            if(acceptedPosition.getPieceAtIndex(i).isType(Piece.king) && acceptedPosition.getPieceAtIndex(i).isWhite() == acceptedPosition.isWhiteToMove()){
                kingIndex = i;
                break;
            }
        }

        inCheck = enemyCanSeeSquare(kingIndex);

        return inCheck;
    }


    public List<Move> generateMoves() {
    long startTime = System.nanoTime();
      var moves =  generateMoves(true);
    long finishTime = System.nanoTime();

    times.add(finishTime - startTime);
    long sum = 0;

    for(int i = 0; i < times.size(); i++) {
        sum += times.get(i);
    }

    long avg = sum / times.size();

    System.out.println("Average time to compute: " + avg + " nanoseconds");

    return moves;
    }

    private List<Move> generateMoves(boolean withCheckLogic) {
          List<Move> moves = new ArrayList<>();
   
        for (int index = 0; index < 64; index++) {
              
            Piece piece = acceptedPosition.getPieceAtIndex(index);
              
            if (piece == Piece.emptyTile) {
                continue;
            }

            if (piece.isWhite() != acceptedPosition.isWhiteToMove()) {
                continue;
            }

            if(piece.isSlidingPiece()) {
                moves.addAll(generateSlidingMoves(index, piece));
            }

            if(piece.isType(Piece.pawn)) {
                List<Move> pawnMoves = new ArrayList<>();
                pawnMoves.addAll(generatePawnAttackMoves(index));

                if(withCheckLogic) {
                    System.out.println("here1");
                    pawnMoves.addAll(generatePawnMovementMoves(index));
                    System.out.println("here2");
                }
                //promotion check
                for(int i = 0; i < pawnMoves.size(); i++) {
                    if(pawnMoves.get(i).targetSquare >= 56 || pawnMoves.get(i).targetSquare <= 7) {
                        Move move = pawnMoves.get(i);
                        moves.add(new Move(move.startSquare, move.targetSquare, false, false, true));
                        pawnMoves.remove(i);
                    }
                }

                moves.addAll(pawnMoves);

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
            moves.addAll(generateEnPessant());

           for (int i = moves.size() - 1; i >= 0; i--) {

            if(inCheck(moves.get(i))) {
                moves.remove(i);
            }
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

    public Move getLegalMove(int startingIndex, int targetIndex) {
        List<Move> legalMoves = generateMoves();
        Move requestedMove = new Move(startingIndex, targetIndex);

        for(int i = 0; i < legalMoves.size(); i++) {
            if (requestedMove.startSquare == legalMoves.get(i).startSquare && requestedMove.targetSquare == legalMoves.get(i).targetSquare) {
                return legalMoves.get(i);
            }
        }
        return null;
    }
}
