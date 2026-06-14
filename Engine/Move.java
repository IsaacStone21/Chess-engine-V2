package Engine;

import java.util.Objects;

public class Move {
        public int startSquare;
        public int targetSquare;
        public boolean isEnPessant;
        public boolean isCastling;
        public boolean isPromotion;

        public Move(int startIndex, int targetIndex) {
            startSquare = startIndex;
            targetSquare = targetIndex;
            isEnPessant = false;
            isCastling = false;
            isPromotion = false;
        }

        public Move(int startIndex, int targetIndex, boolean enPessantMove, boolean castlingMove, boolean pawnPromotion) {
            startSquare = startIndex;
            targetSquare = targetIndex;
            isEnPessant = enPessantMove;
            isCastling = castlingMove;
            isPromotion = pawnPromotion;
        }

        @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        Move other = (Move) obj;

        return startSquare == other.startSquare &&
               targetSquare == other.targetSquare;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startSquare, targetSquare);
        }
    }
