package GUI;
import javax.swing.JFrame;

import Engine.FENUtil;

public class LaunchPage {
    private JFrame frame;
    
    private GameManager window;
   

    public static PiecePNG blackPawnPNG = new PiecePNG("GUI\\PNGs\\Black_Pawn.png", false, false);
    public static PiecePNG whitePawnPNG = new PiecePNG("GUI\\PNGs\\White_Pawn.png", true, false);
    public static PiecePNG blackKnightPNG = new PiecePNG("GUI\\PNGs\\Black_Knight.png", false, false);
    public static PiecePNG whiteKnightPNG = new PiecePNG("GUI\\PNGs\\White_Knight.png", true, false);
    public static PiecePNG blackBishopPNG = new PiecePNG("GUI\\PNGs\\Black_Bishop.png", false, true);
    public static PiecePNG whiteBishopPNG = new PiecePNG("GUI\\PNGs\\White_Bishop.png", true, true);
    public static PiecePNG blackRookPNG = new PiecePNG("GUI\\PNGs\\Black_Rook.png", false, true);
    public static PiecePNG whiteRookPNG = new PiecePNG("GUI\\PNGs\\White_Rook.png", true, true);
    public static PiecePNG blackQueenPNG = new PiecePNG("GUI\\PNGs\\Black_Queen.png", false, true);
    public static PiecePNG whiteQueenPNG = new PiecePNG("GUI\\PNGs\\White_Queen.png", true, true);
    public static PiecePNG blackKingPNG = new PiecePNG("GUI\\PNGs\\Black_King.png", false, false);
    public static PiecePNG whiteKingPNG = new PiecePNG("GUI\\PNGs\\White_King.png", true, false);
    public static final int tileSize = 55;

  
 
    public LaunchPage() {
        frame = new JFrame();
        frame.setLayout(null);
        frame.setSize(8*tileSize + 10, 8*tileSize + 35);
        
        frame.setTitle("Chess");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        window = new GameManager();
        window.setLayout(null);
        
        frame.add(window);
        window.setVisible(true);

        frame.setVisible(true);
    }
}