package com.hypestats.controller;

import com.hypestats.model.GameState;
import com.hypestats.model.LobbyTracker;
import com.hypestats.model.events.*;
import com.hypestats.services.HypixelAPIService;
import com.hypestats.services.PlayerData;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Controller for the lobby tracker view
 */
@Slf4j
public class LobbyTrackerController implements LobbyEventListener {
    private LobbyTracker lobbyTracker;
    private HypixelAPIService apiService;
    private ExecutorService executorService;
    
    @Getter
    private Map<String, PlayerData> playerDataMap = new HashMap<>();
    
    private List<Consumer<String>> statusUpdateListeners = new ArrayList<>();
    private List<Runnable> playerDataUpdateListeners = new ArrayList<>();
    
    private AtomicBoolean fetchingStats = new AtomicBoolean(false);
    private Instant lastApiFetch = Instant.MIN;
    
    // Rate limiting vars
    private static final int FETCH_BATCH_SIZE = 4; // Number of players to fetch at once
    private static final Duration MIN_FETCH_INTERVAL = Duration.ofSeconds(3); // Minimum time between API call batches
    
    /**
     * Default constructor for FXML loader
     * Will be initialized later with the proper service in MainController
     */
    public LobbyTrackerController() {
        log.info("Creating LobbyTrackerController with default constructor");
        this.lobbyTracker = new LobbyTracker();
        this.executorService = Executors.newCachedThreadPool();
        lobbyTracker.addListener(this);
    }
    
