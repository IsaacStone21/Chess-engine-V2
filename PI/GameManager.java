package PI;

import java.io.IOException;

import EngineUtil.*;

enum GameState {
    INITIALIZING,
    WAITINGFORPLAYERMOVE,
    WAITINGFORENGINEMOVE,
    PLAYINGENGINEMOVE,
    GAMEOVER,
    ERROR
}

public class GameManager {
    GameState gameState;
    Board board;
    MoveGenerator moveGenerator;
    TeensyLink teensyLink;
    boolean playerIsWhite;
    PositionTracker positionTracker;

    //the move found in WAITINGFORENGINEMOVE, held until PLAYINGENGINEMOVE sends it to the gantry
    short engineMove = Move.none;

    //how long the board has to stay the same before a scan is trusted, so a move that's still being made
    //(like castling rook first, where the rook's half is a legal move on its own) isn't taken as finished
    static final long settleMillis = 1000;
    String lastScan = "";
    long lastScanChangeTime;


    //only sets things up; the blocking hardware work happens in the INITIALIZING state
    public GameManager() throws IOException {

        gameState = GameState.INITIALIZING;
        board = Board.createBoard();
        moveGenerator = new MoveGenerator();
        Engine.initiateEngine();
        teensyLink = new TeensyLink("COM5");
    }

    //each case does its work and sets the next state; the loop ends on GAMEOVER or ERROR
    public void run() throws IOException, InterruptedException {
        try {
            while (gameState != GameState.GAMEOVER && gameState != GameState.ERROR) {
                switch (gameState) {
                    case INITIALIZING -> initialize();

                    case WAITINGFORPLAYERMOVE -> {
                        short playerMove = getPlayerMove();
                        if(playerMove != Move.none) {
                            logPlayerMove(playerMove);
                        }
                    }

                    case WAITINGFORENGINEMOVE -> {
                        engineMove = getEngineMove();
                        gameState = engineMove == Move.none ? GameState.ERROR : GameState.PLAYINGENGINEMOVE;
                    }

                    case PLAYINGENGINEMOVE -> playEngineMove(engineMove);

                    //the while condition exits before these are reached
                    case GAMEOVER, ERROR -> {}
                }
            }

            System.out.println(gameState == GameState.GAMEOVER ? "Game over" : "Stopped because of an error");
        } finally {
            teensyLink.close();
        }
    }

    private void initialize() throws IOException, InterruptedException {
        //the Teensy answers SCAN with ERR until it's been initialized, so this has to come first
        String response = teensyLink.initialize().strip();
        if(!response.equals("OK")) {
            System.out.println("Teensy failed to initialize: " + response);
            gameState = GameState.ERROR;
            return;
        }

        playerIsWhite = true;
        boolean recievedPlayerColor = false;

        int blackKingIndex = 4;
        int whiteKingIndex = 60;

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < 15000 && !recievedPlayerColor) {
            //SCAN replies with just the 64 characters, there's no "OK " in front to split off
            String feedback = teensyLink.scan().strip();

            if(feedback.charAt(blackKingIndex) == 'E' && feedback.charAt(whiteKingIndex) == 'W') {
                playerIsWhite = false;
                recievedPlayerColor = true;
            } else if(feedback.charAt(whiteKingIndex) == 'E' && feedback.charAt(blackKingIndex) == 'B') {
                playerIsWhite = true;
                recievedPlayerColor = true;
            }
        }

        if(!recievedPlayerColor) {
            System.out.println("No king was lifted, so no color was chosen");
            gameState = GameState.ERROR;
            return;
        }

        positionTracker = new PositionTracker();
        board.setPlayerColor(playerIsWhite);

        beginGameLoop();
    }

    public void beginGameLoop() {
        if(!playerIsWhite) {
            gameState = GameState.WAITINGFORENGINEMOVE;
        } else {
            gameState = GameState.WAITINGFORPLAYERMOVE;
        }
    }

    public short getPlayerMove() throws IOException, InterruptedException {
        String scannedPostion = teensyLink.scan().strip();
        long now = System.currentTimeMillis();

        //the board changed since the last scan, so the player may still be moving; start timing again
        if(!scannedPostion.equals(lastScan)) {
            lastScan = scannedPostion;
            lastScanChangeTime = now;
            return Move.none;
        }

        if(now - lastScanChangeTime < settleMillis) {
            return Move.none;
        }

        short[] moves = new short[moveGenerator.getNumLegalMoves(board.getPosition())];
        moveGenerator.generateMoves(board.getPosition(), moves);

        return positionTracker.getPlayerMove(scannedPostion, board.getPosition(), moves);
    }

    //called after every logged move: the game is over if the side to move has no legal moves, otherwise
    //it's whoever's turn the board says it is
    public void updateGameStatus() {
        if(moveGenerator.getNumLegalMoves(board.getPosition()) == 0) {
            gameState = GameState.GAMEOVER;
        } else if (board.isEngineTurn()) {
            gameState = GameState.WAITINGFORENGINEMOVE;
        } else {
            gameState = GameState.WAITINGFORPLAYERMOVE;
        }
    }

    public void logPlayerMove(short move) {
        board.logMove(move);
        updateGameStatus();
    }

    public short getEngineMove() {
        if(!board.isEngineTurn()) {
            return Move.none;
        }
        short move = Move.none;
        //a copy, so the search can't leave the real game position changed
        Position snapshot = board.copyPosition();
        try {
            move = board.findBookMove(snapshot);
            if (move == Move.none) {
                move = board.findEngineMove(snapshot);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        return move;
    }

    public void playEngineMove(short engineMove) throws IOException, InterruptedException{
        int startSquareIndex = Move.startSquare(engineMove);
        String startIndex = String.valueOf(startSquareIndex);
        String targetIndex = String.valueOf(Move.targetSquare(engineMove));
        boolean isWhitePiece = (board.getPosition().getWhitePieces() & (1L << startSquareIndex)) != 0;
        String pieceColor = isWhitePiece ? "W" : "B";

        //the firmware reads MOVE <color> <start> <target>, so the color goes first
        String[] args = {pieceColor, startIndex, targetIndex};

        //only log the move once the gantry confirms it, otherwise the engine would think it's on the board
        String response = teensyLink.move(args).strip();
        if(!response.equals("OK")) {
            System.out.println("Gantry failed to play the engine's move: " + response);
            gameState = GameState.ERROR;
            return;
        }

        board.logMove(engineMove);
        updateGameStatus();
    }


}
