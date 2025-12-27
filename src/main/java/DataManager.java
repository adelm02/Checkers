import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    private static final String DATA_DIR = "data/";
    private static final String PLAYERS_FILE = DATA_DIR + "players.csv";
    private static final String RESULTS_FILE = DATA_DIR + "results.csv";

    private Map<String, Player> players;
    private List<GameResult> gameResults;

    public DataManager() {
        players = new HashMap<>();
        gameResults = new ArrayList<>();
        ensureDataDirectory();
        loadData();
    }

    private void ensureDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void loadData() {
        loadPlayers();
        loadResults();
    }


    private void loadPlayers() {
        File file = new File(PLAYERS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // skip hlavičku
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String name = parts[0];
                    int gamesPlayed = Integer.parseInt(parts[1]);
                    int gamesWon = Integer.parseInt(parts[2]);
                    int totalMoves = Integer.parseInt(parts[3]);
                    long totalTime = Long.parseLong(parts[4]);

                    Player player = new Player(name);
                    player.setStats(gamesPlayed, gamesWon, totalMoves, totalTime);
                    players.put(name, player);
                }
            }
        } catch (IOException e) {
            System.err.println("Chyba při načítání hráčů: " + e.getMessage());
        }
    }

    // Načtení výsledků z CSV
    private void loadResults() {
        File file = new File(RESULTS_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine(); // skip hlavičku
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String white = parts[0];
                    String black = parts[1];
                    String winner = parts[2];
                    int moves = Integer.parseInt(parts[3]);
                    long duration = Long.parseLong(parts[4]);

                    GameResult result = new GameResult(white, black, winner, moves, duration);
                    gameResults.add(result);
                }
            }
        } catch (IOException e) {
            System.err.println("Chyba při načítání výsledků: " + e.getMessage());
        }
    }

    // Uložení hráčů do CSV
    private void savePlayers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(PLAYERS_FILE))) {

            writer.println("name,gamesPlayed,gamesWon,totalMoves,totalTime");

            // Data
            for (Player player : players.values()) {
                writer.printf("%s,%d,%d,%d,%d%n",
                        player.getName(),
                        player.getGamesPlayed(),
                        player.getGamesWon(),
                        player.getTotalMoves(),
                        player.getTotalTimeMillis());
            }
        } catch (IOException e) {
            System.err.println("Chyba při ukládání hráčů: " + e.getMessage());
        }
    }

    // Uložení výsledků do CSV
    private void saveResults() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(RESULTS_FILE))) {
            writer.println("white,black,winner,moves,duration");

            for (GameResult result : gameResults) {
                writer.printf("%s,%s,%s,%d,%d%n",
                        result.getWhitePlayerName(),
                        result.getBlackPlayerName(),
                        result.getWinner(),
                        result.getTotalMoves(),
                        result.getGameDurationMillis());
            }
        } catch (IOException e) {
            System.err.println("Chyba při ukládání výsledků: " + e.getMessage());
        }
    }

    public void saveData() {
        savePlayers();
        saveResults();
    }

    public Player loginPlayer(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Jméno hráče nesmí být prázdné.");
        }

        String key = name.trim();

        // Pokud hráč existuje, vrátíme ho
        if (players.containsKey(key)) {
            return players.get(key);
        }

        // Pokud neexistuje, vytvořím nového
        Player player = new Player(key);
        players.put(key, player);
        saveData();
        return player;
    }

    public void addGameResult(GameResult result) {
        gameResults.add(result);

        // Aktualizace statistik hráčů
        Player whitePlayer = players.get(result.getWhitePlayerName());
        Player blackPlayer = players.get(result.getBlackPlayerName());

        if (whitePlayer != null) {
            boolean won = result.getWinner().equals(whitePlayer.getName());
            whitePlayer.addGameResult(won, result.getTotalMoves(), result.getGameDurationMillis());
        }

        if (blackPlayer != null) {
            boolean won = result.getWinner().equals(blackPlayer.getName());
            blackPlayer.addGameResult(won, result.getTotalMoves(), result.getGameDurationMillis());
        }

        saveData();
    }

    public List<GameResult> getAllResults() {
        return new ArrayList<>(gameResults);
    }

    public List<GameResult> getTopResults(int limit) {
        List<GameResult> sorted = new ArrayList<>(gameResults);
        sorted.sort(null);
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    public List<Player> getTopPlayers(int limit) {
        List<Player> playerList = new ArrayList<>(players.values());
        playerList.sort((p1, p2) -> Double.compare(p2.getWinRate(), p1.getWinRate()));
        return playerList.subList(0, Math.min(limit, playerList.size()));
    }

    public Player getPlayer(String name) {
        return players.get(name);
    }

    public boolean playerExists(String name) {
        return players.containsKey(name);
    }
}