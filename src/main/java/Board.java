/*
 * Board
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Game board for checkers.
 * Handles the game state, pieces, turns and user clicks.
 */
public class Board extends Canvas {
    public final int size = 8;
    public final int squareSize;

    private List<Piece> pieces;

    private final Image blackPieceImage;
    private final Image whitePieceImage;
    private final Image queenBlackImage;
    private final Image queenWhiteImage;

    private Piece selectedPiece = null;
    private boolean whiteTurn = false;
    private boolean mustContinueJump = false;

    private final Player whitePlayer;
    private final Player blackPlayer;
    private int moveCount = 0;
    private final long gameStartTime;
    private final DataManager dataManager;
    private boolean gameEnded = false;

    private final Label infoLabel;

    public Board(int width, int height, Player whitePlayer, Player blackPlayer, DataManager dataManager, Label infoLabel) {
        super(width, height);

        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.dataManager = dataManager;
        this.infoLabel = infoLabel; // label save
        this.gameStartTime = System.currentTimeMillis();

        this.pieces = new ArrayList<>();
        this.squareSize = Math.min(width, height) / size;

        this.blackPieceImage = loadImage("/images/black.png");
        this.whitePieceImage = loadImage("/images/white.png");
        this.queenBlackImage = loadImage("/images/qeenB.png");
        this.queenWhiteImage = loadImage("/images/qeenW.png");

        initializePieces();
        drawBoard();

        this.setOnMouseClicked(event -> handleClick(event.getX(), event.getY()));
    }

    private Image loadImage(String path) {
        var url = Objects.requireNonNull(Board.class.getResource(path), "Error resource: " + path);
        return new Image(url.toExternalForm());
    }

