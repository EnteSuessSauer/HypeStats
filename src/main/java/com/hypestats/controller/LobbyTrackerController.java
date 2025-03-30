package com.hypestats.controller;

import com.hypestats.model.LobbyPlayer;
import com.hypestats.model.LobbyTracker;
import com.hypestats.model.MinecraftLog;
import com.hypestats.model.PlayerStats;
import com.hypestats.model.BedwarsStats;
import com.hypestats.util.HypixelApiService;
import com.hypestats.util.MinecraftLogWatcher;
import com.hypestats.util.MinecraftClientLogFinder;
import com.hypestats.util.MinecraftClientLogFinder.LogFileInfo;
import com.hypestats.util.PlayerAuthenticityChecker;
import com.hypestats.util.SettingsManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.Optional;

/**
 * Controller for the lobby tracker view
 */
@Slf4j
public class LobbyTrackerController {
    @FXML
    private TextField logPathField;
    
    @FXML
    private Button browseButton;
    
    @FXML
    private Button defaultPathButton;
    
    @FXML
    private Button autoDetectButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Button startButton;
    
    @FXML
    private Button stopButton;
    
    @FXML
    private TextArea logOutputArea;
    
    @FXML
    private Button clearLogButton;
    
    @FXML
    private Button clearPlayersButton;
    
    @FXML
    private TableView<LobbyPlayer> playerTable;
    
    @FXML
    private TableColumn<LobbyPlayer, String> usernameColumn;
    
    @FXML
    private TableColumn<LobbyPlayer, String> levelColumn;
    
    @FXML
    private TableColumn<LobbyPlayer, String> winsColumn;
    
    @FXML
    private TableColumn<LobbyPlayer, String> wlRatioColumn;
    
    @FXML
    private TableColumn<LobbyPlayer, String> kdRatioColumn;
    
    @FXML
    private TableColumn<LobbyPlayer, String> finalKdRatioColumn;
    
    @FXML
    private TableColumn<LobbyPlayer, String> statusColumn;
    
    @FXML
    private TableColumn<LobbyPlayer, String> authenticityColumn;
    
    @FXML
    private Label playerCountLabel;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label currentLobbyLabel;
    
    @FXML
    private Label currentClientLabel;
    
    private SettingsManager settingsManager;
    private HypixelApiService apiService;
    private MinecraftLogWatcher logWatcher;
    private PlayerAuthenticityChecker authenticityChecker;
    
    private ObservableList<LobbyPlayer> players = FXCollections.observableArrayList();
    private Map<String, LobbyPlayer> playerMap = new HashMap<>();
    
    private String currentClientName = "Unknown";
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        settingsManager = SettingsManager.getInstance();
        apiService = new HypixelApiService();
        authenticityChecker = new PlayerAuthenticityChecker();
        
