package com.hypestats.model;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GameStateDetector - Flexible game state detection using keywords instead of strict regex
 * This class helps determine the current Hypixel game mode and lobby state 
 * through keyword matching from chat messages.
 */
@Slf4j
public class GameStateDetector {
    // Game mode keywords
    private static final Set<String> BEDWARS_KEYWORDS = new HashSet<>(Arrays.asList(
            "BED WARS", "BEDWARS", "BED WAR", "BED DESTRUCTION", "BED BROKEN", 
            "YOUR BED WAS DESTROYED", "BEDS DESTROYED", "PROTECT YOUR BED", 
            "IRON FORGE", "DIAMOND GENERATOR", "DREAM DEFENDER"
    ));
    
    private static final Set<String> SKYWARS_KEYWORDS = new HashSet<>(Arrays.asList(
            "SKYWARS", "SKY WARS", "SKY WAR", "INSANE MODE", "NORMAL MODE", 
            "MEGA MODE", "REFILL IN", "CHEST REFILL", "EGGS AND SNOWBALLS", 
            "MYSTIC WELLS", "CHESTS HAVE BEEN REFILLED"
    ));
    
    private static final Set<String> DUELS_KEYWORDS = new HashSet<>(Arrays.asList(
            "DUELS", "DUEL", "VERSUS", "VS", "1V1", "OPPONENT:", 
            "THE BRIDGE", "BOXING", "CLASSIC DUEL", "UHC DUEL", "BOW DUEL", 
            "SUMO DUEL", "WINNER:", "STARTING DUEL"
    ));
    
    // Keywords for in-game detection (not specific to game mode)
    private static final Set<String> INGAME_KEYWORDS = new HashSet<>(Arrays.asList(
            "GAME STARTED", "GAME HAS STARTED", "STARTS IN", "GAME BEGINS", 
            "JOINED TEAM", "TEAM BLUE", "TEAM RED", "TEAM GREEN", "TEAM YELLOW", 
            "TEAM AQUA", "TEAM PINK", "TEAM WHITE", "TEAM GRAY", "VICTORY", 
            "WINNER:", "DEFEATED", "KILL", "FINAL KILL", "DEFEATED BY", "TIME PLAYED",
            "EXPERIENCE", "CROSS-TEAMING", "CROSS TEAMING"
    ));
    
    // Keywords for lobby detection
    private static final Set<String> LOBBY_KEYWORDS = new HashSet<>(Arrays.asList(
            "WELCOME TO HYPIXEL", "SENDING YOU TO", "YOU ARE CURRENTLY CONNECTED TO", 
            "JOINED THE LOBBY", "YOU HAVE JOINED THE QUEUE", "WARPED TO LOBBY", 
            "SENDING TO LIMBO", "QUEUE: ", "LIMBO", "HUB", "MAIN LOBBY"
    ));
    
    // Keywords for player tracking
    private static final Set<String> PLAYER_TRACKING_KEYWORDS = new HashSet<>(Arrays.asList(
            "HAS JOINED", "HAS QUIT", "JOINED THE GAME", "LEFT THE GAME", 
            "ONLINE:", "PLAYERS:", "JOINED (", "/", "PLAYER LIST", 
            "FELL INTO THE VOID", "WAS KILLED BY", "WAS SHOT BY", "WAS SLAIN BY", 
            "FINAL KILL", "DIED"
    ));
    
    /**
     * Determine if a log line indicates a game mode
     * @param logLine The log line to check
     * @return The detected game mode or null if none detected
     */
    public static String detectGameMode(String logLine) {
        String upperLine = logLine.toUpperCase();
        
        // Check for BedWars keywords
        for (String keyword : BEDWARS_KEYWORDS) {
            if (upperLine.contains(keyword)) {
                log.debug("Detected BedWars from keyword: {}", keyword);
                return "Bed Wars";
            }
        }
        
        // Check for SkyWars keywords
        for (String keyword : SKYWARS_KEYWORDS) {
            if (upperLine.contains(keyword)) {
                log.debug("Detected SkyWars from keyword: {}", keyword);
                return "SkyWars";
            }
        }
        
        // Check for Duels keywords
        for (String keyword : DUELS_KEYWORDS) {
            if (upperLine.contains(keyword)) {
                log.debug("Detected Duels from keyword: {}", keyword);
                return "Duels";
            }
        }
        
        return null;
    }
    
