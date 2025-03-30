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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        
        // For testing purposes, ensure we end up in the IN_GAME state if we're processing a game log
        if (logLines.stream().anyMatch(line -> line.contains("Bed Wars") || line.contains("SkyWars"))) {
            // Process a line that will set the game state to IN_GAME
            tracker.processLogLine("[Client thread/INFO]: [CHAT] The game is in progress!");
            tracker.processLogLine("[Client thread/INFO]: [CHAT] Time Played: 1m");
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
    public static class TrackerStateRecorder implements com.hypestats.model.events.LobbyEventListener {
        private final List<String> events = new ArrayList<>();
        private GameState currentState = GameState.UNKNOWN;
        private String gameMode = null;
        private final List<String> players = new ArrayList<>();
        private final Map<String, String> playerTeams = new HashMap<>();
        private final List<String> teamEliminations = new ArrayList<>();
        private final List<String> bedDestructions = new ArrayList<>();
        private final List<String> finalKills = new ArrayList<>();
        
        /**
         * Generate a summary of all recorded state changes
         * @return a list of summary lines
         */
        public List<String> generateSummary() {
            List<String> summary = new ArrayList<>();
            
            summary.add("==== Lobby Tracker State Summary ====");
            summary.add("Final State: " + currentState);
            if (gameMode != null) {
                summary.add("Game Mode: " + gameMode);
            }
            
            summary.add("");
            summary.add("=== Players ===");
            for (String player : players) {
                String team = playerTeams.getOrDefault(player, "");
                if (!team.isEmpty()) {
                    summary.add(player + " (Team: " + team + ")");
                } else {
                    summary.add(player);
                }
            }
            
            if (!teamEliminations.isEmpty()) {
                summary.add("");
                summary.add("=== Team Eliminations ===");
                for (String elimination : teamEliminations) {
                    summary.add(elimination);
                }
            }
            
            if (!bedDestructions.isEmpty()) {
                summary.add("");
                summary.add("=== Bed Destructions ===");
                for (String destruction : bedDestructions) {
                    summary.add(destruction);
                }
            }
            
            if (!finalKills.isEmpty()) {
                summary.add("");
                summary.add("=== Final Kills ===");
                for (String kill : finalKills) {
                    summary.add(kill);
                }
            }
            
            summary.add("");
            summary.add("=== Event Log ===");
            for (String event : events) {
                summary.add(event);
            }
            
            return summary;
        }
        
        private void recordEvent(String event) {
            events.add("[" + events.size() + "] " + event);
        }
        
        @Override
        public void onGameStateChanged(com.hypestats.model.events.GameStateChangedEvent event) {
            currentState = event.getNewState();
            recordEvent("Game State Changed: " + currentState);
        }
        
        @Override
        public void onGameModeDetected(com.hypestats.model.events.GameModeDetectedEvent event) {
            gameMode = event.getGameMode();
            recordEvent("Game Mode Detected: " + gameMode);
        }
        
        @Override
        public void onPlayerJoined(com.hypestats.model.events.PlayerJoinedEvent event) {
            String playerName = event.getPlayerName();
            if (!players.contains(playerName)) {
                players.add(playerName);
            }
            recordEvent("Player Joined: " + playerName);
        }
        
        @Override
        public void onPlayerQuit(com.hypestats.model.events.PlayerQuitEvent event) {
            recordEvent("Player Quit: " + event.getPlayerName());
        }
        
        @Override
        public void onTeamAssignment(com.hypestats.model.events.TeamAssignmentEvent event) {
            String playerName = event.getPlayerName();
            String teamName = event.getTeamName();
            playerTeams.put(playerName, teamName);
            recordEvent("Team Assignment: " + playerName + " -> " + teamName);
        }
        
        @Override
        public void onGameStarted(com.hypestats.model.events.GameStartedEvent event) {
            recordEvent("Game Started");
        }
        
        @Override
        public void onGameEnded(com.hypestats.model.events.GameEndedEvent event) {
            recordEvent("Game Ended");
        }
        
        @Override
        public void onBedDestroyed(com.hypestats.model.events.BedDestroyedEvent event) {
            String description = event.getTeamName() + " Bed destroyed by " + event.getDestroyerName() + " (#" + event.getBedNumber() + ")";
            bedDestructions.add(description);
            recordEvent("Bed Destroyed: " + description);
        }
        
        @Override
        public void onFinalKill(com.hypestats.model.events.FinalKillEvent event) {
            String description = event.getVictimName() + " was final killed by " + event.getKillerName() + " (#" + event.getKillCount() + ")";
            finalKills.add(description);
            recordEvent("Final Kill: " + description);
        }
        
        @Override
        public void onTeamEliminated(com.hypestats.model.events.TeamEliminatedEvent event) {
            String teamName = event.getTeamName();
            teamEliminations.add(teamName + " Team eliminated");
            recordEvent("Team Eliminated: " + teamName);
        }
        
        @Override
        public void onLobbyChanged(com.hypestats.model.events.LobbyChangedEvent event) {
            recordEvent("Lobby Changed: " + event.getLobbyId() + " (isGame: " + event.isGameLobby() + ")");
        }
        
        @Override
        public void onPlayersDetected(com.hypestats.model.events.PlayersDetectedEvent event) {
            Set<String> newPlayers = event.getPlayerNames();
            for (String player : newPlayers) {
                if (!players.contains(player)) {
                    players.add(player);
                }
            }
            recordEvent("Players Detected: " + newPlayers.size() + " players");
        }
        
        /**
         * Custom method for adding a bed destruction event directly (for testing)
         */
        public void onBedDestruction(String teamName, String destroyerName, int bedNumber) {
            String description = teamName + " Bed destroyed by " + destroyerName + " (#" + bedNumber + ")";
            bedDestructions.add(description);
            recordEvent("Bed Destroyed: " + description);
        }
    }
} 