/*
 * GameResult
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stores result of a completed game.
 */
public class GameResult implements  Comparable<GameResult> {

    private final String whitePlayerName;
    private final String blackPlayerName;
    private final String winner;
    private final int totalMoves;
    private final long gameDurationMillis;
    private final LocalDateTime timestamp;

    public GameResult(String whitePlayerName, String blackPlayerName, String winner,
                      int totalMoves, long gameDurationMillis) {
        this.whitePlayerName = whitePlayerName;
        this.blackPlayerName = blackPlayerName;
        this.winner = winner;
        this.totalMoves = totalMoves;
        this.gameDurationMillis = gameDurationMillis;
        this.timestamp = LocalDateTime.now();
    }

    public String getWhitePlayerName() {
        return whitePlayerName;
    }

    public String getBlackPlayerName() {
        return blackPlayerName;
    }

    public String getWinner() {
        return winner;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public long getGameDurationMillis() {
        return gameDurationMillis;
    }

    public long getGameDurationSeconds() {
        return gameDurationMillis / 1000;
    }

    @Override
    public int compareTo(GameResult other) {
        return Long.compare(this.gameDurationMillis, other.gameDurationMillis);
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        long seconds = getGameDurationSeconds();
        long minutes = seconds / 60;
        long secs = seconds % 60;

        return String.format("%s vs %s | Vítěz: %s | Tahy: %d | Čas: %d:%02d | %s",
                whitePlayerName, blackPlayerName, winner, totalMoves,
                minutes, secs, timestamp.format(formatter));
    }
}