package GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import Engine.Board;
import Engine.FENUtil;
import Engine.MoveGenerator;


public class BoardInterface extends JPanel{

    private int tileSize = LaunchPage.tileSize;
    private final Color white = new Color(0xcba88a);
    private final Color black = new Color(0x8b5122);

    PiecePNG pieces[] = new PiecePNG[64];
    Board board;
    MoveGenerator moveGenerator = new MoveGenerator();
   
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


    public BoardInterface() {

    image = null;
    imageCorner = new Point(0, 0);
    board = Board.createBoard();
    board.whiteToMove = true;

    this.setSize(8*tileSize + 10, 8*tileSize + 35);

        ClickListener clickListener = new ClickListener();
        this.addMouseListener(clickListener);

        DragListener dragListener = new DragListener();
        this.addMouseMotionListener(dragListener);

        
    }

   
    public void setTile(PiecePNG newPiece, int index) {
        pieces[index] = newPiece;
        }

    public void updateBoard() {
        board.setPositionFromFEN(FENUtil.startFEN);

    }

    public void updateBoardInterface() {
        pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.square));
        repaint();
    }

        public int getIndex(Point point) {
            double row = (point.getY() - point.getY()%tileSize)/ tileSize;
            double col = (point.getX() - point.getX()%tileSize)/ tileSize;

            int index = (int)(8*row + col);
            return index;
        }

        public PiecePNG[] getBoard() {
            return pieces;
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
            startIndex = index;
            image = pieces[index];
            pieces[index] = null;

            int row = (index - (index % 8)) / 8;
            int col = index - 8*row;
            int newX = col*tileSize + offset;
            int newY = row*tileSize + offset;

            imageCorner = new Point(newX, newY);

        }

        public void mouseReleased(MouseEvent e) {
            int targetIndex = getIndex(currentPoint);

            int row;
            int col;
            int newX;
            int newY;

            boolean isLegalMove = moveGenerator.isLegalMove(startIndex, targetIndex);

            if ((image != null && isLegalMove) ) {
            pieces[targetIndex] = image;
            //updateBoard();
            board.whiteToMove = !board.whiteToMove;

            row = (targetIndex - (targetIndex % 8)) / 8;
            col = targetIndex - 8*row;
            newX = col*tileSize + offset;
            newY = row*tileSize + offset;

            board.updateBoard(FENUtil.PNGPositionToFEN(pieces));

            } else {
                System.out.println("Illegal Move");
                row = (startIndex - (startIndex % 8)) / 8;
                col = startIndex - 8*row;
                newX = col*tileSize + offset;
                newY = row*tileSize + offset;
                pieces[startIndex] = image;
            }
            
            
            imageCorner = new Point(newX, newY);

            
            repaint();
        }
    }

    private class DragListener extends MouseMotionAdapter { 
        public void mouseDragged(MouseEvent e) {
            currentPoint = e.getPoint();

            imageCorner.translate((int)(currentPoint.getX() - prevPt.getX()), (int)(currentPoint.getY() - prevPt.getY()));

            prevPt = currentPoint;
            repaint();
        }
    }
    
}
