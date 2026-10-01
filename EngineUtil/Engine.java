package EngineUtil;

public class Engine {
    //scores are in centipawns; a positive number means the position is favorable for white
    private static final int pawnValue = 100;
    private static final int knightValue = 300;
    private static final int bishopValue = 300;
    private static final int rookValue = 500;
    private static final int queenValue = 900;

    //INF bounds every real score; it's a plain constant because -Integer.MIN_VALUE overflows back to itself
    private static final int INF = 1_000_000;
    //mate scores are MATE minus the ply the mate happens at, so faster mates score higher
    private static final int MATE = 100_000;

    
    private static final int maxPly = 64;

    private static MoveGenerator moveGenerator;
    private static Board board;

    static {
        moveGenerator = new MoveGenerator();
        board = Board.createBoard();
    }

    public static void initiateEngine(){
        new Engine();
    }


    //material balance from white's point of view; kings are left out since both are always on the board
    public static int evaluate(Position position) {
        int white = Long.bitCount(position.whitePawns) * pawnValue
                + Long.bitCount(position.whiteKnights) * knightValue
                + Long.bitCount(position.whiteBishops) * bishopValue
                + Long.bitCount(position.whiteRooks) * rookValue
                + Long.bitCount(position.whiteQueens) * queenValue;

        int black = Long.bitCount(position.blackPawns) * pawnValue
                + Long.bitCount(position.blackKnights) * knightValue
                + Long.bitCount(position.blackBishops) * bishopValue
                + Long.bitCount(position.blackRooks) * rookValue
                + Long.bitCount(position.blackQueens) * queenValue;

        return white - black;
    }

    //negamax needs every score from the side to move's point of view
    private static int evaluateRelative(Position position) {
        int eval = evaluate(position);
        return position.isWhiteToMove() ? eval : -eval;
    }


    //one move buffer per ply, so the search never allocates and a child never overwrites its parent's moves
    private static final short[][] searchBuffers = new short[maxPly][MoveGenerator.maxMoves];

    public static short findBestMove(Position position, int depth) {
        short[] moves = searchBuffers[0];
        int numMoves = moveGenerator.generateMoves(position, moves);

        short bestMove = Move.none;
        int alpha = -INF;

        for(int index = 0; index < numMoves; index++) {
            position.playMove(moves[index]);
            int score = -negamax(position, depth - 1, 1, -INF, -alpha);
            position.undoMove();

            if(score > alpha) {
                alpha = score;
                bestMove = moves[index];
            }
        }

        return bestMove;
    }

    //returns the score of the position for the side to move, searched depth plies deep;
    //alpha is what the side to move is already guaranteed, beta is the most the opponent will allow
    private static int negamax(Position position, int depth, int ply, int alpha, int beta) {
        if(depth == 0 || ply == maxPly) {
            return evaluateRelative(position);
        }

        short[] moves = searchBuffers[ply];
        int numMoves = moveGenerator.generateMoves(position, moves);

        //the generator is strictly legal, so no moves means checkmate or stalemate
        if(numMoves == 0) {
            return moveGenerator.inCheck(position) ? -MATE + ply : 0;
        }

        for(int index = 0; index < numMoves; index++) {
            position.playMove(moves[index]);
            int score = -negamax(position, depth - 1, ply + 1, -beta, -alpha);
            position.undoMove();

            //the opponent already has a better option earlier in the tree, so they'll never allow this position
            if(score >= beta) {
                return beta;
            }
            if(score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }


    //one move buffer per remaining depth, so the recursion never allocates
    private static short[][] moveBuffers = new short[0][];

    public static long getNumPossiblePositions(int depth) {
        if(moveBuffers.length < depth) {
            moveBuffers = new short[depth][MoveGenerator.maxMoves];
        }

        return countPositions(board.getPosition(), depth);
    }

    private static long countPositions(Position position, int depth) {
        if(depth == 0) {
            return 1;
        }

        short[] moves = moveBuffers[depth - 1];
        long numMoves = moveGenerator.generateMoves(position, moves);
        long numPositions = 0;

        for(int index = 0; index < numMoves; index++) {
            position.playMove(moves[index]);
            numPositions += countPositions(position, depth - 1);
            position.undoMove();
        }

        return numPositions;
    }

}
