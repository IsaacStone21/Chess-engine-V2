package Engine;

import java.util.HashMap;
import java.util.Map;

import GUI.LaunchPage;
import GUI.PiecePNG;

public class FENUtil {

    public static final String startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";
    
    private static Map<PiecePNG, Character> PNGToCharMap = new HashMap<>();

    private static Map<Character, PiecePNG> charToPNGMap = new HashMap<>();

    private static Map<Character, Integer> charToIDMap = new HashMap<>();

    private static Map<Character, Piece> charToPieceMap = new HashMap<>();

    private static Map<Integer, Character> pieceToCharMap = new HashMap<>();

    static {
        initializeMaps();
    }

    private static void initializeMaps() {
        PNGToCharMap.put(LaunchPage.blackPawnPNG, 'p');
        PNGToCharMap.put(LaunchPage.whitePawnPNG, 'P');
        PNGToCharMap.put(LaunchPage.blackKnightPNG, 'n');
        PNGToCharMap.put(LaunchPage.whiteKnightPNG, 'N');
        PNGToCharMap.put(LaunchPage.blackBishopPNG, 'b');
        PNGToCharMap.put(LaunchPage.whiteBishopPNG, 'B');
        PNGToCharMap.put(LaunchPage.blackRookPNG, 'r');
        PNGToCharMap.put(LaunchPage.whiteRookPNG, 'R');
        PNGToCharMap.put(LaunchPage.blackQueenPNG, 'q');
        PNGToCharMap.put(LaunchPage.whiteQueenPNG, 'Q');
        PNGToCharMap.put(LaunchPage.blackKingPNG, 'k');
        PNGToCharMap.put(LaunchPage.whiteKingPNG, 'K');
        PNGToCharMap.put(null, 'e');

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
        charToIDMap.put('e', 0); 
        
        charToPNGMap.put('p', LaunchPage.blackPawnPNG);
        charToPNGMap.put('P', LaunchPage.whitePawnPNG);
        charToPNGMap.put('n', LaunchPage.blackKnightPNG);
        charToPNGMap.put('N', LaunchPage.whiteKnightPNG);
        charToPNGMap.put('b', LaunchPage.blackBishopPNG);
        charToPNGMap.put('B', LaunchPage.whiteBishopPNG);
        charToPNGMap.put('r', LaunchPage.blackRookPNG);
        charToPNGMap.put('R', LaunchPage.whiteRookPNG);
        charToPNGMap.put('q', LaunchPage.blackQueenPNG);
        charToPNGMap.put('Q', LaunchPage.whiteQueenPNG);
        charToPNGMap.put('k', LaunchPage.blackKingPNG);
        charToPNGMap.put('K', LaunchPage.whiteKingPNG);  
        
        charToPieceMap.put('p', new Piece(Piece.black,  Piece.pawn));
        charToPieceMap.put('P', new Piece(Piece.white,  Piece.pawn));
        charToPieceMap.put('n', new Piece(Piece.black,  Piece.knight));
        charToPieceMap.put('N', new Piece(Piece.white,  Piece.knight));
        charToPieceMap.put('b', new Piece(Piece.black,  Piece.bishop));
        charToPieceMap.put('B', new Piece(Piece.white,  Piece.bishop));
        charToPieceMap.put('r', new Piece(Piece.black,  Piece.rook));
        charToPieceMap.put('R', new Piece(Piece.white,  Piece.rook));
        charToPieceMap.put('q', new Piece(Piece.black,  Piece.queen));
        charToPieceMap.put('Q', new Piece(Piece.white,  Piece.queen));
        charToPieceMap.put('k', new Piece(Piece.black,  Piece.king));
        charToPieceMap.put('K', new Piece(Piece.white,  Piece.king));
        charToPieceMap.put('e', Piece.emptyTile); 

        pieceToCharMap.put(Piece.black | Piece.pawn, 'p');
        pieceToCharMap.put(Piece.white | Piece.pawn, 'P');
        pieceToCharMap.put(Piece.black | Piece.knight, 'n');
        pieceToCharMap.put(Piece.white |  Piece.knight, 'N');
        pieceToCharMap.put(Piece.black | Piece.bishop, 'b');
        pieceToCharMap.put(Piece.white | Piece.bishop, 'B');
        pieceToCharMap.put(Piece.black | Piece.rook, 'r');
        pieceToCharMap.put(Piece.white | Piece.rook, 'R');
        pieceToCharMap.put(Piece.black | Piece.queen, 'q');
        pieceToCharMap.put(Piece.white | Piece.queen, 'Q');
        pieceToCharMap.put(Piece.black | Piece.king, 'k');
        pieceToCharMap.put(Piece.white | Piece.king, 'K');
        pieceToCharMap.put(0, 'e'); 
    }

    public static String PNGPositionToFEN(PiecePNG[] piece){
        StringBuilder FEN = new StringBuilder();
        int index;
      
        int emptyTiles = 0;
        for(int row = 0; row < 8; row++) {
            //reset empty tiles at the start of each row
            emptyTiles = 0;

            for(int col = 0; col < 8; col++) {
                index = 8*row + col;
                
                Character pieceType = PNGToCharMap.get(piece[index]);
                if(pieceType.equals('e')) {
                    emptyTiles++;
                    // pastEmptyTiles = true;
                } else {
                    if(emptyTiles != 0) FEN.append(emptyTiles);
                    FEN.append(pieceType);
                    emptyTiles = 0;
                    //pastEmptyTiles = false;
                }

                if(col == 7 && (emptyTiles != 0)) FEN.append(emptyTiles);


            }
            if (row != 7) FEN.append("/");
        }

        String FENString = FEN.toString();

        //System.out.println("FEN Position: " + FENString);
        return FENString;
    }

    public static int getPieceID(PiecePNG piece){
        return charToIDMap.get(PNGToCharMap.get(piece));
    }

    public static PiecePNG[] FENtoPNGPosition(String FENString) {
        int index;
        int row = 0;
        int col = 0;
        PiecePNG[] position = new PiecePNG[64];
        

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
                    position[index] = charToPNGMap.get(piece);
                    col++;
                }
        }
            return position;
        }

        public static Piece[] FENtoPosition(String FENString) {
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
                        position[index + j] = Piece.emptyTile;
                        col++;
                    }
                } else {
                    position[index] = charToPieceMap.get(piece);
                    col++;
                }
        }
            return position;
        }

        public static String positionToFEN(Position position){
        StringBuilder FEN = new StringBuilder();
        int index;
        int emptyTiles = 0;

        for(int row = 0; row < 8; row++) {
            //reset empty tiles at the start of each row
            emptyTiles = 0;

            for(int col = 0; col < 8; col++) {
                index = 8*row + col;
                
                Character pieceType = pieceToCharMap.get(position.getPieceAtIndex(index).ID);

                if(pieceType.equals('e')) {
                    emptyTiles++;
                    
                } else {
                    if(emptyTiles != 0) FEN.append(emptyTiles);
                    FEN.append(pieceType);
                    emptyTiles = 0;
                }

                if(col == 7 && (emptyTiles != 0)) FEN.append(emptyTiles);


            }
            if (row != 7) FEN.append("/");
        }

        String FENString = FEN.toString();

        //System.out.println("Position To FEN: " + FENString);
        return FENString;
    }

    }