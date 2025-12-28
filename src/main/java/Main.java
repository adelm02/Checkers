/*
 * Main
 *
 * Version 1.1
 *
 * 2025 Checkers Project
 */
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;

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

        primaryStage.setTitle("Checkers");
        primaryStage.show();
    }

    /**
     * Shows the main menu with game options.
     */
    private void showMainMenu() {
        VBox menuBox = new VBox(20);
        menuBox.setPadding(new Insets(40));
        menuBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Checkers");
        titleLabel.getStyleClass().add("title-label");

        Button newGameButton = new Button("Nová hra");
        newGameButton.getStyleClass().add("button-action");
        newGameButton.setOnAction(e -> {
            showLoginScreen();
        });

        Button statsButton = new Button("Statistiky");
        statsButton.getStyleClass().add("button");
        statsButton.setOnAction(e -> {
            showStatistics();
        });

        Button exitButton = new Button("Konec");
        exitButton.getStyleClass().add("button-cancel");
        exitButton.setOnAction(e -> {
            primaryStage.close();
        });

        menuBox.getChildren().addAll(titleLabel, newGameButton, statsButton, exitButton);

        Scene scene = new Scene(menuBox, 500, 500);
        loadStyles(scene);

        primaryStage.setScene(scene);
    }

    /**
     * Login screen where players enter their names.
     */
    private void showLoginScreen() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Přihlášení hráčů");
        titleLabel.getStyleClass().add("subtitle-label");

        HBox playersBox = new HBox(20);
        playersBox.setAlignment(Pos.CENTER);

        // whitw player
        VBox whiteBox = new VBox(5);
        Label whiteLabel = new Label("Bílý hráč:");
        TextField whiteNameField = new TextField();
        whiteNameField.setPromptText("Jméno");
        whiteBox.getChildren().addAll(whiteLabel, whiteNameField);

        // black player
        VBox blackBox = new VBox(5);
        Label blackLabel = new Label("Černý hráč:");
        TextField blackNameField = new TextField();
        blackNameField.setPromptText("Jméno");
        blackBox.getChildren().addAll(blackLabel, blackNameField);

        playersBox.getChildren().addAll(whiteBox, blackBox);

        Button loginButton = new Button("Přihlásit a hrát");
        loginButton.getStyleClass().add("button-action");

        Button backButton = new Button("Zpět");
        backButton.getStyleClass().add("button-cancel");
        backButton.setOnAction(e -> {
            showMainMenu();
        });

        root.getChildren().addAll(titleLabel, playersBox, loginButton, backButton);

        // login
        loginButton.setOnAction(e -> {
            // "," for csv
            String whiteName = "";
            if (whiteNameField.getText() != null) {
                whiteName = whiteNameField.getText().replace(",", "").trim();
            }

            String blackName = "";
            if (blackNameField.getText() != null) {
                blackName = blackNameField.getText().replace(",", "").trim();
            }

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

        Scene scene = new Scene(root, 600, 350);
        loadStyles(scene);

        primaryStage.setScene(scene);
    }

    /**
     * Statistics screen using TableView.
     */
    private void showStatistics() {
        TabPane tabPane = new TabPane();

        Tab playersTab = new Tab("Nejlepší hráči");
        playersTab.setClosable(false);

        VBox playersBox = new VBox(10);
        playersBox.setPadding(new Insets(15));

        Label playersTitle = new Label("Žebříček hráčů");
        playersTitle.getStyleClass().add("subtitle-label");

        TableView<Player> playersTable = new TableView<>();
        playersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Player, String> nameCol = new TableColumn<>("Jméno");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Player, Integer> winsCol = new TableColumn<>("Výhry");
        winsCol.setCellValueFactory(new PropertyValueFactory<>("gamesWon"));
        winsCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<Player, String> rateCol = new TableColumn<>("Úspěšnost");
        rateCol.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1f %%", c.getValue().getWinRate())
        ));
        rateCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        playersTable.getColumns().addAll(nameCol, winsCol, rateCol);
        playersTable.getItems().addAll(dataManager.getTopPlayers(10));

        playersBox.getChildren().addAll(playersTitle, playersTable);
        playersTab.setContent(playersBox);

        Tab allGamesTab = new Tab("Historie");
        allGamesTab.setClosable(false);

        VBox allGamesBox = new VBox(10);
        allGamesBox.setPadding(new Insets(15));

        Label allGamesTitle = new Label("Odehrané hry");
        allGamesTitle.getStyleClass().add("subtitle-label");

        TableView<GameResult> gamesTable = new TableView<>();
        gamesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<GameResult, String> whiteCol = new TableColumn<>("Bílý");
        whiteCol.setCellValueFactory(new PropertyValueFactory<>("whitePlayerName"));

        TableColumn<GameResult, String> blackCol = new TableColumn<>("Černý");
        blackCol.setCellValueFactory(new PropertyValueFactory<>("blackPlayerName"));

        TableColumn<GameResult, String> winnerCol = new TableColumn<>("Vítěz");
        winnerCol.setCellValueFactory(new PropertyValueFactory<>("winner"));

        TableColumn<GameResult, String> timeCol = new TableColumn<>("Čas");
        timeCol.setCellValueFactory(c -> {
            long s = c.getValue().getGameDurationSeconds();
            return new SimpleStringProperty(String.format("%d:%02d", s / 60, s % 60));
        });
        timeCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        gamesTable.getColumns().addAll(whiteCol, blackCol, winnerCol, timeCol);
        gamesTable.getItems().addAll(dataManager.getAllResults());

        allGamesBox.getChildren().addAll(allGamesTitle, gamesTable);
        allGamesTab.setContent(allGamesBox);
        //completing
        tabPane.getTabs().addAll(playersTab, allGamesTab);

        VBox mainBox = new VBox(15);
        mainBox.setPadding(new Insets(20));
        mainBox.setAlignment(Pos.CENTER);

        Button backButton = new Button("Zpět do menu");
        backButton.getStyleClass().add("button-cancel");
        backButton.setOnAction(e -> {
            showMainMenu();
        });

        mainBox.getChildren().addAll(tabPane, backButton);

        Scene scene = new Scene(mainBox, 800, 600);
        loadStyles(scene);

        primaryStage.setScene(scene);
    }

    private void startGame(Player whitePlayer, Player blackPlayer) {
        Label infoLabel = new Label();
        infoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Board board = new Board(800, 800, whitePlayer, blackPlayer, dataManager, infoLabel);

        VBox gameBox = new VBox(10);
        gameBox.setPadding(new Insets(10));
        gameBox.setAlignment(Pos.CENTER);

        Button backButton = new Button("Ukončit hru");
        backButton.getStyleClass().add("button-cancel");
        backButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Ukončit");
            alert.setHeaderText("Opravdu chcete ukončit hru?");
            alert.setContentText("Hra nebude uložena.");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    showMainMenu();
                }
            });
        });

        gameBox.getChildren().addAll(infoLabel, board, backButton);

        Scene scene = new Scene(gameBox, 850, 950);
        loadStyles(scene);
        primaryStage.setScene(scene);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Helper to load CSS
     */
    private void loadStyles(Scene scene) {
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception ex) {
            System.err.println("Nepodařilo se načíst styles.css");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}