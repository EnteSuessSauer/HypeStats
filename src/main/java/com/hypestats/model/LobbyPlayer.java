package com.hypestats.model;

import lombok.Data;

/**
 * Represents a player detected in the current Minecraft lobby
 */
@Data
public class LobbyPlayer {
    private String uuid;
    private String username;
    private boolean loading;
    private PlayerStats stats;
    private String error;
    
    public LobbyPlayer(String username) {
        this.username = username;
        this.loading = true;
    }
    
    public String getStatusDisplay() {
        if (loading) {
            return "Loading...";
        } else if (error != null) {
            return "Error: " + error;
        } else if (stats == null) {
            return "No stats available";
        } else {
            return "Stats loaded";
        }
    }
} 