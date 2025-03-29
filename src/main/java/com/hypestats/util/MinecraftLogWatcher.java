package com.hypestats.util;

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
    
    /**
     * Create a new MinecraftLogWatcher
     * @param logFilePath Path to the Minecraft log file
     */
    public MinecraftLogWatcher(String logFilePath) {
        this.logFilePath = logFilePath;
    }
    
    /**
     * Add a player listener that will be called when a new player is detected
     * @param listener Consumer that accepts a player name
     */
    public void addPlayerListener(Consumer<String> listener) {
        playerListeners.add(listener);
    }
    
    /**
     * Add a log listener that will be called for each log line
     * @param listener Consumer that accepts a MinecraftLog
     */
    public void addLogListener(Consumer<MinecraftLog> listener) {
        logListeners.add(listener);
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
            listener.accept(playerName);
        }
    }
} 