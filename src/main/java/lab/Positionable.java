/*
 * IPositionable
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */
package lab;

/**
 * Interface for objects with board position.
 */
public interface Positionable {
    int getRow();
    int getCol();
    void setPosition(int row, int col);
}