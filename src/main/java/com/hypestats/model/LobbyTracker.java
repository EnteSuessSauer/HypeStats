package com.hypestats.model;

import com.hypestats.model.events.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tracks players in the current Hypixel lobby
 * Detects lobby changes, player joins/quits, and game state transitions
 */
@Slf4j
public class LobbyTracker {
    @Getter
    private GameState gameState = GameState.UNKNOWN;

    @Getter
    private String gameMode = null;

    @Getter
    private String currentLobby = null;
    
    private Instant lastLobbyChange = Instant.now();

    @Getter
    private final Set<UUID> playersInLobby = ConcurrentHashMap.newKeySet();
    
    @Getter
    private final Map<UUID, String> playerDisplayNames = new ConcurrentHashMap<>();
    
    @Getter
    private final Set<String> rawPlayerNames = ConcurrentHashMap.newKeySet();

    @Getter
    private final Map<String, String> playerTeamMap = new ConcurrentHashMap<>();

    private final List<LobbyEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Process a new log line and update the lobby state accordingly
     * @param logLine The log line to process
     */
    public void processLogLine(String logLine) {
        if (logLine == null || logLine.isEmpty()) {
            return;
        }

        // Extract the chat message part from the log line if it exists
        String logContent = logLine;
        if (logLine.contains("[CHAT] ")) {
            logContent = logLine.substring(logLine.indexOf("[CHAT] ") + 7);
        }

        // Check for server changes first to track player movement between lobbies
        String newLobby = GameStateDetector.detectLobbyChange(logContent);
        if (newLobby != null) {
            handleLobbyChange(newLobby);
            return;
        }

        // Check for player list from /who command
        if (GameStateDetector.isPlayerList(logContent)) {
            processPlayerList(logContent);
            return;
        }

        // Check for game modes and state changes
        updateGameState(logLine);
        
        // Process player-specific events
        processPlayerEvents(logLine);
        
        // Also check for old pattern-based events to maintain compatibility
        processPatternBasedEvents(logLine);
    }
    
    /**
     * Handle a lobby change event
     * @param newLobby The ID of the new lobby
     */
    private void handleLobbyChange(String newLobby) {
        log.info("Detected lobby change to: {}", newLobby);
        lastLobbyChange = Instant.now();
        
        // Clear player tracking data when switching lobbies
        playerTeamMap.clear();
        rawPlayerNames.clear();
        
        if (GameStateDetector.isGameLobby(newLobby)) {
            // If it appears to be a game lobby, update state but wait for confirmation
            setGameState(GameState.WAITING);
            currentLobby = newLobby;
            fireEvent(new LobbyChangedEvent(newLobby, true));
        } else {
            // If it's a hub/lobby, we're definitely not in a game
            setGameState(GameState.NOT_IN_GAME);
            currentLobby = newLobby;
            gameMode = null;
            fireEvent(new LobbyChangedEvent(newLobby, false));
        }
    }
    
    private void updateGameState(String logLine) {
        // Detect game mode from keywords
        String detectedGameMode = GameStateDetector.detectGameMode(logLine);
        if (detectedGameMode != null && (gameMode == null || !gameMode.equals(detectedGameMode))) {
            gameMode = detectedGameMode;
            log.info("Detected game mode: {}", gameMode);
            fireEvent(new GameModeDetectedEvent(gameMode));
        }
        
        // Check if the line indicates being in a game
        if (GameStateDetector.isInGameIndicator(logLine)) {
            // Strong evidence we're in a game
            if (gameState != GameState.IN_GAME) {
                setGameState(GameState.IN_GAME);
                log.info("Game started based on in-game indicators");
                fireEvent(new GameStartedEvent());
            }
        } 
        // Check if the line indicates being in a lobby
        else if (GameStateDetector.isInLobbyIndicator(logLine)) {
            // If we detect a lobby indicator and we were in a game before
            if (gameState == GameState.IN_GAME) {
                // This suggests the game has ended
                setGameState(GameState.NOT_IN_GAME);
                log.info("Game ended based on lobby indicators");
                fireEvent(new GameEndedEvent());
            } else if (gameState == GameState.UNKNOWN) {
                // We now know we're not in a game
                setGameState(GameState.NOT_IN_GAME);
            }
        }
        
        // Check for classic game start patterns (for backward compatibility)
        Matcher gameStartedMatcher = LobbyPatterns.GameState.GAME_STARTED.matcher(logLine);
        if (gameStartedMatcher.find()) {
            log.info("Game started based on pattern match");
            setGameState(GameState.IN_GAME);
            fireEvent(new GameStartedEvent());
            return;
        }
        
        // Check for classic game end patterns (for backward compatibility)
        Matcher gameEndedMatcher = LobbyPatterns.GAME_ENDED.matcher(logLine);
        if (gameEndedMatcher.find()) {
            log.info("Game ended based on pattern match");
            setGameState(GameState.NOT_IN_GAME);
            fireEvent(new GameEndedEvent());
            return;
        }
    }
    
