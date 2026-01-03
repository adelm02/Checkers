/*
 * Board
 *
 * Version 1.1
 *
 * 2025 Checkers Project
 */
package lab;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.Objects;

public class Board extends Canvas {
    public final int size = 8;
    public final int squareSize;

    private final GameModel gameModel;
    private final Label infoLabel;

    // new game
    public Board(int width, int height, Player whitePlayer, Player blackPlayer, DataManager dataManager, Label infoLabel) {
        this(width, height, infoLabel, null, whitePlayer, blackPlayer, dataManager);
    }

    // loaded game
    public Board(int width, int height, Label infoLabel, GameModel loadedModel, Player white, Player black, DataManager dataMgr) {
        super(width, height);
        this.infoLabel = infoLabel;
        this.squareSize = Math.min(width, height) / size;

        // 1. Load images
        Image blackImg = loadImage("/images/black.png");
        Image whiteImg = loadImage("/images/white.png");
        Image queenBlack = loadImage("/images/qeenB.png");
        Image queenWhite = loadImage("/images/qeenW.png");

        if (loadedModel != null) {
            this.gameModel = loadedModel;
        } else {
            // new model = new game
            this.gameModel = new GameModel(
                    white, black, dataMgr,
                    whiteImg, blackImg, queenWhite, queenBlack,
                    this::showAlert,
                    this::showGameEndDialog
            );
        }

        drawBoard();

        this.setOnMouseClicked(event -> {
            int col = (int) (event.getX() / squareSize);
            int row = (int) (event.getY() / squareSize);
            gameModel.handleTileClick(row, col);
            drawBoard();
        });
    }

    private Image loadImage(String path) {
        var url = Objects.requireNonNull(Board.class.getResource(path), "Error resource: " + path);
        return new Image(url.toExternalForm());
    }

    // for saving game
    public GameModel getGameModel() {
        return gameModel;
    }

    private void drawBoard() {
        GraphicsContext gc = getGraphicsContext2D();

        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                gc.setFill((row + col) % 2 == 0 ? Color.GREY : Color.WHITESMOKE);
                gc.fillRect(col * squareSize, row * squareSize, squareSize, squareSize);
            }
        }

        Piece selected = gameModel.getSelectedPiece();
        if (selected != null) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(4);
            gc.strokeRect(selected.getCol() * squareSize, selected.getRow() * squareSize, squareSize, squareSize);
        }

        for (Piece piece : gameModel.getPieces()) {
            piece.draw(gc, squareSize);
        }

        updateGameInfoLabel();
    }

    private void updateGameInfoLabel() {
        long elapsed = (System.currentTimeMillis() - gameModel.getStartTime()) / 1000;
        long seconds = elapsed % 60;
        long minutes = elapsed / 60;

        String infoText = String.format(
                "Bílý: %s  |  Černý: %s%nTahy: %d  |  Na tahu: %s%nČas: %d:%02d",
                gameModel.getWhitePlayer().getName(),
                gameModel.getBlackPlayer().getName(),
                gameModel.getMoveCount(),
                gameModel.isWhiteTurn() ? "Bílý" : "Černý",
                minutes, seconds
        );
        infoLabel.setText(infoText);
    }

    public boolean isGameEnded() {
        return gameModel.isGameEnded();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Upozornění");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showGameEndDialog(String winner) {
        long duration = System.currentTimeMillis() - gameModel.getStartTime();
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Konec hry");
        alert.setHeaderText("Hra skončila!");
        alert.setContentText(String.format("Vítěz: %s%nPočet tahů: %d%nČas hry: %d:%02d%nVýsledek byl uložen.",
                winner, gameModel.getMoveCount(), minutes, secs));
        alert.showAndWait();
    }
}