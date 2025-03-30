package com.hypestats.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

/**
 * Utility class for finding Minecraft log files from various launchers and clients
 */
@Slf4j
public class MinecraftClientLogFinder {
    
    /**
     * Represents a detected Minecraft log file
     */
    public static class LogFileInfo {
        private final String clientName;
        private final String filePath;
        private final File file;
        private final long lastModified;
        
        public LogFileInfo(String clientName, String filePath) {
            this.clientName = clientName;
            this.filePath = filePath;
            this.file = new File(filePath);
            this.lastModified = file.lastModified();
        }
        
        public String getClientName() {
            return clientName;
        }
        
        public String getFilePath() {
            return filePath;
        }
        
        public File getFile() {
            return file;
        }
        
        public long getLastModified() {
            return lastModified;
        }
        
        public boolean exists() {
            return file.exists();
        }
        
        @Override
        public String toString() {
            return clientName + ": " + filePath;
        }
    }
    
    private static final String USER_HOME = System.getProperty("user.home");
    
    // Known Minecraft client log paths for Windows
    private static final Map<String, String> WINDOWS_CLIENT_PATHS = new LinkedHashMap<>();
    static {
        // Default Minecraft Launcher
        WINDOWS_CLIENT_PATHS.put("Minecraft", Paths.get(USER_HOME, "AppData", "Roaming", ".minecraft", "logs", "latest.log").toString());
        
        // BadLion Client
        WINDOWS_CLIENT_PATHS.put("BadLion", Paths.get(USER_HOME, "AppData", "Roaming", ".minecraft", "logs", "blclient", "minecraft", "latest.log").toString());
        
        // Lunar Client
        WINDOWS_CLIENT_PATHS.put("Lunar", Paths.get(USER_HOME, ".lunarclient", "offline", "multiver", "logs", "latest.log").toString());
        WINDOWS_CLIENT_PATHS.put("Lunar (Alt)", Paths.get(USER_HOME, "AppData", "Roaming", ".minecraft", "logs", "lunar", "latest.log").toString());
        
        // Forge
        WINDOWS_CLIENT_PATHS.put("Forge", Paths.get(USER_HOME, "AppData", "Roaming", ".minecraft", "logs", "fml-client-latest.log").toString());
        
        // Feather Client
        WINDOWS_CLIENT_PATHS.put("Feather", Paths.get(USER_HOME, "AppData", "Roaming", ".feather", "logs", "latest.log").toString());
        
        // LabyMod
        WINDOWS_CLIENT_PATHS.put("LabyMod", Paths.get(USER_HOME, "AppData", "Roaming", ".minecraft", "logs", "labymod-1", "latest.log").toString());
        
        // Badlion Legacy
        WINDOWS_CLIENT_PATHS.put("BadLion Legacy", Paths.get(USER_HOME, "AppData", "Roaming", "BadlionClient", "logs", "client.log").toString());
    }
    
    // Known Minecraft client log paths for macOS
    private static final Map<String, String> MAC_CLIENT_PATHS = new LinkedHashMap<>();
    static {
        // Default Minecraft Launcher
        MAC_CLIENT_PATHS.put("Minecraft", Paths.get(USER_HOME, "Library", "Application Support", "minecraft", "logs", "latest.log").toString());
        
        // BadLion Client
        MAC_CLIENT_PATHS.put("BadLion", Paths.get(USER_HOME, "Library", "Application Support", "minecraft", "logs", "blclient", "minecraft", "latest.log").toString());
        
        // Lunar Client
        MAC_CLIENT_PATHS.put("Lunar", Paths.get(USER_HOME, ".lunarclient", "offline", "multiver", "logs", "latest.log").toString());
        
        // Forge
        MAC_CLIENT_PATHS.put("Forge", Paths.get(USER_HOME, "Library", "Application Support", "minecraft", "logs", "fml-client-latest.log").toString());
    }
    
    // Known Minecraft client log paths for Linux
    private static final Map<String, String> LINUX_CLIENT_PATHS = new LinkedHashMap<>();
    static {
        // Default Minecraft Launcher
        LINUX_CLIENT_PATHS.put("Minecraft", Paths.get(USER_HOME, ".minecraft", "logs", "latest.log").toString());
        
        // BadLion Client
        LINUX_CLIENT_PATHS.put("BadLion", Paths.get(USER_HOME, ".minecraft", "logs", "blclient", "minecraft", "latest.log").toString());
        
        // Lunar Client
        LINUX_CLIENT_PATHS.put("Lunar", Paths.get(USER_HOME, ".lunarclient", "offline", "multiver", "logs", "latest.log").toString());
        
        // Forge
        LINUX_CLIENT_PATHS.put("Forge", Paths.get(USER_HOME, ".minecraft", "logs", "fml-client-latest.log").toString());
    }
    
    /**
     * Find all available log files
     * @return List of LogFileInfo objects for all detected log files
     */
    public static List<LogFileInfo> findAllLogFiles() {
        List<LogFileInfo> logFiles = new ArrayList<>();
        Map<String, String> clientPaths;
        
        // Determine OS-specific paths
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            clientPaths = WINDOWS_CLIENT_PATHS;
        } else if (os.contains("mac")) {
            clientPaths = MAC_CLIENT_PATHS;
        } else {
            clientPaths = LINUX_CLIENT_PATHS;
        }
        
        // Check each path
        for (Map.Entry<String, String> entry : clientPaths.entrySet()) {
            String clientName = entry.getKey();
            String logPath = entry.getValue();
            File logFile = new File(logPath);
            
            if (logFile.exists() && logFile.isFile()) {
                logFiles.add(new LogFileInfo(clientName, logPath));
                log.info("Found log file for {}: {}", clientName, logPath);
            }
        }
        
        // Sort by last modified date (newest first)
        logFiles.sort(Comparator.comparingLong(LogFileInfo::getLastModified).reversed());
        
        return logFiles;
    }
    
    /**
     * Find the most recently updated log file
     * @return The most recently updated LogFileInfo or null if none found
     */
    public static LogFileInfo findMostRecentLogFile() {
        List<LogFileInfo> logFiles = findAllLogFiles();
        return logFiles.isEmpty() ? null : logFiles.get(0);
    }
    
    /**
     * Find the default log file for the current OS
     * @return LogFileInfo for the default log file or null if not found
     */
    public static LogFileInfo findDefaultLogFile() {
        String defaultPath = SettingsManager.getDefaultLogPath();
        File defaultFile = new File(defaultPath);
        
        if (defaultFile.exists()) {
            String clientName = "Minecraft";
            return new LogFileInfo(clientName, defaultPath);
        }
        
        return null;
    }
    
    /**
     * Check if a log file exists at the specified path
     * @param logPath Path to check
     * @return true if file exists and is readable, false otherwise
     */
    public static boolean logFileExists(String logPath) {
        if (logPath == null || logPath.isEmpty()) {
            return false;
        }
        
        File file = new File(logPath);
        return file.exists() && file.isFile() && file.canRead();
    }
} 