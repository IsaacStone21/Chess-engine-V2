package Engine;

import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import GUI.LaunchPage;
import GUI.Piece;

public class FENUtil {

    public static final String startFEN = "rbnqknbr/pppppppp/8/8/8/8/PPPPPPPP/RBNQKNBR";
    
    Map<Piece, Character> pieceToCharMap = new HashMap<>();

    Map<Character, Piece> charToPieceMap = new HashMap<>();

    Map<Character, Integer> charToIDMap = new HashMap<>();


    public FENUtil() {   
        pieceToCharMap.put(LaunchPage.blackPawnPNG, 'p');
        pieceToCharMap.put(LaunchPage.whitePawnPNG, 'P');
        pieceToCharMap.put(LaunchPage.blackKingPNG, 'n');
        pieceToCharMap.put(LaunchPage.whiteKnightPNG, 'N');
        pieceToCharMap.put(LaunchPage.blackBishopPNG, 'b');
        pieceToCharMap.put(LaunchPage.whiteBishopPNG, 'B');
        pieceToCharMap.put(LaunchPage.blackRookPNG, 'r');
        pieceToCharMap.put(LaunchPage.whiteRookPNG, 'R');
        pieceToCharMap.put(LaunchPage.blackQueenPNG, 'q');
        pieceToCharMap.put(LaunchPage.whiteQueenPNG, 'Q');
        pieceToCharMap.put(LaunchPage.blackKingPNG, 'k');
        pieceToCharMap.put(LaunchPage.whiteKingPNG, 'K');
        pieceToCharMap.put(null, 'e');

        charToIDMap.put('p', Piece.black | Piece.pawn);
        charToIDMap.put('P', Piece.white | Piece.pawn);
        charToIDMap.put('n', Piece.black | Piece.knight);
        charToIDMap.put('N', Piece.white | Piece.knight);
        charToIDMap.put('b', Piece.black | Piece.bishop);
        charToIDMap.put('B', Piece.white | Piece.bishop);
        charToIDMap.put('r', Piece.black | Piece.rook);
        charToIDMap.put('R', Piece.white | Piece.rook);
        charToIDMap.put('q', Piece.black | Piece.queen);
        charToIDMap.put('Q', Piece.white | Piece.queen);
        charToIDMap.put('k', Piece.black | Piece.king);
        charToIDMap.put('K', Piece.white | Piece.king);
        charToIDMap.put('e', Piece.empty); 
        
        charToPieceMap.put('p', LaunchPage.blackPawnPNG);
        charToPieceMap.put('P', LaunchPage.whitePawnPNG);
        charToPieceMap.put('n', LaunchPage.blackKnightPNG);
        charToPieceMap.put('N', LaunchPage.whiteKnightPNG);
        charToPieceMap.put('b', LaunchPage.blackBishopPNG);
        charToPieceMap.put('B', LaunchPage.whiteBishopPNG);
        charToPieceMap.put('r', LaunchPage.blackRookPNG);
        charToPieceMap.put('R', LaunchPage.whiteRookPNG);
        charToPieceMap.put('q', LaunchPage.blackQueenPNG);
        charToPieceMap.put('Q', LaunchPage.whiteQueenPNG);
        charToPieceMap.put('k', LaunchPage.blackKingPNG);
        charToPieceMap.put('K', LaunchPage.whiteKingPNG);  
        
        
    }

    public String positionToFEN(ImageIcon[] piece){
        StringBuilder FEN = new StringBuilder();
        int index;
        boolean pastEmptyTiles = false;
        int emptyTiles = 0;
        for(int row = 0; row < 8; row++) {
            //reset empty tiles at the start of each row
            emptyTiles = 0;

            for(int col = 0; col < 0; col++) {
                index = 8*row + col;
                
                Character pieceType = pieceToCharMap.get(piece[index]);
                if(pieceType.equals('e')) {
                    emptyTiles++;
                    pastEmptyTiles = true;
                } else {
                    if(pastEmptyTiles) FEN.append(emptyTiles);
                    FEN.append(pieceType);
                    pastEmptyTiles = false;
                }

                if(col == 7 && pastEmptyTiles) FEN.append(emptyTiles);


            }
            if (row != 7) FEN.append("/");
        }

        String FENString = FEN.toString();

        System.out.println("FEN Position: " + FENString);
        return FENString;
    }

    public int getPieceID(Piece piece){
        return charToIDMap.get(pieceToCharMap.get(piece));
    }

    public Piece[] FENtoPosition(String FENString) {
        int index;
        int row = 0;
        int col = 0;
        Piece[] position = new Piece[64];
        

        for (int i = 0; i < FENString.length(); i++) {
            if(FENString.charAt(i) == '/') {
                col = 0;
                row++;
                continue;
            }
            index = 8*row + col;

            Character piece = FENString.charAt(i);

            if(Character.isDigit(piece)) {
                    int numEmptyTiles = Character.getNumericValue(piece);
                    for(int j = 0; j < numEmptyTiles; j++) {
                        position[index + j] = null;
                        col++;
                    }
                } else {
                    position[index] = charToPieceMap.get(piece);
                    col++;
                }
        }
            return position;
        }


    }