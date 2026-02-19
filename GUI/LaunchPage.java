package GUI;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
 

public class LaunchPage implements ActionListener {
    private JFrame frame;

  
    
    private Window window;
   

    public static final int tileSize = Window.tileSize;
  

    

    private ImageIcon blackPawnPNG = new ImageIcon("GUI\\PNGs\\Black_Pawn.png");
    private ImageIcon whitePawnPNG = new ImageIcon("GUI\\PNGs\\White_Pawn.png");
    private ImageIcon blackKnightPNG = new ImageIcon("GUI\\PNGs\\Black_Knight.png");
    private ImageIcon whiteKnightPNG = new ImageIcon("GUI\\PNGs\\White_Knight.png");
    private ImageIcon blackBishopPNG = new ImageIcon("GUI\\PNGs\\Black_Bishop.png");
    private ImageIcon whiteBishopPNG = new ImageIcon("GUI\\PNGs\\White_Bishop.png");
    private ImageIcon blackRookPNG = new ImageIcon("GUI\\PNGs\\Black_Rook.png");
    private ImageIcon whiteRookPNG = new ImageIcon("GUI\\PNGs\\White_Rook.png");
    private ImageIcon blackQueenPNG = new ImageIcon("GUI\\PNGs\\Black_Queen.png");
    private ImageIcon whiteQueenPNG = new ImageIcon("GUI\\PNGs\\White_Queen.png");
    private ImageIcon blackKingPNG = new ImageIcon("GUI\\PNGs\\Black_King.png");
    private ImageIcon whiteKingPNG = new ImageIcon("GUI\\PNGs\\White_King.png");
    private ImageIcon emptyTile = new ImageIcon("GUI\\PNGs\\Empty_Tile.png");

    
    
 
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

//--------------Black Pieces----------------
        window.setTile(blackRookPNG,0);
        window.setTile(blackKnightPNG,1);
        window.setTile(blackBishopPNG,2);
        window.setTile(blackQueenPNG,3);
        window.setTile(blackKingPNG,4);
        window.setTile(blackBishopPNG,5);
        window.setTile(blackKnightPNG,6);
        window.setTile(blackRookPNG,7);

        for (int i = 8; i < 16; i++) {
            window.setTile(blackPawnPNG, i);
        }


        for (int i = 16; i < 48; i++) {
            window.setTile(null, i);
        }

//--------------White Pieces---------------
        for (int i = 48; i < 56; i++) {
            window.setTile(whitePawnPNG, i);
        }
        window.setTile(whiteRookPNG, 56);
        window.setTile(whiteKnightPNG, 57);
        window.setTile(whiteBishopPNG, 58);
        window.setTile(whiteQueenPNG, 59);
        window.setTile(whiteKingPNG, 60);
        window.setTile(whiteBishopPNG, 61);
        window.setTile(whiteKnightPNG, 62);
        window.setTile(whiteRookPNG, 63);

    }


    @Override
    public void actionPerformed(ActionEvent e) {
        
    }

 
}
