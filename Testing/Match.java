package Testing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

//plays an old commit of the engine against the working tree (or against another commit) and reports the score.
//Run it from anywhere in the repository, with no build step:
//
//  java Testing/Match.java <old-commit> [<new-commit>] [--games N] [--time MS] [--depth N] [--threads N]
//
//  java Testing/Match.java HEAD                  working tree vs. the last commit
//  java Testing/Match.java 97a4ffe --time 200    working tree vs. 97a4ffe, 200 ms per move
//  java Testing/Match.java HEAD~2 HEAD           two commits against each other
//
//Each version is compiled into a temp folder and loaded with its own class loader, so both run in this JVM
//without their classes (or the engine's static search state) clashing. Engines that search to a fixed depth
//use the searchDepth from that commit's Board unless --depth is given; time-based engines get --time per move.
//Works with any commit whose engine has Engine.findBestMove and short-encoded moves: 97a4ffe and later.
public class Match {
    //each opening is played twice, once with each version as white, so neither gets the better side of the book
    private static final String[][] openings = {
        {"Ruy Lopez", "e2e4 e7e5 g1f3 b8c6 f1b5 a7a6 b5a4 g8f6"},
        {"Italian Game", "e2e4 e7e5 g1f3 b8c6 f1c4 f8c5 c2c3 g8f6"},
        {"Scotch Game", "e2e4 e7e5 g1f3 b8c6 d2d4 e5d4 f3d4 g8f6"},
        {"Petrov Defense", "e2e4 e7e5 g1f3 g8f6 f3e5 d7d6 e5f3 f6e4"},
        {"Vienna Game", "e2e4 e7e5 b1c3 g8f6 f2f4 d7d5"},
        {"King's Gambit", "e2e4 e7e5 f2f4 e5f4 g1f3 g7g5"},
        {"Sicilian Najdorf", "e2e4 c7c5 g1f3 d7d6 d2d4 c5d4 f3d4 g8f6 b1c3 a7a6"},
        {"Closed Sicilian", "e2e4 c7c5 b1c3 b8c6 g2g3 g7g6 f1g2 f8g7"},
        {"Sicilian Alapin", "e2e4 c7c5 c2c3 g8f6 e4e5 f6d5 d2d4 c5d4"},
        {"French Defense", "e2e4 e7e6 d2d4 d7d5 b1c3 g8f6 c1g5 f8e7"},
        {"Caro-Kann Advance", "e2e4 c7c6 d2d4 d7d5 e4e5 c8f5 g1f3 e7e6"},
        {"Scandinavian Defense", "e2e4 d7d5 e4d5 d8d5 b1c3 d5a5 d2d4 g8f6"},
        {"Pirc Defense", "e2e4 d7d6 d2d4 g8f6 b1c3 g7g6 f1e2 f8g7"},
        {"Alekhine Defense", "e2e4 g8f6 e4e5 f6d5 d2d4 d7d6 g1f3 c8g4"},
        {"Queen's Gambit Declined", "d2d4 d7d5 c2c4 e7e6 b1c3 g8f6 c1g5 f8e7"},
        {"Slav Defense", "d2d4 d7d5 c2c4 c7c6 g1f3 g8f6 b1c3 d5c4"},
        {"Queen's Gambit Accepted", "d2d4 d7d5 c2c4 d5c4 g1f3 g8f6 e2e3 e7e6"},
        {"King's Indian Defense", "d2d4 g8f6 c2c4 g7g6 b1c3 f8g7 e2e4 d7d6"},
        {"Nimzo-Indian Defense", "d2d4 g8f6 c2c4 e7e6 b1c3 f8b4 e2e3 e8g8"},
        {"Queen's Indian Defense", "d2d4 g8f6 c2c4 e7e6 g1f3 b7b6 g2g3 c8b7"},
        {"Grunfeld Defense", "d2d4 g8f6 c2c4 g7g6 b1c3 d7d5 c4d5 f6d5"},
        {"Benoni Defense", "d2d4 g8f6 c2c4 c7c5 d4d5 e7e6 b1c3 e6d5"},
        {"Dutch Defense", "d2d4 f7f5 g2g3 g8f6 f1g2 g7g6 g1f3 f8g7"},
        {"London System", "d2d4 d7d5 g1f3 g8f6 c1f4 e7e6 e2e3 c7c5"},
        {"English Opening", "c2c4 e7e5 b1c3 g8f6 g2g3 d7d5 c4d5 f6d5"},
        {"Reti Opening", "g1f3 d7d5 g2g3 g8f6 f1g2 e7e6 e1g1 f8e7"},
    };

