import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import static java.lang.System.out;

public class Board extends Canvas {
    public final int size = 8;
    public final int squareSize;
    public ArrayList<Piece> pieces;

    private final Image blackPieceImage;
    private final Image whitePieceImage;
    private final Image queenBlackImage;
    private final Image queenWhiteImage;

    private Piece selectedPiece = null;
    private boolean whiteTurn = true;

    private Player whitePlayer;
    private Player blackPlayer;
    private int moveCount = 0;
    private long gameStartTime;
    private DataManager dataManager;
    private boolean gameEnded = false;

    private Image loadImage(String path) {
        var url = Objects.requireNonNull(Board.class.getResource(path), "Nenašel jsem resource: " + path);
        return new Image(url.toExternalForm());
    }

    public Board(int width, int height, Player whitePlayer, Player blackPlayer, DataManager dataManager) {
        super(width, height);

        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.dataManager = dataManager;
        this.gameStartTime = System.currentTimeMillis();

        pieces = new ArrayList<>();
        squareSize = Math.min(width, height) / size;

        blackPieceImage = loadImage("/images/black.png");
        whitePieceImage = loadImage("/images/white.png");
        queenBlackImage = loadImage("/images/qeenB.png");
        queenWhiteImage = loadImage("/images/qeenW.png");

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

        drawBoard();

        this.setOnMouseClicked(event -> {
            if (gameEnded) {
                return;
            }

            int col = (int)(event.getX() / squareSize);
            int row = (int)(event.getY() / squareSize);
            out.println("Klik: r=" + row + ", c=" + col);

            Piece clickedPiece = findPieceAt(row, col);

            boolean mustCapture = false;
            for (Piece p : pieces) {
                if ((whiteTurn && p.getColor() == Piece.PieceColor.WHITE) ||
                        (!whiteTurn && p.getColor() == Piece.PieceColor.BLACK)) {
                    if (hasCaptureFrom(p)) {
                        mustCapture = true;
                        break;
                    }
                }
            }

            if (clickedPiece != null) {
                boolean belongsToCurrent = (whiteTurn && clickedPiece.getColor() == Piece.PieceColor.WHITE) ||
                        (!whiteTurn && clickedPiece.getColor() == Piece.PieceColor.BLACK);
                if (belongsToCurrent) {
                    if (!mustCapture || hasCaptureFrom(clickedPiece)) {
                        selectedPiece = clickedPiece;
                        out.println("Vybraný panáček r=" + row + ", c=" + col);
                    } else {
                        out.println("Panáček, který musí brát");
                    }
                }
            } else if (selectedPiece != null) {
                Piece captured = getCapturedPieceIfAny(selectedPiece, row, col);

                if (mustCapture && captured == null) {
                    out.println("Neplatný tah: je povinné brát");
                } else if (captured != null) {
                    pieces.remove(captured);
                    selectedPiece.setPosition(row, col);
                    boolean promoted = maybePromote(selectedPiece);

                    if (!promoted && hasCaptureFrom(selectedPiece)) {
                        // Vícenásobné skákání
                    } else {
                        selectedPiece = null;
                        whiteTurn = !whiteTurn;
                        moveCount++;
                        checkGameEnd();
                    }
                } else {
                    if (!mustCapture && isValidSimpleMove(selectedPiece, row, col)) {
                        selectedPiece.setPosition(row, col);
                        maybePromote(selectedPiece);
                        selectedPiece = null;
                        whiteTurn = !whiteTurn;
                        moveCount++;
                        checkGameEnd();
                    } else {
                        out.println("Neplatný tah podle pravidel");
                    }
                }
            }

            drawBoard();
        });
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

            showGameEndDialog(winner, gameDuration);
        }
    }

    private boolean canPieceMove(Piece piece) {
        if (hasCaptureFrom(piece)) {
            return true;
        }

        int[][] dirs;
        if (piece.isQueen()) {
            dirs = new int[][] { {-1,-1}, {-1,1}, {1,-1}, {1,1} };
        } else if (piece.getColor() == Piece.PieceColor.WHITE) {
            dirs = new int[][] { {1,-1}, {1,1} };
        } else {
            dirs = new int[][] { {-1,-1}, {-1,1} };
        }

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

    private void showGameEndDialog(String winner, long gameDuration) {
        long seconds = gameDuration / 1000;
        long minutes = seconds / 60;
        long secs = seconds % 60;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Konec hry");
        alert.setHeaderText("Hra skončila!");
        alert.setContentText(String.format("Vítěz: %s\n" + "Počet tahů: %d\n" + "Čas hry: %d:%02d\n" + "Výsledek byl uložen.", winner, moveCount, minutes, secs));

        Optional<ButtonType> result = alert.showAndWait();
    }

    private void drawBoard() {
        GraphicsContext gc = getGraphicsContext2D();

        for(int row = 0; row < size; row++) {
            for(int col = 0; col < size; col++) {
                if((row + col) % 2 == 0) {
                    gc.setFill(Color.GREY);
                } else {
                    gc.setFill(Color.WHITESMOKE);
                }
                gc.fillRect(col * squareSize, row * squareSize, squareSize, squareSize);
            }
        }

        if (selectedPiece != null) {
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(4);
            gc.strokeRect(selectedPiece.getCol() * squareSize, selectedPiece.getRow() * squareSize,
                    squareSize, squareSize);
        }

        drawPieces(gc);
        drawGameInfo(gc);
    }

    private void drawGameInfo(GraphicsContext gc) {
        gc.setFill(Color.BLACK);
        gc.fillText("Bílý: " + whitePlayer.getName(), 10, 20);
        gc.fillText("Černý: " + blackPlayer.getName(), 10, 40);
        gc.fillText("Tahy: " + moveCount, 10, 60);
        gc.fillText("Na tahu: " + (whiteTurn ? "Bílý" : "Černý"), 10, 80);

        long elapsed = (System.currentTimeMillis() - gameStartTime) / 1000;
        long minutes = elapsed / 60;
        long secs = elapsed % 60;
        gc.fillText(String.format("Čas: %d:%02d", minutes, secs), 10, 100);


    }

    private void drawPieces(GraphicsContext gc) {
        for (IDrawable piece : pieces) {
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

    private boolean hasCaptureFrom(Piece piece) {
        int[][] dirs;
        if (piece.isQueen()) {
            dirs = new int[][] { {-2,-2}, {-2,2}, {2,-2}, {2,2} };
        } else if (piece.getColor() == Piece.PieceColor.WHITE) {
            dirs = new int[][] { {2,-2}, {2,2} };
        } else {
            dirs = new int[][] { {-2,-2}, {-2,2} };
        }
        for (int[] d : dirs) {
            int tr = piece.getRow() + d[0];
            int tc = piece.getCol() + d[1];
            if (tr < 0 || tr >= size || tc < 0 || tc >= size) {
                continue;
            }
            if ((tr + tc) % 2 != 0) {
                continue;
            }
            if (findPieceAt(tr, tc) != null) {
                continue;
            }
            int mr = piece.getRow() + d[0]/2;
            int mc = piece.getCol() + d[1]/2;
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
        if (Math.abs(dr) == 2 && Math.abs(dc) == 2) {
            if (targetRow >= 0 && targetRow < size && targetCol >= 0 && targetCol < size &&
                    (targetRow + targetCol) % 2 == 0 && findPieceAt(targetRow, targetCol) == null) {
                int mr = piece.getRow() + dr/2;
                int mc = piece.getCol() + dc/2;
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
        int dc = Math.abs(targetCol - piece.getCol());
        if (piece.isQueen()) {
            return Math.abs(dr) == 1 && dc == 1;
        }
        if (piece.getColor() == Piece.PieceColor.WHITE) {
            return dr == 1 && dc == 1;
        } else {
            return dr == -1 && dc == 1;
        }
    }

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

