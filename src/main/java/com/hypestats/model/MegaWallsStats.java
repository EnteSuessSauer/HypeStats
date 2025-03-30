package com.hypestats.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Mega Walls specific game statistics
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MegaWallsStats extends GameStats {
    private int assists;
    private int finalKills;
    private int finalDeaths;
    private int finalAssists;
    private double kdRatio;
    private double finalKdRatio;
    
    /**
     * Calculate derived statistics specific to Mega Walls
     */
    @Override
    public void calculateDerivedStats() {
        super.calculateDerivedStats();
        
        // Calculate KD ratio if deaths are available
        this.kdRatio = getDeaths() > 0 ? (double) getKills() / getDeaths() : getKills();
        
        // Calculate Final KD ratio if final deaths are available
        this.finalKdRatio = finalDeaths > 0 ? (double) finalKills / finalDeaths : finalKills;
    }
    
    /**
     * Get the final K/D ratio as a formatted string
     */
    public String getFormattedFinalKDRatio() {
        return String.format("%.2f", finalKdRatio);
    }

    // Getters and setters
    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public int getFinalKills() {
        return finalKills;
    }

    public void setFinalKills(int finalKills) {
        this.finalKills = finalKills;
    }

    public int getFinalDeaths() {
        return finalDeaths;
    }

    public void setFinalDeaths(int finalDeaths) {
        this.finalDeaths = finalDeaths;
    }

    public int getFinalAssists() {
        return finalAssists;
    }

    public void setFinalAssists(int finalAssists) {
        this.finalAssists = finalAssists;
    }

    public double getKdRatio() {
        return kdRatio;
    }

    public void setKdRatio(double kdRatio) {
        this.kdRatio = kdRatio;
    }

    public double getFinalKDRatio() {
        return finalKdRatio;
    }

    public void setFinalKDRatio(double finalKdRatio) {
        this.finalKdRatio = finalKdRatio;
    }
} 