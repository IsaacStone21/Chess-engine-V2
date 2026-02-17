package GUI;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LaunchPage implements ActionListener {
    private JFrame frame;
    // private JPanel[] whiteTiles = new JPanel[32];
    // private JPanel[] blackTiles = new JPanel[32];
    private JLabel[] pieces = new JLabel[64];
    private JPanel[] tiles = new JPanel[64];

    private final int tileSize = 55;
    private final Color white = new Color(0xcba88a);
    private final Color black = new Color(0x8b5122);

    private ImageIcon blackPawnPNG = new ImageIcon("GUI\\PNGs\\Black_Pawn.png");
    private ImageIcon whitePawnPNG = new ImageIcon("GUI\\PNGs\\White_Pawn.png");
    private ImageIcon blackKnightPNG = new ImageIcon("GUI\\PNGs\\Black_Knight.png");
    private ImageIcon whiteKnightPNG = new ImageIcon("GUI\\PNGs\\White_Knight.png");
    private ImageIcon blackBishopPNG = new ImageIcon("GUI\\PNGs\\Black_Bishop.png");
    private ImageIcon whiteBishopPNG = new ImageIcon("GUI\\PNGs\\White_Bishop.png");
    private ImageIcon blackRookPNG = new ImageIcon("GUI\\PNGs\\Black_Rook.png");
    private ImageIcon whiteRookPNG = new ImageIcon("GUI\\PNGs\\\\White_Rook.png");
    private ImageIcon blackQueenPNG = new ImageIcon("GUI\\PNGs\\Black_Queen.png");
    private ImageIcon whiteQueenPNG = new ImageIcon("GUI\\PNGs\\White_Queen.png");
    private ImageIcon blackKingPNG = new ImageIcon("GUI\\PNGs\\Black_King.png");
    private ImageIcon whiteKingPNG = new ImageIcon("GUI\\PNGs\\White_King.png");

    
    
 
    public LaunchPage() {
        frame = new JFrame();
        frame.setSize(8*tileSize + 15, 8*tileSize + 30);
        frame.setLayout(null);
         frame.setTitle("Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        
       
        for (int row = 0; row < 8; row++) {

            for (int col = 0; col < 8; col++) {
                int index = row*8 + col;
                boolean isWhite = (row + col) % 2 == 0;
                Color color = isWhite ? white : black;

                tiles[index] = new JPanel();
                tiles[index].setBounds(col * tileSize, row * tileSize, tileSize, tileSize);
            
                tiles[index].setVisible(true);
                tiles[index].setBackground(color);
                frame.add(tiles[index]);
            }
        }

        for (int i = 0; i < 64; i++) {
            pieces[i] = new JLabel();
            tiles[i].add(pieces[i]);
        }

       
        
        setStartPosition();

       
    }

    
    public void setStartPosition() {

//--------------Black Pieces----------------
        pieces[0].setIcon(blackRookPNG);
        pieces[1].setIcon(blackKnightPNG);
        pieces[2].setIcon(blackBishopPNG);
        pieces[3].setIcon(blackQueenPNG);
        pieces[4].setIcon(blackKingPNG);
        pieces[5].setIcon(blackBishopPNG);
        pieces[6].setIcon(blackKnightPNG);
        pieces[7].setIcon(blackRookPNG);

        for (int i = 8; i < 16; i++) {
            pieces[i].setIcon(blackPawnPNG);
        }

//--------------White Pieces---------------
        for (int i = 48; i < 56; i++) {
            pieces[i].setIcon(whitePawnPNG);
        }
        pieces[56].setIcon(whiteRookPNG);
        pieces[57].setIcon(whiteKnightPNG);
        pieces[58].setIcon(whiteBishopPNG);
        pieces[59].setIcon(whiteQueenPNG);
        pieces[60].setIcon(whiteKingPNG);
        pieces[61].setIcon(whiteBishopPNG);
        pieces[62].setIcon(whiteKnightPNG);
        pieces[63].setIcon(whiteRookPNG);

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        
    }
    
}
