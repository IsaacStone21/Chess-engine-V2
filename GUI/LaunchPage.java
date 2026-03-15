package GUI;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import Engine.FENUtil;

public class LaunchPage {
    private JFrame frame;

    private FENUtil FENConverter = new FENUtil();
    
    private Window window;
   

    public static Piece blackPawnPNG = new Piece("GUI\\PNGs\\Black_Pawn.png", false, false);
    public static Piece whitePawnPNG = new Piece("GUI\\PNGs\\White_Pawn.png", true, false);
    public static Piece blackKnightPNG = new Piece("GUI\\PNGs\\Black_Knight.png", false, false);
    public static Piece whiteKnightPNG = new Piece("GUI\\PNGs\\White_Knight.png", true, false);
    public static Piece blackBishopPNG = new Piece("GUI\\PNGs\\Black_Bishop.png", false, true);
    public static Piece whiteBishopPNG = new Piece("GUI\\PNGs\\White_Bishop.png", true, true);
    public static Piece blackRookPNG = new Piece("GUI\\PNGs\\Black_Rook.png", false, true);
    public static Piece whiteRookPNG = new Piece("GUI\\PNGs\\White_Rook.png", true, true);
    public static Piece blackQueenPNG = new Piece("GUI\\PNGs\\Black_Queen.png", false, true);
    public static Piece whiteQueenPNG = new Piece("GUI\\PNGs\\White_Queen.png", true, true);
    public static Piece blackKingPNG = new Piece("GUI\\PNGs\\Black_King.png", false, false);
    public static Piece whiteKingPNG = new Piece("GUI\\PNGs\\White_King.png", true, false);
    public static final int tileSize = 55;

  
 
    public LaunchPage() {
        frame = new JFrame();
        frame.setSize(8*tileSize + 10, 8*tileSize + 35);
        
        frame.setTitle("Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        window = new Window();
        
        window.setVisible(true);
        
        setStartPosition();

        frame.add(window);

        frame.setVisible(true);
    }

   
    public void setStartPosition() {

        Piece[] startPosition = FENConverter.FENtoPosition(FENUtil.startFEN);
        for (int i = 0; i < 64; i++) {

            window.setTile(startPosition[i], i);
        }
    }

    public void updatePosition(String FEN) {
        ImageIcon[] icons = FENConverter.FENtoPosition(FEN);

        for (int index = 0; index < 64; index++) {
            window.setTile(icons[index], index);
        }
        window.repaint();
    }
}