    /**
     * Full constructor with API service
     * @param apiService HypixelAPIService instance
     */
    public LobbyTrackerController(HypixelAPIService apiService) {
        this();
        log.info("Initializing LobbyTrackerController with API service");
        this.apiService = apiService;
        
        // Start periodic status updates
        Timer statusTimer = new Timer(true);
        statusTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String status = lobbyTracker.getStatusText();
                notifyStatusUpdate(status);
            }
        }, 0, 1000);
    }
    
    /**
     * Set the API service
     * This is used after FXML initialization with the default constructor
     * @param apiService HypixelAPIService instance
     */
    public void setApiService(HypixelAPIService apiService) {
        log.info("Setting API service for LobbyTrackerController");
        this.apiService = apiService;
        
        // Start periodic status updates if not already started
        if (apiService != null) {
            Timer statusTimer = new Timer(true);
            statusTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    String status = lobbyTracker.getStatusText();
                    notifyStatusUpdate(status);
                }
            }, 0, 1000);
        }
    }
    
    /**
     * Initialize the controller
     * This can be called to re-initialize after settings change
     */
    public void initialize() {
        log.info("Initializing LobbyTrackerController");
        // Reset player data
        playerDataMap.clear();
        notifyPlayerDataUpdate();
        
        // Update status text
        notifyStatusUpdate(lobbyTracker.getStatusText());
    }
    
    /**
     * Process a log line from the Minecraft log file
     * @param logLine The log line to process
     */
    public void processLogLine(String logLine) {
        lobbyTracker.processLogLine(logLine);
        
        // If we're in a stable game state and have players, ensure we have stats
        if (lobbyTracker.getGameState() == GameState.IN_GAME && 
            lobbyTracker.isLobbyStable() && 
            !lobbyTracker.getRawPlayerNames().isEmpty() && 
            Duration.between(lastApiFetch, Instant.now()).toMinutes() >= 1) {
            // Try to fetch stats for all detected players
            fetchStatsForAllPlayers();
        }
    }
    
    /**
     * Register for status updates
     * @param listener Callback to receive status updates
     */
    public void addStatusUpdateListener(Consumer<String> listener) {
        statusUpdateListeners.add(listener);
    }
    
    /**
     * Register for player data updates
     * @param listener Callback for when player data updates
     */
    public void addPlayerDataUpdateListener(Runnable listener) {
        playerDataUpdateListeners.add(listener);
    }
    
    private void notifyStatusUpdate(String status) {
        Platform.runLater(() -> {
            for (Consumer<String> listener : statusUpdateListeners) {
                try {
                    listener.accept(status);
                } catch (Exception e) {
                    log.error("Error notifying status listener", e);
                }
            }
        });
    }
    
    private void notifyPlayerDataUpdate() {
        Platform.runLater(() -> {
            for (Runnable listener : playerDataUpdateListeners) {
                try {
                    listener.run();
                } catch (Exception e) {
                    log.error("Error notifying player data listener", e);
                }
            }
        });
    }
    
    /**
     * Fetch stats for all detected players, with rate limiting
     */
    public void fetchStatsForAllPlayers() {
        // Avoid concurrent fetches
        if (fetchingStats.getAndSet(true)) {
            return;
        }
        
        // Only fetch stats if we're in a game and not in the hub/lobby
        if (lobbyTracker.getGameState() != GameState.IN_GAME) {
            fetchingStats.set(false);
            log.debug("Not fetching stats - not in a game");
            return;
        }
        
        // Check if API service is available
        if (apiService == null) {
            fetchingStats.set(false);
            log.warn("Cannot fetch stats - API service not initialized");
            notifyStatusUpdate("API service not available");
            return;
        }
        
        lastApiFetch = Instant.now();
        
        executorService.submit(() -> {
            try {
                log.info("Fetching stats for all players in lobby...");
                notifyStatusUpdate("Fetching player stats...");
                
                // Get all players we know about
                Set<String> players = new HashSet<>(lobbyTracker.getRawPlayerNames());
                List<String> playersList = new ArrayList<>(players);
                
                // Calculate batches based on available API limit
                int remainingApiCalls = apiService.getRemainingLimit();
                int totalBatches = (playersList.size() + FETCH_BATCH_SIZE - 1) / FETCH_BATCH_SIZE;
                int possibleBatches = remainingApiCalls / FETCH_BATCH_SIZE;
                int batchesToProcess = Math.min(totalBatches, possibleBatches);
                
                if (batchesToProcess == 0) {
                    log.warn("Not enough API calls remaining ({}) to fetch any player stats", remainingApiCalls);
                    notifyStatusUpdate("API limit reached, waiting to fetch stats");
                    return;
                }
                
                log.info("Processing {} batches of {} players each ({} players total, {} API calls remaining)",
                        batchesToProcess, FETCH_BATCH_SIZE, players.size(), remainingApiCalls);
                
                for (int i = 0; i < batchesToProcess; i++) {
                    int fromIndex = i * FETCH_BATCH_SIZE;
                    int toIndex = Math.min(fromIndex + FETCH_BATCH_SIZE, playersList.size());
                    
                    if (fromIndex >= playersList.size()) {
                        break;
                    }
                    
                    List<String> batch = playersList.subList(fromIndex, toIndex);
                    log.debug("Fetching batch {}/{}: {} players", i + 1, batchesToProcess, batch.size());
                    
                    // Fetch each player in the batch
                    for (String playerName : batch) {
                        // Skip players we already have data for
                        if (playerDataMap.containsKey(playerName)) {
                            continue;
                        }
                        
                        try {
                            PlayerData playerData = apiService.getPlayerData(playerName);
                            if (playerData != null) {
                                playerDataMap.put(playerName, playerData);
                                log.debug("Fetched data for player: {}", playerName);
                                notifyStatusUpdate("Fetched data for " + playerName);
                            }
                        } catch (Exception e) {
                            log.error("Error fetching player data for {}", playerName, e);
                        }
                    }
                    
                    // Notify UI of updates
                    notifyPlayerDataUpdate();
                    
                    // Wait between batches to avoid hammering the API
                    if (i < batchesToProcess - 1) {
                        Thread.sleep(MIN_FETCH_INTERVAL.toMillis());
                    }
                }
                
                log.info("Completed fetching stats for players. API calls remaining: {}", apiService.getRemainingLimit());
                notifyStatusUpdate(lobbyTracker.getStatusText());
                
            } catch (InterruptedException e) {
                log.warn("Stat fetching interrupted", e);
                notifyStatusUpdate("Stat fetching interrupted");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error fetching player stats", e);
                notifyStatusUpdate("Error fetching player stats");
            } finally {
                fetchingStats.set(false);
            }
        });
    }

    @Override
    public void onGameStateChanged(GameStateChangedEvent event) {
        log.info("Game state changed: {}", event.getNewState());
        
        if (event.getNewState() == GameState.IN_GAME) {
            // When a game starts, we'll want to fetch player stats
            executorService.submit(() -> {
                // Wait a moment to let player detection happen first
                try {
                    Thread.sleep(5000);
                    fetchStatsForAllPlayers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } else if (event.getNewState() == GameState.NOT_IN_GAME) {
            // When we leave a game, clear the player data
            playerDataMap.clear();
            notifyPlayerDataUpdate();
        }
    }

    @Override
    public void onGameStarted(GameStartedEvent event) {
        log.info("Game started event");
        notifyStatusUpdate("Game started!");
    }

    @Override
    public void onGameEnded(GameEndedEvent event) {
        log.info("Game ended event");
        notifyStatusUpdate("Game ended");
    }

    @Override
    public void onLobbyChanged(LobbyChangedEvent event) {
        log.info("Lobby changed to: {} (isGame: {})", event.getLobbyId(), event.isGameLobby());
        
        if (event.isGameLobby()) {
            notifyStatusUpdate("Joined game lobby: " + event.getLobbyId());
        } else {
            notifyStatusUpdate("Joined hub: " + event.getLobbyId());
            // Clear player data when leaving game lobbies
            playerDataMap.clear();
            notifyPlayerDataUpdate();
        }
    }

    @Override
    public void onPlayerJoined(PlayerJoinedEvent event) {
        log.debug("Player joined: {}", event.getPlayerName());
    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent event) {
        log.debug("Player quit: {}", event.getPlayerName());
        // Remove player data when they leave
        playerDataMap.remove(event.getPlayerName());
        notifyPlayerDataUpdate();
    }

    @Override
    public void onPlayersDetected(PlayersDetectedEvent event) {
        log.info("Detected {} players", event.getPlayerNames().size());
        
        // If we're in a game, try to fetch stats for these players
        if (lobbyTracker.getGameState() == GameState.IN_GAME && 
            event.getPlayerNames().size() > 0) {
            
            executorService.submit(() -> {
                // Wait a moment to avoid hammering the API
                try {
                    Thread.sleep(1000);
                    fetchStatsForAllPlayers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    /**
     * Get the current state of the lobby tracker
     * @return The current game state
     */
    public GameState getGameState() {
        return lobbyTracker.getGameState();
    }
    
    /**
     * Get the current game mode
     * @return The current game mode or null if not in a game
     */
    public String getGameMode() {
        return lobbyTracker.getGameMode();
    }
    
    /**
     * Get all raw player names currently known
     * @return Set of player names
     */
    public Set<String> getPlayerNames() {
        return new HashSet<>(lobbyTracker.getRawPlayerNames());
    }
    
    /**
     * Get a player's team assignment
     * @param playerName The player name
     * @return The team name or null if not assigned
     */
    public String getPlayerTeam(String playerName) {
        return lobbyTracker.getPlayerTeamMap().get(playerName);
    }
    
    /**
     * Get a map of player names to their teams
     * @return Map of player names to team names
     */
    public Map<String, String> getPlayerTeams() {
        return new HashMap<>(lobbyTracker.getPlayerTeamMap());
    }
    
    /**
     * Get the current lobby ID
     * @return The current lobby ID or null if not in a lobby
     */
    public String getCurrentLobby() {
        return lobbyTracker.getCurrentLobby();
    }
    
    /**
     * Force the system to fetch player stats
     */
    public void forceFetchPlayerStats() {
        fetchStatsForAllPlayers();
    }
    
    /**
     * Send a command to the player's chat
     * This can be used to trigger commands like /who to get player lists
     * @param command The command to send
     */
    public void sendCommand(String command) {
        // This would be implemented if we had a way to send commands to Minecraft
        log.info("Command requested: {}", command);
    }
    
    /**
     * Clean up any resources
     */
    public void shutdown() {
        executorService.shutdown();
    }
    
    /**
     * Refresh the log file contents
     * @param event ActionEvent or null if triggered by hotkey
     */
    public void refreshLogFile(javafx.event.ActionEvent event) {
        log.info("Manual log file refresh requested");
        // Implement log file refresh logic here
        // This would typically read the current log file and process any new lines
        notifyStatusUpdate("Refreshing log file...");
        
        // After refreshing, update the status
        notifyStatusUpdate(lobbyTracker.getStatusText());
    }
    
    /**
     * Browse for a log file
     * @param event ActionEvent
     */
    public void browseLogFile(javafx.event.ActionEvent event) {
        log.info("Browse log file requested");
        notifyStatusUpdate("Browse functionality not implemented");
    }
    
    /**
     * Auto-detect the Minecraft log file
     * @param event ActionEvent
     */
    public void autoDetectLogFile(javafx.event.ActionEvent event) {
        log.info("Auto-detect log file requested");
        notifyStatusUpdate("Auto-detect functionality not implemented");
    }
    
    /**
     * Use the default log file path
     * @param event ActionEvent
     */
    public void useDefaultPath(javafx.event.ActionEvent event) {
        log.info("Use default path requested");
        notifyStatusUpdate("Default path functionality not implemented");
    }
    
    /**
     * Start monitoring the log file
     * @param event ActionEvent
     */
    public void startMonitoring(javafx.event.ActionEvent event) {
        log.info("Start monitoring requested");
        notifyStatusUpdate("Monitoring started");
    }
    
    /**
     * Stop monitoring the log file
     * @param event ActionEvent
     */
    public void stopMonitoring(javafx.event.ActionEvent event) {
        log.info("Stop monitoring requested");
        notifyStatusUpdate("Monitoring stopped");
    }
    
    /**
     * Clear the log output
     * @param event ActionEvent
     */
    public void clearLog(javafx.event.ActionEvent event) {
        log.info("Clear log requested");
        notifyStatusUpdate("Log cleared");
    }
    
    /**
     * Clear the player list
     * @param event ActionEvent
     */
    public void clearPlayers(javafx.event.ActionEvent event) {
        log.info("Clear players requested");
        playerDataMap.clear();
        notifyPlayerDataUpdate();
        notifyStatusUpdate("Player list cleared");
    }
    
    /**
     * Refresh player stats
     * @param event ActionEvent
     */
    public void refreshStats(javafx.event.ActionEvent event) {
        log.info("Refresh stats requested");
        if (apiService == null) {
            notifyStatusUpdate("API service not available");
            return;
        }
        forceFetchPlayerStats();
    }
} 