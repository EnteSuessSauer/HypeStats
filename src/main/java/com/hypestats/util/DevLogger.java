package com.hypestats.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Logger for development and testing that logs all events to a separate file
 */
@Slf4j
public class DevLogger {
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PREFIX = "hypestats-dev-";
    private static final String LOG_FILE_EXTENSION = ".log";
    
    private static PrintWriter logWriter;
    private static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();
    private static boolean initialized = false;
    private static Thread loggerThread;
    private static boolean running = false;
    
    /**
     * Initialize the dev logger
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        
        try {
            // Create logs directory if it doesn't exist
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            // Create a new log file with timestamp
            SimpleDateFormat fileTimestamp = new SimpleDateFormat("yyyyMMdd-HHmmss");
            String filename = LOG_FILE_PREFIX + fileTimestamp.format(new Date()) + LOG_FILE_EXTENSION;
            File logFile = new File(logDir.toFile(), filename);
            
            // Open writer
            logWriter = new PrintWriter(new FileWriter(logFile, true), true);
            log.info("Dev logger initialized, writing to: {}", logFile.getAbsolutePath());
            
            // Start background thread to process log queue
            running = true;
            loggerThread = new Thread(() -> {
                while (running) {
                    try {
                        String message = logQueue.poll();
                        if (message != null && logWriter != null) {
                            logWriter.println(message);
                        } else {
                            // Sleep a bit to avoid spinning if queue is empty
                            Thread.sleep(100);
                        }
                    } catch (Exception e) {
                        log.error("Error in dev logger thread", e);
                    }
                }
            });
            loggerThread.setDaemon(true);
            loggerThread.start();
            
            // Mark as initialized
            initialized = true;
            
            // Log initialization
            log("Dev logger started");
            log("Java version: " + System.getProperty("java.version"));
            log("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        } catch (IOException e) {
            log.error("Failed to initialize dev logger", e);
        }
    }
    
    /**
     * Log a message with timestamp
     * @param message Message to log
     */
    public static void log(String message) {
        if (!initialized) {
            return;
        }
        
        String timestamp = timestampFormat.format(new Date());
        String formattedMessage = timestamp + " | " + message;
        
        // Add to queue to be processed by background thread
        logQueue.add(formattedMessage);
        
        // Also log to regular logger
        log.debug("[DEV] {}", message);
    }
    
    /**
     * Log an exception with stack trace
     * @param message Message to log
     * @param throwable Exception to log
     */
    public static void log(String message, Throwable throwable) {
        if (!initialized) {
            return;
        }
        
        log(message + ": " + throwable.getMessage());
        
        // Generate stack trace string
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        
        log("Stack trace:\n" + sb);
    }
    
    /**
     * Shutdown the logger
     */
    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        
        log("Dev logger shutting down");
        
        // Stop background thread
        running = false;
        if (loggerThread != null) {
            try {
                loggerThread.join(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
            loggerThread = null;
        }
        
        // Close writer
        if (logWriter != null) {
            logWriter.close();
            logWriter = null;
        }
        
        initialized = false;
    }
} 