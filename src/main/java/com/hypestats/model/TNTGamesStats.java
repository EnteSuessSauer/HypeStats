package com.hypestats.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * TNT Games specific statistics
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TNTGamesStats extends GameStats {
    private int tntRunWins;
    private int tntRunRecord; // in seconds
    private int bowSpleefWins;
    private int wizardsWins;
    private int pvpRunWins;
    
    /**
     * Get the formatted TNT Run record
     */
    public String getFormattedTNTRunRecord() {
        return tntRunRecord + "s";
    }
    
    /**
     * Check if there are meaningful TNT Games stats to display
     */
    @Override
    public boolean hasStats() {
        return super.hasStats() || tntRunWins > 0 || bowSpleefWins > 0 || 
               wizardsWins > 0 || pvpRunWins > 0 || tntRunRecord > 0;
    }
} 