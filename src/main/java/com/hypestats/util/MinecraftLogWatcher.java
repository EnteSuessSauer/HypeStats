package com.hypestats.util;

import com.hypestats.HypeStatsApp;
import com.hypestats.model.MinecraftLog;
import com.hypestats.model.LobbyTracker;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;
import static java.nio.file.StandardOpenOption.*;

/**
 * Watches a Minecraft log file for player joins and other events
 */
@Slf4j
public class MinecraftLogWatcher {
    private static final Pattern LOG_PATTERN = Pattern.compile("\\[(\\d{2}:\\d{2}:\\d{2})\\] \\[.+?\\]: (.*)");
    private static final Pattern PLAYER_JOIN_PATTERN = Pattern.compile("ONLINE: (.+)");
    private static final Pattern LOBBY_JOIN_PATTERN = Pattern.compile(".*has joined \\((?:.|\\d)+/\\d+\\)!$");
    
    // Polling interval in milliseconds
    private static final long POLLING_INTERVAL = 3000; // 3 seconds
    
    private final String logFilePath;
    private WatchService watchService;
    private Thread watcherThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private final List<Consumer<String>> playerListeners = new ArrayList<>();
    private final List<Consumer<MinecraftLog>> logListeners = new ArrayList<>();
    private final Random random = new Random();
    
    // New addition: LobbyTracker integration
    private final LobbyTracker lobbyTracker = new LobbyTracker();
    
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
    
    @SuppressWarnings("unused")
    private long timestamp; // Timestamp is used for tracking when log lines were processed
    
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
     * Add a lobby event listener
     * @param listener LobbyTracker.LobbyEventListener
     */
    public void addLobbyListener(com.hypestats.model.events.LobbyEventListener listener) {
        lobbyTracker.addListener(listener);
        
        if (HypeStatsApp.isTestMode()) {
            try {
                DevLogger.log("LogWatcher: Lobby event listener added");
            } catch (Exception e) {
                log.error("Error logging in test mode", e);
            }
        }
    }
    
    /**
     * Remove a lobby event listener
     * @param listener LobbyTracker.LobbyEventListener
     */
    public void removeLobbyListener(com.hypestats.model.events.LobbyEventListener listener) {
        lobbyTracker.removeListener(listener);
    }
    
    /**
     * Get the lobby tracker instance
     * @return LobbyTracker
     */
    public LobbyTracker getLobbyTracker() {
        return lobbyTracker;
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
        
        // Process the log line through the lobby tracker
        lobbyTracker.processLogLine("[Client thread/INFO]: " + message);
        
        // Check for players in the message (legacy support)
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
     * Read new lines from the log file
     * @param logPath Path to the log file
     * @param lastPos Last position read from the file
     * @return List of new lines
     * @throws IOException If an error occurs reading the file
     */
    private List<String> readNewLines(Path logPath, long lastPos) throws IOException {
        List<String> newLines = new ArrayList<>();
        
        try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
            // If lastPos is 0, read from the beginning
            // Otherwise, seek to the last position
            if (lastPos > 0 && lastPos < raf.length()) {
                raf.seek(lastPos);
            }
            
            // Read new lines
            String line;
            while ((line = raf.readLine()) != null) {
                // RandomAccessFile.readLine() returns bytes in platform's default encoding
                // Convert to String with proper encoding
                line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                newLines.add(line);
            }
        } catch (IOException e) {
            log.error("Error reading log file: {}", e.getMessage());
            // Fall back to the old method if RandomAccessFile fails
            return fallbackReadNewLines(logPath, lastPos);
        }
        
        // If we didn't get any new lines but the file size has changed,
        // it might be due to buffering or text encoding issues
        // Try falling back to reading the entire file
        if (newLines.isEmpty() && Files.size(logPath) > lastPos) {
            log.debug("No new lines detected but file size changed, using fallback method");
            return fallbackReadNewLines(logPath, lastPos);
        }
        
        return newLines;
    }
    