    //a game still going after this many plies (200 moves each) is called a draw
    private static final int maxPlies = 400;

    private static final long defaultMoveTime = 100;
    //what a fixed-depth engine searches when its commit's Board doesn't say
    private static final int fallbackDepth = 5;

    private enum Outcome { WIN, DRAW, LOSS, ABORTED }

    //outcome is from the new version's side
    private record GameResult(int game, String opening, boolean newIsWhite, Outcome outcome, String reason, int moves) {}

    private record Settings(int games, long moveTime, Integer depth, int threads) {}

    public static void main(String[] args) throws Exception {
        List<String> refs = new ArrayList<>();
        int games = openings.length * 2;
        long moveTime = defaultMoveTime;
        Integer depth = null;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);

        for(int index = 0; index < args.length; index++) {
            switch(args[index]) {
                case "--games" -> games = Integer.parseInt(args[++index]);
                case "--time" -> moveTime = Long.parseLong(args[++index]);
                case "--depth" -> depth = Integer.parseInt(args[++index]);
                case "--threads" -> threads = Integer.parseInt(args[++index]);
                default -> refs.add(args[index]);
            }
        }

        if(refs.isEmpty() || refs.size() > 2) {
            System.out.println("usage: java Testing/Match.java <old-commit> [<new-commit>] [--games N] [--time MS] [--depth N] [--threads N]");
            System.out.println("  with one commit, it plays against the working tree");
            return;
        }

        //every opening is played as a pair, so an odd count is rounded up
        games += games % 2;
        Settings settings = new Settings(games, moveTime, depth, Math.min(threads, games));

        Path root = Path.of(git(Path.of("."), "rev-parse", "--show-toplevel"));
        Path temp = Files.createTempDirectory("engine-match");

        //each side builds in its own folder, so the same commit can be given twice
        boolean failed = false;
        try {
            Version oldVersion = Version.build(root, temp.resolve("old"), refs.get(0));
            Version newVersion = Version.build(root, temp.resolve("new"), refs.size() > 1 ? refs.get(1) : null);
            run(newVersion, oldVersion, settings);
        } catch(IllegalStateException e) {
            //expected problems (bad commit, too old, doesn't compile) already say what's wrong; no stack trace needed
            System.err.println("\nerror: " + e.getMessage());
            failed = true;
        } finally {
            deleteRecursively(temp);
        }

