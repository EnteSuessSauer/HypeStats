package com.hypestats.model;

import lombok.Data;

/**
 * Represents a player's Bedwars statistics from Hypixel
 */
@Data
public class PlayerStats {
    private String uuid;
    private String username;
    private double level;
    private int wins;
    private int losses;
    private double wlRatio;
    private int kills;
    private int deaths;
    private double kdRatio;
    private int finalKills;
    private int finalDeaths;
    private double finalKdRatio;
    private Integer winstreak; // May be null if hidden
    private int bedsBroken;
    private int bedsLost;
    private int gamesPlayed;
    private String rank;
    private String rankColor;
    
    // For JavaFX TableView compatibility
    public String getFormattedWLRatio() {
        return String.format("%.2f", wlRatio);
    }
    
    public String getFormattedKDRatio() {
        return String.format("%.2f", kdRatio);
    }
    
    public String getFormattedFinalKDRatio() {
        return String.format("%.2f", finalKdRatio);
    }
    
    public String getFormattedLevel() {
        return String.format("%.0f✫", level);
    }
    
    public String getWinstreakDisplay() {
        return winstreak != null ? winstreak.toString() : "Hidden";
    }
} 