    private void processPlayerList(String logContent) {
        // Process player names from /who command or similar
        // This is a simple extraction for now - can be expanded
        String[] parts = logContent.split("[,:]");
        Set<String> newPlayers = new HashSet<>();
        
        for (String part : parts) {
            // Extract player names, typically enclosed in brackets or other patterns
            String trimmed = part.trim();
            // Remove ranks and other decorations
            if (trimmed.contains("[") && trimmed.contains("]")) {
                trimmed = trimmed.replaceAll("\\[.*?\\]", "").trim();
            }
            if (trimmed.contains("(") && trimmed.contains(")")) {
                // Extract what's before the parentheses
                trimmed = trimmed.substring(0, trimmed.indexOf("(")).trim();
            }
            
            // Validate it looks like a player name
            if (trimmed.length() > 2 && trimmed.length() <= 16 && 
                !trimmed.contains(" ") && !trimmed.startsWith("/")) {
                newPlayers.add(trimmed);
                rawPlayerNames.add(trimmed);
            }
        }
        
        log.debug("Processed player list, found {} players", newPlayers.size());
        if (!newPlayers.isEmpty()) {
            fireEvent(new PlayersDetectedEvent(newPlayers));
        }
    }
    
    private void processPlayerEvents(String logLine) {
        String logContent = logLine;
        if (logLine.contains("[CHAT] ")) {
            logContent = logLine.substring(logLine.indexOf("[CHAT] ") + 7);
        }
        
        // Check for player join/quit events using flexible keyword detection
        if (logContent.contains(" joined ") || logContent.contains(" joined.") || 
            logContent.contains(" has joined ")) {
            String playerName = extractPlayerName(logContent, "joined");
            if (playerName != null) {
                rawPlayerNames.add(playerName);
                log.debug("Player joined: {}", playerName);
                fireEvent(new PlayerJoinedEvent(playerName));
            }
        }
        
        if (logContent.contains(" quit ") || logContent.contains(" quit.") || 
            logContent.contains(" has quit ") || logContent.contains(" left ")) {
            String playerName = extractPlayerName(logContent, "quit");
            if (playerName == null) {
                playerName = extractPlayerName(logContent, "left");
            }
            if (playerName != null) {
                rawPlayerNames.remove(playerName);
                log.debug("Player quit: {}", playerName);
                fireEvent(new PlayerQuitEvent(playerName));
            }
        }
        
        // Check for team assignments using keyword detection
        String[] teams = {"red", "blue", "green", "yellow", "aqua", "white", "pink", "gray", "purple", "orange"};
        for (String team : teams) {
            if (logContent.toLowerCase().contains(team + " team") || 
                logContent.toLowerCase().contains("team " + team) ||
                logContent.toLowerCase().contains("joined " + team)) {
                // Try to extract player name from team assignment
                String playerName = null;
                
                // Common patterns
                if (logContent.contains("You joined ")) {
                    playerName = "You"; // The player
                } else if (logContent.toLowerCase().contains(" joined " + team)) {
                    int index = logContent.toLowerCase().indexOf(" joined " + team);
                    if (index > 0) {
                        playerName = logContent.substring(0, index).trim();
                    }
                } else if (logContent.toLowerCase().contains(" has been added to " + team)) {
                    int index = logContent.toLowerCase().indexOf(" has been added to " + team);
                    if (index > 0) {
                        playerName = logContent.substring(0, index).trim();
                    }
                }
                
                if (playerName != null) {
                    playerTeamMap.put(playerName, team);
                    log.debug("Player {} assigned to team {}", playerName, team);
                    fireEvent(new TeamAssignmentEvent(playerName, team));
                }
            }
        }
    }
    
