package GUI;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import EngineUtil.Board;
import EngineUtil.Engine;
import EngineUtil.FENUtil;
import EngineUtil.Move;
import EngineUtil.MoveGenerator;
import EngineUtil.Piece;


public class GameManager extends JPanel{

    private int tileSize = LaunchPage.tileSize;
    private final Color white = new Color(0xcba88a);
    private final Color black = new Color(0x8b5122);

    PiecePNG pieces[] = new PiecePNG[64];
   
    int startIndex;
    int targetIndex;
    PiecePNG image;
    int width;
    int height;
    Point imageCorner;
    Point prevPt;
    Point currentPoint;
    int index;
    final int offset = 5;

    boolean draggable;

    private enum GameStatus {
        ONGOING,
        CHECKMATE,
        STALEMATE
    }

    private boolean playerIsWhite;
    private GameStatus gameStatus;


    private Board board;
    private MoveGenerator moveGenerator;


    public GameManager() {

    image = null;
    imageCorner = new Point(0, 0);

    this.setSize(8*tileSize + 10, 8*tileSize + 35);

        ClickListener clickListener = new ClickListener();
        this.addMouseListener(clickListener);

        DragListener dragListener = new DragListener();
        this.addMouseMotionListener(dragListener);

        board = Board.createBoard();
        moveGenerator = new MoveGenerator();
        Engine.initiateEngine();

        String[] options = {"White", "Black"};

        //sets player color
          playerIsWhite = JOptionPane.showOptionDialog(null, "What color would you like to be?",
         "Color Selector", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
          LaunchPage.blackPawnPNG, options, 0) == 0;

        board.setPlayerColor(playerIsWhite);

        if (!playerIsWhite) {
            board.playEngineMove();
            pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.getPosition()));
        }
        gameStatus = GameStatus.ONGOING;

        updateBoardInterface();
    }


    private void updateBoardInterface() {
        pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.getPosition()));
        repaint(); 
    }

    private void updateGameStatus() {
        if(moveGenerator.getNumLegalMoves(board.getPosition()) != 0) {
            gameStatus = GameStatus.ONGOING;
        } else if (moveGenerator.inCheck(board.getPosition())) {
            gameStatus = GameStatus.CHECKMATE;
        } else {
            gameStatus = GameStatus.STALEMATE;
        }
    }

    private void endGame() {
        String text = gameStatus == GameStatus.CHECKMATE ? "Checkmate" : "Stalemate";

        if (gameStatus != GameStatus.ONGOING) {
            JPanel endScreen = new JPanel(null);

            int frameWidth = 250;
            int frameHeight = 125;

            // Center it on the 800x800 chess board
            int x = (8 * tileSize - frameWidth) / 2;
            int y = (8 * tileSize - frameHeight) / 2;

            endScreen.setBounds(x, y, frameWidth, frameHeight);
            endScreen.setBackground(Color.lightGray);

            JLabel checkmateText = new JLabel(text);
            checkmateText.setFont(new Font("Arial", Font.BOLD, 24));

            // Center the label manually
            checkmateText.setBounds(35, 40, 180, 40);

            endScreen.add(checkmateText);

            add(endScreen);

            revalidate();
            repaint();

            return;
        }
    }

    private void playMove(short move) {
        board.logMove(move);
        updateBoardInterface();
    }

    //returns the chosen piece type, or Piece.empty if the dialog was closed
    private int askPromotionPiece() {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};

        int choice = JOptionPane.showOptionDialog(this, "Promote pawn to:",
         "Promotion", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
          null, options, options[0]);

        //options line up with Move.promotionPieces
        return choice < 0 ? Piece.empty : Move.promotionPieces[choice];
    }

    private int getIndex(Point point) {
            double row = (point.getY() - point.getY()%tileSize)/ tileSize;
            double col = (point.getX() - point.getX()%tileSize)/ tileSize;

            int index = (int)(8*row + col);
            return index;
        }


    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        
    
        for (int i = 0; i < 64; i++) {
            int row = (i - (i % 8)) / 8;
            int col = i - 8 * row;
            boolean isWhite = (row + col) % 2 == 0;
            Color color = isWhite ? white : black;
            g.setColor(color);
            g.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
        }
        
        
        for (int i = 0; i < 64; i++) {
            if (pieces[i] != null) {
                int row = (i - (i % 8)) / 8;
                int col = i - 8 * row;
                int x = col * tileSize + offset;
                int y = row * tileSize + offset;
                pieces[i].paintIcon(this, g, x, y);
            }
        }
        
        
        if (image != null) {
            image.paintIcon(this, g, (int)imageCorner.getX(), (int)imageCorner.getY());
        }
    }


    private class ClickListener extends MouseAdapter{
        public void mousePressed(MouseEvent e) {
            prevPt = e.getPoint();
            int index = getIndex(prevPt);
        if (playerIsWhite == FENUtil.FENtoPosition(FENUtil.PNGPositionToFEN(pieces))[index].isWhite() && pieces[index] != null){
            startIndex = index;
            image = pieces[index];
            pieces[index] = null;

            int row = (index - (index % 8)) / 8;
            int col = index - 8*row;
            int newX = col*tileSize + offset;
            int newY = row*tileSize + offset;

            imageCorner = new Point(newX, newY);
            draggable = true;
            } else {
                draggable = false;
            }
        }

        public void mouseReleased(MouseEvent e) {
            if(draggable) {
                int targetIndex = getIndex(currentPoint);

            int row;
            int col;
            int newX;
            int newY;

            short requestedMove = moveGenerator.getLegalMove(startIndex, targetIndex, board.getPosition());

            if (requestedMove != Move.none && Move.isPromotion(requestedMove)) {
                int promotionPiece = askPromotionPiece();
                //closing the dialog cancels the move and puts the pawn back
                requestedMove = promotionPiece == Piece.empty ? Move.none
                        : moveGenerator.getLegalMove(startIndex, targetIndex, promotionPiece, board.getPosition());
            }


            if ((image != null && requestedMove != Move.none) ) {

            row = (targetIndex - (targetIndex % 8)) / 8;
            col = targetIndex - 8*row;
            newX = col*tileSize + offset;
            newY = row*tileSize + offset;

            //player move
            playMove(requestedMove);

            updateGameStatus();

            if(gameStatus != GameStatus.ONGOING) {
                endGame();
                pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.getPosition()));
                repaint();
                return;
            }

            pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.getPosition()));

            board.playEngineMove();

            updateGameStatus();
            
            if(gameStatus != GameStatus.ONGOING) {
                endGame();
                pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.getPosition()));
                repaint();
                return;
            }            

            pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.getPosition()));

            image = null;

            } else {
                row = (startIndex - (startIndex % 8)) / 8;
                col = startIndex - 8*row;
                newX = col*tileSize + offset;
                newY = row*tileSize + offset;
                pieces[startIndex] = image;
                image = null;
                draggable = false;
            }
                        
            imageCorner = new Point(newX, newY);

            repaint();
            }
            //System.out.println("Board FEN: " + FENUtil.positionToFEN(board.getPosition()));
            //System.out.println("Board eval: " + Engine.getPositionEval(board.getPosition()));
        }
    }

    private class DragListener extends MouseMotionAdapter { 
        public void mouseDragged(MouseEvent e) {
           if(draggable){
            currentPoint = e.getPoint();

            imageCorner.translate((int)(currentPoint.getX() - prevPt.getX()), (int)(currentPoint.getY() - prevPt.getY()));

            prevPt = currentPoint;
            repaint();
           } 
        }
    }
    
}
