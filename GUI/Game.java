package GUI;

import Engine.*;
import javax.swing.JOptionPane;


public class Game {

    public enum Color{
        WHITE,
        BLACK
    }

    private enum GameStatus {
        ONGOING,
        CHECKMATE,
        STALEMATE
    }

    private Color playerColor;
    private Color activeColor;
    private GameStatus gameStatus;
    private Board board;


    private Game() {}

    public void startGame() {
        board = Board.createBoard();

        playerColor = setPlayerColor();

        activeColor = Color.WHITE;

        gameStatus = GameStatus.ONGOING;
    }

    private static Color setPlayerColor() {

        String[] options = {"White", "Black"};

        //Creates popup asking player what color they would like to play
        Color color = 
        JOptionPane.showOptionDialog(null, "What color would you like to be?",
         "Color Selector", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
          LaunchPage.blackPawnPNG, options, 0) == 0 ? Color.WHITE : Color.BLACK;

        return color;
    }

    private Move getEngineMove() {
        return board.getBoardMove();
    }

    private Move getPlayerMove() {
        return new Move(0, 0);
    }

    private void updateGameStatus() {
        if(board.getNumLegalMoves() != 0) {
            gameStatus = GameStatus.ONGOING; 
        } else if(board.inCheck()) {
            gameStatus = GameStatus.CHECKMATE;
        } else {
            gameStatus = GameStatus.STALEMATE;
        }
    }

}
