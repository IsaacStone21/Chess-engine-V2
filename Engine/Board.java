package Engine;

import java.util.ArrayList;
import java.util.List;


public class Board extends Position {

    private static Position position;
    private static List<Move> acceptedMoves;
   

    static Board board;

    private Board() {
        this.setStartPosition();
        acceptedMoves = new ArrayList<>();
        setColor(true);
    }



    public static Board createBoard() {
        if (board == null) {
            board = new Board();
        } 
        return board;
    }

   public static Position getPosition() {
    return position;
   }

   public void logMove(Move acceptedMove) {
    acceptedMoves.addLast(acceptedMove);
    playMove(acceptedMove);
   }

   private void setStartPosition() {
    position = new Position();
   }

}
