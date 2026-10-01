package GUI;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import EngineUtil.Board;
import EngineUtil.Engine;
import EngineUtil.FENUtil;
import EngineUtil.Move;
import EngineUtil.MoveGenerator;
import EngineUtil.Piece;
import EngineUtil.Position;


public class GameManager extends JPanel{

    private int tileSize = LaunchPage.tileSize;
    private final Color white = new Color(0xcba88a);
    private final Color black = new Color(0x8b5122);

    PiecePNG pieces[] = new PiecePNG[64];
   
    int startIndex;
    int targetIndex;
    PiecePNG image;
    int width;
    int height;
    Point imageCorner;
    Point prevPt;
    Point currentPoint;
    int index;
    final int offset = 5;

    boolean draggable;

    private enum GameStatus {
        ONGOING,
        CHECKMATE,
        STALEMATE,
        REPETITION,
        FIFTY_MOVE_RULE,
        INSUFFICIENT_MATERIAL
    }

    private boolean playerIsWhite;
    private GameStatus gameStatus;


    private Board board;
    private MoveGenerator moveGenerator;

    //the engine searches on its own thread so the window keeps responding while it thinks. It's a single
    //thread because the engine's search state is static, so two searches must never run at once; it's a
    //daemon so it doesn't keep the program alive after the window closes
    private final ExecutorService engineThread = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Engine");
        thread.setDaemon(true);
        return thread;
    });

    //the player can't move while this is set
    private boolean engineThinking;

    //bumped every new game, so a search that finishes after its game was abandoned gets ignored
    private int gameNumber;

    //end screen: a result card that fades in over the board once the game is over; "View Board" hides it
    //so the final position can be studied, and clicking the board brings it back
    private boolean endScreenVisible;
    private float endScreenAlpha;
    private Timer fadeTimer;
    private int hoveredButton = -1;

    private static final int newGameButton = 0;
    private static final int viewBoardButton = 1;
    private static final String[] buttonLabels = {"New Game", "View Board"};

    //pause before the card fades in, so the final move is visible first
    private static final int endScreenDelay = 600;

    private static final int cardWidth = 300;
    private static final int cardHeight = 226;
    private static final int buttonWidth = 126;
    private static final int buttonHeight = 36;

    private final Color winColor = new Color(0x5f9e4a);
    private final Color lossColor = new Color(0xc0453c);
    private final Color drawColor = new Color(0xb8901f);
    private final Color cardColor = new Color(0x2b2420);
    private final Color secondaryButtonColor = new Color(0x3d342d);
    private final Color cardTextColor = new Color(0xf2ebe4);
    private final Color cardMutedTextColor = new Color(0xb5a99d);

    private final Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 28);
    private final Font outcomeFont = new Font(Font.SANS_SERIF, Font.BOLD, 15);
    private final Font detailFont = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private final Font buttonFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private final Font hintFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    //eval bar: a strip to the right of the board showing how the engine rates the position, with white's
    //share filling up from the bottom
    private static final int barGap = 8;
    private static final int barWidth = 28;
    //the board, then the bar, then a margin matching the gap so the bar isn't jammed against the window edge
    static final int panelWidth = 8 * LaunchPage.tileSize + barGap + barWidth + barGap;
    static final int panelHeight = 8 * LaunchPage.tileSize;

    private final Color barWhiteColor = new Color(0xf2ebe4);
    private final Color barBlackColor = new Color(0x2b2420);
    private final Color barMidlineColor = new Color(128, 128, 128, 140);
    private final Font barFont = new Font(Font.SANS_SERIF, Font.BOLD, 10);

    //white's share of the bar, from 0 to 1: the target is the latest eval and the shown value eases toward
    //it, so the bar slides instead of jumping
    private float barTarget;
    private float barShown;
    private String barLabel;
    //the label sits at the end of whichever side is ahead
    private boolean barLabelOnWhite;
    private Timer barTimer;


    public GameManager() {

    image = null;
    imageCorner = new Point(0, 0);

    this.setSize(panelWidth, panelHeight);

        ClickListener clickListener = new ClickListener();
        this.addMouseListener(clickListener);

        DragListener dragListener = new DragListener();
        this.addMouseMotionListener(dragListener);

        board = Board.createBoard();
        moveGenerator = new MoveGenerator();
        Engine.initiateEngine();

        fadeTimer = new Timer(15, e -> stepFade());
        barTimer = new Timer(15, e -> stepBar());

        startNewGame();
    }

    private void startNewGame() {
        gameNumber++;
        if (engineThinking) {
            board.stopEngine();
            engineThinking = false;
        }

        board.newGame();
        gameStatus = GameStatus.ONGOING;
        hideEndScreen();
        resetEvalBar();
        image = null;
        draggable = false;

        String[] options = {"White", "Black"};

        //sets player color
          playerIsWhite = JOptionPane.showOptionDialog(this, "What color would you like to be?",
         "Color Selector", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
          LaunchPage.blackPawnPNG, options, 0) == 0;

        board.setPlayerColor(playerIsWhite);

        updateBoardInterface();
        startEngineMove();
    }

    //hands a copy of the position to the engine thread; the move comes back to finishEngineMove on the
    //Swing thread, so the real board is only ever touched from one thread
    private void startEngineMove() {
        if (!board.isEngineTurn()) {
            return;
        }

        engineThinking = true;
        int game = gameNumber;
        Position snapshot = board.copyPosition();

        engineThread.execute(() -> {
            short move = Move.none;
            int score = 0;
            int depth = 0;
            try {
                move = board.findEngineMove(snapshot);
                //read here, on the thread that just ran the search, so another search can't overwrite them first
                score = Engine.getLastScore();
                depth = Engine.getLastDepth();
            } catch (RuntimeException e) {
                //still report back, or engineThinking would stay set and lock the board for good
                e.printStackTrace();
            }

            short engineMove = move;
            //the search scores from the engine's side; the bar wants white's
            int whiteScore = snapshot.isWhiteToMove() ? score : -score;
            int searchDepth = depth;
            SwingUtilities.invokeLater(() -> finishEngineMove(engineMove, whiteScore, searchDepth, game));
        });
    }

    private void finishEngineMove(short move, int whiteScore, int depth, int game) {
        if (game != gameNumber) {
            return;
        }

        engineThinking = false;

        if (move != Move.none) {
            playMove(move);
        }

        //depth 0 means the engine didn't search (it had only one legal move), so there's no new score
        if (depth > 0) {
            setEval(whiteScore);
        }

        updateGameStatus();
        if (gameStatus != GameStatus.ONGOING) {
            endGame();
        }
    }


    private void updateBoardInterface() {
        pieces = FENUtil.FENtoPNGPosition(FENUtil.positionToFEN(board.getPosition()));
        repaint(); 
    }

    //checkmate and stalemate come first: a mate delivered on the move that would also trigger a draw rule still counts
    private void updateGameStatus() {
        Position position = board.getPosition();

        if (moveGenerator.getNumLegalMoves(position) == 0) {
            gameStatus = moveGenerator.inCheck(position) ? GameStatus.CHECKMATE : GameStatus.STALEMATE;
        } else if (position.isThreefoldRepetition()) {
            gameStatus = GameStatus.REPETITION;
        } else if (position.getHalfmoveClock() >= 100) {
            gameStatus = GameStatus.FIFTY_MOVE_RULE;
        } else if (position.isInsufficientMaterial()) {
            gameStatus = GameStatus.INSUFFICIENT_MATERIAL;
        } else {
            gameStatus = GameStatus.ONGOING;
        }
    }

    //every ending except checkmate is a draw
    private boolean isDraw() {
        return gameStatus != GameStatus.ONGOING && gameStatus != GameStatus.CHECKMATE;
    }

    //big heading on the end card; the rule-based draws share "Draw" and name the rule on the line below
    private String titleText() {
        return switch (gameStatus) {
            case CHECKMATE -> "Checkmate";
            case STALEMATE -> "Stalemate";
            default -> "Draw";
        };
    }

    //called once the game is over: drops any piece still being dragged and fades the result card in
    private void endGame() {
        image = null;
        draggable = false;

        if (gameStatus == GameStatus.CHECKMATE) {
            boolean whiteWon = !whiteHasNoMoves();
            setBar(whiteWon ? 1f : 0f, whiteWon ? "1-0" : "0-1", whiteWon);
        } else {
            setBar(0.5f, "½", true);
        }

        updateBoardInterface();
        showEndScreen(endScreenDelay);
    }

    private void resetEvalBar() {
        barTimer.stop();
        barTarget = 0.5f;
        barShown = 0.5f;
        barLabel = "0.0";
        barLabelOnWhite = true;
        repaint();
    }

    //whiteScore is the engine's search score in centipawns, from white's side, for the position it just moved into
    private void setEval(int whiteScore) {
        boolean whiteAhead = whiteScore >= 0;

        if (Engine.isMateScore(whiteScore)) {
            //the score was measured before the engine's move, which used up one of the plies to mate; the side
            //delivering mate moves on every other remaining ply, so that's half of them rounded up, which
            //works out to the original ply count halved
            int movesToMate = Engine.pliesToMate(whiteScore) / 2;
            setBar(whiteAhead ? 1f : 0f, "M" + movesToMate, whiteAhead);
            return;
        }

        //maps the score to white's expected share of the points, so the first pawn or two of advantage moves the
        //bar a lot while +10 against +11 barely moves it; kept off the ends so those are reserved for mates
        double share = 1 / (1 + Math.exp(-0.00368 * whiteScore));
        share = Math.max(0.05, Math.min(0.95, share));

        double pawns = Math.abs(whiteScore) / 100.0;
        String label = pawns < 10 ? String.format(Locale.ROOT, "%.1f", pawns) : String.valueOf(Math.round(pawns));

        setBar((float) share, label, whiteAhead);
    }

    private void setBar(float whiteShare, String label, boolean labelOnWhite) {
        barTarget = whiteShare;
        barLabel = label;
        barLabelOnWhite = labelOnWhite;
        barTimer.restart();
        repaint();
    }

    private void stepBar() {
        barShown += (barTarget - barShown) * 0.2f;
        if (Math.abs(barTarget - barShown) < 0.002f) {
            barShown = barTarget;
            barTimer.stop();
        }
        repaint();
    }

    private void paintEvalBar(Graphics2D g2) {
        int boardSize = 8 * tileSize;
        int x = boardSize + barGap;

        Shape oldClip = g2.getClip();
        g2.clip(new RoundRectangle2D.Float(x, 0, barWidth, boardSize, 8, 8));

        g2.setColor(barBlackColor);
        g2.fillRect(x, 0, barWidth, boardSize);

        int whiteHeight = Math.round(barShown * boardSize);
        g2.setColor(barWhiteColor);
        g2.fillRect(x, boardSize - whiteHeight, barWidth, whiteHeight);

        //marks the even point, so small advantages are easy to read
        g2.setColor(barMidlineColor);
        g2.fillRect(x, boardSize / 2 - 1, barWidth, 2);

        g2.setClip(oldClip);

        g2.setFont(barFont);
        int centerX = x + barWidth / 2;
        if (barLabelOnWhite) {
            g2.setColor(barBlackColor);
            drawCentered(g2, barLabel, centerX, boardSize - 6);
        } else {
            g2.setColor(barWhiteColor);
            drawCentered(g2, barLabel, centerX, 6 + g2.getFontMetrics().getAscent());
        }
    }

    private void showEndScreen(int delay) {
        endScreenVisible = true;
        endScreenAlpha = 0;
        fadeTimer.setInitialDelay(delay);
        fadeTimer.restart();
        repaint();
    }

    private void hideEndScreen() {
        fadeTimer.stop();
        endScreenVisible = false;
        endScreenAlpha = 0;
        hoveredButton = -1;
        setCursor(Cursor.getDefaultCursor());
        repaint();
    }

    private void stepFade() {
        endScreenAlpha = Math.min(1f, endScreenAlpha + 0.08f);
        if (endScreenAlpha >= 1f) {
            fadeTimer.stop();
        }
        repaint();
    }

    private void handleEndScreenClick(Point point) {
        if (!endScreenVisible) {
            showEndScreen(0);
            return;
        }

        //ignore clicks until the card has finished fading in
        if (endScreenAlpha < 1f) {
            return;
        }

        int button = buttonAt(point);
        if (button == newGameButton) {
            startNewGame();
        } else if (button == viewBoardButton) {
            hideEndScreen();
        }
    }

    private Rectangle cardBounds() {
        int boardSize = 8 * tileSize;
        return new Rectangle((boardSize - cardWidth) / 2, (boardSize - cardHeight) / 2, cardWidth, cardHeight);
    }

    //buttons sit side by side along the bottom of the card
    private Rectangle buttonBounds(int button) {
        Rectangle card = cardBounds();
        int gap = 12;
        int left = card.x + (cardWidth - 2 * buttonWidth - gap) / 2;
        int top = card.y + cardHeight - buttonHeight - 20;
        return new Rectangle(left + button * (buttonWidth + gap), top, buttonWidth, buttonHeight);
    }

    //returns the end screen button under the point, or -1 if there isn't one
    private int buttonAt(Point point) {
        if (!endScreenVisible || endScreenAlpha < 1f) {
            return -1;
        }
        for (int button = 0; button < buttonLabels.length; button++) {
            if (buttonBounds(button).contains(point)) {
                return button;
            }
        }
        return -1;
    }

    //in checkmate or stalemate the side to move is the one that has no legal moves
    private boolean whiteHasNoMoves() {
        return board.getPosition().isWhiteToMove();
    }

    private Color resultColor() {
        if (isDraw()) {
            return drawColor;
        }
        boolean whiteWon = !whiteHasNoMoves();
        return whiteWon == playerIsWhite ? winColor : lossColor;
    }

    private String outcomeText() {
        return switch (gameStatus) {
            case STALEMATE -> "Draw";
            case REPETITION -> "Threefold repetition";
            case FIFTY_MOVE_RULE -> "Fifty-move rule";
            case INSUFFICIENT_MATERIAL -> "Insufficient material";
            default -> !whiteHasNoMoves() == playerIsWhite ? "You win!" : "The engine wins";
        };
    }

    private String detailText() {
        int fullMoves = (board.getNumMoves() + 1) / 2;
        String moves = fullMoves == 1 ? "1 move" : fullMoves + " moves";
        String stuck = whiteHasNoMoves() ? "White" : "Black";
        String other = whiteHasNoMoves() ? "Black" : "White";

        return switch (gameStatus) {
            case CHECKMATE -> other + " checkmated " + stuck + " in " + moves;
            case REPETITION -> "Same position three times, " + moves;
            case FIFTY_MOVE_RULE -> "No capture or pawn move in 50 moves";
            case INSUFFICIENT_MATERIAL -> "Neither side can checkmate";
            default -> stuck + " has no legal moves after " + moves;
        };
    }

    private int findKingSquare(boolean white) {
        int kingID = (white ? Piece.white : Piece.black) | Piece.king;
        for (int square = 0; square < 64; square++) {
            if (board.getPosition().getPieceAtIndex(square).ID == kingID) {
                return square;
            }
        }
        return -1;
    }

    //glows behind the king that can't move: red when mated, amber when stalemated. Draws by rule leave both
    //kings able to move, so both glow amber
    private void paintKingHighlights(Graphics2D g2) {
        if (gameStatus == GameStatus.CHECKMATE || gameStatus == GameStatus.STALEMATE) {
            paintKingHighlight(g2, whiteHasNoMoves());
        } else {
            paintKingHighlight(g2, true);
            paintKingHighlight(g2, false);
        }
    }

    private void paintKingHighlight(Graphics2D g2, boolean whiteKing) {
        int square = findKingSquare(whiteKing);
        if (square < 0) {
            return;
        }

        Color base = gameStatus == GameStatus.CHECKMATE ? new Color(0xe0302a) : drawColor;
        int x = (square % 8) * tileSize;
        int y = (square / 8) * tileSize;

        g2.setPaint(new RadialGradientPaint(x + tileSize / 2f, y + tileSize / 2f, tileSize * 0.75f,
                new float[] {0f, 1f},
                new Color[] {withAlpha(base, 235), withAlpha(base, 0)}));
        g2.fillRect(x, y, tileSize, tileSize);
    }

    private void paintEndScreen(Graphics2D g2) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, endScreenAlpha));

        //dim the board
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, 8 * tileSize, 8 * tileSize);

        //the card eases up into place as it fades in
        int slide = Math.round((1f - endScreenAlpha) * 16);
        Rectangle card = cardBounds();
        card.translate(0, slide);
        int centerX = card.x + card.width / 2;
        Color accent = resultColor();

        g.setColor(new Color(0, 0, 0, 90));
        g.fillRoundRect(card.x + 2, card.y + 6, card.width, card.height, 20, 20);
        g.setColor(cardColor);
        g.fillRoundRect(card.x, card.y, card.width, card.height, 20, 20);

        //accent strip along the top edge, clipped to the card's rounded corners
        Shape oldClip = g.getClip();
        g.clip(new RoundRectangle2D.Float(card.x, card.y, card.width, card.height, 20, 20));
        g.setColor(accent);
        g.fillRect(card.x, card.y, card.width, 6);
        g.setClip(oldClip);

        //the winner's king for a checkmate, both kings for a stalemate
        PiecePNG whiteKing = LaunchPage.whiteKingPNG;
        PiecePNG blackKing = LaunchPage.blackKingPNG;
        int iconTop = card.y + 22;
        if (gameStatus == GameStatus.CHECKMATE) {
            PiecePNG winnerKing = whiteHasNoMoves() ? blackKing : whiteKing;
            winnerKing.paintIcon(this, g, centerX - winnerKing.getIconWidth() / 2, iconTop);
        } else {
            whiteKing.paintIcon(this, g, centerX - whiteKing.getIconWidth() - 4, iconTop);
            blackKing.paintIcon(this, g, centerX + 4, iconTop);
        }

        g.setColor(cardTextColor);
        g.setFont(titleFont);
        drawCentered(g, titleText(), centerX, card.y + 104);

        g.setColor(accent.brighter());
        g.setFont(outcomeFont);
        drawCentered(g, outcomeText(), centerX, card.y + 130);

        g.setColor(cardMutedTextColor);
        g.setFont(detailFont);
        drawCentered(g, detailText(), centerX, card.y + 150);

        g.setFont(buttonFont);
        for (int button = 0; button < buttonLabels.length; button++) {
            Rectangle bounds = buttonBounds(button);
            bounds.translate(0, slide);

            Color fill = button == newGameButton ? accent : secondaryButtonColor;
            if (button == hoveredButton) {
                fill = fill.brighter();
            }

            g.setColor(fill);
            g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);
            g.setColor(cardTextColor);
            drawCentered(g, buttonLabels[button], bounds.x + bounds.width / 2,
                    bounds.y + (bounds.height + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2);
        }

        g.dispose();
    }

    //small pill at the top of the board while the end screen is hidden
    private void paintReturnHint(Graphics2D g2) {
        String text = titleText() + "  ·  click to show result";
        g2.setFont(hintFont);
        FontMetrics metrics = g2.getFontMetrics();

        int width = metrics.stringWidth(text) + 24;
        int height = 24;
        int x = (8 * tileSize - width) / 2;
        int y = 8;

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(x, y, width, height, height, height);
        g2.setColor(cardTextColor);
        g2.drawString(text, x + 12, y + (height + metrics.getAscent() - metrics.getDescent()) / 2);
    }

    private static void drawCentered(Graphics2D g, String text, int centerX, int baseline) {
        g.drawString(text, centerX - g.getFontMetrics().stringWidth(text) / 2, baseline);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private void playMove(short move) {
        board.logMove(move);
        updateBoardInterface();
    }

    //returns the chosen piece type, or Piece.empty if the dialog was closed
    private int askPromotionPiece() {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};

        int choice = JOptionPane.showOptionDialog(this, "Promote pawn to:",
         "Promotion", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
          null, options, options[0]);

        //options line up with Move.promotionPieces
        return choice < 0 ? Piece.empty : Move.promotionPieces[choice];
    }

    //returns the square under the point, or -1 if it's off the board (e.g. over the eval bar)
    private int getIndex(Point point) {
            if (point.getX() < 0 || point.getY() < 0 || point.getX() >= 8 * tileSize || point.getY() >= 8 * tileSize) {
                return -1;
            }

            double row = (point.getY() - point.getY()%tileSize)/ tileSize;
            double col = (point.getX() - point.getX()%tileSize)/ tileSize;

            int index = (int)(8*row + col);
            return index;
        }


    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        for (int i = 0; i < 64; i++) {
            int row = (i - (i % 8)) / 8;
            int col = i - 8 * row;
            boolean isWhite = (row + col) % 2 == 0;
            Color color = isWhite ? white : black;
            g.setColor(color);
            g.fillRect(col * tileSize, row * tileSize, tileSize, tileSize);
        }

        if (gameStatus != GameStatus.ONGOING) {
            paintKingHighlights(g2);
        }

        for (int i = 0; i < 64; i++) {
            if (pieces[i] != null) {
                int row = (i - (i % 8)) / 8;
                int col = i - 8 * row;
                int x = col * tileSize + offset;
                int y = row * tileSize + offset;
                pieces[i].paintIcon(this, g, x, y);
            }
        }

        paintEvalBar(g2);

        
        if (image != null) {
            image.paintIcon(this, g, (int)imageCorner.getX(), (int)imageCorner.getY());
        }

        if (gameStatus != GameStatus.ONGOING) {
            if (endScreenVisible) {
                paintEndScreen(g2);
            } else {
                paintReturnHint(g2);
            }
        }
    }


    private class ClickListener extends MouseAdapter{
        public void mousePressed(MouseEvent e) {
            //once the game is over the board only responds to the end screen
            if (gameStatus != GameStatus.ONGOING) {
                handleEndScreenClick(e.getPoint());
                return;
            }

            if (engineThinking) {
                draggable = false;
                return;
            }

            prevPt = e.getPoint();
            //a click with no drag never sets currentPoint, so start it where the press happened
            currentPoint = prevPt;
            int index = getIndex(prevPt);
            if (index < 0) {
                draggable = false;
                return;
            }
        if (playerIsWhite == FENUtil.FENtoPosition(FENUtil.PNGPositionToFEN(pieces))[index].isWhite() && pieces[index] != null){
            startIndex = index;
            image = pieces[index];
            pieces[index] = null;

            int row = (index - (index % 8)) / 8;
            int col = index - 8*row;
            int newX = col*tileSize + offset;
            int newY = row*tileSize + offset;

            imageCorner = new Point(newX, newY);
            draggable = true;
            } else {
                draggable = false;
            }
        }

        public void mouseReleased(MouseEvent e) {
            if(draggable) {
                int targetIndex = getIndex(currentPoint);

            int row;
            int col;
            int newX;
            int newY;

            //dropping the piece off the board puts it back
            short requestedMove = targetIndex < 0 ? Move.none
                    : moveGenerator.getLegalMove(startIndex, targetIndex, board.getPosition());

            if (requestedMove != Move.none && Move.isPromotion(requestedMove)) {
                int promotionPiece = askPromotionPiece();
                //closing the dialog cancels the move and puts the pawn back
                requestedMove = promotionPiece == Piece.empty ? Move.none
                        : moveGenerator.getLegalMove(startIndex, targetIndex, promotionPiece, board.getPosition());
            }


            if ((image != null && requestedMove != Move.none) ) {

            row = (targetIndex - (targetIndex % 8)) / 8;
            col = targetIndex - 8*row;
            newX = col*tileSize + offset;
            newY = row*tileSize + offset;

            //player move
            playMove(requestedMove);

            updateGameStatus();

            if(gameStatus != GameStatus.ONGOING) {
                endGame();
                return;
            }

            //the player's move is already on screen; the engine's reply shows up when its search finishes
            startEngineMove();

            image = null;

            } else {
                row = (startIndex - (startIndex % 8)) / 8;
                col = startIndex - 8*row;
                newX = col*tileSize + offset;
                newY = row*tileSize + offset;
                pieces[startIndex] = image;
                image = null;
                draggable = false;
            }
                        
            imageCorner = new Point(newX, newY);

            repaint();
            }
            //System.out.println("Board FEN: " + FENUtil.positionToFEN(board.getPosition()));
            //System.out.println("Board eval: " + Engine.evaluate(board.getPosition()));
        }
    }

    private class DragListener extends MouseMotionAdapter { 
        public void mouseDragged(MouseEvent e) {
           if(draggable){
            currentPoint = e.getPoint();

            imageCorner.translate((int)(currentPoint.getX() - prevPt.getX()), (int)(currentPoint.getY() - prevPt.getY()));

            prevPt = currentPoint;
            repaint();
           }
        }

        public void mouseMoved(MouseEvent e) {
            int button = buttonAt(e.getPoint());
            if (button != hoveredButton) {
                hoveredButton = button;
                setCursor(button >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                repaint();
            }
        }
    }
    
}