    private String extractPlayerName(String logContent, String eventType) {
        // Simple player name extraction - this can be expanded with more patterns
        String[] parts = logContent.split("\\s+");
        
        // Common pattern: "PlayerName joined the game"
        for (int i = 0; i < parts.length - 2; i++) {
            if (parts[i+1].toLowerCase().startsWith(eventType.toLowerCase())) {
                // Simple validation that it looks like a player name
                String candidate = parts[i].replace(":", "");
                if (candidate.length() >= 3 && candidate.length() <= 16) {
                    // Remove ranks and other decorations
                    if (candidate.contains("[") && candidate.contains("]")) {
                        candidate = candidate.replaceAll("\\[.*?\\]", "").trim();
                    }
                    return candidate;
                }
            }
        }
        
        // Another common pattern: "PlayerName has joined the lobby!"
        if (logContent.contains(" has " + eventType)) {
            int index = logContent.indexOf(" has " + eventType);
            if (index > 0) {
                String candidate = logContent.substring(0, index).trim();
                if (candidate.length() >= 3 && candidate.length() <= 16) {
                    // Remove ranks and other decorations
                    if (candidate.contains("[") && candidate.contains("]")) {
                        candidate = candidate.replaceAll("\\[.*?\\]", "").trim();
                    }
                    return candidate;
                }
            }
        }
        
        return null;
    }
    
    private void processPatternBasedEvents(String logLine) {
        // Process traditional pattern-based events for backward compatibility
        
        // BedWars specific
        if (gameMode != null && gameMode.equals("Bed Wars")) {
            Matcher bedDestructionMatcher = LobbyPatterns.BedWars.BED_DESTRUCTION.matcher(logLine);
            if (bedDestructionMatcher.find()) {
                String team = bedDestructionMatcher.group(1);
                String bedNumberStr = bedDestructionMatcher.group(2);
                String destroyer = bedDestructionMatcher.group(3);
                
                // Handle commas in numbers
                int bedNumber = Integer.parseInt(bedNumberStr.replace(",", ""));
                
                log.debug("Bed destruction detected: {} destroyed bed #{} of team {}", destroyer, bedNumber, team);
                fireEvent(new BedDestroyedEvent(team, destroyer, bedNumber));
                return;
            }
            
            Matcher finalKillCounterMatcher = LobbyPatterns.BedWars.FINAL_KILL_COUNTER.matcher(logLine);
            if (finalKillCounterMatcher.find()) {
                String victim = finalKillCounterMatcher.group(1);
                String killer = finalKillCounterMatcher.group(2);
                String finalKillCountStr = finalKillCounterMatcher.group(3);
                
                // Handle commas in numbers
                int finalKillCount = Integer.parseInt(finalKillCountStr.replace(",", ""));
                
                log.debug("Final kill detected: {} killed {} (count: {})", killer, victim, finalKillCount);
                fireEvent(new FinalKillEvent(victim, killer, finalKillCount));
                return;
            }
        }
        
        // Team elimination (applicable to multiple game modes)
        Matcher teamEliminationMatcher = LobbyPatterns.BedWars.TEAM_ELIMINATED.matcher(logLine);
        if (teamEliminationMatcher.find()) {
            String team = teamEliminationMatcher.group(1);
            log.debug("Team eliminated: {}", team);
            fireEvent(new TeamEliminatedEvent(team));
            return;
        }
    }

    private void setGameState(GameState newState) {
        if (this.gameState != newState) {
            log.info("Game state changed: {} -> {}", this.gameState, newState);
            this.gameState = newState;
            fireEvent(new GameStateChangedEvent(newState));
        }
    }

