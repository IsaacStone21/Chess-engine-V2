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


    //piece-square tables: a bonus (or penalty) for a piece standing on each square, written from white's side
    //with rank 8 on the first row so they line up with square indices (0 = a8); black reads them through
    //square ^ 56, which flips the board vertically
    private static final int[] pawnMiddlegameTable = {
          0,  0,  0,  0,  0,  0,  0,  0,
         50, 50, 50, 50, 50, 50, 50, 50,
         10, 10, 20, 30, 30, 20, 10, 10,
          5,  5, 10, 25, 25, 10,  5,  5,
          0,  0,  0, 20, 20,  0,  0,  0,
          5, -5,-10,  0,  0,-10, -5,  5,
          5, 10, 10,-20,-20, 10, 10,  5,
          0,  0,  0,  0,  0,  0,  0,  0
    };

    //in the endgame a pawn is worth more the closer it gets to promoting
    private static final int[] pawnEndgameTable = {
          0,  0,  0,  0,  0,  0,  0,  0,
         80, 80, 80, 80, 80, 80, 80, 80,
         50, 50, 50, 50, 50, 50, 50, 50,
         30, 30, 30, 30, 30, 30, 30, 30,
         20, 20, 20, 20, 20, 20, 20, 20,
         10, 10, 10, 10, 10, 10, 10, 10,
         10, 10, 10, 10, 10, 10, 10, 10,
          0,  0,  0,  0,  0,  0,  0,  0
    };

    private static final int[] knightTable = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };

    private static final int[] bishopTable = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };

    private static final int[] rookTable = {
          0,  0,  0,  0,  0,  0,  0,  0,
          5, 10, 10, 10, 10, 10, 10,  5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
         -5,  0,  0,  0,  0,  0,  0, -5,
          0,  0,  0,  5,  5,  0,  0,  0
    };

    private static final int[] queenTable = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
         -5,  0,  5,  5,  5,  5,  0, -5,
          0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };

    //with queens and rooks around the king hides behind its pawns...
    private static final int[] kingMiddlegameTable = {
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
         20, 20,  0,  0,  0,  0, 20, 20,
         20, 30, 10,  0,  0, 10, 30, 20
    };

    //...but once they're traded off it becomes a fighting piece that belongs in the center
    private static final int[] kingEndgameTable = {
        -50,-40,-30,-20,-20,-30,-40,-50,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -50,-30,-30,-30,-30,-30,-30,-50
    };

    //game phase counts the non-pawn material left: 24 with every piece on the board, 0 with only kings and pawns
    private static final int knightPhase = 1;
    private static final int bishopPhase = 1;
    private static final int rookPhase = 2;
    private static final int queenPhase = 4;
    private static final int totalPhase = 24;

    //mop-up kicks in once a side is this far ahead and the board has emptied out to this phase or less
    private static final int mopUpMaterialLead = 2 * pawnValue;
    private static final int mopUpMaxPhase = 10;


    //positive scores favor white; kings are left out of material since both are always on the board
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

        int material = white - black;
        int phase = gamePhase(position);

        int score = material;

        score += squareBonus(position.whiteKnights, knightTable, true) - squareBonus(position.blackKnights, knightTable, false);
        score += squareBonus(position.whiteBishops, bishopTable, true) - squareBonus(position.blackBishops, bishopTable, false);
        score += squareBonus(position.whiteRooks, rookTable, true) - squareBonus(position.blackRooks, rookTable, false);
        score += squareBonus(position.whiteQueens, queenTable, true) - squareBonus(position.blackQueens, queenTable, false);

        //pawns and kings want different squares as the game goes on, so blend their two tables by phase
        int middlegame = squareBonus(position.whitePawns, pawnMiddlegameTable, true) - squareBonus(position.blackPawns, pawnMiddlegameTable, false)
                + squareBonus(position.whiteKing, kingMiddlegameTable, true) - squareBonus(position.blackKing, kingMiddlegameTable, false);
        int endgame = squareBonus(position.whitePawns, pawnEndgameTable, true) - squareBonus(position.blackPawns, pawnEndgameTable, false)
                + squareBonus(position.whiteKing, kingEndgameTable, true) - squareBonus(position.blackKing, kingEndgameTable, false);
        score += (middlegame * phase + endgame * (totalPhase - phase)) / totalPhase;

        score += mopUp(position, material, phase);

        return score;
    }

    //promotions can push the raw count past 24, so it's capped
    private static int gamePhase(Position position) {
        int phase = Long.bitCount(position.whiteKnights | position.blackKnights) * knightPhase
                + Long.bitCount(position.whiteBishops | position.blackBishops) * bishopPhase
                + Long.bitCount(position.whiteRooks | position.blackRooks) * rookPhase
                + Long.bitCount(position.whiteQueens | position.blackQueens) * queenPhase;
        return Math.min(phase, totalPhase);
    }

    private static int squareBonus(long pieces, int[] table, boolean white) {
        int bonus = 0;
        while(pieces != 0) {
            int square = Long.numberOfTrailingZeros(pieces);
            bonus += table[white ? square : square ^ 56];
            pieces &= pieces - 1;
        }
        return bonus;
    }

    //in a won endgame the mate is usually deeper than the search can see, so without help the engine just
    //shuffles; rewarding it for driving the losing king to the edge and walking its own king over to help
    //gives every move toward the mate a slightly better score for the search to follow
    private static int mopUp(Position position, int material, int phase) {
        if(Math.abs(material) < mopUpMaterialLead || phase > mopUpMaxPhase) {
            return 0;
        }

        boolean whiteWinning = material > 0;
        int winningKing = Long.numberOfTrailingZeros(whiteWinning ? position.whiteKing : position.blackKing);
        int losingKing = Long.numberOfTrailingZeros(whiteWinning ? position.blackKing : position.whiteKing);

        int bonus = distanceFromCenter(losingKing) * 10 + (14 - kingDistance(winningKing, losingKing)) * 4;
        bonus = bonus * (totalPhase - phase) / totalPhase;

        return whiteWinning ? bonus : -bonus;
    }

    //manhattan distance to the nearest of the four center squares: 0 in the center, 6 in a corner
    private static int distanceFromCenter(int square) {
        int file = square & 7;
        int rank = square >>> 3;
        return Math.max(3 - file, file - 4) + Math.max(3 - rank, rank - 4);
    }

    private static int kingDistance(int squareA, int squareB) {
        return Math.abs((squareA & 7) - (squareB & 7)) + Math.abs((squareA >>> 3) - (squareB >>> 3));
    }

    //negamax needs every score from the side to move's point of view
    private static int evaluateRelative(Position position) {
        int eval = evaluate(position);
        return position.isWhiteToMove() ? eval : -eval;
    }


    //one move buffer per ply, so the search never allocates and a child never overwrites its parent's moves
    private static final short[][] searchBuffers = new short[maxPly][MoveGenerator.maxMoves];
    private static final int[][] scoreBuffers = new int[maxPly][MoveGenerator.maxMoves];

    //check extensions stop past this ply, so a long run of checks can't blow up the search
    private static int extensionPlyLimit;

    //iterative deepening stops here even with time left; check extensions double it, which still fits in maxPly
    private static final int maxDepth = maxPly / 2;

    //the clock is only read every this many nodes (minus one, as a bit mask), since reading it is slow
    private static final int timeCheckMask = 2047;

    private static long nodes;
    private static long deadline;
    //set once time runs out (or stopSearch is called); every search call then unwinds immediately and its
    //score is thrown away. Volatile because stopSearch is called from a different thread than the search
    private static volatile boolean aborted;

    //deepest search that finished and its score, for whoever wants to see how hard the engine thought
    private static int lastDepth;
    private static int lastScore;

    //transposition table: what earlier searches learned about each position, keyed by its hash. The same position
    //is reached through many move orders, so a stored result often saves searching it again; and even when the
    //stored search was too shallow to reuse its score, its best move is the best first guess for move ordering.
    //Entries are spread over parallel arrays, indexed by the low bits of the hash, and survive between searches
    private static final int tableSizeBits = 20;
    private static final int tableMask = (1 << tableSizeBits) - 1;
    private static final long[] tableKeys = new long[1 << tableSizeBits];
    private static final short[] tableMoves = new short[1 << tableSizeBits];
    private static final int[] tableScores = new int[1 << tableSizeBits];
    private static final byte[] tableDepths = new byte[1 << tableSizeBits];
    private static final byte[] tableBounds = new byte[1 << tableSizeBits];

    //what a stored score means: alpha-beta cuts searches short, so often the score is only known to be at
    //least (a beta cutoff) or at most (no move beat alpha) the stored value. Zero marks an empty slot
    private static final byte exactScore = 1;
    private static final byte lowerBound = 2;
    private static final byte upperBound = 3;

    //the table's best move is searched before everything else, captures included
    private static final int tableMoveOrderingScore = 1_000_000;

    //searches 1 ply deep, then 2, then 3... until the time runs out, and returns the best move found.
    //each search reuses the previous one's best move as its first guess, so the shallow searches pay for
    //themselves by making the deeper ones prune better
    public static short findBestMove(Position position, long timeLimitMillis) {
        deadline = System.nanoTime() + timeLimitMillis * 1_000_000;
        nodes = 0;
        aborted = false;
        lastDepth = 0;
        lastScore = 0;

        short[] moves = searchBuffers[0];
        int[] scores = scoreBuffers[0];
        int numMoves = moveGenerator.generateMoves(position, moves);

        if(numMoves == 0) {
            return Move.none;
        }

        //sort the root once so the first iteration starts from the last search's best move, if it saw this
        //position, and then the obvious captures
        long rootKey = position.getHash();
        scoreMoves(position, moves, scores, numMoves, probeMove(rootKey));
        for(int index = 0; index < numMoves; index++) {
            pickNextMove(moves, scores, index, numMoves);
        }

        short bestMove = moves[0];

        //nothing to think about
        if(numMoves == 1) {
            return bestMove;
        }

        for(int depth = 1; depth <= maxDepth; depth++) {
            extensionPlyLimit = depth * 2;

            int alpha = -INF;
            int bestIndex = -1;

            for(int index = 0; index < numMoves; index++) {
                position.playMove(moves[index]);
                int score = -negamax(position, depth - 1, 1, -INF, -alpha);
                position.undoMove();

                if(aborted) {
                    break;
                }

                if(score > alpha) {
                    alpha = score;
                    bestIndex = index;
                }
            }

            //moves[0] is the previous iteration's best and is always searched first, so even when time ran
            //out partway through, any move that beat it at this depth is a real improvement
            if(bestIndex >= 0) {
                bestMove = moves[bestIndex];
                moveToFront(moves, bestIndex);
            }

            if(aborted) {
                break;
            }

            lastDepth = depth;
            lastScore = alpha;
            store(rootKey, depth, exactScore, alpha, bestMove, 0);

            //a forced mate was found; searching deeper can't find anything better
            if(alpha >= MATE - maxPly) {
                break;
            }
        }

        return bestMove;
    }

    //makes a running search return as soon as possible with the best move it has so far
    public static void stopSearch() {
        aborted = true;
    }

    public static int getLastDepth() {
        return lastDepth;
    }

    public static int getLastScore() {
        return lastScore;
    }

    public static boolean isMateScore(int score) {
        return Math.abs(score) >= MATE - maxPly;
    }

    //plies from the searched position until the mate lands; only meaningful when isMateScore(score)
    public static int pliesToMate(int score) {
        return MATE - Math.abs(score);
    }

    private static boolean outOfTime() {
        if((++nodes & timeCheckMask) == 0 && System.nanoTime() >= deadline) {
            aborted = true;
        }
        return aborted;
    }

    //the stored best move for this position, or Move.none if the table hasn't seen it
    private static short probeMove(long key) {
        int index = (int) key & tableMask;
        return tableBounds[index] != 0 && tableKeys[index] == key ? tableMoves[index] : Move.none;
    }

    private static void store(long key, int depth, byte bound, int score, short move, int ply) {
        int index = (int) key & tableMask;
        boolean samePosition = tableBounds[index] != 0 && tableKeys[index] == key;

        //a deeper result for the same position is worth more than this one; anything else gets overwritten,
        //since a newer entry is more likely to be needed again soon
        if(samePosition && tableDepths[index] > depth) {
            return;
        }

        //a search where no move beat alpha doesn't know the best move, so keep the one already stored
        if(move == Move.none && samePosition) {
            move = tableMoves[index];
        }

        tableKeys[index] = key;
        tableMoves[index] = move;
        tableScores[index] = scoreToTable(score, ply);
        tableDepths[index] = (byte) depth;
        tableBounds[index] = bound;
    }

    //mate scores count plies from the root, but a table entry can be reached at any ply, so they're stored
    //counting from the position itself and converted back when read
    private static int scoreToTable(int score, int ply) {
        if(score >= MATE - maxPly) {
            return score + ply;
        }
        if(score <= -(MATE - maxPly)) {
            return score - ply;
        }
        return score;
    }

    private static int scoreFromTable(int score, int ply) {
        if(score >= MATE - maxPly) {
            return score - ply;
        }
        if(score <= -(MATE - maxPly)) {
            return score + ply;
        }
        return score;
    }

    //shifts moves[0..index-1] back one slot and puts moves[index] first, keeping the rest in order
    private static void moveToFront(short[] moves, int index) {
        short move = moves[index];
        System.arraycopy(moves, 0, moves, 1, index);
        moves[0] = move;
    }

    //returns the score of the position for the side to move, searched depth plies deep;
    //alpha is what the side to move is already guaranteed, beta is the most the opponent will allow
    private static int negamax(Position position, int depth, int ply, int alpha, int beta) {
        if(outOfTime()) {
            return 0;
        }

        //a position that already came up can be repeated into a draw, and the fifty-move rule ends the game, so
        //both score as draws: the engine then avoids repeating when it's winning and heads for it when losing
        if(position.isRepetition() || position.getHalfmoveClock() >= 100) {
            return 0;
        }

        if(ply == maxPly) {
            return evaluateRelative(position);
        }

        boolean inCheck = moveGenerator.inCheck(position);

        //checks are forcing and are how most mates happen, so searching one ply deeper after each check lets
        //the engine see mating sequences that would otherwise be cut off at the horizon
        if(inCheck && ply < extensionPlyLimit) {
            depth++;
        }

        if(depth == 0) {
            return quiescence(position, ply, alpha, beta);
        }

        long key = position.getHash();
        int tableIndex = (int) key & tableMask;
        short tableMove = Move.none;

        if(tableBounds[tableIndex] != 0 && tableKeys[tableIndex] == key) {
            tableMove = tableMoves[tableIndex];

            //the score is only reusable if it came from a search at least this deep
            if(tableDepths[tableIndex] >= depth) {
                int score = scoreFromTable(tableScores[tableIndex], ply);
                byte bound = tableBounds[tableIndex];

                if(bound == exactScore) {
                    return Math.max(alpha, Math.min(beta, score));
                }
                if(bound == lowerBound && score >= beta) {
                    return beta;
                }
                if(bound == upperBound && score <= alpha) {
                    return alpha;
                }
            }
        }

        short[] moves = searchBuffers[ply];
        int[] scores = scoreBuffers[ply];
        int numMoves = moveGenerator.generateMoves(position, moves);

        //the generator is strictly legal, so no moves means checkmate or stalemate
        if(numMoves == 0) {
            return inCheck ? -MATE + ply : 0;
        }

        scoreMoves(position, moves, scores, numMoves, tableMove);

        int originalAlpha = alpha;
        short bestMove = Move.none;

        for(int index = 0; index < numMoves; index++) {
            pickNextMove(moves, scores, index, numMoves);
            position.playMove(moves[index]);
            int score = -negamax(position, depth - 1, ply + 1, -beta, -alpha);
            position.undoMove();

            //a stopped search's scores are meaningless, so none of them may reach the table
            if(aborted) {
                return 0;
            }

            //the opponent already has a better option earlier in the tree, so they'll never allow this position
            if(score >= beta) {
                store(key, depth, lowerBound, beta, moves[index], ply);
                return beta;
            }
            if(score > alpha) {
                alpha = score;
                bestMove = moves[index];
            }
        }

        store(key, depth, alpha > originalAlpha ? exactScore : upperBound, alpha, bestMove, ply);
        return alpha;
    }

    //stopping the search in the middle of a trade misjudges the position (QxP looks like a free pawn when the
    //recapture is one ply past the horizon), so at the end of the main search this keeps playing captures
    //until the position is quiet. The side to move may also decline every capture and keep the static
    //eval ("stand pat"), since in a real game nobody is forced to capture
    private static int quiescence(Position position, int ply, int alpha, int beta) {
        if(outOfTime()) {
            return 0;
        }

        int standPat = evaluateRelative(position);

        if(ply == maxPly) {
            return standPat;
        }
        if(standPat >= beta) {
            return beta;
        }
        if(standPat > alpha) {
            alpha = standPat;
        }

        short[] moves = searchBuffers[ply];
        int[] scores = scoreBuffers[ply];
        int numMoves = moveGenerator.generateMoves(position, moves);

        if(numMoves == 0) {
            return moveGenerator.inCheck(position) ? -MATE + ply : 0;
        }

        numMoves = keepCapturesAndPromotions(position, moves, numMoves);
        scoreMoves(position, moves, scores, numMoves, Move.none);

        for(int index = 0; index < numMoves; index++) {
            pickNextMove(moves, scores, index, numMoves);
            position.playMove(moves[index]);
            int score = -quiescence(position, ply + 1, -beta, -alpha);
            position.undoMove();

            if(score >= beta) {
                return beta;
            }
            if(score > alpha) {
                alpha = score;
            }
        }

        return alpha;
    }

    //compacts the move list down to captures and queen promotions and returns the new length;
    //underpromotions are dropped since they're almost never better and would only bloat the search
    private static int keepCapturesAndPromotions(Position position, short[] moves, int numMoves) {
        long enemies = position.isWhiteToMove() ? position.getBlackPieces() : position.getWhitePieces();
        int kept = 0;

        for(int index = 0; index < numMoves; index++) {
            short move = moves[index];
            boolean capture = (enemies & Position.bit(Move.targetSquare(move))) != 0 || Move.isEnPessant(move);

            if(Move.isPromotion(move) ? Move.promotionPiece(move) == Piece.queen : capture) {
                moves[kept++] = move;
            }
        }

        return kept;
    }

    //indexed by piece type (bishop = 1, pawn = 2, rook = 3, knight = 4, queen = 5, king = 6); the king is 0
    //because it can only capture undefended pieces, so taking with it never risks anything
    private static final int[] orderingValues = {0, bishopValue, pawnValue, rookValue, knightValue, queenValue, 0};

    //alpha-beta prunes the most when the best move is searched first, so likely good moves get high scores:
    //the transposition table's move first, then captures by most valuable victim and least valuable attacker
    //(MVV-LVA), plus promotions
    private static void scoreMoves(Position position, short[] moves, int[] scores, int numMoves, short tableMove) {
        boolean white = position.isWhiteToMove();

        for(int index = 0; index < numMoves; index++) {
            short move = moves[index];

            if(move == tableMove) {
                scores[index] = tableMoveOrderingScore;
                continue;
            }

            int score = 0;

            int victim = Move.isEnPessant(move) ? Piece.pawn : position.pieceTypeAt(Move.targetSquare(move), !white);
            if(victim != Piece.empty) {
                int attacker = position.pieceTypeAt(Move.startSquare(move), white);
                score += 10 * orderingValues[victim] - orderingValues[attacker];
            }

            if(Move.isPromotion(move)) {
                score += orderingValues[Move.promotionPiece(move)];
            }

            scores[index] = score;
        }
    }

    //moves the best remaining move into slot `start`; one selection sort step at a time, because most nodes
    //cut off after the first move or two and fully sorting the list would be wasted work
    private static void pickNextMove(short[] moves, int[] scores, int start, int numMoves) {
        int best = start;
        for(int index = start + 1; index < numMoves; index++) {
            if(scores[index] > scores[best]) {
                best = index;
            }
        }

        short move = moves[start];
        moves[start] = moves[best];
        moves[best] = move;

        int score = scores[start];
        scores[start] = scores[best];
        scores[best] = score;
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
