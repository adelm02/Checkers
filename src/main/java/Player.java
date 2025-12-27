import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int gamesPlayed;
    private int gamesWon;
    private int totalMoves;
    private long totalTimeMillis;

    public Player(String name) {
        this.name = name;
        this.gamesPlayed = 0;
        this.gamesWon = 0;
        this.totalMoves = 0;
        this.totalTimeMillis = 0;
    }

    public String getName() {
        return name;
    }

    // Nastavení statistik při načítání z CSV
    public void setStats(int gamesPlayed, int gamesWon, int totalMoves, long totalTimeMillis) {
        this.gamesPlayed = gamesPlayed;
        this.gamesWon = gamesWon;
        this.totalMoves = totalMoves;
        this.totalTimeMillis = totalTimeMillis;
    }

    public void addGameResult(boolean won, int moves, long timeMillis) {
        gamesPlayed++;
        if (won) {
            gamesWon++;
        }
        totalMoves += moves;
        totalTimeMillis += timeMillis;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public double getWinRate() {
        return gamesPlayed > 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
    }

    public double getAverageMoves() {
        return gamesPlayed > 0 ? (double) totalMoves / gamesPlayed : 0;
    }

    public int getTotalMoves() {
        return totalMoves;
    }

    public long getTotalTimeMillis() {
        return totalTimeMillis;
    }

    public long getAverageTimeSeconds() {
        return gamesPlayed > 0 ? (totalTimeMillis / 1000) / gamesPlayed : 0;
    }

    @Override
    public String toString() {
        return String.format("%s - Hry: %d, Vyhry: %d (%.1f%%), Prumer tahu: %.1f",
                name, gamesPlayed, gamesWon, getWinRate(), getAverageMoves());
    }
}