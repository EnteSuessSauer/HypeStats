package com.hypestats.model;

import lombok.Data;

/**
 * Base class for game mode statistics
 */
@Data
public class GameStats {
    // Common statistics across game modes
    private String gameType;
    private int wins;
    private int losses;
    private double wlRatio;
    private int kills;
    private int deaths;
    private double kdRatio;
    private int coins;
    private int gamesPlayed;
    private double level;
    private Integer winstreak; // May be hidden/null
    
    // Additional stats (varies by game)
    private int score;
    
    /**
     * Calculate derived statistics like ratios
     */
    public void calculateDerivedStats() {
        // Calculate win/loss ratio
        wlRatio = losses > 0 ? (double) wins / losses : wins;
        
        // Calculate kill/death ratio
        kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        
        // Ensure gamesPlayed is set if it wasn't provided directly
        if (gamesPlayed == 0 && (wins > 0 || losses > 0)) {
            gamesPlayed = wins + losses;
        }
    }
    
    /**
     * Formatted stat getters for display
     */
    public String getFormattedWLRatio() {
        return String.format("%.2f", wlRatio);
    }
    
    public String getFormattedKDRatio() {
        return String.format("%.2f", kdRatio);
    }
    
    public String getFormattedLevel() {
        return String.format("%.0f", level);
    }
    
    public String getWinstreakDisplay() {
        return winstreak != null ? winstreak.toString() : "Hidden";
    }
    
    /**
     * Check if this game mode has meaningful stats to display
     * @return true if there are meaningful stats
     */
    public boolean hasStats() {
        return wins > 0 || kills > 0 || gamesPlayed > 0 || coins > 0 || score > 0 || level > 0;
    }
} 