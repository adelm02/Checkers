/*
 * IDrawable
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */
package lab;
import javafx.scene.canvas.GraphicsContext;

/**
 * Interface for drawable objects.
 */
public interface DrawableSimulable {
    void draw(GraphicsContext gc, int squareSize);
}