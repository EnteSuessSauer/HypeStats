package com.hypestats.model;

/**
 * Class representing UHC game statistics
 */
public class UHCStats extends GameStats {
    private int score;
    private double kdRatio;

    public UHCStats() {
        super();
    }

    @Override
    public void calculateDerivedStats() {
        // Calculate KD ratio if deaths are available
        this.kdRatio = getDeaths() > 0 ? (double) getKills() / getDeaths() : getKills();
    }

    // Getters and setters
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getKdRatio() {
        return kdRatio;
    }

    public void setKdRatio(double kdRatio) {
        this.kdRatio = kdRatio;
    }
} 