        // Setup table columns
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        levelColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            return new SimpleStringProperty(stats != null ? stats.getFormattedLevel() : "");
        });
        winsColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            if (stats == null) return new SimpleStringProperty("");
            BedwarsStats bedwarsStats = stats.getBedwarsStats();
            return new SimpleStringProperty(bedwarsStats != null ? String.valueOf(bedwarsStats.getWins()) : "");
        });
        wlRatioColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            if (stats == null) return new SimpleStringProperty("");
            BedwarsStats bedwarsStats = stats.getBedwarsStats();
            return new SimpleStringProperty(bedwarsStats != null ? bedwarsStats.getFormattedWLRatio() : "");
        });
        kdRatioColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            if (stats == null) return new SimpleStringProperty("");
            BedwarsStats bedwarsStats = stats.getBedwarsStats();
            return new SimpleStringProperty(bedwarsStats != null ? bedwarsStats.getFormattedKDRatio() : "");
        });
        finalKdRatioColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            if (stats == null) return new SimpleStringProperty("");
            BedwarsStats bedwarsStats = stats.getBedwarsStats();
            return new SimpleStringProperty(bedwarsStats != null ? bedwarsStats.getFormattedFinalKDRatio() : "");
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        authenticityColumn.setCellValueFactory(new PropertyValueFactory<>("authenticityDisplay"));
        
        // Add custom cell factory for authenticity column to apply styles
        authenticityColumn.setCellFactory(column -> new TableCell<LobbyPlayer, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                
                if (item == null || empty) {
                    setText(null);
                    getStyleClass().removeAll("auth-low", "auth-medium-low", "auth-medium-high", "auth-high");
                    return;
                }
                
                setText(item);
                
                // Apply style based on the player's authenticity
                LobbyPlayer player = getTableView().getItems().get(getIndex());
                getStyleClass().removeAll("auth-low", "auth-medium-low", "auth-medium-high", "auth-high");
                
                if (player.getStats() != null) {
                    double highestProb = Math.max(player.getNickProbability(), player.getAltProbability());
                    getStyleClass().add(authenticityChecker.getProbabilityStyleClass(highestProb));
                }
            }
        });
        
        playerTable.setItems(players);
        
        // Load saved log path
        String savedLogPath = settingsManager.getLogPath();
        if (savedLogPath != null && !savedLogPath.isEmpty()) {
            logPathField.setText(savedLogPath);
            updateClientLabel(getClientNameFromPath(savedLogPath));
        }
        
        // Set up refresh button
        refreshButton.setOnAction(this::refreshLogFile);
        refreshButton.setDisable(true); // Initially disabled until log monitoring starts
        
        updatePlayerCount();
        updateCurrentLobbyLabel("");
    }
    
    /**
     * Auto-detect Minecraft log files
     * @param event ActionEvent
     */
    @FXML
    private void autoDetectLogFile(ActionEvent event) {
        List<LogFileInfo> logFiles = MinecraftClientLogFinder.findAllLogFiles();
        
        if (logFiles.isEmpty()) {
            appendToLogOutput("No Minecraft log files found. Please browse for one manually.");
            return;
        }
        
        if (logFiles.size() == 1) {
            // Just one log file found, use it automatically
            LogFileInfo logFileInfo = logFiles.get(0);
            logPathField.setText(logFileInfo.getFilePath());
            updateClientLabel(logFileInfo.getClientName());
            settingsManager.setLogPath(logFileInfo.getFilePath());
            appendToLogOutput("Auto-detected log file: " + logFileInfo.getFilePath() + " (" + logFileInfo.getClientName() + ")");
        } else {
            // Multiple log files found, show dialog to choose
            LogFileInfo selected = showLogFileSelectionDialog(logFiles);
            if (selected != null) {
                logPathField.setText(selected.getFilePath());
                updateClientLabel(selected.getClientName());
                settingsManager.setLogPath(selected.getFilePath());
                appendToLogOutput("Selected log file: " + selected.getFilePath() + " (" + selected.getClientName() + ")");
            }
        }
    }
    
    /**
     * Show a dialog for the user to select from multiple log files
     * @param logFiles List of detected log files
     * @return Selected LogFileInfo or null if canceled
     */
    private LogFileInfo showLogFileSelectionDialog(List<LogFileInfo> logFiles) {
        // Create the custom dialog
        Dialog<LogFileInfo> dialog = new Dialog<>();
        dialog.setTitle("Select Minecraft Log File");
        dialog.setHeaderText("Multiple Minecraft log files detected.\nPlease select the one you want to use:");
        
        // Set the button types
        ButtonType selectButtonType = new ButtonType("Select", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectButtonType, ButtonType.CANCEL);
        
        // Create the log file list view
        ListView<LogFileInfo> listView = new ListView<>();
        ObservableList<LogFileInfo> items = FXCollections.observableArrayList(logFiles);
        listView.setItems(items);
        
        // Custom cell factory to show client name, path, and last modified time
        listView.setCellFactory(new Callback<ListView<LogFileInfo>, ListCell<LogFileInfo>>() {
            @Override
            public ListCell<LogFileInfo> call(ListView<LogFileInfo> param) {
                return new ListCell<LogFileInfo>() {
                    @Override
                    protected void updateItem(LogFileInfo item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            VBox vbox = new VBox(2);
                            
                            Label nameLabel = new Label(item.getClientName());
                            nameLabel.setStyle("-fx-font-weight: bold;");
                            
                            Label pathLabel = new Label(item.getFilePath());
                            pathLabel.setStyle("-fx-font-size: 0.9em;");
                            
                            // Format the last modified date
                            LocalDateTime lastModified = LocalDateTime.ofInstant(
                                    Instant.ofEpochMilli(item.getLastModified()), 
                                    ZoneId.systemDefault());
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            
                            Label timeLabel = new Label("Last modified: " + lastModified.format(formatter));
                            timeLabel.setStyle("-fx-font-size: 0.9em; -fx-font-style: italic;");
                            
                            vbox.getChildren().addAll(nameLabel, pathLabel, timeLabel);
                            setGraphic(vbox);
                        }
                    }
                };
            }
        });
        
        // Set the list view as the dialog content
        dialog.getDialogPane().setContent(listView);
        
        // Enable/Disable Select button depending on selection
        Node selectButton = dialog.getDialogPane().lookupButton(selectButtonType);
        selectButton.setDisable(true);
        
        listView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectButton.setDisable(newValue == null);
        });
        
        // Set the result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == selectButtonType) {
                return listView.getSelectionModel().getSelectedItem();
            }
            return null;
        });
        
        // Show the dialog and process the result
        Optional<LogFileInfo> result = dialog.showAndWait();
        return result.orElse(null);
    }
    
    /**
     * Browse for the Minecraft log file
     * @param event ActionEvent
     */
    @FXML
    private void browseLogFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Minecraft Log File");
        
        // Set initial directory if possible
        String currentPath = logPathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.exists()) {
                fileChooser.setInitialDirectory(currentFile.getParentFile());
            }
        }
        
        // Set file extension filter
        FileChooser.ExtensionFilter logFilter = new FileChooser.ExtensionFilter("Log Files", "*.log");
        fileChooser.getExtensionFilters().add(logFilter);
        
        // Show dialog
        File selectedFile = fileChooser.showOpenDialog(logPathField.getScene().getWindow());
        if (selectedFile != null) {
            logPathField.setText(selectedFile.getAbsolutePath());
            updateClientLabel(getClientNameFromPath(selectedFile.getAbsolutePath()));
            settingsManager.setLogPath(selectedFile.getAbsolutePath());
        }
    }
    
    /**
     * Use the most recent log file
     * @param event ActionEvent
     */
    @FXML
    private void useDefaultPath(ActionEvent event) {
        LogFileInfo mostRecent = MinecraftClientLogFinder.findMostRecentLogFile();
        if (mostRecent != null) {
            logPathField.setText(mostRecent.getFilePath());
            updateClientLabel(mostRecent.getClientName());
            settingsManager.setLogPath(mostRecent.getFilePath());
            appendToLogOutput("Using most recent log file: " + mostRecent.getFilePath() + " (" + mostRecent.getClientName() + ")");
        } else {
            // Fall back to default if no log files found
            String defaultPath = SettingsManager.getDefaultLogPath();
            logPathField.setText(defaultPath);
            updateClientLabel("Minecraft");
            settingsManager.setLogPath(defaultPath);
            appendToLogOutput("Using default log file path: " + defaultPath);
        }
    }
    
    /**
     * Attempt to determine the client name from a log file path
     * @param path The log file path
     * @return The client name or "Unknown"
     */
    private String getClientNameFromPath(String path) {
        if (path == null || path.isEmpty()) {
            return "Unknown";
        }
        
        if (path.contains("blclient")) {
            return "BadLion";
        } else if (path.contains("BadlionClient")) {
            return "BadLion Legacy";
        } else if (path.contains("lunarclient") || path.contains("lunar")) {
            return "Lunar";
        } else if (path.contains("fml-client")) {
            return "Forge";
        } else if (path.contains(".feather")) {
            return "Feather";
        } else if (path.contains("labymod")) {
            return "LabyMod";
        } else if (path.contains(".minecraft")) {
            return "Minecraft";
        }
        
        return "Unknown";
    }
    
    /**
     * Update the current client label
     * @param clientName The client name
     */
    private void updateClientLabel(String clientName) {
        this.currentClientName = clientName;
        
        if (currentClientLabel != null) {
            currentClientLabel.setText("Client: " + clientName);
        }
    }
    
    /**
     * Start monitoring the Minecraft log file
     * @param event ActionEvent
     */
    @FXML
    private void startMonitoring(ActionEvent event) {
        String logPath = logPathField.getText();
        if (logPath == null || logPath.isEmpty()) {
            appendToLogOutput("ERROR: No log file path specified");
            return;
        }
        
        // Verify the log file exists
        if (!MinecraftClientLogFinder.logFileExists(logPath)) {
            appendToLogOutput("ERROR: Log file does not exist or is not readable: " + logPath);
            
            // Prompt user to auto-detect
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Log File Not Found");
            alert.setHeaderText("The specified log file was not found:");
            alert.setContentText(logPath + "\n\nWould you like to auto-detect Minecraft log files?");
            
            ButtonType autoDetectButton = new ButtonType("Auto-Detect");
            ButtonType browseButton = new ButtonType("Browse Manually");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            
            alert.getButtonTypes().setAll(autoDetectButton, browseButton, cancelButton);
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent()) {
                if (result.get() == autoDetectButton) {
                    autoDetectLogFile(null);
                    
                    // If we have a valid path now, proceed
                    if (MinecraftClientLogFinder.logFileExists(logPathField.getText())) {
                        logPath = logPathField.getText();
                    } else {
                        return;
                    }
                } else if (result.get() == browseButton) {
                    browseLogFile(null);
                    
                    // If we have a valid path now, proceed
                    if (MinecraftClientLogFinder.logFileExists(logPathField.getText())) {
                        logPath = logPathField.getText();
                    } else {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        
        // Check if API key is set
        String apiKey = settingsManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            appendToLogOutput("ERROR: No Hypixel API key set. Please set your API key in Settings first");
            return;
        }
        
        // Initialize log watcher
        if (logWatcher != null && logWatcher.isRunning()) {
            logWatcher.stop();
        }
        
        logWatcher = new MinecraftLogWatcher(logPath);
        
        // Add player listener (legacy support)
        logWatcher.addPlayerListener(new Consumer<String>() {
            @Override
            public void accept(String playerName) {
                Platform.runLater(() -> {
                    addPlayerToList(playerName);
                });
            }
        });
        
        // Add log listener
        logWatcher.addLogListener(new Consumer<MinecraftLog>() {
            @Override
            public void accept(MinecraftLog log) {
                Platform.runLater(() -> {
                    appendToLogOutput(formatLogEntry(log));
                });
            }
        });
        
        // Add lobby event listener
        logWatcher.addLobbyListener(new LobbyTracker.LobbyEventListener() {
            @Override
            public void onLobbyChange(String oldLobby, String newLobby) {
                Platform.runLater(() -> {
                    appendToLogOutput("Lobby changed from " + oldLobby + " to " + newLobby);
                    clearPlayers(null);
                    updateCurrentLobbyLabel(newLobby);
                });
            }
            
            @Override
            public void onPlayerJoin(String playerName) {
                Platform.runLater(() -> {
                    addPlayerToList(playerName);
                });
            }
            
            @Override
            public void onPlayerQuit(String playerName) {
                Platform.runLater(() -> {
                    removePlayerFromList(playerName);
                });
            }
            
            @Override
            public void onPlayerEliminated(String playerName) {
                Platform.runLater(() -> {
                    appendToLogOutput("Player eliminated: " + playerName);
                    LobbyPlayer player = playerMap.get(playerName);
                    if (player != null) {
                        player.setError("Eliminated");
                        playerTable.refresh();
                    }
                });
            }
            
            @Override
            public void onPlayerListUpdate(List<String> players) {
                Platform.runLater(() -> {
                    appendToLogOutput("Player list updated: " + String.join(", ", players));
                    updateAllPlayers(players);
                });
            }
            
            @Override
            public void onGameStart() {
                Platform.runLater(() -> {
                    appendToLogOutput("Game started - Fetching player stats");
                    
                    // Now that the game has started, fetch stats for all players with rate limiting
                    fetchStatsForAllPlayers();
                });
            }
            
            @Override
            public void onTeamEliminated() {
                Platform.runLater(() -> {
                    appendToLogOutput("A team has been eliminated");
                });
            }
            
            @Override
            public void onUserEliminated() {
                Platform.runLater(() -> {
                    appendToLogOutput("You have been eliminated!");
                });
            }
            
            @Override
            public void onReturnToLobby() {
                Platform.runLater(() -> {
                    appendToLogOutput("Returned to main lobby");
                    
                    // Clear error states on players
                    for (LobbyPlayer player : players) {
                        if ("Not in game - stats load on demand".equals(player.getError())) {
                            player.setError(null);
                        }
                    }
                    playerTable.refresh();
                });
            }
            
            @Override
            public void onQueueJoin(String gameType) {
                Platform.runLater(() -> {
                    appendToLogOutput("Joined queue for " + gameType);
                });
            }
            
            @Override
            public void onGameModeDetected(String gameMode) {
                Platform.runLater(() -> {
                    appendToLogOutput("Game mode detected: " + gameMode);
                    
                    // Fetch player stats now that we're in a specific game mode
                    fetchStatsForAllPlayers();
                });
            }
            
            @Override
            public void onTeamAssignment(String team) {
                Platform.runLater(() -> {
                    appendToLogOutput("Assigned to " + team + " team");
                });
            }
            
            @Override
            public void onGameCountdown(int seconds) {
                Platform.runLater(() -> {
                    appendToLogOutput("Game starting in " + seconds + " seconds");
                });
            }
            
            @Override
            public void onGameEnd() {
                Platform.runLater(() -> {
                    appendToLogOutput("Game has ended");
                });
            }
            
            @Override
            public void onBedDestruction(String teamColor, String destroyer, int bedNumber) {
                Platform.runLater(() -> {
                    appendToLogOutput(teamColor + " team's bed was destroyed by " + destroyer + " (Bed #" + bedNumber + ")");
                });
            }
            
            @Override
            public void onBedDestroyed() {
                Platform.runLater(() -> {
                    appendToLogOutput("Your bed was destroyed - you are now in spectator mode");
                });
            }
            
            @Override
            public void onRespawnCountdown(int seconds) {
                Platform.runLater(() -> {
                    appendToLogOutput("Respawning in " + seconds + " seconds");
                });
            }
            
            @Override
            public void onRespawned() {
                Platform.runLater(() -> {
                    appendToLogOutput("You have respawned");
                });
            }
            
            @Override
            public void onPlayerFinalKill(String victim, String killer, int killCount) {
                Platform.runLater(() -> {
                    appendToLogOutput(victim + " was " + killer + "'s final kill #" + killCount);
                });
            }
            
            @Override
            public void onItemPurchase(String item, String currency) {
                Platform.runLater(() -> {
                    appendToLogOutput("Purchased " + item + " with " + currency);
                });
            }
            
            @Override
            public void onUpgradePurchase(String player, String upgrade) {
                Platform.runLater(() -> {
                    appendToLogOutput(player + " purchased " + upgrade);
                });
            }
            
            @Override
            public void onExperienceGain(int amount, String type, String reason) {
                Platform.runLater(() -> {
                    appendToLogOutput("+" + amount + " " + type + " XP (" + reason + ")");
                });
            }
            
            @Override
            public void onTokensGain(int amount, String reason) {
                Platform.runLater(() -> {
                    appendToLogOutput("+" + amount + " tokens (" + reason + ")");
                });
            }
            
            @Override
            public void onResourceGain(int amount, String resource) {
                Platform.runLater(() -> {
                    appendToLogOutput("+" + amount + " " + resource);
                });
            }
            
            @Override
            public void onTicketGain(int amount, String type, String reason) {
                Platform.runLater(() -> {
                    appendToLogOutput("+" + amount + " " + type + " Tickets (" + reason + ")");
                });
            }
            
            @Override
            public void onItemDeposit(String item) {
                Platform.runLater(() -> {
                    appendToLogOutput("Deposited " + item + " into team chest");
                });
            }
            
            @Override
            public void onPlayerDeath(String player, String deathType, String killer) {
                Platform.runLater(() -> {
                    if (killer != null) {
                        appendToLogOutput(player + " was " + deathType + " by " + killer);
                    } else {
                        appendToLogOutput(player + " " + deathType);
                    }
                });
            }
            
            @Override
            public void onCrossTeamingWarning() {
                Platform.runLater(() -> {
                    appendToLogOutput("Cross-teaming warning displayed - report cross-teamers using /report");
                });
            }
            
            @Override
            public void onTeamChat(String team, String level, String message) {
                Platform.runLater(() -> {
                    appendToLogOutput("[" + team + "] [" + level + "]: " + message);
                });
            }
            
            @Override
            public void onServerConnection(String server, int port) {
                Platform.runLater(() -> {
                    appendToLogOutput("Connecting to server: " + server + ":" + port);
                    
                    // Update the current client if connecting to Hypixel
                    if (server.contains("hypixel")) {
                        updateClientLabel(currentClientName + " (Hypixel)");
                    }
                });
            }
            
            @Override
            public void onPlayerReconnect(String player) {
                Platform.runLater(() -> {
                    appendToLogOutput(player + " has reconnected to the game");
                });
            }
            
            @Override
            public void onReconnect(String destination) {
                Platform.runLater(() -> {
                    appendToLogOutput("Reconnecting to " + destination);
                    
                    // This is a definite game join, fetch stats
                    fetchStatsForAllPlayers();
                });
            }
            
            @Override
            public void onPartyWarp(String warper, String destination) {
                Platform.runLater(() -> {
                    appendToLogOutput(warper + " warped the party to " + destination);
                    
                    // Clear existing player data since we're joining a new game
                    clearPlayers(null);
                    
                    // Note: We'll wait for player join events before fetching stats
                });
            }
        });
        
        // Start the watcher
        boolean success = logWatcher.start();
        if (success) {
            // Update UI
            startButton.setDisable(true);
            stopButton.setDisable(false);
            refreshButton.setDisable(false);
            
            statusLabel.setText("Running");
            statusLabel.getStyleClass().remove("status-stopped");
            statusLabel.getStyleClass().add("status-running");
            
            appendToLogOutput("Started monitoring log file: " + logPath);
        } else {
            logWatcher = null;
            appendToLogOutput("ERROR: Failed to start monitoring log file");
        }
    }
    
    /**
     * Stop monitoring the Minecraft log file
     * @param event ActionEvent
     */
    @FXML
    private void stopMonitoring(ActionEvent event) {
        if (logWatcher != null) {
            logWatcher.stop();
            logWatcher = null;
            
            appendToLogOutput("Stopped monitoring log file");
            
            // Update UI
            startButton.setDisable(false);
            stopButton.setDisable(true);
            refreshButton.setDisable(true);
            
            statusLabel.setText("Stopped");
            statusLabel.getStyleClass().remove("status-running");
            statusLabel.getStyleClass().add("status-stopped");
        }
    }
    
    /**
     * Clear the log output area
     * @param event ActionEvent
     */
    @FXML
    private void clearLog(ActionEvent event) {
        logOutputArea.clear();
    }
    
    /**
     * Clear the player list
     * @param event ActionEvent
     */
    @FXML
    private void clearPlayers(ActionEvent event) {
        players.clear();
        playerMap.clear();
        updatePlayerCount();
        
        if (logWatcher != null) {
            logWatcher.getLobbyTracker().clearPlayers();
        }
        
        appendToLogOutput("Player list cleared");
    }
    
    /**
     * Refresh player stats
     * @param event ActionEvent
     */
    @FXML
    private void refreshStats(ActionEvent event) {
        String apiKey = settingsManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            appendToLogOutput("ERROR: No Hypixel API key set. Please set your API key in Settings first");
            return;
        }
        
        // Check rate limit before attempting to refresh all players
        int playerCount = players.size();
        int availableRequests = apiService.getRemainingRequests();
        
        if (playerCount > availableRequests) {
            appendToLogOutput("WARNING: Not enough API requests available. " +
                "Need " + playerCount + " but only have " + availableRequests + ". " +
                "Only refreshing " + availableRequests + " players.");
            
            // Only refresh as many players as we have requests for
            int count = 0;
            for (LobbyPlayer player : players) {
                if (count >= availableRequests) break;
                
                player.setLoading(true);
                player.setError(null);
                fetchPlayerStats(player, apiKey);
                count++;
            }
        } else {
            // We have enough requests for all players
            for (LobbyPlayer player : players) {
                player.setLoading(true);
                player.setError(null);
                fetchPlayerStats(player, apiKey);
            }
        }
        
        playerTable.refresh();
    }
    
    /**
     * Update the current lobby label
     * @param lobbyId The current lobby ID
     */
    private void updateCurrentLobbyLabel(String lobbyId) {
        if (lobbyId == null || lobbyId.isEmpty()) {
            currentLobbyLabel.setText("Not connected");
        } else {
            currentLobbyLabel.setText("Lobby: " + lobbyId);
        }
    }
    
    /**
     * Remove a player from the list
     * @param username The player's username
     */
    private void removePlayerFromList(String username) {
        LobbyPlayer player = playerMap.get(username);
        if (player != null) {
            players.remove(player);
            playerMap.remove(username);
            updatePlayerCount();
        }
    }
    
    /**
     * Update the player list with a new set of players
     * @param playerNames List of player names
     */
    private void updateAllPlayers(List<String> playerNames) {
        // Remove players that are not in the new list
        playerMap.keySet().retainAll(playerNames);
        players.removeIf(player -> !playerNames.contains(player.getUsername()));
        
        // Add new players
        for (String playerName : playerNames) {
            if (!playerMap.containsKey(playerName)) {
                addPlayerToList(playerName);
            }
        }
        
        updatePlayerCount();
    }
    
    /**
     * Add a player to the list
     * @param username The player's username
     */
    private void addPlayerToList(String username) {
        // Skip if player is already in the list
        if (playerMap.containsKey(username)) {
            return;
        }
        
        LobbyPlayer player = new LobbyPlayer(username);
        players.add(player);
        playerMap.put(username, player);
        updatePlayerCount();
        
        // We'll only fetch stats if we're in a game session, not in the lobby
        // This will be controlled by the LobbyTracker
        boolean shouldFetchStats = logWatcher != null && 
                                   logWatcher.getLobbyTracker() != null && 
                                   logWatcher.getLobbyTracker().isInGame();
        
        String apiKey = settingsManager.getApiKey();
        if (shouldFetchStats && apiKey != null && !apiKey.isEmpty()) {
            // Before making the API request, check if we're within rate limits
            if (apiService.canMakeRequests()) {
                fetchPlayerStats(player, apiKey);
            } else {
                int remainingRequests = apiService.getRemainingRequests();
                player.setLoading(false);
                player.setError("Rate limit reached: " + remainingRequests + " remaining. Stats will load later.");
                
                // Log the rate limit issue
                appendToLogOutput("API rate limit reached (" + 
                    apiService.getCurrentRequestCount() + " requests). " +
                    "Will try again when limit resets.");
            }
        } else if (!shouldFetchStats) {
            player.setLoading(false);
            player.setError("Not in game - stats load on demand");
        } else if (apiKey == null || apiKey.isEmpty()) {
            player.setLoading(false);
            player.setError("No API key set");
        }
    }
    
    /**
     * Fetch player stats asynchronously
     * @param player LobbyPlayer object
     * @param apiKey Hypixel API key
     */
    private void fetchPlayerStats(LobbyPlayer player, String apiKey) {
        player.setLoading(true);
        player.setError(null);
        
        Task<PlayerStats> task = new Task<PlayerStats>() {
            @Override
            protected PlayerStats call() throws Exception {
                return apiService.getPlayerStats(player.getUsername(), apiKey);
            }
            
            @Override
            protected void succeeded() {
                PlayerStats stats = getValue();
                player.setStats(stats);
                player.setUuid(stats.getUuid());
                player.setLoading(false);
                
                // Calculate authenticity probabilities
                double nickProb = authenticityChecker.calculateNickProbability(stats);
                double altProb = authenticityChecker.calculateAltProbability(stats);
                
                player.setNickProbability(nickProb);
                player.setAltProbability(altProb);
                player.setNickProbabilityText(authenticityChecker.getNickProbabilityText(nickProb));
                player.setAltProbabilityText(authenticityChecker.getAltProbabilityText(altProb));
                player.setAuthenticityStyleClass(authenticityChecker.getProbabilityStyleClass(
                        Math.max(nickProb, altProb)));
                
                playerTable.refresh();
            }
            
            @Override
            protected void failed() {
                player.setLoading(false);
                Throwable exception = getException();
                if (exception != null) {
                    String errorMsg = exception.getMessage();
                    player.setError(errorMsg != null ? errorMsg : "Unknown error");
                    
                    // Log API errors to help diagnose issues
                    appendToLogOutput("API error for " + player.getUsername() + ": " + 
                        (errorMsg != null ? errorMsg : "Unknown error"));
                } else {
                    player.setError("Failed to fetch stats");
                }
                playerTable.refresh();
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Format a log entry for display
     * @param log MinecraftLog object
     * @return Formatted string
     */
    private String formatLogEntry(MinecraftLog log) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timestamp = log.getTimestamp().format(formatter);
        return String.format("[%s] %s", timestamp, log.getMessage());
    }
    
    /**
     * Append a message to the log output area
     * @param message Message to append
     */
    private void appendToLogOutput(String message) {
        // Add timestamp to message
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String formattedMessage = String.format("[%s] %s", timestamp, message);
        
        logOutputArea.appendText(formattedMessage + "\n");
        
        // Auto-scroll to bottom
        logOutputArea.setScrollTop(Double.MAX_VALUE);
    }
    
    /**
     * Update the player count label
     */
    private void updatePlayerCount() {
        playerCountLabel.setText("Players: " + players.size());
    }
    
    /**
     * Manually refresh the log file
     * @param event ActionEvent
     */
    @FXML
    public void refreshLogFile(ActionEvent event) {
        if (logWatcher != null && logWatcher.isRunning()) {
            appendToLogOutput("Manually refreshing log file...");
            
            // Run the refresh in a background thread to avoid UI freezing
            Task<Boolean> refreshTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return logWatcher.forceRefresh();
                }
                
                @Override
                protected void succeeded() {
                    Boolean result = getValue();
                    Platform.runLater(() -> {
                        if (Boolean.TRUE.equals(result)) {
                            appendToLogOutput("Log file refreshed successfully");
                        } else {
                            appendToLogOutput("Failed to refresh log file");
                        }
                    });
                }
                
                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        appendToLogOutput("Error refreshing log file: " + getException().getMessage());
                    });
                }
            };
            
            Thread refreshThread = new Thread(refreshTask);
            refreshThread.setDaemon(true);
            refreshThread.start();
        } else {
            appendToLogOutput("Log watcher is not running");
        }
    }
    
    /**
     * Fetch stats for all players in the current lobby with rate limiting
     */
    private void fetchStatsForAllPlayers() {
        String apiKey = settingsManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            appendToLogOutput("ERROR: No Hypixel API key set. Please set your API key in Settings first");
            return;
        }
        
        // Don't fetch if we're not in a game
        boolean inGame = logWatcher != null && 
                        logWatcher.getLobbyTracker() != null && 
                        logWatcher.getLobbyTracker().isInGame();
                        
        if (!inGame) {
            appendToLogOutput("Not in a game, skipping player stats fetch");
            return;
        }
        
        // Check available requests
        int availableRequests = apiService.getRemainingRequests();
        int playerCount = players.size();
        
        // Filter to only players that need stats (don't have them yet)
        List<LobbyPlayer> playersNeedingStats = players.stream()
            .filter(p -> p.getStats() == null && p.getError() == null || 
                    "Not in game - stats load on demand".equals(p.getError()))
            .collect(java.util.stream.Collectors.toList());
        
        int needStatCount = playersNeedingStats.size();
        
        if (needStatCount == 0) {
            appendToLogOutput("All players already have stats");
            return;
        }
        
        if (needStatCount > availableRequests) {
            appendToLogOutput("WARNING: Not enough API requests available. " +
                "Need " + needStatCount + " but only have " + availableRequests + ". " +
                "Fetching stats for " + availableRequests + " players only.");
            
            // Prioritize players that are more likely to be relevant
            // This is just a basic algorithm that could be improved
            playersNeedingStats.sort((p1, p2) -> {
                // Players with no error message are higher priority
                if (p1.getError() == null && p2.getError() != null) return -1;
                if (p1.getError() != null && p2.getError() == null) return 1;
                return 0;
            });
            
            // Only take as many as we can handle
            playersNeedingStats = playersNeedingStats.subList(0, Math.min(availableRequests, playersNeedingStats.size()));
        }
        
        appendToLogOutput("Fetching stats for " + playersNeedingStats.size() + " players");
        
        // Process the requests with a slight delay between each to avoid overwhelming the API
        final List<LobbyPlayer> finalList = playersNeedingStats;
        Thread fetchThread = new Thread(() -> {
            try {
                for (int i = 0; i < finalList.size(); i++) {
                    final int index = i;
                    LobbyPlayer player = finalList.get(index);
                    
                    // Update UI in the platform thread
                    Platform.runLater(() -> {
                        player.setLoading(true);
                        player.setError(null);
                        playerTable.refresh();
                    });
                    
                    try {
                        // Fetch the stats
                        final PlayerStats stats = apiService.getPlayerStats(player.getUsername(), apiKey);
                        
                        // Update UI with the result
                        Platform.runLater(() -> {
                            player.setStats(stats);
                            player.setUuid(stats.getUuid());
                            player.setLoading(false);
                            
                            // Calculate authenticity probabilities
                            double nickProb = authenticityChecker.calculateNickProbability(stats);
                            double altProb = authenticityChecker.calculateAltProbability(stats);
                            
                            player.setNickProbability(nickProb);
                            player.setAltProbability(altProb);
                            player.setNickProbabilityText(authenticityChecker.getNickProbabilityText(nickProb));
                            player.setAltProbabilityText(authenticityChecker.getAltProbabilityText(altProb));
                            player.setAuthenticityStyleClass(authenticityChecker.getProbabilityStyleClass(
                                    Math.max(nickProb, altProb)));
                            
                            playerTable.refresh();
                            
                            // Log progress
                            if (index % 5 == 0 || index == finalList.size() - 1) {
                                appendToLogOutput("Fetched stats for " + (index + 1) + "/" + finalList.size() + " players");
                            }
                        });
                        
                        // Small delay to avoid overwhelming the API
                        Thread.sleep(100);
                    } catch (Exception e) {
                        final String errorMessage = e.getMessage();
                        
                        // Update UI with the error
                        Platform.runLater(() -> {
                            player.setLoading(false);
                            player.setError(errorMessage != null ? errorMessage : "Error fetching stats");
                            playerTable.refresh();
                            
                            // Log the error
                            appendToLogOutput("Error fetching stats for " + player.getUsername() + ": " + 
                                (errorMessage != null ? errorMessage : "Unknown error"));
                            
                            // If we hit the rate limit, stop processing
                            if (errorMessage != null && errorMessage.contains("Rate limit")) {
                                appendToLogOutput("Rate limit reached. Stopping further requests.");
                            }
                        });
                        
                        // If we hit the rate limit, break the loop
                        if (errorMessage != null && errorMessage.contains("Rate limit")) {
                            break;
                        }
                        
                        // Longer delay on error
                        Thread.sleep(500);
                    }
                }
            } catch (InterruptedException e) {
                // Thread was interrupted
                Platform.runLater(() -> {
                    appendToLogOutput("Stat fetching was interrupted: " + e.getMessage());
                });
                Thread.currentThread().interrupt();
            }
        });
        
        fetchThread.setDaemon(true);
        fetchThread.start();
    }
} 