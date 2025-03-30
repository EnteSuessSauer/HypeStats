package com.hypestats.model;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for reading and processing Minecraft log files for testing
 */
@Slf4j
public class LogProcessor {
    
    /**
     * Load a test log file from the resources directory
     * @param fileName the name of the log file (without the path)
     * @return a list of log lines
     */
    public static List<String> loadLogFile(String fileName) {
        try (InputStream is = LogProcessor.class.getClassLoader().getResourceAsStream("logs/" + fileName)) {
            if (is == null) {
                log.error("Could not find log file: {}", fileName);
                return new ArrayList<>();
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.toList());
            }
        } catch (IOException e) {
            log.error("Failed to read log file: {}", fileName, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Process a log file with a LobbyTracker and return the final state
     * @param fileName the name of the log file (without the path)
     * @return a populated LobbyTracker with the final state
     */
    public static LobbyTracker processLogFile(String fileName) {
        List<String> logLines = loadLogFile(fileName);
        return processLogLines(logLines);
    }
    
    /**
     * Process a list of log lines with a LobbyTracker and return the final state
     * @param logLines the list of log lines to process
     * @return a populated LobbyTracker with the final state
     */
    public static LobbyTracker processLogLines(List<String> logLines) {
        LobbyTracker tracker = new LobbyTracker();
        
        for (String line : logLines) {
            tracker.processLogLine(line);
        }
        
        return tracker;
    }
    
    /**
     * Process a log file with a LobbyTracker and generate a summary output file
     * @param inputFileName the name of the input log file (without the path)
     * @param outputFileName the name of the output summary file (without the path)
     * @return true if successful, false otherwise
     */
    public static boolean processAndGenerateSummary(String inputFileName, String outputFileName) {
        List<String> logLines = loadLogFile(inputFileName);
        if (logLines.isEmpty()) {
            return false;
        }
        
        return processAndGenerateSummary(logLines, outputFileName);
    }
    
    /**
     * Process a list of log lines with a LobbyTracker and generate a summary output file
     * @param logLines the list of log lines to process
     * @param outputFileName the name of the output summary file (without the path)
     * @return true if successful, false otherwise
     */
    public static boolean processAndGenerateSummary(List<String> logLines, String outputFileName) {
        LobbyTracker tracker = new LobbyTracker();
        TrackerStateRecorder recorder = new TrackerStateRecorder();
        tracker.addListener(recorder);
        
        for (String line : logLines) {
            tracker.processLogLine(line);
        }
        
        List<String> summaryLines = recorder.generateSummary();
        
        try {
            Path outputDir = Paths.get("target", "test-output");
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve(outputFileName);
            Files.write(outputFile, summaryLines, StandardCharsets.UTF_8);
            log.info("Wrote summary to: {}", outputFile);
            return true;
        } catch (IOException e) {
            log.error("Failed to write summary file: {}", outputFileName, e);
            return false;
        }
    }
    
    /**
     * A LobbyEventListener that records state changes for testing
     */
    public static class TrackerStateRecorder implements LobbyTracker.LobbyEventListener {
        private final List<String> events = new ArrayList<>();
        private String currentLobby = "";
        private boolean inGame = false;
        private final List<String> players = new ArrayList<>();
        private String gameMode = "";
        private String team = "";
        
        /**
         * Generate a summary of the recorded events
         * @return a list of summary lines
         */
        public List<String> generateSummary() {
            List<String> summary = new ArrayList<>();
            summary.add("=== Lobby Tracker State Summary ===");
            summary.add("Current Lobby: " + currentLobby);
            summary.add("In Game: " + inGame);
            summary.add("Game Mode: " + gameMode);
            summary.add("Team: " + team);
            summary.add("Players (" + players.size() + "):");
            players.forEach(player -> summary.add("  - " + player));
            summary.add("");
            summary.add("=== Event Log ===");
            events.forEach(event -> summary.add(event));
            return summary;
        }
        
        /**
         * Get the list of recorded events
         * @return the list of events
         */
        public List<String> getEvents() {
            return events;
        }
        
        /**
         * Get the current lobby
         * @return the current lobby
         */
        public String getCurrentLobby() {
            return currentLobby;
        }
        
        /**
         * Check if the player is in a game
         * @return true if in a game, false otherwise
         */
        public boolean isInGame() {
            return inGame;
        }
        
        /**
         * Get the list of tracked players
         * @return the list of players
         */
        public List<String> getPlayers() {
            return players;
        }
        
        /**
         * Get the detected game mode
         * @return the game mode
         */
        public String getGameMode() {
            return gameMode;
        }
        
        /**
         * Get the player's assigned team
         * @return the team
         */
        public String getTeam() {
            return team;
        }

        @Override
        public void onLobbyChange(String oldLobby, String newLobby) {
            events.add("Lobby Change: " + oldLobby + " -> " + newLobby);
            currentLobby = newLobby;
        }

        @Override
        public void onPlayerJoin(String playerName) {
            events.add("Player Join: " + playerName);
            if (!players.contains(playerName)) {
                players.add(playerName);
            }
        }

        @Override
        public void onPlayerQuit(String playerName) {
            events.add("Player Quit: " + playerName);
            players.remove(playerName);
        }

        @Override
        public void onPlayerEliminated(String playerName) {
            events.add("Player Eliminated: " + playerName);
        }

        @Override
        public void onPlayerListUpdate(List<String> players) {
            events.add("Player List Update: " + players.size() + " players");
            this.players.clear();
            this.players.addAll(players);
        }

        @Override
        public void onGameStart() {
            events.add("Game Start");
            inGame = true;
        }

        @Override
        public void onTeamEliminated() {
            events.add("Team Eliminated");
        }

        @Override
        public void onUserEliminated() {
            events.add("User Eliminated");
        }

        @Override
        public void onReturnToLobby() {
            events.add("Return to Lobby");
            inGame = false;
        }

        @Override
        public void onQueueJoin(String gameType) {
            events.add("Queue Join: " + gameType);
        }

        @Override
        public void onGameModeDetected(String gameMode) {
            events.add("Game Mode Detected: " + gameMode);
            this.gameMode = gameMode;
        }

        @Override
        public void onTeamAssignment(String team) {
            events.add("Team Assignment: " + team);
            this.team = team;
        }

        @Override
        public void onGameCountdown(int seconds) {
            events.add("Game Countdown: " + seconds + " seconds");
        }

        @Override
        public void onGameEnd() {
            events.add("Game End");
        }

        @Override
        public void onBedDestruction(String teamColor, String destroyer, int bedNumber) {
            events.add("Bed Destruction: " + teamColor + " bed destroyed by " + destroyer + " (#" + bedNumber + ")");
        }

        @Override
        public void onBedDestroyed() {
            events.add("Your Bed Destroyed");
        }

        @Override
        public void onRespawnCountdown(int seconds) {
            events.add("Respawn Countdown: " + seconds + " seconds");
        }

        @Override
        public void onRespawned() {
            events.add("Respawned");
        }

        @Override
        public void onPlayerFinalKill(String victim, String killer, int killCount) {
            events.add("Final Kill: " + victim + " was " + killer + "'s final #" + killCount);
        }

        @Override
        public void onItemPurchase(String item, String currency) {
            events.add("Item Purchase: " + item + " with " + currency);
        }

        @Override
        public void onUpgradePurchase(String player, String upgrade) {
            events.add("Upgrade Purchase: " + player + " purchased " + upgrade);
        }

        @Override
        public void onExperienceGain(int amount, String type, String reason) {
            events.add("Experience Gain: +" + amount + " " + type + " (" + reason + ")");
        }

        @Override
        public void onTokensGain(int amount, String reason) {
            events.add("Tokens Gain: +" + amount + " tokens (" + reason + ")");
        }

        @Override
        public void onResourceGain(int amount, String resource) {
            events.add("Resource Gain: +" + amount + " " + resource);
        }

        @Override
        public void onTicketGain(int amount, String type, String reason) {
            events.add("Ticket Gain: +" + amount + " " + type + " tickets (" + reason + ")");
        }

        @Override
        public void onItemDeposit(String item) {
            events.add("Item Deposit: " + item + " into team chest");
        }

        @Override
        public void onPlayerDeath(String player, String deathType, String killer) {
            if (killer != null) {
                events.add("Player Death: " + player + " was " + deathType + " by " + killer);
            } else {
                events.add("Player Death: " + player + " " + deathType);
            }
        }

        @Override
        public void onCrossTeamingWarning() {
            events.add("Cross Teaming Warning");
        }

        @Override
        public void onTeamChat(String team, String level, String message) {
            events.add("Team Chat [" + team + "] [" + level + "]: " + message);
        }

        @Override
        public void onServerConnection(String server, int port) {
            events.add("Server Connection: " + server + ":" + port);
        }

        @Override
        public void onPlayerReconnect(String player) {
            events.add("Player Reconnect: " + player);
        }

        @Override
        public void onReconnect(String destination) {
            events.add("Reconnect to: " + destination);
        }

        @Override
        public void onPartyWarp(String warper, String destination) {
            events.add("Party Warp: " + warper + " warped to " + destination);
        }
    }
} 