        //exiting inside the catch would skip the cleanup above
        if(failed) {
            System.exit(1);
        }
    }

    private static void run(Version newVersion, Version oldVersion, Settings settings) throws Exception {
        //one pair of engines per worker thread, each with its own class loaders, so games can run side by side
        ThreadLocal<Player[]> players = ThreadLocal.withInitial(() -> new Player[] {
            new Player(newVersion, settings), new Player(oldVersion, settings)
        });

        Player[] sample = players.get();
        System.out.println(sample[0].describe() + "   vs   " + sample[1].describe());
        System.out.println(settings.games() + " games, " + settings.threads() + " at a time\n");

        ExecutorService pool = Executors.newFixedThreadPool(settings.threads());
        CompletionService<GameResult> results = new ExecutorCompletionService<>(pool);

        for(int game = 0; game < settings.games(); game++) {
            int number = game;
            results.submit(() -> {
                Player[] pair = players.get();
                String[] opening = openings[(number / 2) % openings.length];
                return playGame(number + 1, pair[0], pair[1], number % 2 == 0, opening);
            });
        }

        int wins = 0;
        int draws = 0;
        int losses = 0;
        int aborted = 0;

        try {
            for(int finished = 1; finished <= settings.games(); finished++) {
                GameResult result = results.take().get();

                switch(result.outcome()) {
                    case WIN -> wins++;
                    case DRAW -> draws++;
                    case LOSS -> losses++;
                    case ABORTED -> aborted++;
                }

                String white = result.newIsWhite() ? newVersion.label() : oldVersion.label();
                String black = result.newIsWhite() ? oldVersion.label() : newVersion.label();
                System.out.printf("%3d/%d  %-24s %14s %-7s %-14s %-36s +%d =%d -%d%n",
                        finished, settings.games(), result.opening(), white, scoreText(result), black,
                        result.reason() + ", " + result.moves() + " moves", wins, draws, losses);
            }
        } finally {
            pool.shutdownNow();
        }

        printSummary(newVersion.label(), oldVersion.label(), wins, draws, losses, aborted);
    }

    //"1-0" style result from white's side
    private static String scoreText(GameResult result) {
        return switch(result.outcome()) {
            case DRAW -> "1/2";
            case ABORTED -> "*";
            case WIN -> result.newIsWhite() ? "1-0" : "0-1";
            case LOSS -> result.newIsWhite() ? "0-1" : "1-0";
        };
    }

    private static void printSummary(String newLabel, String oldLabel, int wins, int draws, int losses, int aborted) {
        int games = wins + draws + losses;
        System.out.println();
        System.out.printf("%s vs %s: +%d =%d -%d%n", newLabel, oldLabel, wins, draws, losses);
        if(aborted > 0) {
            System.out.println(aborted + " game(s) aborted and left out (the two versions disagreed on a move's legality)");
        }
        if(games == 0) {
            return;
        }

        double score = (wins + draws / 2.0) / games;

        //95% Wilson interval on the score; unlike the plain plus-or-minus formula it still gives a real bound
        //when one side wins every game, which is common against much older commits
        double z = 1.96;
        double center = (score + z * z / (2 * games)) / (1 + z * z / games);
        double halfWidth = z * Math.sqrt(score * (1 - score) / games + z * z / (4.0 * games * games)) / (1 + z * z / games);

        System.out.printf("score %.1f%%, Elo difference %s (95%% range %s to %s)%n", score * 100,
                eloText(score), eloText(center - halfWidth), eloText(center + halfWidth));
    }

    //Elo difference that would produce this expected score
    private static String eloText(double score) {
        //rounding can leave a 100% bound a hair under 1, which would print an absurd finite number
        if(score >= 1 - 1e-9) {
            return "+inf";
        }
        if(score <= 1e-9) {
            return "-inf";
        }
        //rounded to a whole number first so an even score prints as +0 rather than -0
        return String.format("%+d", Math.round(-400 * Math.log10(1 / score - 1)));
    }

    private static GameResult playGame(int game, Player newPlayer, Player oldPlayer, boolean newIsWhite, String[] opening) throws Exception {
        newPlayer.newGame();
        oldPlayer.newGame();

        //the new version's position also referees: it decides legality, mate and the draw rules
        Player referee = newPlayer;
        Map<String, Integer> seenPositions = new HashMap<>();
        int fiftyMoveClock = 0;
        int plies = 0;

        for(String text : opening[1].split(" ")) {
            int move = parseMove(text);
            newPlayer.play(move);
            oldPlayer.play(move);
            plies++;
        }
        seenPositions.merge(referee.positionKey(), 1, Integer::sum);

        while(true) {
            int moves = (plies + 1) / 2;
            boolean whiteToMove = referee.whiteToMove();
            //whether the side to move is the new version
            boolean newToMove = whiteToMove == newIsWhite;

            if(referee.legalMoveCount() == 0) {
                if(!referee.inCheck()) {
                    return new GameResult(game, opening[0], newIsWhite, Outcome.DRAW, "stalemate", moves);
                }
                return new GameResult(game, opening[0], newIsWhite, newToMove ? Outcome.LOSS : Outcome.WIN, "checkmate", moves);
            }
            if(seenPositions.getOrDefault(referee.positionKey(), 0) >= 3) {
                return new GameResult(game, opening[0], newIsWhite, Outcome.DRAW, "threefold repetition", moves);
            }
            if(fiftyMoveClock >= 100) {
                return new GameResult(game, opening[0], newIsWhite, Outcome.DRAW, "fifty-move rule", moves);
            }
            if(referee.insufficientMaterial()) {
                return new GameResult(game, opening[0], newIsWhite, Outcome.DRAW, "insufficient material", moves);
            }
            if(plies >= maxPlies) {
                return new GameResult(game, opening[0], newIsWhite, Outcome.DRAW, "move limit", moves);
            }

            Player mover = newToMove ? newPlayer : oldPlayer;
            Outcome forfeit = newToMove ? Outcome.LOSS : Outcome.WIN;

            int move;
            try {
                move = mover.think();
            } catch(InvocationTargetException e) {
                return new GameResult(game, opening[0], newIsWhite, forfeit, mover.label + " crashed: " + e.getCause(), moves);
            }
            if(move < 0) {
                return new GameResult(game, opening[0], newIsWhite, forfeit, mover.label + " returned no move", moves);
            }

            boolean resetsClock = referee.resetsFiftyMoveClock(move);

            if(!referee.tryPlay(move)) {
                return new GameResult(game, opening[0], newIsWhite, forfeit, mover.label + " played illegal " + moveName(move), moves);
            }
            //the referee allowed it, so if the old version doesn't, their move generators disagree and nobody is at fault
            if(!oldPlayer.tryPlay(move)) {
                return new GameResult(game, opening[0], newIsWhite, Outcome.ABORTED, oldPlayer.label + " rejects " + moveName(move), moves);
            }

            plies++;
            fiftyMoveClock = resetsClock ? 0 : fiftyMoveClock + 1;
            seenPositions.merge(referee.positionKey(), 1, Integer::sum);
        }
    }

    //moves cross between versions as start | target << 6 | promotion piece << 12, each side translating to its own encoding
    private static int encodeMove(int start, int target, int promotionPiece) {
        return start | (target << 6) | (promotionPiece << 12);
    }

    //openings are written as start and target squares, e.g. "e2e4"
    private static int parseMove(String text) {
        return encodeMove(square(text.substring(0, 2)), square(text.substring(2, 4)), 0);
    }

    //square indices start at a8, so ranks count down as the index goes up
    private static int square(String name) {
        return (7 - (name.charAt(1) - '1')) * 8 + (name.charAt(0) - 'a');
    }

    private static String squareName(int square) {
        return "" + (char) ('a' + (square & 7)) + (char) ('8' - (square >>> 3));
    }

    //promotion pieces use the engine's piece type numbers: bishop 1, rook 3, knight 4, queen 5
    private static String moveName(int move) {
        String promotion = switch(move >>> 12) {
            case 1 -> "b";
            case 3 -> "r";
            case 4 -> "n";
            case 5 -> "q";
            default -> "";
        };
        return squareName(move & 63) + squareName((move >>> 6) & 63) + promotion;
    }

    //one commit's compiled classes
    private record Version(String label, Path classes) {
        //ref null means the working tree, uncommitted changes included
        static Version build(Path root, Path temp, String ref) throws Exception {
            Files.createDirectories(temp);
            String label;
            Path sources;

            if(ref == null) {
                label = "working tree";
                sources = root;
            } else {
                label = git(root, "rev-parse", "--short", ref + "^{commit}");
                sources = temp.resolve("src");
                Path zip = temp.resolve("source.zip");
                git(root, "archive", "--format=zip", "-o", zip.toString(), label, "EngineUtil", "GUI");
                unzip(zip, sources);
            }

            System.out.print("Building " + label + "... ");
            Path classes = temp.resolve("classes");

            //GUI comes along because FENUtil imports from it; none of it is ever loaded
            List<String> arguments = new ArrayList<>(List.of("-d", classes.toString(), "-encoding", "UTF-8", "-nowarn", "-Xlint:none", "-proc:none"));
            arguments.addAll(javaFiles(sources.resolve("EngineUtil")));
            arguments.addAll(javaFiles(sources.resolve("GUI")));

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if(compiler == null) {
                throw new IllegalStateException("No Java compiler available; run this with a JDK, not a JRE");
            }
            ByteArrayOutputStream errors = new ByteArrayOutputStream();
            if(compiler.run(null, null, errors, arguments.toArray(String[]::new)) != 0) {
                System.out.println("failed");
                throw new IllegalStateException("Couldn't compile " + label + ":\n" + errors.toString(StandardCharsets.UTF_8));
            }

            System.out.println("done");
            return new Version(label, classes);
        }
    }

    //one engine version behind its own class loader; every engine call goes through reflection, since each
    //version's classes are different classes as far as the JVM is concerned
    private static final class Player {
        final String label;
        private final boolean timeBased;
        private final long moveTime;
        private final int depth;

        private final Constructor<?> newPosition;
        private final Object moveGenerator;
        private final Method findBestMove;
        private final Method getLegalMove;
        private final Method legalMoveCount;
        private final Method inCheck;
        private final Method playMove;
        private final Method startSquare;
        private final Method targetSquare;
        private final Method promotionPiece;

        private final Field[] pieceBoards;
        private final Field whiteToMove;
        private final Field castlingRights;
        private final Field enPassantSquare;
        private final Field whitePawns, blackPawns, whiteRooks, blackRooks, whiteQueens, blackQueens;
        private final Field whiteKnights, blackKnights, whiteBishops, blackBishops;

        private Object position;

        Player(Version version, Settings settings) {
            label = version.label();

            try {
                //the platform loader as parent, so nothing can be picked up from another version's classes
                ClassLoader loader = new URLClassLoader(new URL[] {version.classes().toUri().toURL()}, ClassLoader.getPlatformClassLoader());
                Class<?> engine = Class.forName("EngineUtil.Engine", true, loader);
                Class<?> positionClass = Class.forName("EngineUtil.Position", true, loader);
                Class<?> generatorClass = Class.forName("EngineUtil.MoveGenerator", true, loader);
                Class<?> moveClass = Class.forName("EngineUtil.Move", true, loader);

                Method search = null;
                for(Method method : engine.getMethods()) {
                    if(method.getName().equals("findBestMove") && method.getParameterCount() == 2
                            && method.getParameterTypes()[0] == positionClass) {
                        search = method;
                    }
                }
                if(search == null) {
                    throw new IllegalStateException(label + " has no Engine.findBestMove(Position, ...); only 97a4ffe and later can play");
                }
                findBestMove = search;
                timeBased = search.getParameterTypes()[1] == long.class;
                moveTime = settings.moveTime();
                depth = settings.depth() != null ? settings.depth() : commitSearchDepth(loader);

                newPosition = positionClass.getConstructor();
                moveGenerator = generatorClass.getConstructor().newInstance();
                getLegalMove = generatorClass.getMethod("getLegalMove", int.class, int.class, int.class, positionClass);
                legalMoveCount = generatorClass.getMethod("getNumLegalMoves", positionClass);
                inCheck = generatorClass.getMethod("inCheck", positionClass);
                playMove = positionClass.getMethod("playMove", short.class);
                startSquare = moveClass.getMethod("startSquare", short.class);
                targetSquare = moveClass.getMethod("targetSquare", short.class);
                promotionPiece = moveClass.getMethod("promotionPiece", short.class);

                whitePawns = field(positionClass, "whitePawns");
                whiteKnights = field(positionClass, "whiteKnights");
                whiteBishops = field(positionClass, "whiteBishops");
                whiteRooks = field(positionClass, "whiteRooks");
                whiteQueens = field(positionClass, "whiteQueens");
                Field whiteKing = field(positionClass, "whiteKing");
                blackPawns = field(positionClass, "blackPawns");
                blackKnights = field(positionClass, "blackKnights");
                blackBishops = field(positionClass, "blackBishops");
                blackRooks = field(positionClass, "blackRooks");
                blackQueens = field(positionClass, "blackQueens");
                Field blackKing = field(positionClass, "blackKing");
                pieceBoards = new Field[] {whitePawns, whiteKnights, whiteBishops, whiteRooks, whiteQueens, whiteKing,
                        blackPawns, blackKnights, blackBishops, blackRooks, blackQueens, blackKing};
                whiteToMove = field(positionClass, "whiteToMove");
                castlingRights = field(positionClass, "castlingRights");
                enPassantSquare = field(positionClass, "enPassantSquare");
            } catch(ReflectiveOperationException | IOException e) {
                throw new IllegalStateException(label + " doesn't have the engine API this match runner needs", e);
            }
        }

        //fixed-depth commits kept their depth in Board.searchDepth
        private static int commitSearchDepth(ClassLoader loader) {
            try {
                return field(Class.forName("EngineUtil.Board", true, loader), "searchDepth").getInt(null);
            } catch(ReflectiveOperationException e) {
                return fallbackDepth;
            }
        }

        private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        }

        String describe() {
            return label + (timeBased ? " (" + moveTime + " ms per move)" : " (fixed depth " + depth + ")");
        }

        void newGame() throws ReflectiveOperationException {
            position = newPosition.newInstance();
        }

        //returns -1 if the engine had no move to give
        int think() throws ReflectiveOperationException {
            Object limit = timeBased ? (Object) moveTime : (Object) depth;
            short move = (short) findBestMove.invoke(null, position, limit);
            if(move == 0) {
                return -1;
            }
            return encodeMove((int) startSquare.invoke(null, move), (int) targetSquare.invoke(null, move),
                    (int) promotionPiece.invoke(null, move));
        }

        void play(int move) throws ReflectiveOperationException {
            if(!tryPlay(move)) {
                throw new IllegalStateException(label + " rejects " + moveName(move));
            }
        }

        //plays the move if this version agrees it's legal
        boolean tryPlay(int move) throws ReflectiveOperationException {
            short own = (short) getLegalMove.invoke(moveGenerator, move & 63, (move >>> 6) & 63, move >>> 12, position);
            if(own == 0) {
                return false;
            }
            playMove.invoke(position, own);
            return true;
        }

        int legalMoveCount() throws ReflectiveOperationException {
            return (int) legalMoveCount.invoke(moveGenerator, position);
        }

        boolean inCheck() throws ReflectiveOperationException {
            return (boolean) inCheck.invoke(moveGenerator, position);
        }

        boolean whiteToMove() throws IllegalAccessException {
            return whiteToMove.getBoolean(position);
        }

        //everything that makes two positions the same for the repetition rule
        String positionKey() throws IllegalAccessException {
            StringBuilder key = new StringBuilder();
            for(Field board : pieceBoards) {
                key.append(board.getLong(position)).append(',');
            }
            return key.append(whiteToMove.getBoolean(position)).append(',')
                    .append(castlingRights.getInt(position)).append(',')
                    .append(enPassantSquare.getInt(position)).toString();
        }

        //pawn moves and captures reset the fifty-move count; call it before the move is played
        boolean resetsFiftyMoveClock(int move) throws IllegalAccessException {
            long pawns = whitePawns.getLong(position) | blackPawns.getLong(position);
            long occupied = 0;
            for(Field board : pieceBoards) {
                occupied |= board.getLong(position);
            }
            return (pawns & (1L << (move & 63))) != 0 || (occupied & (1L << ((move >>> 6) & 63))) != 0;
        }

        //bare kings, or kings plus a single bishop or knight
        boolean insufficientMaterial() throws IllegalAccessException {
            long heavy = whitePawns.getLong(position) | blackPawns.getLong(position)
                    | whiteRooks.getLong(position) | blackRooks.getLong(position)
                    | whiteQueens.getLong(position) | blackQueens.getLong(position);
            long minors = whiteKnights.getLong(position) | blackKnights.getLong(position)
                    | whiteBishops.getLong(position) | blackBishops.getLong(position);
            return heavy == 0 && Long.bitCount(minors) <= 1;
        }
    }

    //runs git and returns its trimmed output; errors go straight to the console
    private static String git(Path directory, String... arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(arguments));

        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String output;
        try(InputStream in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        if(process.waitFor() != 0) {
            throw new IllegalStateException("git " + String.join(" ", arguments) + " failed");
        }
        return output;
    }

    private static void unzip(Path zip, Path target) throws IOException {
        try(ZipInputStream in = new ZipInputStream(Files.newInputStream(zip))) {
            for(ZipEntry entry; (entry = in.getNextEntry()) != null; ) {
                Path out = target.resolve(entry.getName()).normalize();
                if(!out.startsWith(target)) {
                    throw new IOException("Zip entry outside the target folder: " + entry.getName());
                }
                if(entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(in, out);
                }
            }
        }
    }

    private static List<String> javaFiles(Path directory) throws IOException {
        try(Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.toString().endsWith(".java")).map(Path::toString).toList();
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        try(Stream<Path> paths = Files.walk(directory)) {
            for(Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