    /**
     * Sets up initial piece positions on the board.
     */
    private void initializePieces() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < size; col++) {
                if ((row + col) % 2 == 0) {
                    pieces.add(new Piece(whitePieceImage, row, col, Piece.PieceColor.WHITE));
                }
            }
        }

        for (int row = size - 3; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if ((row + col) % 2 == 0) {
                    pieces.add(new Piece(blackPieceImage, row, col, Piece.PieceColor.BLACK));
                }
            }
        }
    }

    /**
     * Returns true if the game has already finished.
     */
    public boolean isGameEnded() {
        return gameEnded;
    }

    /**
     * Handles mouse clicks on the board.
     */
    private void handleClick(double x, double y) {
        if (gameEnded) {
            return;
        }

        int col = (int) (x / squareSize);
        int row = (int) (y / squareSize);
        Piece clickedPiece = findPieceAt(row, col);
        // if in middle of a multi-jump, only allow continue
        if (mustContinueJump) {
            if (clickedPiece != null && clickedPiece != selectedPiece) {
                showAlert("Musíš dokončit skákání s vybranou figurkou!");
                return;
            }
        }

        boolean globalMustCapture = checkGlobalMustCapture();

        if (clickedPiece != null) {
            handlePieceSelection(clickedPiece, globalMustCapture);
        } else if (selectedPiece != null) {
            handleMoveAttempt(row, col, globalMustCapture);
        }

        drawBoard();
    }


    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Upozornění");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Checks if any piece of current player can capture.
     */
    private boolean checkGlobalMustCapture() {
        for (Piece p : pieces) {
            if ((whiteTurn && p.getColor() == Piece.PieceColor.WHITE) ||
                    (!whiteTurn && p.getColor() == Piece.PieceColor.BLACK)) {
                if (hasCaptureFrom(p)) return true;
            }
        }
        return false;
    }

    /**
     * Handles when player clicks on a piece.
     */
    private void handlePieceSelection(Piece clickedPiece, boolean globalMustCapture) {
        if (mustContinueJump) return;

        boolean belongsToCurrent = (whiteTurn && clickedPiece.getColor() == Piece.PieceColor.WHITE) ||
                (!whiteTurn && clickedPiece.getColor() == Piece.PieceColor.BLACK);

        if (belongsToCurrent) {
            if (!globalMustCapture || hasCaptureFrom(clickedPiece)) {
                selectedPiece = clickedPiece;
            } else {
                showAlert("Musíš táhnout figurkou, která může brát!");
            }
        }
    }

    /**
     * Handles when player tries to move selected piece.
     */
    private void handleMoveAttempt(int row, int col, boolean globalMustCapture) {
        Piece captured = getCapturedPieceIfAny(selectedPiece, row, col);

        if (globalMustCapture && captured == null) {
            showAlert("Neplatný tah: je povinné brát!");
            return;
        }

        if (captured != null) {
            pieces.remove(captured);
            selectedPiece.setPosition(row, col);
            boolean promoted = maybePromote(selectedPiece);

            if (!promoted && hasCaptureFrom(selectedPiece)) {
                mustContinueJump = true;
                showAlert("Musíš skákat dál!");
            } else {
                endTurn();
            }
        } else if (!globalMustCapture && isValidSimpleMove(selectedPiece, row, col)) {
            selectedPiece.setPosition(row, col);
            maybePromote(selectedPiece);
            endTurn();
        } else {
            showAlert("Neplatný tah.");
        }
    }

    /* End current turn and switch players */
    private void endTurn() {
        mustContinueJump = false;
        selectedPiece = null;
        whiteTurn = !whiteTurn;
        moveCount++;
        checkGameEnd();
    }

    /**
     * Checks if game has ended (no pieces or no valid moves).
     */
    private void checkGameEnd() {
        int whitePieces = 0;  // count of white pieces remaining
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
            showGameEndDialog(winner, gameDuration);
        }
    }

    private void showGameEndDialog(String winner, long gameDuration) {
        long seconds = gameDuration / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Konec hry");
        alert.setHeaderText("Hra skončila!");

        alert.setContentText(String.format("Vítěz: %s%nPočet tahů: %d%nČas hry: %d:%02d%nVýsledek byl uložen.",
                winner, moveCount, minutes, secs));

        alert.showAndWait();
    }

    /**
     * Draws the board and all pieces.
     */
    private void drawBoard() {
        GraphicsContext gc = getGraphicsContext2D();

        // draw checkerboard
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if ((row + col) % 2 == 0) {
                    gc.setFill(Color.GREY);
                } else {
                    gc.setFill(Color.WHITESMOKE);
                }
                gc.fillRect(col * squareSize, row * squareSize, squareSize, squareSize);
            }
        }

        // highlight selected piece
        if (selectedPiece != null) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(4);
            gc.strokeRect(selectedPiece.getCol() * squareSize, selectedPiece.getRow() * squareSize,
                    squareSize, squareSize);
        }

        drawPieces(gc);
        updateGameInfoLabel();
    }


    private void updateGameInfoLabel() {
        long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
        long minutes = elapsed / 60;
        long secs = elapsed % 60;

        String infoText = String.format(
                "Bílý: %s  |  Černý: %s%nTahy: %d  |  Na tahu: %s%nČas: %d:%02d", whitePlayer.getName(), blackPlayer.getName(), moveCount,
                whiteTurn ? "Bílý" : "Černý",
                minutes, secs
        );
        infoLabel.setText(infoText);
    }

    private void drawPieces(GraphicsContext gc) {
        for (Piece piece : pieces) {
            piece.draw(gc, squareSize);
        }
    }

    private Piece findPieceAt(int row, int col) {
        for (Piece piece : pieces) {
            if (piece.getRow() == row && piece.getCol() == col) {
                return piece;
            }
        }
        return null;
    }

    private boolean isOpponent(Piece a, Piece b) {
        return a != null && b != null && a.getColor() != b.getColor();
    }

    /**
     * Returns valid movement directions for a piece.
     * Queens can move in all diagonals, regular pieces only forward.
     */
    private int[][] getDirections(Piece piece) {
        if (piece.isQueen()) {
            return new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        } else if (piece.getColor() == Piece.PieceColor.WHITE) {
            return new int[][]{{1, -1}, {1, 1}};
        } else {
            return new int[][]{{-1, -1}, {-1, 1}};
        }
    }

    private boolean canPieceMove(Piece piece) {
        if (hasCaptureFrom(piece)) {
            return true;
        }
        int[][] dirs = getDirections(piece);

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

    /**
     * Checks if piece can capture from current position.
     */
    private boolean hasCaptureFrom(Piece piece) {
        int[][] dirs = getDirections(piece);

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

    /**
     * Returns the piece that would be captured by this move, or null.
     */
    private Piece getCapturedPieceIfAny(Piece piece, int targetRow, int targetCol) {
        int dr = targetRow - piece.getRow();
        int dc = targetCol - piece.getCol();

        if (Math.abs(dr) == 2 && Math.abs(dc) == 2) {

            //direction check
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
     * Promotes piece to queen if it reaches the end.
     */
    private boolean maybePromote(Piece piece) {
        boolean promoted = false;
        if (!piece.isQueen()) {
            if (piece.getColor() == Piece.PieceColor.WHITE && piece.getRow() == size - 1) {
                piece.setQueen(true);
                piece.setImage(queenWhiteImage);
                promoted = true;
            } else if (piece.getColor() == Piece.PieceColor.BLACK && piece.getRow() == 0) {
                piece.setQueen(true);
                piece.setImage(queenBlackImage);
                promoted = true;
            }
        }
        return promoted;
    }
}