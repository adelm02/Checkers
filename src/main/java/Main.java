/*
 * Main
 *
 * Version 1.0
 *
 * 2025 Checkers Project
 */
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;


/**
 * Main entry point for the Checkers game.
 * Handles all UI screens and navigation.
 */
public class Main extends Application {
    private DataManager dataManager;
    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.dataManager = new DataManager();

        showMainMenu();

        primaryStage.setTitle("Dámy - Checkers");
        primaryStage.show();
    }

    /**
     * Shows the main menu with game options.
     */
    private void showMainMenu() {
        VBox menuBox = new VBox(15);
        menuBox.setPadding(new Insets(20));
        menuBox.setAlignment(Pos.CENTER);
        menuBox.setStyle("-fx-background-color: #f0f0f0;");

        Label titleLabel = new Label("DÁMY");
        titleLabel.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        Button newGameButton = new Button("Nová hra");
        newGameButton.setPrefWidth(200);
        newGameButton.setOnAction(e -> showLoginScreen());

        Button statsButton = new Button("Statistiky");
        statsButton.setPrefWidth(200);
        statsButton.setOnAction(e -> showStatistics());

        Button exitButton = new Button("Konec");
        exitButton.setPrefWidth(200);
        exitButton.setOnAction(e -> primaryStage.close());

        menuBox.getChildren().addAll(titleLabel, newGameButton, statsButton, exitButton);

        Scene scene = new Scene(menuBox, 400, 400);
        primaryStage.setScene(scene);
    }

    /**
     * Login screen where players enter their names.
     */
    private void showLoginScreen() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);
        grid.setStyle("-fx-background-color: #f0f0f0;");

        Label titleLabel = new Label("Přihlášení hráčů");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        GridPane.setColumnSpan(titleLabel, 2);

        Label whiteLabel = new Label("Bílý hráč:");
        whiteLabel.setStyle("-fx-font-weight: bold;");
        TextField whiteNameField = new TextField();
        whiteNameField.setPromptText("Jméno");

        Label blackLabel = new Label("Černý hráč:");
        blackLabel.setStyle("-fx-font-weight: bold;");
        TextField blackNameField = new TextField();
        blackNameField.setPromptText("Jméno");

        Button loginButton = new Button("Přihlásit a hrát");
        loginButton.setPrefWidth(150);

        Button backButton = new Button("Zpět");
        backButton.setPrefWidth(150);
        backButton.setOnAction(e -> showMainMenu());

        grid.add(titleLabel, 0, 0, 2, 1);
        grid.add(whiteLabel, 0, 1);
        grid.add(whiteNameField, 0, 2);
        grid.add(blackLabel, 1, 1);
        grid.add(blackNameField, 1, 2);
        grid.add(loginButton, 0, 4);
        grid.add(backButton, 0, 5, 2, 1);

        loginButton.setOnAction(e -> {
            String whiteName = whiteNameField.getText() == null ? "" : whiteNameField.getText().trim();
            String blackName = blackNameField.getText() == null ? "" : blackNameField.getText().trim();

            if (whiteName.isEmpty() || blackName.isEmpty()) {
                showAlert("Chyba", "Vyplňte obě jména hráčů.");
                return;
            }
            if (whiteName.equals(blackName)) {
                showAlert("Chyba", "Hráči musí mít různá jména!");
                return;
            }

            Player whitePlayer = dataManager.loginPlayer(whiteName);
            Player blackPlayer = dataManager.loginPlayer(blackName);

            startGame(whitePlayer, blackPlayer);
        });

        Scene scene = new Scene(grid, 600, 300);
        primaryStage.setScene(scene);
    }

    /**
     * Statistics screen with top players and game history.
     */
    private void showStatistics() {
        TabPane tabPane = new TabPane();

        Tab playersTab = new Tab("Nejlepší hráči");
        playersTab.setClosable(false);
        VBox playersBox = new VBox(10);
        playersBox.setPadding(new Insets(10));
        Label playersTitle = new Label("TOP 10 HRÁČŮ");
        playersTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        TextArea playersArea = new TextArea();
        playersArea.setEditable(false);
        playersArea.setPrefHeight(400);
        List<Player> topPlayers = dataManager.getTopPlayers(10);
        StringBuilder playersSb = new StringBuilder();
        int rank = 1;
        for (Player player : topPlayers) {
            playersSb.append(String.format("%d. %s%n", rank++, player.toString()));
        }
        playersArea.setText(playersSb.toString());
        playersBox.getChildren().addAll(playersTitle, playersArea);
        playersTab.setContent(playersBox);

        // all games
        Tab allGamesTab = new Tab("Všechny hry");
        allGamesTab.setClosable(false);
        VBox allGamesBox = new VBox(10);
        allGamesBox.setPadding(new Insets(10));
        Label allGamesTitle = new Label("HISTORIE VŠECH HER");
        allGamesTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        TextArea allGamesArea = new TextArea();
        allGamesArea.setEditable(false);
        allGamesArea.setPrefHeight(400);
        List<GameResult> allGames = dataManager.getAllResults();
        StringBuilder allGamesSb = new StringBuilder();
        for (GameResult result : allGames) {
            allGamesSb.append(result.toString()).append("\n");
        }
        allGamesArea.setText(allGamesSb.toString());
        allGamesBox.getChildren().addAll(allGamesTitle, allGamesArea);
        allGamesTab.setContent(allGamesBox);

        tabPane.getTabs().addAll(playersTab, allGamesTab);

        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(10));

        Button backButton = new Button("Zpět do menu");
        backButton.setOnAction(e -> showMainMenu());

        mainBox.getChildren().addAll(tabPane, backButton);

        Scene scene = new Scene(mainBox, 800, 600);
        primaryStage.setScene(scene);
    }

    /**
     * Starts a new game with the given players.
     */
    private void startGame(Player whitePlayer, Player blackPlayer) {
        Label infoLabel = new Label();
        infoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 10;");

        Board board = new Board(800, 800, whitePlayer, blackPlayer, dataManager, infoLabel);

        VBox gameBox = new VBox(10);
        gameBox.setPadding(new Insets(10));
        gameBox.setAlignment(Pos.CENTER); // Zarovnání na střed

        Button backButton = new Button("Zpět do menu");
        backButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ukončit hru");
            alert.setHeaderText("Opravdu chcete ukončit hru?");
            alert.setContentText("Hra nebude uložena.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    showMainMenu();
                }
            });
        });

        // above game
        gameBox.getChildren().addAll(infoLabel, board, backButton);

        Scene scene = new Scene(gameBox, 850, 950);
        primaryStage.setScene(scene);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
