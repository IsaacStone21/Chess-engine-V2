package PI;

import EngineUtil.*;

public class PositionTracker {

    public short getPlayerMove(String scannedPosition, Position position, short[] moves) {
        short move = Move.none;

        Position savedPosition = new Position(position);

        for(int i = 0; i < moves.length; i++) {
            savedPosition.playMove(moves[i]);

            String newPositionColorString = getPositionToColorString(savedPosition);

            if(newPositionColorString.equals(scannedPosition)) {
                move = moves[i];
                break;
            }
            savedPosition.undoMove();
        }

        return move;
    }


    public static String getPositionToColorString(Position position) {

        char[] colorString = new char[64];

        long whitePieces = position.getWhitePieces();
        long blackPieces = position.getBlackPieces();

        for(int i = 0; i < 64; i++) {
            if((whitePieces & (1L << i)) != 0) {
                colorString[i] = 'W';
            } else if((blackPieces & (1L << i)) != 0) {
                colorString[i] = 'B';
            } else {
                colorString[i] = 'E';
            }
        }

        return new String(colorString);
    }
    
}
