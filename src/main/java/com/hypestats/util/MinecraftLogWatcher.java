package com.hypestats.util;

import com.hypestats.HypeStatsApp;
import com.hypestats.model.MinecraftLog;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

/**
 * Watches a Minecraft log file for player joins and other events
 */
@Slf4j
public class MinecraftLogWatcher {

    private static final Pattern LOG_PATTERN = Pattern.compile("\\[(\\d{2}:\\d{2}:\\d{2})\\] \\[.+?\\]: (.*)");
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("ONLINE: (.+)");
    private static final Pattern LOBBY_JOIN_PATTERN = Pattern.compile(".*has joined \\((?:.|\\d)+/\\d+\\)!$");
    
    private final String logFilePath;
    private WatchService watchService;
    private Thread watcherThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private final List<Consumer<String>> playerListeners = new ArrayList<>();
    private final List<Consumer<MinecraftLog>> logListeners = new ArrayList<>();
    private final Random random = new Random();
    
    // Mock data for test mode
    private static final String[] TEST_USERNAMES = {
        "Technoblade", "Dream", "GeorgeNotFound", "Sapnap", "BadBoyHalo", 
        "Skeppy", "TommyInnit", "Tubbo", "Philza", "Wilbur", 
        "Fundy", "Quackity", "Purpled", "Awesamdude", "CaptainPuffy"
    };
    
    private static final String[] TEST_LOG_MESSAGES = {
        "Connecting to server...",
        "Preparing spawn area: 0%",
        "Preparing spawn area: 90%",
        "Time elapsed: 2 s",
        "Joining world...",
        "Loading resource packs...",
        "Loaded 15 advancements",
        "ONLINE: {PLAYERS}",
        "Player {PLAYER} has joined (16/24)!",
        "You are playing on server mode.bedwars161",
        "The game starts in 10 seconds!",
        "The game has started!",
        "[MVP+] {PLAYER}: anyone want to team?",
        "[VIP] {PLAYER}: where is everyone?",
        "Your bed was destroyed by {PLAYER}!",
        "{PLAYER} has been eliminated!",
        "VICTORY! {PLAYER} team wins!"
    };
    
    /**
     * Create a new MinecraftLogWatcher
     * @param logFilePath Path to the Minecraft log file
     */
    public MinecraftLogWatcher(String logFilePath) {
        this.logFilePath = logFilePath;
        
        if (HypeStatsApp.isTestMode()) {
            try {
                DevLogger.log("LogWatcher: Created with path: " + logFilePath);
            } catch (Exception e) {
                log.error("Error logging in test mode", e);
            }
        }
    }
    
    /**
     * Add a player listener that will be called when a new player is detected
     * @param listener Consumer that accepts a player name
     */
    public void addPlayerListener(Consumer<String> listener) {
        playerListeners.add(listener);
        
        if (HypeStatsApp.isTestMode()) {
            try {
                DevLogger.log("LogWatcher: Player listener added");
            } catch (Exception e) {
                log.error("Error logging in test mode", e);
            }
        }
    }
    
    /**
     * Add a log listener that will be called for each log line
     * @param listener Consumer that accepts a MinecraftLog
     */
    public void addLogListener(Consumer<MinecraftLog> listener) {
        logListeners.add(listener);
        
        if (HypeStatsApp.isTestMode()) {
            try {
                DevLogger.log("LogWatcher: Log listener added");
            } catch (Exception e) {
                log.error("Error logging in test mode", e);
            }
        }
    }
    
