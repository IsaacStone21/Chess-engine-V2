package GUI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JPanel;



public class Window extends JPanel{


 public static final int tileSize = 55;
    private final Color white = new Color(0xcba88a);
    private final Color black = new Color(0x8b5122);

    ImageIcon pieces[] = new ImageIcon[64];
   
    ImageIcon image;
    int width;
    int height;
    Point imageCorner;
    Point prevPt;
    Point currentPoint;
    int index;
    final int offset = 5;


    public Window() {


    image = null;
    imageCorner = new Point(0, 0);

    this.setSize(8*tileSize + 10, 8*tileSize + 35);

        ClickListener clickListener = new ClickListener();
        this.addMouseListener(clickListener);

        DragListener dragListener = new DragListener();
        this.addMouseMotionListener(dragListener);
    }

   
    public void setTile(ImageIcon newPiece, int index) {
        pieces[index] = newPiece;
        }

        public int getIndex(Point point) {
            double row = (point.getY() - point.getY()%tileSize)/ tileSize;
            double col = (point.getX() - point.getX()%tileSize)/ tileSize;

            int index = (int)(8*row + col);
            return index;
        }

        public ImageIcon[] getBoard() {
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
            image = pieces[getIndex(prevPt)];
            pieces[index] = null;

            int row = (index - (index % 8)) / 8;
            int col = index - 8*row;
            int newX = col*tileSize + offset;
            int newY = row*tileSize + offset;

            imageCorner = new Point(newX, newY);

        }

        public void mouseReleased(MouseEvent e) {
            int newIndex = getIndex(currentPoint);

            if (image != null) {
            pieces[newIndex] = image;
            }
            int row = (newIndex - (newIndex % 8)) / 8;
            int col = newIndex - 8*row;
            int newX = col*tileSize + offset;
            int newY = row*tileSize + offset;
            
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
