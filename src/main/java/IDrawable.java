/*
 * IDrawable
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */
import javafx.scene.canvas.GraphicsContext;

/**
 * Interface for drawable objects.
 */
public interface IDrawable {
    void draw(GraphicsContext gc, int squareSize);
}