    /**
     * Start monitoring the log file
     * @return true if monitoring started successfully, false otherwise
     */
    public boolean start() {
        if (running.get()) {
            log.info("Log watcher already running");
            return true;
        }
        
        if (HypeStatsApp.isTestMode()) {
            try {
                DevLogger.log("LogWatcher: Starting in TEST MODE");
                
                // In test mode, we'll generate fake log entries and player data
                running.set(true);
                watcherThread = new Thread(this::runTestMode);
                watcherThread.setDaemon(true);
                watcherThread.start();
                
                return true;
            } catch (Exception e) {
                log.error("Error starting test mode log watcher", e);
                if (HypeStatsApp.isTestMode()) {
                    DevLogger.log("LogWatcher: Error starting in test mode", e);
                }
                return false;
            }
        }
        
        try {
            // Create file if it doesn't exist
            Path logPath = Paths.get(logFilePath);
            if (!Files.exists(logPath)) {
                log.warn("Log file does not exist: {}", logFilePath);
                return false;
            }
            
            // Create watch service
            watchService = FileSystems.getDefault().newWatchService();
            Path parent = logPath.getParent();
            parent.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            
            // Start monitoring thread
            running.set(true);
            watcherThread = new Thread(this::monitorLogFile);
            watcherThread.setDaemon(true);
            watcherThread.start();
            
            log.info("Started monitoring log file: {}", logFilePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to start log watcher", e);
            return false;
        }
    }
    
    /**
     * Generate simulated log entries and player data in test mode
     */
    private void runTestMode() {
        try {
            DevLogger.log("LogWatcher: Test mode thread started");
            
            // First generate some initial log entries
            for (int i = 0; i < 5; i++) {
                String message = getRandomTestLogMessage();
                processTestLogMessage(message);
                Thread.sleep(random.nextInt(500) + 300);
            }
            
            // Then add some players to the lobby
            int playerCount = random.nextInt(6) + 5; // 5-10 players
            DevLogger.log("LogWatcher: Adding " + playerCount + " test players");
            
            String playerList = "";
            for (int i = 0; i < playerCount; i++) {
                if (i > 0) playerList += ", ";
                playerList += getRandomTestUsername();
            }
            
            processTestLogMessage("ONLINE: " + playerList);
            
            // Continue with random log messages while running
            while (running.get()) {
                // 10% chance to add a new player
                if (random.nextInt(100) < 10) {
                    String player = getRandomTestUsername();
                    processTestLogMessage(player + " has joined (16/24)!");
                }
                
                // Generate a random log message
                String message = getRandomTestLogMessage();
                processTestLogMessage(message);
                
                // Wait a bit before next log
                Thread.sleep(random.nextInt(3000) + 1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            DevLogger.log("LogWatcher: Test mode thread interrupted");
        } catch (Exception e) {
            log.error("Error in test mode log generation", e);
            DevLogger.log("LogWatcher: Error in test log generation", e);
        }
    }
    
    /**
     * Process a test log message
     */
    private void processTestLogMessage(String message) {
        LocalDateTime timestamp = LocalDateTime.now();
        MinecraftLog logEntry = new MinecraftLog(timestamp, message, MinecraftLog.LogType.INFO);
        
        // Notify log listeners
        for (Consumer<MinecraftLog> listener : logListeners) {
            try {
                listener.accept(logEntry);
            } catch (Exception e) {
                log.error("Error in log listener", e);
                if (HypeStatsApp.isTestMode()) {
                    DevLogger.log("LogWatcher: Error in log listener", e);
                }
            }
        }
        
        // Check for players in the message
        if (message.startsWith("ONLINE:")) {
            String playerList = message.substring(8).trim();
            String[] players = playerList.split(", ");
            for (String player : players) {
                notifyPlayerListeners(player);
            }
        } else if (message.contains("has joined")) {
            String player = message.split(" has joined")[0];
            notifyPlayerListeners(player);
        }
    }
    
    /**
     * Get a random username from the test data
     */
    private String getRandomTestUsername() {
        return TEST_USERNAMES[random.nextInt(TEST_USERNAMES.length)];
    }
    
    /**
     * Get a random log message from the test data
     */
    private String getRandomTestLogMessage() {
        String message = TEST_LOG_MESSAGES[random.nextInt(TEST_LOG_MESSAGES.length)];
        
        // Replace placeholders with random player names
        if (message.contains("{PLAYER}")) {
            message = message.replace("{PLAYER}", getRandomTestUsername());
        }
        
        if (message.contains("{PLAYERS}")) {
            int count = random.nextInt(5) + 1;
            StringBuilder players = new StringBuilder();
            for (int i = 0; i < count; i++) {
                if (i > 0) players.append(", ");
                players.append(getRandomTestUsername());
            }
            message = message.replace("{PLAYERS}", players.toString());
        }
        
        return message;
    }
    
    /**
     * Stop monitoring the log file
     */
    public void stop() {
        running.set(false);
        
        if (watcherThread != null) {
            watcherThread.interrupt();
            watcherThread = null;
        }
        
        if (watchService != null) {
            try {
                watchService.close();
                watchService = null;
            } catch (IOException e) {
                log.error("Error closing watch service", e);
            }
        }
        
        log.info("Stopped monitoring log file");
        
        if (HypeStatsApp.isTestMode()) {
            try {
                DevLogger.log("LogWatcher: Stopped");
            } catch (Exception e) {
                log.error("Error logging in test mode", e);
            }
        }
    }
    
    /**
     * Check if the watcher is running
     * @return true if the watcher is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Monitor the log file for changes
     */
    private void monitorLogFile() {
        Path logPath = Paths.get(logFilePath);
        String fileName = logPath.getFileName().toString();
        long lastPos = 0;
        
        try {
            // Initial read to catch existing content
            lastPos = processLogFile(logPath, lastPos);
            
            // Watch for changes
            while (running.get()) {
                WatchKey key = watchService.take();
                
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // Skip overflow events
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        continue;
                    }
                    
                    // Check if this is our log file
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path changedPath = ev.context();
                    
                    if (changedPath.getFileName().toString().equals(fileName)) {
                        lastPos = processLogFile(logPath, lastPos);
                    }
                }
                
                // Reset the key - required to continue receiving events
                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            // Thread was interrupted, exit gracefully
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.error("Error monitoring log file", e);
            if (HypeStatsApp.isTestMode()) {
                DevLogger.log("LogWatcher: Error monitoring log file", e);
            }
        }
    }
    
    /**
     * Process the log file from the given position
     * @param logPath Path to the log file
     * @param lastPos Last position read from the file
     * @return New last position
     * @throws IOException If an error occurs reading the file
     */
    private long processLogFile(Path logPath, long lastPos) throws IOException {
        // Check if file has been truncated or rotated
        long fileSize = Files.size(logPath);
        if (fileSize < lastPos) {
            lastPos = 0;
        }
        
        // Read new content
        List<String> newLines = readNewLines(logPath, lastPos);
        
        // Process new lines
        for (String line : newLines) {
            processLogLine(line);
        }
        
        return fileSize;
    }
    
    /**
     * Read new lines from the log file
     * @param logPath Path to the log file
     * @param lastPos Last position read from the file
     * @return List of new lines
     * @throws IOException If an error occurs reading the file
     */
    private List<String> readNewLines(Path logPath, long lastPos) throws IOException {
        List<String> lines = new ArrayList<>();
        
        List<String> allLines = Files.readAllLines(logPath);
        
        // Approximation: if lastPos is 0, read all lines
        // Otherwise, read the last few lines assuming they're new
        if (lastPos == 0) {
            return allLines;
        } else {
            // This is an approximation since we don't have the exact line numbers
            int approximateNewLines = 20; // Read last 20 lines as an estimate
            int startIndex = Math.max(0, allLines.size() - approximateNewLines);
            return allLines.subList(startIndex, allLines.size());
        }
    }
    
    /**
     * Process a single log line
     * @param line Log line to process
     */
    private void processLogLine(String line) {
        Matcher logMatcher = LOG_PATTERN.matcher(line);
        
        if (logMatcher.matches()) {
            String timeStr = logMatcher.group(1);
            String message = logMatcher.group(2);
            
            // Parse timestamp
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            LocalDateTime timestamp = LocalDateTime.now().withHour(Integer.parseInt(timeStr.substring(0, 2)))
                    .withMinute(Integer.parseInt(timeStr.substring(3, 5)))
                    .withSecond(Integer.parseInt(timeStr.substring(6, 8)));
            
            // Create log entry
            MinecraftLog logEntry = new MinecraftLog(timestamp, message, MinecraftLog.LogType.INFO);
            
            // Notify log listeners
            for (Consumer<MinecraftLog> listener : logListeners) {
                listener.accept(logEntry);
            }
            
            // Check for player joins
            checkForPlayerJoin(message);
        }
    }
    
    /**
     * Check for player joins in the log message
     * @param message Log message to check
     */
    private void checkForPlayerJoin(String message) {
        // Check for ONLINE: pattern (used in some server plugins)
        Matcher onlineMatcher = PLAYER_JOIN_PATTERN.matcher(message);
        if (onlineMatcher.matches()) {
            String playerList = onlineMatcher.group(1);
            String[] players = playerList.split(", ");
            
            for (String player : players) {
                notifyPlayerListeners(player);
            }
            return;
        }
        
        // Check for lobby join pattern
        Matcher lobbyMatcher = LOBBY_JOIN_PATTERN.matcher(message);
        if (lobbyMatcher.matches()) {
            // Extract player name from "PlayerName has joined (x/y)!"
            String playerName = message.split(" has joined")[0];
            notifyPlayerListeners(playerName);
        }
    }
    
    /**
     * Notify player listeners of a new player
     * @param playerName Player name to notify about
     */
    private void notifyPlayerListeners(String playerName) {
        for (Consumer<String> listener : playerListeners) {
            try {
                listener.accept(playerName);
                if (HypeStatsApp.isTestMode()) {
                    DevLogger.log("LogWatcher: Notified listener about player: " + playerName);
                }
            } catch (Exception e) {
                log.error("Error in player listener for player: {}", playerName, e);
                if (HypeStatsApp.isTestMode()) {
                    DevLogger.log("LogWatcher: Error in player listener for: " + playerName, e);
                }
            }
        }
    }
} 