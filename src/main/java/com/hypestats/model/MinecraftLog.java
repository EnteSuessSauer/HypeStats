package com.hypestats.model;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a log entry from the Minecraft log file
 */
@Data
@AllArgsConstructor
public class MinecraftLog {
    private LocalDateTime timestamp;
    private String message;
    private LogType type;
    
    public enum LogType {
        INFO,
        WARN,
        ERROR
    }
} 