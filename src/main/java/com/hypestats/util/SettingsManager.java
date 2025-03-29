package com.hypestats.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Manages application settings
 */
@Slf4j
public class SettingsManager {
    private static final String SETTINGS_DIR = ".hypestats";
    private static final String SETTINGS_FILE = "settings.properties";
    private static final String API_KEY = "hypixel.api.key";
    private static final String LOG_PATH = "minecraft.log.path";
    
    private final Properties properties;
    private final File settingsFile;
    
    private static SettingsManager instance;
    
    /**
     * Get the singleton instance of SettingsManager
     * @return SettingsManager instance
     */
    public static synchronized SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }
    
    private SettingsManager() {
        properties = new Properties();
        
        // Create settings directory in user home if it doesn't exist
        Path settingsDir = Paths.get(System.getProperty("user.home"), SETTINGS_DIR);
        try {
            if (!Files.exists(settingsDir)) {
                Files.createDirectories(settingsDir);
            }
        } catch (IOException e) {
            log.error("Failed to create settings directory: {}", settingsDir, e);
        }
        
        // Load settings from file
        settingsFile = settingsDir.resolve(SETTINGS_FILE).toFile();
        loadSettings();
    }
    
    /**
     * Load settings from the properties file
     */
    private void loadSettings() {
        if (settingsFile.exists()) {
            try (InputStream is = new FileInputStream(settingsFile)) {
                properties.load(is);
                log.info("Settings loaded from {}", settingsFile);
            } catch (IOException e) {
                log.error("Failed to load settings from {}", settingsFile, e);
            }
        }
    }
    
    /**
     * Save settings to the properties file
     */
    private void saveSettings() {
        try (OutputStream os = new FileOutputStream(settingsFile)) {
            properties.store(os, "HypeStats Settings");
            log.info("Settings saved to {}", settingsFile);
        } catch (IOException e) {
            log.error("Failed to save settings to {}", settingsFile, e);
        }
    }
    
    /**
     * Get the Hypixel API key
     * @return API key, or null if not set
     */
    public String getApiKey() {
        return properties.getProperty(API_KEY);
    }
    
    /**
     * Set the Hypixel API key
     * @param apiKey API key to set
     */
    public void setApiKey(String apiKey) {
        properties.setProperty(API_KEY, apiKey);
        saveSettings();
    }
    
    /**
     * Get the Minecraft log file path
     * @return Log file path, or null if not set
     */
    public String getLogPath() {
        return properties.getProperty(LOG_PATH);
    }
    
    /**
     * Set the Minecraft log file path
     * @param logPath Log file path to set
     */
    public void setLogPath(String logPath) {
        properties.setProperty(LOG_PATH, logPath);
        saveSettings();
    }
    
    /**
     * Check if the settings contain a value for the specified key
     * @param key Key to check
     * @return true if the key exists and has a non-empty value, false otherwise
     */
    public boolean hasProperty(String key) {
        return properties.getProperty(key) != null && !properties.getProperty(key).isEmpty();
    }
    
    /**
     * Get the default Minecraft log file path based on the operating system
     * @return Default log file path
     */
    public static String getDefaultLogPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        if (os.contains("win")) {
            return Paths.get(userHome, "AppData", "Roaming", ".minecraft", "logs", "latest.log").toString();
        } else if (os.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", "minecraft", "logs", "latest.log").toString();
        } else {
            // Linux and others
            return Paths.get(userHome, ".minecraft", "logs", "latest.log").toString();
        }
    }
} 