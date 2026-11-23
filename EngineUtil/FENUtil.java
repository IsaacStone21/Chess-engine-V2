package EngineUtil;

import java.util.HashMap;
import java.util.Map;

public class FENUtil {

    public static final String startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR";

    private static Map<Character, Piece> charToPieceMap = new HashMap<>();

    private static Map<Integer, Character> pieceToCharMap = new HashMap<>();

    static {
        initializeMaps();
    }

    private static void initializeMaps() {
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

        public static String positionToFEN(Position pieces){
        StringBuilder FEN = new StringBuilder();
        int index;
        int emptyTiles = 0;

        for(int row = 0; row < 8; row++) {
            //reset empty tiles at the start of each row
            emptyTiles = 0;

            for(int col = 0; col < 8; col++) {
                index = 8*row + col;
                
                Character pieceType = pieceToCharMap.get(pieces.getPieceAtIndex(index).ID);

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