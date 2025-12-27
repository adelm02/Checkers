import javafx.scene.canvas.GraphicsContext;

public interface IDrawable {
    void draw(GraphicsContext gc, int squareSize);
}