    private void fireEvent(LobbyEvent event) {
        for (LobbyEventListener listener : listeners) {
            try {
                if (event instanceof GameStateChangedEvent) {
                    listener.onGameStateChanged((GameStateChangedEvent) event);
                } else if (event instanceof GameModeDetectedEvent) {
                    listener.onGameModeDetected((GameModeDetectedEvent) event);
                } else if (event instanceof LobbyChangedEvent) {
                    listener.onLobbyChanged((LobbyChangedEvent) event);
                } else if (event instanceof PlayerJoinedEvent) {
                    listener.onPlayerJoined((PlayerJoinedEvent) event);
                } else if (event instanceof PlayerQuitEvent) {
                    listener.onPlayerQuit((PlayerQuitEvent) event);
                } else if (event instanceof TeamAssignmentEvent) {
                    listener.onTeamAssignment((TeamAssignmentEvent) event);
                } else if (event instanceof GameStartedEvent) {
                    listener.onGameStarted((GameStartedEvent) event);
                } else if (event instanceof GameEndedEvent) {
                    listener.onGameEnded((GameEndedEvent) event);
                } else if (event instanceof BedDestroyedEvent) {
                    listener.onBedDestroyed((BedDestroyedEvent) event);
                } else if (event instanceof FinalKillEvent) {
                    listener.onFinalKill((FinalKillEvent) event);
                } else if (event instanceof TeamEliminatedEvent) {
                    listener.onTeamEliminated((TeamEliminatedEvent) event);
                } else if (event instanceof PlayersDetectedEvent) {
                    listener.onPlayersDetected((PlayersDetectedEvent) event);
                }
            } catch (Exception e) {
                log.error("Error notifying listener {} of event {}", listener, event, e);
            }
        }
    }
    
    /**
     * Check if enough time has passed since last lobby change to consider it stable
     * @return true if the lobby is considered stable, false otherwise
     */
    public boolean isLobbyStable() {
        // Consider lobby stable after 5 seconds
        return Duration.between(lastLobbyChange, Instant.now()).getSeconds() >= 5;
    }
    
    /**
     * Get the current game state as meaningful status text
     * @return Status text describing the current game state
     */
    public String getStatusText() {
        StringBuilder status = new StringBuilder();
        
        switch (gameState) {
            case IN_GAME:
                status.append("In Game");
                if (gameMode != null) {
                    status.append(" (").append(gameMode).append(")");
                }
                break;
            case WAITING:
                status.append("In Lobby");
                if (currentLobby != null) {
                    status.append(" (").append(currentLobby).append(")");
                }
                break;
            case NOT_IN_GAME:
                status.append("In Hub");
                if (currentLobby != null) {
                    status.append(" (").append(currentLobby).append(")");
                }
                break;
            default:
                status.append("Unknown State");
        }
        
        // Add player count if available
        int playerCount = rawPlayerNames.size();
        if (playerCount > 0) {
            status.append(" - ").append(playerCount).append(" players");
        }
        
        return status.toString();
    }
    
    /**
     * Check if the current state is IN_GAME
     * @return true if the current state is IN_GAME, false otherwise
     */
    public boolean isInGame() {
        return gameState == GameState.IN_GAME;
    }

    /**
     * Get the current players in the game
     * @return A set of player names currently in the game
     */
    public Set<String> getCurrentPlayers() {
        return Collections.unmodifiableSet(rawPlayerNames);
    }

    /**
     * Reset the tracker state
     */
    public void reset() {
        gameState = GameState.UNKNOWN;
        gameMode = null;
        currentLobby = null;
        lastLobbyChange = Instant.now();
        playersInLobby.clear();
        playerDisplayNames.clear();
        rawPlayerNames.clear();
        playerTeamMap.clear();
    }

    /**
     * Add a lobby event listener
     * @param listener The listener to add
     */
    public void addListener(LobbyEventListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            log.debug("Added listener: {}", listener);
        }
    }

    /**
     * Remove a lobby event listener
     * @param listener The listener to remove
     */
    public void removeListener(LobbyEventListener listener) {
        listeners.remove(listener);
        log.debug("Removed listener: {}", listener);
    }
} 