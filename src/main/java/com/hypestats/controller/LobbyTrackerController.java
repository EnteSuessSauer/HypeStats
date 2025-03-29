package com.hypestats.controller;

import com.hypestats.model.LobbyPlayer;
import com.hypestats.model.MinecraftLog;
import com.hypestats.model.PlayerStats;
import com.hypestats.util.HypixelApiService;
import com.hypestats.util.MinecraftLogWatcher;
import com.hypestats.util.SettingsManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.function.Consumer;

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
    private Label playerCountLabel;
    
    @FXML
    private Button refreshButton;
    
    private SettingsManager settingsManager;
    private HypixelApiService apiService;
    private MinecraftLogWatcher logWatcher;
    
    private ObservableList<LobbyPlayer> players = FXCollections.observableArrayList();
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        settingsManager = SettingsManager.getInstance();
        apiService = new HypixelApiService();
        
        // Setup table columns
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        levelColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            return new SimpleStringProperty(stats != null ? stats.getFormattedLevel() : "");
        });
        winsColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            return stats != null ? new SimpleStringProperty(String.valueOf(stats.getWins())) : new SimpleStringProperty("");
        });
        wlRatioColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            return new SimpleStringProperty(stats != null ? stats.getFormattedWLRatio() : "");
        });
        kdRatioColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            return new SimpleStringProperty(stats != null ? stats.getFormattedKDRatio() : "");
        });
        finalKdRatioColumn.setCellValueFactory(cellData -> {
            PlayerStats stats = cellData.getValue().getStats();
            return new SimpleStringProperty(stats != null ? stats.getFormattedFinalKDRatio() : "");
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        
        playerTable.setItems(players);
        
        // Load saved log path
        String savedLogPath = settingsManager.getLogPath();
        if (savedLogPath != null && !savedLogPath.isEmpty()) {
            logPathField.setText(savedLogPath);
        }
        
        updatePlayerCount();
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
            settingsManager.setLogPath(selectedFile.getAbsolutePath());
        }
    }
    
    /**
     * Use the default log file path
     * @param event ActionEvent
     */
    @FXML
    private void useDefaultPath(ActionEvent event) {
        String defaultPath = SettingsManager.getDefaultLogPath();
        logPathField.setText(defaultPath);
        settingsManager.setLogPath(defaultPath);
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
        
        // Add player listener
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
        
        // Start watching
        boolean started = logWatcher.start();
        
        if (started) {
            startButton.setDisable(true);
            stopButton.setDisable(false);
            statusLabel.setText("Monitoring");
            statusLabel.getStyleClass().remove("status-stopped");
            statusLabel.getStyleClass().add("status-running");
            appendToLogOutput("Started monitoring log file: " + logPath);
        } else {
            appendToLogOutput("ERROR: Failed to start monitoring log file: " + logPath);
        }
    }
    
    /**
     * Stop monitoring the Minecraft log file
     * @param event ActionEvent
     */
    @FXML
    private void stopMonitoring(ActionEvent event) {
        if (logWatcher != null && logWatcher.isRunning()) {
            logWatcher.stop();
            
            startButton.setDisable(false);
            stopButton.setDisable(true);
            statusLabel.setText("Not monitoring");
            statusLabel.getStyleClass().remove("status-running");
            statusLabel.getStyleClass().add("status-stopped");
            
            appendToLogOutput("Stopped monitoring log file");
        }
    }
    
    /**
     * Clear the log output
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
        updatePlayerCount();
    }
    
    /**
     * Refresh the stats for all players
     * @param event ActionEvent
     */
    @FXML
    private void refreshStats(ActionEvent event) {
        String apiKey = settingsManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            appendToLogOutput("ERROR: No Hypixel API key set. Please set your API key in Settings first");
            return;
        }
        
        for (LobbyPlayer player : players) {
            if (player.getStats() == null && !player.isLoading() && player.getError() == null) {
                fetchPlayerStats(player, apiKey);
            }
        }
    }
    
    /**
     * Add a player to the list
     * @param username Player username
     */
    private void addPlayerToList(String username) {
        // Check if player already exists
        for (LobbyPlayer existingPlayer : players) {
            if (existingPlayer.getUsername().equalsIgnoreCase(username)) {
                appendToLogOutput("Player " + username + " already in list");
                return;
            }
        }
        
        // Create new player
        LobbyPlayer player = new LobbyPlayer(username);
        players.add(player);
        updatePlayerCount();
        
        appendToLogOutput("Added player to list: " + username);
        
        // Fetch player stats
        String apiKey = settingsManager.getApiKey();
        if (apiKey != null && !apiKey.isEmpty()) {
            fetchPlayerStats(player, apiKey);
        }
    }
    
    /**
     * Fetch stats for a player
     * @param player LobbyPlayer to fetch stats for
     * @param apiKey Hypixel API key
     */
    private void fetchPlayerStats(LobbyPlayer player, String apiKey) {
        player.setLoading(true);
        
        Task<PlayerStats> task = new Task<>() {
            @Override
            protected PlayerStats call() throws Exception {
                return apiService.getPlayerStats(player.getUsername(), apiKey);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    player.setStats(getValue());
                    player.setLoading(false);
                    playerTable.refresh();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    if (exception instanceof HypixelApiService.ApiException) {
                        player.setError(exception.getMessage());
                    } else {
                        player.setError("Failed to fetch stats");
                    }
                    player.setLoading(false);
                    playerTable.refresh();
                });
            }
        };
        
        new Thread(task).start();
    }
    
    /**
     * Format a log entry for display
     * @param log MinecraftLog to format
     * @return Formatted log entry
     */
    private String formatLogEntry(MinecraftLog log) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timestamp = log.getTimestamp().format(formatter);
        return "[" + timestamp + "] " + log.getMessage();
    }
    
    /**
     * Append a message to the log output
     * @param message Message to append
     */
    private void appendToLogOutput(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String timestamp = sdf.format(new Date());
        
        logOutputArea.appendText("[" + timestamp + "] " + message + System.lineSeparator());
        
        // Auto-scroll to bottom
        logOutputArea.setScrollTop(Double.MAX_VALUE);
    }
    
    /**
     * Update the player count label
     */
    private void updatePlayerCount() {
        playerCountLabel.setText(players.size() + " players");
    }
} 