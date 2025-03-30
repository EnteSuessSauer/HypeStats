package com.hypestats.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Bedwars specific game statistics
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BedwarsStats extends GameStats {
    private int finalKills;
    private int finalDeaths;
    private double finalKDRatio;
    private int bedsBroken;
    private int bedsLost;
    private double bedRatio;
    
    /**
     * Calculate derived statistics specific to Bedwars
     */
    @Override
    public void calculateDerivedStats() {
        super.calculateDerivedStats();
        
        // Calculate final kill/death ratio
        finalKDRatio = finalDeaths > 0 ? (double) finalKills / finalDeaths : finalKills;
        
        // Calculate bed ratio
        bedRatio = bedsLost > 0 ? (double) bedsBroken / bedsLost : bedsBroken;
    }
    
    /**
     * Get the formatted final K/D ratio
     */
    public String getFormattedFinalKDRatio() {
        return String.format("%.2f", finalKDRatio);
    }
    
    /**
     * Get the formatted bed ratio
     */
    public String getFormattedBedRatio() {
        return String.format("%.2f", bedRatio);
    }
} 