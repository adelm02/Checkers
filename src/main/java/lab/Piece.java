/*
 * Piece
 *
 * Version 1.1
 *
 * 2025 Checkers Project
 */
package lab;

import javafx.scene.image.Image;
import java.io.Serializable;

/**
 * Represents a checker piece.
 */
public class Piece extends GameObject implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PieceColor {
        WHITE, BLACK
    }

    private boolean isQueen;
    private final PieceColor color;

    public Piece(Image image, int row, int col, PieceColor color) {
        super(image, row, col);
        this.color = color;
    }

    public boolean isQueen() {
        return isQueen;
    }

    public void setQueen(boolean isQueen) {
        this.isQueen = isQueen;
    }

    public PieceColor getColor() {
        return color;
    }

    @Override
    public void draw(javafx.scene.canvas.GraphicsContext gc, int squareSize) {
        super.draw(gc, squareSize);
    }
}