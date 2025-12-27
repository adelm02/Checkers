/*
 * IPositionable
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */

/**
 * Interface for objects with board position.
 */
public interface IPositionable {
    int getRow();
    int getCol();
    void setPosition(int row, int col);
}