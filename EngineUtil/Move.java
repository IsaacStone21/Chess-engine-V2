package EngineUtil;

//moves are plain shorts; this class only holds the static helpers that pack and read them
public final class Move {
        //bit layout (bit 15 unused):
        //  0-5   start square
        //  6-11  target square
        //  12    promotion flag
        //  13    castling flag        | when the promotion flag is set, bits 13-14 instead hold
        //  14    en pessant flag      | the promotion piece: 00 knight, 01 bishop, 10 rook, 11 queen
        //a promotion can never be a castle or en pessant, so the two meanings of bits 13-14 never collide
        private static final int squareMask = 0b111111;
        private static final int targetShift = 6;
        private static final int promotionFlag = 1 << 12;
        private static final int castlingFlag = 1 << 13;
        private static final int enPessantFlag = 1 << 14;
        private static final int promotionPieceShift = 13;

        //a8 -> a8 can never be a real move, so 0 stands in for "no move"
        public static final short none = 0;

        //index = 2 bit promotion code stored in bits 13-14
        private static final int[] promotionCodeToPiece = {Piece.knight, Piece.bishop, Piece.rook, Piece.queen};

        //the pieces a pawn can promote to, strongest first
        public static final int[] promotionPieces = {Piece.queen, Piece.rook, Piece.bishop, Piece.knight};

        private Move() {}

        public static short create(int startIndex, int targetIndex) {
            return (short) (startIndex | (targetIndex << targetShift));
        }

        public static short castling(int startIndex, int targetIndex) {
            return (short) (create(startIndex, targetIndex) | castlingFlag);
        }

        public static short enPessant(int startIndex, int targetIndex) {
            return (short) (create(startIndex, targetIndex) | enPessantFlag);
        }

        public static short promotion(int startIndex, int targetIndex, int pieceType) {
            return (short) (create(startIndex, targetIndex) | promotionFlag | (promotionCode(pieceType) << promotionPieceShift));
        }

        private static int promotionCode(int pieceType) {
            switch(pieceType) {
                case Piece.knight: return 0;
                case Piece.bishop: return 1;
                case Piece.rook: return 2;
                case Piece.queen: return 3;
                default: throw new IllegalArgumentException("A pawn cannot promote to piece type " + pieceType);
            }
        }

        public static int startSquare(short move) {
            return move & squareMask;
        }

        public static int targetSquare(short move) {
            return (move >>> targetShift) & squareMask;
        }

        public static boolean isPromotion(short move) {
            return (move & promotionFlag) != 0;
        }

        //returns the piece type the pawn promotes to, or Piece.empty if this move isn't a promotion
        public static int promotionPiece(short move) {
            if(!isPromotion(move)) {
                return Piece.empty;
            }
            return promotionCodeToPiece[(move >>> promotionPieceShift) & 0b11];
        }

        public static boolean isCastling(short move) {
            return (move & (promotionFlag | castlingFlag)) == castlingFlag;
        }

        public static boolean isEnPessant(short move) {
            return (move & (promotionFlag | enPessantFlag)) == enPessantFlag;
        }
    }
