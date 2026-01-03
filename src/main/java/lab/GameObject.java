/*
 * GameObject
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */
package lab;

import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import java.io.Serializable;

/**
 * Base class for drawable game objects.
 */
public abstract class GameObject implements DrawableSimulable, Positionable, Serializable {
    private static final long serialVersionUID = 1L;

    // DŮLEŽITÉ: transient = neukládat obrázek do souboru (obrázky nejdou serializovat)
    protected transient Image image;

    private final double pieceScale = 1.8;
    protected int row;
    protected int col;

    public GameObject(Image image, int row, int col) {
        this.image = image;
        this.row = row;
        this.col = col;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getCol() {
        return col;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    @Override
    public void setPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public void draw(GraphicsContext gc, int squareSize) {
        if (image == null) return;

        double pieceSize = squareSize * pieceScale;
        double aspect = image.getHeight() / image.getWidth();
        double drawWidth = pieceSize;
        double drawHeight = pieceSize * aspect;
        double offsetX = (squareSize - drawWidth) / 2;
        double offsetY = (squareSize - drawHeight) / 2;
        gc.drawImage(image, col * squareSize + offsetX, row * squareSize + offsetY, drawWidth, drawHeight);
    }
}