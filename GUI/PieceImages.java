package GUI;

import java.util.HashMap;
import java.util.Map;

//converts between FEN piece placement and the GUI's array of piece images; null means an empty square
public class PieceImages {

    private static Map<PiecePNG, Character> PNGToCharMap = new HashMap<>();

    private static Map<Character, PiecePNG> charToPNGMap = new HashMap<>();

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
                } else {
                    if(emptyTiles != 0) FEN.append(emptyTiles);
                    FEN.append(pieceType);
                    emptyTiles = 0;
                }

                if(col == 7 && (emptyTiles != 0)) FEN.append(emptyTiles);
            }
            if (row != 7) FEN.append("/");
        }

        return FEN.toString();
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
}
