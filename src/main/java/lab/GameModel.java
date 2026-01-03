package lab;

import javafx.scene.image.Image;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/*
 * GameModel
 *
 * Version 1.1
 *
 * 2025 Checkers Project
 */

/**
 * Represents the game logic and state independent of the view.
 * Handles moves, rules, and game state management.
 */
public class GameModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Piece> pieces;
    private Piece selectedPiece = null;
    private boolean whiteTurn = false;
    private boolean mustContinueJump = false;
    private boolean gameEnded = false;
    private int moveCount = 0;
    private final long gameStartTime;

    private final Player whitePlayer;
    private final Player blackPlayer;

    private transient DataManager dataManager;

    // Transient = not saving to file (images, callbacks)
    private transient Image whiteImg, blackImg, queenWhiteImg, queenBlackImg;
    private transient Consumer<String> onMessage;
    private transient Consumer<String> onGameEnd;

    public GameModel(Player white, Player black, DataManager dataMgr,
                     Image whiteImg, Image blackImg, Image qWhite, Image qBlack,
                     Consumer<String> onMessage, Consumer<String> onGameEnd) {
        this.whitePlayer = white;
        this.blackPlayer = black;
        this.dataManager = dataMgr;
        this.whiteImg = whiteImg;
        this.blackImg = blackImg;
        this.queenWhiteImg = qWhite;
        this.queenBlackImg = qBlack;
        this.onMessage = onMessage;
        this.onGameEnd = onGameEnd;

        this.gameStartTime = System.currentTimeMillis();
        this.pieces = new ArrayList<>();
        initializePieces();
    }

    /**
     * Sets up initial piece positions.
     */
    private void initializePieces() {
        int size = 8;
        // White pieces
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < size; col++) {
                if ((row + col) % 2 == 0) {
                    pieces.add(new Piece(whiteImg, row, col, Piece.PieceColor.WHITE));
                }
            }
        }
        // Black pieces
        for (int row = size - 3; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if ((row + col) % 2 == 0) {
                    pieces.add(new Piece(blackImg, row, col, Piece.PieceColor.BLACK));
                }
            }
        }
    }


    /**
     * Saves the current game state to a binary file.
     */
    public void saveGame(String filePath) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(this);
            if (onMessage != null) onMessage.accept("Hra byla uložena.");
        } catch (IOException e) {
            if (onMessage != null) onMessage.accept("Chyba při ukládání: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads the game state from a binary file.
     * Must be static to create a new instance.
     */
    public static GameModel loadGame(String filePath,
                                     Image w, Image b, Image qw, Image qb,
                                     Consumer<String> msg, Consumer<String> end,
                                     DataManager dataManager) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            GameModel loadedModel = (GameModel) in.readObject();

            loadedModel.restoreState(w, b, qw, qb, msg, end, dataManager);
            return loadedModel;
        } catch (IOException | ClassNotFoundException e) {
            msg.accept("Nepodařilo se načíst hru.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Restores images/callbacks after deserialization.
     */
    private void restoreState(Image w, Image b, Image qw, Image qb,
                              Consumer<String> msg, Consumer<String> end,
                              DataManager dataManager) {
        this.whiteImg = w;
        this.blackImg = b;
        this.queenWhiteImg = qw;
        this.queenBlackImg = qb;
        this.onMessage = msg;
        this.onGameEnd = end;
        this.dataManager = dataManager;

        // Restore images to all pieces
        for (Piece p : pieces) {
            if (p.isQueen()) {
                p.setImage(p.getColor() == Piece.PieceColor.WHITE ? qw : qb);
            } else {
                p.setImage(p.getColor() == Piece.PieceColor.WHITE ? w : b);
            }
        }
    }

    //Game logic
    public void handleTileClick(int row, int col) {
        if (gameEnded) return;

        Piece clickedPiece = findPieceAt(row, col);

        if (mustContinueJump) {
            if (clickedPiece != null && clickedPiece != selectedPiece) {
                onMessage.accept("Musíš dokončit skákání s vybranou figurkou!");
                return;
            }
        }

        boolean globalMustCapture = checkGlobalMustCapture();

        if (clickedPiece != null) {
            if (mustContinueJump) return;
            boolean belongsToCurrent = (whiteTurn && clickedPiece.getColor() == Piece.PieceColor.WHITE) ||
                    (!whiteTurn && clickedPiece.getColor() == Piece.PieceColor.BLACK);

            if (belongsToCurrent) {
                if (!globalMustCapture || hasCaptureFrom(clickedPiece)) {
                    selectedPiece = clickedPiece;
                } else {
                    onMessage.accept("Musíš táhnout figurkou, která může brát!");
                }
            }
        } else if (selectedPiece != null) {
            tryMove(row, col, globalMustCapture);
        }
    }

    private void tryMove(int row, int col, boolean globalMustCapture) {
        Piece captured = getCapturedPieceIfAny(selectedPiece, row, col);

        if (globalMustCapture && captured == null) {
            onMessage.accept("Neplatný tah: je povinné brát!");
            return;
        }

        if (captured != null) {
            pieces.remove(captured);
            selectedPiece.setPosition(row, col);
            boolean promoted = maybePromote(selectedPiece);

            if (!promoted && hasCaptureFrom(selectedPiece)) {
                mustContinueJump = true;
                onMessage.accept("Musíš skákat dál!");
            } else {
                endTurn();
            }
        } else if (!globalMustCapture && isValidSimpleMove(selectedPiece, row, col)) {
            selectedPiece.setPosition(row, col);
            maybePromote(selectedPiece);
            endTurn();
        } else {
            onMessage.accept("Neplatný tah.");
        }
    }

    private void endTurn() {
        mustContinueJump = false;
        selectedPiece = null;
        whiteTurn = !whiteTurn;
        moveCount++;
        checkGameEnd();
    }

    //streams
    private boolean checkGlobalMustCapture() {
        return pieces.stream().anyMatch(p ->
                ((whiteTurn && p.getColor() == Piece.PieceColor.WHITE) ||
                        (!whiteTurn && p.getColor() == Piece.PieceColor.BLACK))
                        && hasCaptureFrom(p)
        );
    }

    private Piece findPieceAt(int row, int col) {
        return pieces.stream()
                .filter(p -> p.getRow() == row && p.getCol() == col)
                .findFirst()
                .orElse(null);
    }

    //helper
    private boolean hasCaptureFrom(Piece piece) {
        int[][] dirs = getDirections(piece);
        int size = 8;

        for (int[] d : dirs) {
            int tr = piece.getRow() + d[0] * 2;
            int tc = piece.getCol() + d[1] * 2;

            if (tr < 0 || tr >= size || tc < 0 || tc >= size) {
                continue;
            }
            if (findPieceAt(tr, tc) != null) {
                continue;
            }

            int mr = piece.getRow() + d[0];
            int mc = piece.getCol() + d[1];
            Piece mid = findPieceAt(mr, mc);
            if (isOpponent(piece, mid)) {
                return true;
            }
        }
        return false;
    }

    private Piece getCapturedPieceIfAny(Piece piece, int targetRow, int targetCol) {
        int dr = targetRow - piece.getRow();
        int dc = targetCol - piece.getCol();
        int size = 8;

        if (Math.abs(dr) == 2 && Math.abs(dc) == 2) {
            int stepRow = dr / 2;
            int stepCol = dc / 2;

            boolean directionAllowed = false;
            for (int[] d : getDirections(piece)) {
                if (d[0] == stepRow && d[1] == stepCol) {
                    directionAllowed = true;
                    break;
                }
            }

            if (!directionAllowed) {
                return null;
            }

            if (targetRow >= 0 && targetRow < size && targetCol >= 0 && targetCol < size &&
                    findPieceAt(targetRow, targetCol) == null) {
                int mr = piece.getRow() + dr / 2;
                int mc = piece.getCol() + dc / 2;
                Piece mid = findPieceAt(mr, mc);
                if (isOpponent(piece, mid)) {
                    return mid;
                }
            }
        }
        return null;
    }

    private boolean isValidSimpleMove(Piece piece, int targetRow, int targetCol) {
        if ((targetRow + targetCol) % 2 != 0) {
            return false;
        }
        if (findPieceAt(targetRow, targetCol) != null) {
            return false;
        }

        int dr = targetRow - piece.getRow();
        int dc = targetCol - piece.getCol();

        int[][] dirs = getDirections(piece);
        for (int[] d : dirs) {
            if (d[0] == dr && d[1] == dc) {
                return true;
            }
        }
        return false;
    }

    /**
     * Promotes a piece to queen if reaches opposite end of the board.
     */
    private boolean maybePromote(Piece piece) {
        boolean promoted = false;
        int size = 8;
        if (!piece.isQueen()) {
            if (piece.getColor() == Piece.PieceColor.WHITE && piece.getRow() == size - 1) {
                piece.setQueen(true);
                piece.setImage(queenWhiteImg);
                promoted = true;
            } else if (piece.getColor() == Piece.PieceColor.BLACK && piece.getRow() == 0) {
                piece.setQueen(true);
                piece.setImage(queenBlackImg);
                promoted = true;
            }
        }
        return promoted;
    }

    private void checkGameEnd() {
        int whitePieces = 0;
        int blackPieces = 0;
        boolean whiteCanMove = false;
        boolean blackCanMove = false;

        for (Piece p : pieces) {
            if (p.getColor() == Piece.PieceColor.WHITE) {
                whitePieces++;
                if (!whiteCanMove && canPieceMove(p)) {
                    whiteCanMove = true;
                }
            } else {
                blackPieces++;
                if (!blackCanMove && canPieceMove(p)) {
                    blackCanMove = true;
                }
            }
        }

        String winner = null;
        if (whitePieces == 0 || !whiteCanMove) {
            winner = blackPlayer.getName();
        } else if (blackPieces == 0 || !blackCanMove) {
            winner = whitePlayer.getName();
        }

        if (winner != null) {
            gameEnded = true;
            long gameDuration = System.currentTimeMillis() - gameStartTime;
            GameResult result = new GameResult(
                    whitePlayer.getName(),
                    blackPlayer.getName(),
                    winner,
                    moveCount,
                    gameDuration
            );
            dataManager.addGameResult(result);

            onGameEnd.accept(winner);
        }
    }

    private boolean canPieceMove(Piece piece) {
        if (hasCaptureFrom(piece)) {
            return true;
        }
        int[][] dirs = getDirections(piece);
        int size = 8;

        for (int[] d : dirs) {
            int tr = piece.getRow() + d[0];
            int tc = piece.getCol() + d[1];
            if (tr >= 0 && tr < size && tc >= 0 && tc < size &&
                    (tr + tc) % 2 == 0 && findPieceAt(tr, tc) == null) {
                return true;
            }
        }
        return false;
    }

    private int[][] getDirections(Piece piece) {
        if (piece.isQueen()) {
            return new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        } else if (piece.getColor() == Piece.PieceColor.WHITE) {
            return new int[][]{{1, -1}, {1, 1}};
        } else {
            return new int[][]{{-1, -1}, {-1, 1}};
        }
    }

    private boolean isOpponent(Piece a, Piece b) {
        return a != null && b != null && a.getColor() != b.getColor();
    }

    public List<Piece> getPieces() { return pieces; }
    public Piece getSelectedPiece() { return selectedPiece; }
    public boolean isWhiteTurn() { return whiteTurn; }
    public int getMoveCount() { return moveCount; }
    public boolean isGameEnded() { return gameEnded; }
    public Player getWhitePlayer() { return whitePlayer; }
    public Player getBlackPlayer() { return blackPlayer; }
    public long getStartTime() { return gameStartTime; }
}