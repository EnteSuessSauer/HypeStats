package com.hypestats.model;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks players in the current Hypixel lobby
 * Detects lobby changes, player joins/quits, and game state transitions
 */
@Slf4j
public class LobbyTracker {
    // Current state
    private String currentLobby = "";
    private String currentGameType = "";
    private boolean inGame = false;
    private boolean inQueue = false;
    private final Set<String> currentPlayers = new HashSet<>();
    private final List<LobbyEventListener> eventListeners = new ArrayList<>();
    
    /**
     * Process a new log line and update the lobby state accordingly
     * @param logLine The log line to process
     */
    public void processLogLine(String logLine) {
        // Check for main lobby indicators - these take precedence to reset game state
        if (LobbyPatterns.LobbyNavigation.MAIN_LOBBY.matcher(logLine).find()) {
            if (inGame) {
                log.info("Detected return to main lobby");
                inGame = false;
                inQueue = false;
                currentGameType = "";
                
                // Notify listeners
                for (LobbyEventListener listener : eventListeners) {
                    listener.onReturnToLobby();
                }
            }
            return;
        }
        
        // Check if bed was destroyed (player is now a spectator)
        if (LobbyPatterns.BedWars.BED_DESTROYED_SPECTATOR.matcher(logLine).find()) {
            log.info("Your bed was destroyed - now spectating");
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onBedDestroyed();
            }
            return;
        }
        
