package Engine;

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
    private boolean wrapsBoard(Move move) {
        int startingIndex = move.startSquare;
        int targetIndex = move.targetSquare;

        int startRow = (startingIndex - (startingIndex % 8)) / 8;
        int startCol = startingIndex - 8 * startRow;

        int attackedRow = (targetIndex - (targetIndex % 8)) / 8;
        int attackedCol = targetIndex - 8 * attackedRow;

        return Math.abs(attackedCol - startCol) > 2;
    }


    public int getNumLegalMoves(Position currentPosition) {
        return generateMoves(currentPosition).size();
    }


    private List<Move> generateSlidingMoves(int startingIndex, Piece piece, Position currentPosition) {
        List<Move> moves = new ArrayList<>();
    
        int startDirIndex = piece.isType(Piece.bishop) ? 4 : 0;
        int endDirIndex = piece.isType(Piece.rook) ? 4 : 8;
        boolean isWhite = piece.isWhite();

        for(int directionIndex = startDirIndex; directionIndex < endDirIndex; directionIndex++) {
            for (int dist = 1; dist <= numSquaresToEdge[startingIndex][directionIndex]; dist++) {
                int targetSquare = startingIndex + (dist * directionOffsets[directionIndex]);

                Piece targetPiece = currentPosition.getPieceAtIndex(targetSquare);
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

    private List<Move> generatePawnMovementMoves(int startingIndex, Position currentPosition) {
        List<Move> moves = new ArrayList<>();

        if(startingIndex < 8 || startingIndex > 55) {
            return moves;
        }

        boolean isWhitePawn = currentPosition.getPieceAtIndex(startingIndex).isWhite();

        int row = (startingIndex - (startingIndex % 8)) / 8;
        boolean onStartSquare = (isWhitePawn && row == 6) || (!isWhitePawn && row == 1);

        int directionIndex = isWhitePawn ? directionOffsets[0] : directionOffsets[1];

        if (currentPosition.getPieceAtIndex(startingIndex + directionIndex).isType(Piece.empty)) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex));

          if (onStartSquare && currentPosition.getPieceAtIndex(startingIndex + 2*directionIndex).isType(Piece.empty)) {
            moves.add(new Move(startingIndex, startingIndex + 2*directionIndex));
          }

        }

        return moves;
    }

    private List<Move> generatePawnAttackMoves(int startingIndex, Position currentPosition) {
        List<Move> moves = new ArrayList<>();
            if(startingIndex <= 7 || startingIndex >= 56) {
                return moves;
            }
        boolean isWhitePawn = currentPosition.isWhiteToMove();

        int directionIndex = isWhitePawn ? directionOffsets[0] : directionOffsets[1];

        Piece attackedPiece = currentPosition.getPieceAtIndex(startingIndex + directionIndex - 1);
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex - 1));
        }


        attackedPiece = currentPosition.getPieceAtIndex(startingIndex + directionIndex + 1);
        
        if(attackedPiece.ID != 0 && attackedPiece.isWhite() != isWhitePawn) {
            moves.add(new Move(startingIndex, startingIndex + directionIndex + 1));
        }

        return moves;
    }

    private List<Move> generateEnPessant(Position currentPosition) {
        List<Move> moves = new ArrayList<>();

        if (currentPosition.getLastMove() == null) {
            return moves;
        }

        Move lastMove = currentPosition.getLastMove();


        boolean whiteToMove = currentPosition.isWhiteToMove();

        int startIndex = lastMove.startSquare;
        int endIndex = lastMove.targetSquare;

        int row = (startIndex - (startIndex % 8)) / 8;

        int offset = (whiteToMove ? directionOffsets[1] : directionOffsets[0]);

        if (row != (whiteToMove ? 1 : 6) || startIndex + 2*offset != endIndex) {
            return moves;
        }

        boolean isCorrectPawn = currentPosition.getPieceAtIndex(endIndex + directionOffsets[2]).isWhite() == whiteToMove && currentPosition.getPieceAtIndex(endIndex + directionOffsets[2]).isType(Piece.pawn);
        if(isCorrectPawn) {
            moves.add(new Move(endIndex + directionOffsets[2], endIndex - offset, true, false, false));
        }

            isCorrectPawn = currentPosition.getPieceAtIndex(endIndex + directionOffsets[3]).isWhite() == whiteToMove && currentPosition.getPieceAtIndex(endIndex + directionOffsets[3]).isType(Piece.pawn);        
 
        if(isCorrectPawn) {
            moves.add(new Move(endIndex + directionOffsets[3], endIndex - offset, true, false, false));
        }
        return moves;
    } 

    private List<Move> generateKingMoves(int startingIndex, Position currentPosition) {
        List<Move> moves = new ArrayList<>();

        boolean isWhiteKing = currentPosition.getPieceAtIndex(startingIndex).isWhite();

        for(int i = 0; i < 8; i++) {
            int attackedIndex = startingIndex + directionOffsets[i];

            Move newMove = new Move(startingIndex, attackedIndex);

            if(attackedIndex > 63 || attackedIndex < 0 || wrapsBoard(newMove)) {
                continue;
            }

            Piece attackedPiece = currentPosition.getPieceAtIndex(attackedIndex);

            if (attackedPiece.ID != 0 && attackedPiece.isWhite() == isWhiteKing) continue;

            moves.add(newMove);
        }


        return moves;
    }

    private List<Move> generateKnightMoves(int startingIndex, Position currentPosition) {
        List<Move> moves = new ArrayList<>();

        Piece attackingPiece = currentPosition.getPieceAtIndex(startingIndex);
        boolean isWhiteKnight = attackingPiece.isWhite();

        for(int i = 0; i < 8; i++) {
            int attackedIndex = startingIndex + knightOffsets[i];

            Move newMove = new Move(startingIndex, attackedIndex);

            if(attackedIndex > 63 || attackedIndex < 0 || wrapsBoard(newMove)) {
                continue;
            }

            Piece attackedPiece = currentPosition.getPieceAtIndex(attackedIndex);

          
            if(attackedPiece.ID != 0 && attackedPiece.isWhite() == isWhiteKnight) {
                continue;
            }

            moves.add(newMove);

        }

        return moves;
    }

    private List<Move> generateCastlingMoves(Position currentPosition) {

        List<Move> moves = new ArrayList<>();
        boolean whiteToMove = currentPosition.isWhiteToMove();

        //checks if the king and rook involved have moved at anytime during the game
        boolean canCastleKingside = whiteToMove ? currentPosition.canCastle(3) : currentPosition.canCastle(1);

        boolean canCastleQueenSide = whiteToMove ? currentPosition.canCastle(2) : currentPosition.canCastle(0);
        

        if(!(canCastleKingside || canCastleQueenSide)) {
            return moves;
        }


        int row = whiteToMove ? 7 : 0;

        //check if the king is castling while in check or through check
        //checks if it castles into check later
        canCastleKingside = canCastleKingside && !enemyCanSeeSquare(8*row + 4, currentPosition) && !enemyCanSeeSquare(8*row + 5, currentPosition);


        canCastleQueenSide = canCastleQueenSide && !enemyCanSeeSquare(8*row + 4, currentPosition) && !enemyCanSeeSquare(8*row + 3, currentPosition);

        //checks if there is pieces in the path of the rook and king
        canCastleKingside = canCastleKingside && (currentPosition.getPieceAtIndex(8*row + 5).isType(Piece.empty)) && (currentPosition.getPieceAtIndex(8*row + 6).isType(Piece.empty));
        canCastleQueenSide = canCastleQueenSide && (currentPosition.getPieceAtIndex(8*row + 1).isType(Piece.empty)) && (currentPosition.getPieceAtIndex(8*row + 2).isType(Piece.empty)) && (currentPosition.getPieceAtIndex(8*row + 3).isType(Piece.empty));


        if(canCastleKingside) {
          moves.add(whiteToMove ? whiteKingsideCastle : blackKingsideCastle);
        }

        if (canCastleQueenSide) {
            moves.add(whiteToMove ? whiteQueensideCastle : blackQueensideCastle);
        }

        return moves;

    }

    private boolean enemyCanSeeSquare(int index, Position position) {
        boolean canSeeSquare = false;

        //switch board color to check if enemy pieces can see king
        Position positionCopy = new Position(position);
        
        positionCopy.switchTurns();

        List<Move> moves = generateMoves(false, positionCopy);

        for(int i = 0; i < moves.size() && !canSeeSquare; i++) {
            canSeeSquare = moves.get(i).targetSquare == index;
        }      

        return canSeeSquare;
    }

    private boolean inCheck(Move desiredMove, Position currentPosition) {

        boolean inCheck = false;
        int startingIndex = desiredMove.startSquare;  
        int targetIndex = desiredMove.targetSquare;
        int kingIndex = 0;

        Position positionCopy = new Position(currentPosition);
 
        //hypothetical move
        positionCopy.playMove(new Move(startingIndex, targetIndex));

        positionCopy.switchTurns();


        for(int i = 0; i < 64; i++) {
            if(positionCopy.getPieceAtIndex(i).isType(Piece.king) && positionCopy.getPieceAtIndex(i).isWhite() == positionCopy.isWhiteToMove()){
                kingIndex = i;
                break;
            }
        }
        

        inCheck = enemyCanSeeSquare(kingIndex, positionCopy);

        //undo hypothetical move
        // board.edit(startingIndex, board.getPieceAtIndex(targetIndex));
        // board.edit(targetIndex, capturedPiece);


        return inCheck;
    }

    public boolean inCheck(Position currentPosition) {
        boolean inCheck = false;
        int kingIndex = 100;

        for(int i = 0; i < 64; i++) {
            if(currentPosition.getPieceAtIndex(i).isType(Piece.king) && currentPosition.getPieceAtIndex(i).isWhite() == currentPosition.isWhiteToMove()){
                System.out.println("King found");
                kingIndex = i;
                break;
            }
        }

        if(kingIndex != 100) {
            inCheck = enemyCanSeeSquare(kingIndex, currentPosition);
            return inCheck;
        }

        System.out.println("King not found");
        return inCheck;
    
    }


    public List<Move> generateMoves(Position currentPosition) {
        
    long startTime = System.nanoTime();
      var moves =  generateMoves(true, currentPosition);
    long finishTime = System.nanoTime();

    times.add(finishTime - startTime);
    long sum = 0;

    for(int i = 0; i < times.size(); i++) {
        sum += times.get(i);
    }

    long avg = sum / times.size();

    System.out.println("Average time to compute: " + avg + " nanoseconds");
    System.out.println("Num legal Moves: " + moves.size());

    return moves;
    }

    private List<Move> generateMoves(boolean withCheckLogic, Position currentPosition) {
          List<Move> moves = new ArrayList<>();
   
        for (int index = 0; index < 64; index++) {
              
            Piece piece = currentPosition.getPieceAtIndex(index);
              
            if (piece == Piece.emptyTile) {
                continue;
            }

            if (piece.isWhite() != currentPosition.isWhiteToMove()) {
                continue;
            }

            if(piece.isSlidingPiece()) {
                moves.addAll(generateSlidingMoves(index, piece, currentPosition));
            }

            if(piece.isType(Piece.pawn)) {
                List<Move> pawnMoves = new ArrayList<>();
                pawnMoves.addAll(generatePawnAttackMoves(index, currentPosition));

                if(withCheckLogic) {
                    pawnMoves.addAll(generatePawnMovementMoves(index, currentPosition));
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
                moves.addAll(generateKnightMoves(index, currentPosition));
            }

            if(piece.isType(Piece.king)){ 
                moves.addAll(generateKingMoves(index, currentPosition));
            }
        }
        
        if(withCheckLogic) {

            moves.addAll(generateCastlingMoves(currentPosition));
            moves.addAll(generateEnPessant(currentPosition));

           for (int i = moves.size() - 1; i >= 0; i--) {

            if(inCheck(moves.get(i), currentPosition)) {
                moves.remove(i);
            }
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