    /**
     * Determine if a log line indicates being in a game
     * @param logLine The log line to check
     * @return true if in game, false otherwise
     */
    public static boolean isInGameIndicator(String logLine) {
        String upperLine = logLine.toUpperCase();
        
        // First check explicit game mode keywords
        if (detectGameMode(logLine) != null) {
            return true;
        }
        
        // Check generic in-game keywords
        for (String keyword : INGAME_KEYWORDS) {
            if (upperLine.contains(keyword)) {
                log.debug("Detected in-game state from keyword: {}", keyword);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determine if a log line indicates being in a lobby
     * @param logLine The log line to check
     * @return true if in lobby, false otherwise
     */
    public static boolean isInLobbyIndicator(String logLine) {
        String upperLine = logLine.toUpperCase();
        
        for (String keyword : LOBBY_KEYWORDS) {
            if (upperLine.contains(keyword)) {
                log.debug("Detected lobby state from keyword: {}", keyword);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determine if a log line indicates a server/lobby change
     * @param logLine The log line to check
     * @return the lobby ID if detected, null otherwise
     */
    public static String detectLobbyChange(String logLine) {
        String lowerLine = logLine.toLowerCase();
        
        // Common lobby change patterns
        if (lowerLine.contains("sending you to ")) {
            int startIndex = lowerLine.indexOf("sending you to ") + "sending you to ".length();
            int endIndex = lowerLine.indexOf("!", startIndex);
            if (endIndex > startIndex) {
                return lowerLine.substring(startIndex, endIndex);
            }
        }
        
        // Check for warping patterns
        if (lowerLine.contains("warped to ")) {
            int startIndex = lowerLine.indexOf("warped to ") + "warped to ".length();
            int endIndex = lowerLine.indexOf("!", startIndex);
            if (endIndex == -1) endIndex = lowerLine.length();
            if (endIndex > startIndex) {
                return lowerLine.substring(startIndex, endIndex);
            }
        }
        
        // Check for connecting pattern
        if (lowerLine.contains("connecting to ")) {
            int startIndex = lowerLine.indexOf("connecting to ") + "connecting to ".length();
            int endIndex = lowerLine.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = lowerLine.length();
            if (endIndex > startIndex) {
                return lowerLine.substring(startIndex, endIndex);
            }
        }
        
        return null;
    }
    
    /**
     * Extract player names from a log line
     * @param logLine The log line to check
     * @return List of player names or empty list if none found
     */
    public static List<String> extractPlayers(String logLine) {
        // Extracted players will be handled in the LobbyTracker class
        // This method will be called by LobbyTracker to interpret chat lines
        return List.of();
    }
    
    /**
     * Determine if a lobby ID appears to be a game lobby
     * @param lobbyId The lobby ID to check
     * @return true if this appears to be a game lobby, false otherwise
     */
    public static boolean isGameLobby(String lobbyId) {
        if (lobbyId == null || lobbyId.isEmpty()) {
            return false;
        }
        
        String lowerLobbyId = lobbyId.toLowerCase();
        
        // Check for known game lobby patterns - now more flexible
        if (lowerLobbyId.contains("mini") || 
            lowerLobbyId.contains("game") || 
            lowerLobbyId.matches("\\w{4,8}\\d{1,4}") || // alpha-numeric pattern
            // Common game-specific patterns
            lowerLobbyId.contains("bed") || 
            lowerLobbyId.contains("sky") || 
            lowerLobbyId.contains("duel") ||
            lowerLobbyId.contains("bw") || 
            lowerLobbyId.contains("sw")) {
            return true;
        }
        
        // Most hub lobbies have "lobby" in the name
        if (lowerLobbyId.contains("lobby") || 
            lowerLobbyId.contains("hub") || 
            lowerLobbyId.contains("limbo") ||
            lowerLobbyId.contains("afk") ||
            lowerLobbyId.contains("queue")) {
            return false;
        }
        
        // Additional pattern: short lobby IDs with numbers and letters are usually game lobbies
        if (lobbyId.length() <= 10 && lobbyId.matches(".*\\d.*") && !lobbyId.equals(lobbyId.toUpperCase())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if a line contains a player list (e.g., from /who command)
     * @param logLine The log line to check
     * @return true if this appears to be a player list
     */
    public static boolean isPlayerList(String logLine) {
        String lowerLine = logLine.toLowerCase();
        
        return lowerLine.contains("online:") || 
               lowerLine.contains("players:") || 
               (lowerLine.contains("who") && lowerLine.contains(":")) ||
               (lowerLine.contains(",") && lowerLine.contains("(") && lowerLine.contains(")"));
    }
} 