package com.hypestats.model;

/**
 * Class representing Build Battle game statistics
 */
public class BuildBattleStats extends GameStats {
    private int score;
    private double winRate;

    public BuildBattleStats() {
        super();
    }

    @Override
    public void calculateDerivedStats() {
        // Calculate win rate if games played is available
        this.winRate = getGamesPlayed() > 0 ? (double) getWins() / getGamesPlayed() : 0;
    }

    // Getters and setters
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public double getWinRate() {
        return winRate;
    }

    public void setWinRate(double winRate) {
        this.winRate = winRate;
    }
} 