    /**
     * Fallback method to read new lines from the log file
     * This is used when RandomAccessFile approach fails
     * @param logPath Path to the log file
     * @param lastPos Last position read from the file
     * @return List of new lines
     * @throws IOException If an error occurs reading the file
     */
    private List<String> fallbackReadNewLines(Path logPath, long lastPos) throws IOException {
        List<String> allLines = Files.readAllLines(logPath);
        
        // If lastPos is 0, read all lines
        if (lastPos == 0) {
            return allLines;
        }
        
        // If lastPos > 0, we need to try and estimate which lines are new
        // Compare file size to lastPos to determine approximate percentage of new content
        long fileSize = Files.size(logPath);
        if (fileSize <= lastPos) {
            // File hasn't grown or has been truncated
            return Collections.emptyList();
        }
        
        double newContentRatio = 1.0 - ((double) lastPos / fileSize);
        int estimatedNewLines = (int) Math.ceil(allLines.size() * newContentRatio);
        
        // Ensure we read at least a minimum number of lines and not more than the file has
        estimatedNewLines = Math.max(20, estimatedNewLines);
        estimatedNewLines = Math.min(allLines.size(), estimatedNewLines);
        
        int startIndex = Math.max(0, allLines.size() - estimatedNewLines);
        return allLines.subList(startIndex, allLines.size());
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
     * Process a single log line
     * @param line Log line to process
     */
    private void processLogLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String message = matcher.group(2);
            
            // Create log entry
            MinecraftLog logEntry = new MinecraftLog(
                    LocalDateTime.now(),
                    message,
                    message.contains("ERROR") ? MinecraftLog.LogType.ERROR : MinecraftLog.LogType.INFO
            );
            
            // Process through lobby tracker
            lobbyTracker.processLogLine(line);
            
            // Notify log listeners
            for (Consumer<MinecraftLog> listener : logListeners) {
                try {
                    listener.accept(logEntry);
                } catch (Exception e) {
                    log.error("Error in log listener", e);
                }
            }
            
            // Legacy support for existing player detection
            checkForPlayerJoin(message);
        }
    }
    
    /**
     * Check for player join in a message
     */
    private void checkForPlayerJoin(String message) {
        // ONLINE: player1, player2, player3
        Matcher matcher = PLAYER_JOIN_PATTERN.matcher(message);
        if (matcher.find()) {
            String playerList = matcher.group(1);
            String[] players = playerList.split(", ");
            for (String player : players) {
                notifyPlayerListeners(player.trim());
            }
            return;
        }
        
        // player has joined (5/12)!
        if (LOBBY_JOIN_PATTERN.matcher(message).matches()) {
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
    
    /**
     * Manually force a refresh of the log file
     * This can be called externally to force reading the file
     * @return True if successful, false otherwise
     */
    public boolean forceRefresh() {
        if (!running.get()) {
            return false;
        }
        
        try {
            Path logPath = Paths.get(logFilePath);
            if (!Files.exists(logPath)) {
                log.warn("Log file does not exist: {}", logFilePath);
                return false;
            }
            
            // Force read with direct I/O to bypass OS caching where possible
            try (FileChannel channel = FileChannel.open(logPath, READ, SYNC)) {
                // Just opening with SYNC can help flush OS buffers
                channel.force(true);
            } catch (Exception e) {
                log.debug("Could not open file with SYNC option, falling back to standard method: {}", e.getMessage());
                // Fallback to standard RandomAccessFile if FileChannel fails
                try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
                    // Just opening and closing the file can help flush buffers
                }
            }
            
            // Update modification time to force refresh in file system
            logPath.toFile().setLastModified(System.currentTimeMillis());
            
            // Use non-cached file reading
            List<String> lines = new ArrayList<>();
            try (RandomAccessFile raf = new RandomAccessFile(logPath.toFile(), "r")) {
                String line;
                while ((line = raf.readLine()) != null) {
                    // Convert line to proper encoding
                    line = new String(line.getBytes("ISO-8859-1"), "UTF-8");
                    lines.add(line);
                }
            }
            
            // Process each line
            for (String line : lines) {
                processLogLine(line);
            }
            
            return true;
        } catch (IOException e) {
            log.error("Error forcing log file refresh", e);
            return false;
        }
    }
    
    /**
     * Monitor the log file for changes
     */
    private void monitorLogFile() {
        Path logPath = Paths.get(logFilePath);
        String fileName = logPath.getFileName().toString();
        long lastPos = 0;
        long lastPollTime = System.currentTimeMillis();
        
        try {
            // Initial read to catch existing content
            lastPos = processLogFile(logPath, lastPos);
            
            // Watch for changes
            while (running.get()) {
                // Wait for events with a timeout to enable polling
                WatchKey key = watchService.poll(POLLING_INTERVAL, java.util.concurrent.TimeUnit.MILLISECONDS);
                
                // Check if we need to poll based on time elapsed
                long currentTime = System.currentTimeMillis();
                boolean shouldPoll = (currentTime - lastPollTime) >= POLLING_INTERVAL;
                
                if (key != null) {
                    // Process events from the watch service
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
                            lastPollTime = currentTime; // Reset poll timer when we process an event
                        }
                    }
                    
                    // Reset the key - required to continue receiving events
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                } else if (shouldPoll) {
                    // No events but polling interval has elapsed
                    log.debug("Polling log file for changes");
                    lastPos = processLogFile(logPath, lastPos);
                    lastPollTime = currentTime;
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
} 