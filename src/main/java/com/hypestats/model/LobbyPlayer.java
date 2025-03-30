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
    
    // Authenticity fields
    private double nickProbability = 0.0;
    private double altProbability = 0.0;
    private String nickProbabilityText = "Unknown";
    private String altProbabilityText = "Unknown";
    private String authenticityStyleClass = "";
    
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
    
    /**
     * Get a combined authenticity display text
     * @return A string describing the player's authenticity
     */
    public String getAuthenticityDisplay() {
        if (loading || stats == null) {
            return "Unknown";
        }
        
        if (nickProbability > altProbability) {
            return nickProbabilityText;
        } else {
            return altProbabilityText;
        }
    }
} 