        // Check for bed destruction
        Matcher bedDestructionMatcher = LobbyPatterns.BedWars.BED_DESTRUCTION.matcher(logLine);
        if (bedDestructionMatcher.find()) {
            String teamColor = bedDestructionMatcher.group(1);
            String bedNumberStr = bedDestructionMatcher.group(2);
            // Parse the number by removing commas
            int bedNumber = Integer.parseInt(bedNumberStr.replace(",", ""));
            String destroyer = bedDestructionMatcher.group(3);
            
            log.info("Bed destroyed: {} team's bed (#{}) by {}", teamColor, bedNumber, destroyer);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onBedDestruction(teamColor, destroyer, bedNumber);
            }
            return;
        }
        
        // Check for player respawn countdown
        Matcher respawnCountdownMatcher = LobbyPatterns.BedWars.PLAYER_RESPAWN_COUNTDOWN.matcher(logLine);
        if (respawnCountdownMatcher.find()) {
            int seconds = Integer.parseInt(respawnCountdownMatcher.group(1));
            
            log.info("Respawning in {} seconds", seconds);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onRespawnCountdown(seconds);
            }
            return;
        }
        
        // Check for player respawn
        if (LobbyPatterns.BedWars.PLAYER_RESPAWNED.matcher(logLine).find()) {
            log.info("Player respawned");
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onRespawned();
            }
            return;
        }
        
        // Check for final kill with counter
        Matcher finalKillCounterMatcher = LobbyPatterns.BedWars.FINAL_KILL_COUNTER.matcher(logLine);
        if (finalKillCounterMatcher.find()) {
            String victim = finalKillCounterMatcher.group(1);
            String killer = finalKillCounterMatcher.group(2);
            String finalKillCountStr = finalKillCounterMatcher.group(3);
            // Parse the number by removing commas
            int finalKillCount = Integer.parseInt(finalKillCountStr.replace(",", ""));
            
            log.info("Final kill: {} was killed by {} (#{} final kill)", victim, killer, finalKillCount);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onPlayerFinalKill(victim, killer, finalKillCount);
                listener.onPlayerEliminated(victim); // Also notify with the simpler interface
            }
            return;
        }
        
        // Check for item purchase
        Matcher itemPurchaseMatcher = LobbyPatterns.Economy.ITEM_PURCHASE.matcher(logLine);
        if (itemPurchaseMatcher.find()) {
            String item = itemPurchaseMatcher.group(1);
            String currency = itemPurchaseMatcher.group(2);
            
            log.info("Purchased item: {} with {}", item, currency);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onItemPurchase(item, currency);
            }
            return;
        }
        
        // Check for upgrade purchase
        Matcher upgradePurchaseMatcher = LobbyPatterns.Economy.UPGRADE_PURCHASE.matcher(logLine);
        if (upgradePurchaseMatcher.find()) {
            String player = upgradePurchaseMatcher.group(1);
            String upgrade = upgradePurchaseMatcher.group(2);
            
            log.info("Upgrade purchased: {} by {}", upgrade, player);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onUpgradePurchase(player, upgrade);
            }
            return;
        }
        
        // Check for experience gain
        Matcher experienceGainMatcher = LobbyPatterns.Economy.EXPERIENCE_GAIN.matcher(logLine);
        if (experienceGainMatcher.find()) {
            int amount = Integer.parseInt(experienceGainMatcher.group(1));
            String type = experienceGainMatcher.group(2);
            String reason = experienceGainMatcher.group(3);
            
            log.info("Gained {} {} XP for {}", amount, type, reason);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onExperienceGain(amount, type, reason);
            }
            return;
        }
        
        // Check for tokens gain
        Matcher tokensGainMatcher = LobbyPatterns.Economy.TOKENS_GAIN.matcher(logLine);
        if (tokensGainMatcher.find()) {
            int amount = Integer.parseInt(tokensGainMatcher.group(1));
            String reason = tokensGainMatcher.group(2);
            
            log.info("Gained {} tokens for {}", amount, reason);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onTokensGain(amount, reason);
            }
            return;
        }
        
        // Check for resource gain
        Matcher resourceGainMatcher = LobbyPatterns.Economy.RESOURCE_GAIN.matcher(logLine);
        if (resourceGainMatcher.find()) {
            int amount = Integer.parseInt(resourceGainMatcher.group(1));
            String resource = resourceGainMatcher.group(2);
            
            log.info("Gained {} {}", amount, resource);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onResourceGain(amount, resource);
            }
            return;
        }
        
        // Check for ticket gain
        Matcher ticketGainMatcher = LobbyPatterns.Economy.TICKET_GAIN.matcher(logLine);
        if (ticketGainMatcher.find()) {
            int amount = Integer.parseInt(ticketGainMatcher.group(1));
            String type = ticketGainMatcher.group(2);
            String reason = ticketGainMatcher.group(3);
            
            log.info("Gained {} {} tickets for {}", amount, type, reason);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onTicketGain(amount, type, reason);
            }
            return;
        }
        
        // Check for item deposit
        Matcher itemDepositMatcher = LobbyPatterns.Economy.ITEM_DEPOSIT.matcher(logLine);
        if (itemDepositMatcher.find()) {
            String item = itemDepositMatcher.group(1);
            
            log.info("Deposited {} into team chest", item);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onItemDeposit(item);
            }
            return;
        }
        
        // Check for player death (void)
        Matcher playerDeathVoidMatcher = LobbyPatterns.PlayerTracking.PLAYER_DEATH_VOID.matcher(logLine);
        if (playerDeathVoidMatcher.find()) {
            String player = playerDeathVoidMatcher.group(1);
            
            log.info("Player died (void): {}", player);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onPlayerDeath(player, "void", null);
            }
            return;
        }
        
        // Check for player death by another player
        Matcher playerDeathByPlayerMatcher = LobbyPatterns.PlayerTracking.PLAYER_DEATH_BY_PLAYER.matcher(logLine);
        if (playerDeathByPlayerMatcher.find()) {
            String victim = playerDeathByPlayerMatcher.group(1);
            String deathType = playerDeathByPlayerMatcher.group(2);
            String killer = playerDeathByPlayerMatcher.group(3);
            
            log.info("Player died: {} was {} by {}", victim, deathType, killer);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onPlayerDeath(victim, deathType, killer);
            }
            return;
        }
        
        // Check for cross-teaming warning
        if (LobbyPatterns.GameState.CROSS_TEAMING_WARNING.matcher(logLine).find()) {
            log.info("Cross-teaming warning displayed");
            
            // This is a good indicator that we're in a game
            if (!inGame) {
                inGame = true;
                inQueue = false;
            }
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onCrossTeamingWarning();
            }
            return;
        }
        
        // Check for team chat
        Matcher teamChatMatcher = LobbyPatterns.PlayerTracking.TEAM_CHAT.matcher(logLine);
        if (teamChatMatcher.find()) {
            String level = teamChatMatcher.group(1);
            String team = teamChatMatcher.group(2);
            String message = teamChatMatcher.group(3);
            
            log.info("Team chat [{}] {}: {}", team, level, message);
            
            // Strong indicator we're in a game
            if (!inGame) {
                inGame = true;
                inQueue = false;
            }
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onTeamChat(team, level, message);
            }
            return;
        }
        
        // Check for server connection
        Matcher connectingMatcher = LobbyPatterns.LobbyNavigation.CONNECTING.matcher(logLine);
        if (connectingMatcher.find()) {
            String server = connectingMatcher.group(1);
            int port = Integer.parseInt(connectingMatcher.group(2));
            
            log.info("Connecting to server: {}:{}", server, port);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onServerConnection(server, port);
            }
            return;
        }
        
        // Check for reconnect command
        Matcher reconnectMatcher = LobbyPatterns.LobbyNavigation.RECONNECT.matcher(logLine);
        if (reconnectMatcher.find()) {
            String player = reconnectMatcher.group(1);
            String destination = reconnectMatcher.group(2);
            
            if (player != null) {
                log.info("Player {} has reconnected", player);
                
                // Notify listeners
                for (LobbyEventListener listener : eventListeners) {
                    listener.onPlayerReconnect(player);
                }
            } else if (destination != null) {
                log.info("Reconnecting to {}", destination);
                
                // This means we're joining a game directly via reconnect
                inGame = true;
                inQueue = false;
                
                // Notify listeners
                for (LobbyEventListener listener : eventListeners) {
                    listener.onReconnect(destination);
                }
            }
            return;
        }
        
        // Check for party warp
        Matcher partyWarpMatcher = LobbyPatterns.LobbyNavigation.PARTY_WARP.matcher(logLine);
        if (partyWarpMatcher.find()) {
            String warper = partyWarpMatcher.group(1);
            String destination = partyWarpMatcher.group(2);
            
            if (warper == null) {
                warper = partyWarpMatcher.group(3);
                destination = partyWarpMatcher.group(4);
            }
            
            log.info("Party warped by {} to {}", warper, destination);
            
            // This is a clear indicator we're about to join a game
            inGame = true;
            inQueue = false;
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onPartyWarp(warper, destination);
            }
            return;
        }
        
        // Check for queue join
        Matcher queueMatcher = LobbyPatterns.LobbyNavigation.QUEUE_JOIN.matcher(logLine);
        if (queueMatcher.find()) {
            String gameType = queueMatcher.group(1);
            inQueue = true;
            currentGameType = gameType;
            log.info("Joined queue for: {}", gameType);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onQueueJoin(gameType);
            }
            return;
        }
        
        // Check for lobby change
        Matcher lobbyMatcher = LobbyPatterns.LobbyNavigation.LOBBY_CHANGE.matcher(logLine);
        if (lobbyMatcher.find()) {
            String newLobby = lobbyMatcher.group(1);
            handleLobbyChange(newLobby);
            return;
        }
        
        // Check for game mode detection
        Matcher gameModeMatcher = LobbyPatterns.GameState.GAME_MODE.matcher(logLine);
        if (gameModeMatcher.find()) {
            String gameMode = gameModeMatcher.group(1);
            currentGameType = gameMode;
            log.info("Detected game mode: {}", gameMode);
            
            // Implies we're likely in a game
            if (!inGame) {
                inGame = true;
                inQueue = false;
                
                // Notify listeners
                for (LobbyEventListener listener : eventListeners) {
                    listener.onGameModeDetected(gameMode);
                }
            }
            return;
        }
        
        // Check for game banner title (more reliable game mode detection)
        Matcher gameBannerTitleMatcher = LobbyPatterns.GameBanners.BANNER_TITLE.matcher(logLine);
        if (gameBannerTitleMatcher.find()) {
            String gameMode = gameBannerTitleMatcher.group(2);
            currentGameType = gameMode;
            log.info("Detected game mode from banner: {}", gameMode);
            
            // This is a very reliable indicator we're in a game
            inGame = true;
            inQueue = false;
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onGameModeDetected(gameMode);
            }
            return;
        }
        
        // Check for game banner (top or bottom)
        if (LobbyPatterns.GameBanners.BANNER_TOP.matcher(logLine).find() || 
            LobbyPatterns.GameBanners.BANNER_FOOTER.matcher(logLine).find()) {
            log.info("Detected game banner");
            
            // This is a strong indicator that a game is starting
            inGame = true;
            inQueue = false;
            
            // We don't send an event for just the banner borders
            return;
        }
        
        // Check for team assignment
        Matcher teamMatcher = LobbyPatterns.GameState.TEAM_ASSIGNMENT.matcher(logLine);
        if (teamMatcher.find()) {
            String team = teamMatcher.group(1);
            log.info("Assigned to team: {}", team);
            
            // This is a strong indicator we're in a game
            inGame = true;
            inQueue = false;
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onTeamAssignment(team);
            }
            return;
        }
        
        // Check for game start countdown (5, 10, 30 seconds)
        Matcher countdownMatcher = LobbyPatterns.GameState.GAME_START_COUNTDOWN.matcher(logLine);
        if (countdownMatcher.find()) {
            int seconds = Integer.parseInt(countdownMatcher.group(1));
            
            // Only handle significant countdown intervals
            if (seconds == 5 || seconds == 10 || seconds == 30 || seconds == 60) {
                log.info("Game starting in {} seconds", seconds);
                
                // We're definitely in a game at this point
                inGame = true;
                inQueue = false;
                
                // Notify listeners
                for (LobbyEventListener listener : eventListeners) {
                    listener.onGameCountdown(seconds);
                }
            }
            return;
        }
        
        // Check for player join
        Matcher joinMatcher = LobbyPatterns.PlayerTracking.PLAYER_JOIN.matcher(logLine);
        if (joinMatcher.find()) {
            String playerName = joinMatcher.group(1);
            int currentCount = Integer.parseInt(joinMatcher.group(2));
            int maxCount = Integer.parseInt(joinMatcher.group(3));
            handlePlayerJoin(playerName, currentCount, maxCount);
            return;
        }
        
        // Check for player quit
        Matcher quitMatcher = LobbyPatterns.PlayerTracking.PLAYER_QUIT.matcher(logLine);
        if (quitMatcher.find()) {
            String playerName = quitMatcher.group(1);
            handlePlayerQuit(playerName);
            return;
        }
        
        // Check for complete player list from /who command
        Matcher listMatcher = LobbyPatterns.PlayerTracking.PLAYER_LIST.matcher(logLine);
        if (listMatcher.find()) {
            String playerList = listMatcher.group(1);
            handlePlayerList(playerList);
            return;
        }
        
        // Check for game starting (1 second or bedwars message)
        if (LobbyPatterns.GameState.GAME_START_FINAL.matcher(logLine).find() || 
            LobbyPatterns.GameBanners.GAME_START_BEDWARS.matcher(logLine).find() ||
            LobbyPatterns.GameState.GAME_STARTED.matcher(logLine).find()) {
            handleGameStart();
            return;
        }
        
        // Check for final kill
        Matcher finalKillMatcher = LobbyPatterns.BedWars.FINAL_KILL.matcher(logLine);
        if (finalKillMatcher.find()) {
            String playerName = finalKillMatcher.group(1);
            handlePlayerEliminated(playerName);
            return;
        }
        
        // Check for team elimination
        Matcher teamEliminatedMatcher = LobbyPatterns.BedWars.TEAM_ELIMINATED.matcher(logLine);
        if (teamEliminatedMatcher.find()) {
            String teamName = teamEliminatedMatcher.group(1);
            log.info("Team eliminated: {}", teamName);
            
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onTeamEliminated();
            }
            return;
        }
        
        // Check for player elimination (the user)
        if (LobbyPatterns.GameState.PLAYER_ELIMINATED.matcher(logLine).find()) {
            log.info("User eliminated");
            
            for (LobbyEventListener listener : eventListeners) {
                listener.onUserEliminated();
            }
            return;
        }
        
        // Check for victory or game end
        if (LobbyPatterns.GameState.VICTORY.matcher(logLine).find() || 
            LobbyPatterns.GameState.GAME_END.matcher(logLine).find()) {
            log.info("Game ended");
            
            // Game is over, but we'll wait for the lobby change to reset state
            for (LobbyEventListener listener : eventListeners) {
                listener.onGameEnd();
            }
        }
    }
    
    /**
     * Handle a lobby change event
     * @param newLobby The ID of the new lobby
     */
    private void handleLobbyChange(String newLobby) {
        log.info("Lobby change detected: {}", newLobby);
        
        // Clear current player list
        currentPlayers.clear();
        
        // Update current lobby
        String oldLobby = currentLobby;
        currentLobby = newLobby;
        
        // Reset game state on lobby change
        // But we'll set inGame based on lobby ID pattern
        boolean wasInGame = inGame;
        inGame = isGameLobby(newLobby);
        inQueue = false;
        
        // Notify listeners
        for (LobbyEventListener listener : eventListeners) {
            listener.onLobbyChange(oldLobby, newLobby);
            
            // If we've left a game and entered a non-game lobby
            if (wasInGame && !inGame) {
                listener.onReturnToLobby();
            }
        }
    }
    
    /**
     * Check if a lobby ID appears to be a game lobby rather than a hub
     * @param lobbyId The lobby ID to check
     * @return true if this looks like a game lobby
     */
    private boolean isGameLobby(String lobbyId) {
        // Most game lobbies have patterns like:
        // - mode.bedwars123
        // - mega12345
        // - 0123ab (short random characters/numbers)
        
        if (lobbyId == null || lobbyId.isEmpty()) {
            return false;
        }
        
        // Check for known game lobby patterns
        if (lobbyId.contains("bedwars") || 
            lobbyId.contains("skywars") || 
            lobbyId.contains("mega") || 
            lobbyId.contains("duels") ||
            lobbyId.contains("bw") ||
            lobbyId.contains("sw")) {
            return true;
        }
        
        // Most hub lobbies have "lobby" in the name
        if (lobbyId.contains("lobby") || 
            lobbyId.contains("hub") || 
            lobbyId.contains("limbo")) {
            return false;
        }
        
        // Default to false - we'll rely on other game indicators
        return false;
    }
    
    /**
     * Handle a player join event
     * @param playerName The name of the player who joined
     * @param currentCount The current player count
     * @param maxCount The maximum player count
     */
    private void handlePlayerJoin(String playerName, int currentCount, int maxCount) {
        log.debug("Player joined: {} ({}/{})", playerName, currentCount, maxCount);
        
        // Add player to the set
        if (currentPlayers.add(playerName)) {
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onPlayerJoin(playerName);
            }
        }
    }
    
    /**
     * Handle a player quit event
     * @param playerName The name of the player who quit
     */
    private void handlePlayerQuit(String playerName) {
        log.debug("Player quit: {}", playerName);
        
        // Remove player from the set
        if (currentPlayers.remove(playerName)) {
            // Notify listeners
            for (LobbyEventListener listener : eventListeners) {
                listener.onPlayerQuit(playerName);
            }
        }
    }
    
    /**
     * Handle a player elimination event (final kill)
     * @param playerName The name of the player who was eliminated
     */
    private void handlePlayerEliminated(String playerName) {
        log.debug("Player eliminated: {}", playerName);
        
        // Notify listeners
        for (LobbyEventListener listener : eventListeners) {
            listener.onPlayerEliminated(playerName);
        }
    }
    
    /**
     * Handle a game start event
     */
    private void handleGameStart() {
        log.info("Game started");
        
        inGame = true;
        
        // Notify listeners
        for (LobbyEventListener listener : eventListeners) {
            listener.onGameStart();
        }
    }
    
    /**
     * Handle a complete player list from the /who command
     * @param playerList Comma-separated list of players
     */
    private void handlePlayerList(String playerList) {
        log.info("Player list received: {}", playerList);
        
        // Process the player list
        String[] players = playerList.split(", ");
        Set<String> newPlayers = new HashSet<>();
        
        for (String player : players) {
            player = player.trim();
            if (!player.isEmpty()) {
                newPlayers.add(player);
                
                // Notify about new players
                if (!currentPlayers.contains(player)) {
                    for (LobbyEventListener listener : eventListeners) {
                        listener.onPlayerJoin(player);
                    }
                }
            }
        }
        
        // Find players who left
        Set<String> leftPlayers = new HashSet<>(currentPlayers);
        leftPlayers.removeAll(newPlayers);
        
        for (String player : leftPlayers) {
            for (LobbyEventListener listener : eventListeners) {
                listener.onPlayerQuit(player);
            }
        }
        
        // Update current players
        currentPlayers.clear();
        currentPlayers.addAll(newPlayers);
        
        // Notify about full list update
        for (LobbyEventListener listener : eventListeners) {
            listener.onPlayerListUpdate(new ArrayList<>(currentPlayers));
        }
    }
    
    /**
     * Add a lobby event listener
     * @param listener The listener to add
     */
    public void addListener(LobbyEventListener listener) {
        eventListeners.add(listener);
    }
    
    /**
     * Remove a lobby event listener
     * @param listener The listener to remove
     */
    public void removeListener(LobbyEventListener listener) {
        eventListeners.remove(listener);
    }
    
    /**
     * Get the current lobby ID
     * @return The current lobby ID
     */
    public String getCurrentLobby() {
        return currentLobby;
    }
    
    /**
     * Check if currently in a game
     * @return true if in a game, false otherwise
     */
    public boolean isInGame() {
        return inGame;
    }
    
    /**
     * Get the current set of players in the lobby
     * @return Unmodifiable set of player names
     */
    public Set<String> getCurrentPlayers() {
        return new HashSet<>(currentPlayers);
    }
    
    /**
     * Clear all tracked players
     */
    public void clearPlayers() {
        currentPlayers.clear();
        
        // Notify listeners
        for (LobbyEventListener listener : eventListeners) {
            listener.onPlayerListUpdate(new ArrayList<>());
        }
    }
    
    /**
     * Interface for lobby event listeners
     */
    public interface LobbyEventListener {
        /**
         * Called when the lobby changes
         * @param oldLobby The old lobby ID
         * @param newLobby The new lobby ID
         */
        void onLobbyChange(String oldLobby, String newLobby);
        
        /**
         * Called when a player joins the lobby
         * @param playerName The name of the player who joined
         */
        void onPlayerJoin(String playerName);
        
        /**
         * Called when a player leaves the lobby
         * @param playerName The name of the player who left
         */
        void onPlayerQuit(String playerName);
        
        /**
         * Called when a player is eliminated (final kill)
         * @param playerName The name of the player who was eliminated
         */
        void onPlayerEliminated(String playerName);
        
        /**
         * Called when the player list is updated
         * @param players The new list of players
         */
        void onPlayerListUpdate(List<String> players);
        
        /**
         * Called when a game starts
         */
        void onGameStart();
        
        /**
         * Called when a team is eliminated
         */
        void onTeamEliminated();
        
        /**
         * Called when the user is eliminated
         */
        void onUserEliminated();
        
        /**
         * Called when the user returns to the main lobby
         */
        default void onReturnToLobby() {}
        
        /**
         * Called when the user joins a queue
         * @param gameType The type of game queued for
         */
        default void onQueueJoin(String gameType) {}
        
        /**
         * Called when the game mode is detected
         * @param gameMode The detected game mode
         */
        default void onGameModeDetected(String gameMode) {}
        
        /**
         * Called when the user is assigned to a team
         * @param team The team name
         */
        default void onTeamAssignment(String team) {}
        
        /**
         * Called a game countdown happens
         * @param seconds Seconds remaining until game start
         */
        default void onGameCountdown(int seconds) {}
        
        /**
         * Called a game ends
         */
        default void onGameEnd() {}
        
        // New event handlers
        
        /**
         * Called when a bed is destroyed
         * @param teamColor The color of the team whose bed was destroyed
         * @param destroyer The player who destroyed the bed
         * @param bedNumber The bed number (counter)
         */
        default void onBedDestruction(String teamColor, String destroyer, int bedNumber) {}
        
        /**
         * Called when the player's bed is destroyed
         */
        default void onBedDestroyed() {}
        
        /**
         * Called during respawn countdown
         * @param seconds Seconds remaining until respawn
         */
        default void onRespawnCountdown(int seconds) {}
        
        /**
         * Called when the player respawns
         */
        default void onRespawned() {}
        
        /**
         * Called a player gets a final kill with counter
         * @param victim The player who was killed
         * @param killer The player who got the kill
         * @param killCount The killer's final kill count
         */
        default void onPlayerFinalKill(String victim, String killer, int killCount) {}
        
        /**
         * Called when the player purchases an item
         * @param item The item purchased
         * @param currency The currency used
         */
        default void onItemPurchase(String item, String currency) {}
        
        /**
         * Called when a player purchases an upgrade
         * @param player The player who purchased the upgrade
         * @param upgrade The upgrade purchased
         */
        default void onUpgradePurchase(String player, String upgrade) {}
        
        /**
         * Called when the player gains experience
         * @param amount The amount of experience gained
         * @param type The type of experience
         * @param reason The reason for gaining experience
         */
        default void onExperienceGain(int amount, String type, String reason) {}
        
        /**
         * Called when the player gains tokens
         * @param amount The amount of tokens gained
         * @param reason The reason for gaining tokens
         */
        default void onTokensGain(int amount, String reason) {}
        
        /**
         * Called when the player gains a resource
         * @param amount The amount gained
         * @param resource The resource type (Iron, Gold, etc.)
         */
        default void onResourceGain(int amount, String resource) {}
        
        /**
         * Called when the player gains tickets
         * @param amount The amount of tickets gained
         * @param type The type of tickets
         * @param reason The reason for gaining tickets
         */
        default void onTicketGain(int amount, String type, String reason) {}
        
        /**
         * Called when the player deposits an item into the team chest
         * @param item The item deposited
         */
        default void onItemDeposit(String item) {}
        
        /**
         * Called when a player dies
         * @param player The player who died
         * @param deathType The type of death
         * @param killer The killer (null if not applicable)
         */
        default void onPlayerDeath(String player, String deathType, String killer) {}
        
        /**
         * Called when a cross-teaming warning is displayed
         */
        default void onCrossTeamingWarning() {}
        
        /**
         * Called when a team chat message is detected
         * @param team The team color
         * @param level The level indicator
         * @param message The chat message
         */
        default void onTeamChat(String team, String level, String message) {}
        
        /**
         * Called when connecting to a server
         * @param server The server address
         * @param port The server port
         */
        default void onServerConnection(String server, int port) {}
        
        /**
         * Called when a player reconnected
         * @param player The player who reconnected
         */
        default void onPlayerReconnect(String player) {}
        
        /**
         * Called when reconnecting to a game
         * @param destination The destination game
         */
        default void onReconnect(String destination) {}
        
        /**
         * Called when a party warp happens
         * @param warper The player who warped
         * @param destination The destination game
         */
        default void onPartyWarp(String warper, String destination) {}
    }
} 