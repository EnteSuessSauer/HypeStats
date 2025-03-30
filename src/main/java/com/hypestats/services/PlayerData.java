package com.hypestats.services;

import lombok.Builder;
import lombok.Data;

/**
 * Represents player statistics and data from the Hypixel API.
 */
@Data
@Builder
public class PlayerData {
    private String playerName;
    private String uuid;
    private String rank;
    
    // General stats
    private int level;
    private int achievementPoints;
    private String firstLogin;
    private String lastLogin;
    
    // BedWars specific stats
    private int bedwarsLevel;
    private int bedwarsWins;
    private int bedwarsLosses;
    private int bedwarsKills;
    private int bedwarsFinalKills;
    private int bedwarsDeaths;
    private int bedwarsFinalDeaths;
    private int bedwarsBedsBroken;
    private int bedwarsBedsLost;
    private double bedwarsWinLossRatio;
    private double bedwarsKillDeathRatio;
    private double bedwarsFinalKillDeathRatio;
    
    // SkyWars specific stats
    private int skywarsLevel;
    private int skywarsWins;
    private int skywarsLosses;
    private int skywarsKills;
    private int skywarsDeaths;
    private double skywarsWinLossRatio;
    private double skywarsKillDeathRatio;
    
    // Duels specific stats
    private int duelsWins;
    private int duelsLosses;
    private int duelsKills;
    private int duelsDeaths;
    private double duelsWinLossRatio;
    private double duelsKillDeathRatio;
    
    /**
     * Calculate a win/loss ratio safely (avoiding division by zero)
     */
    public static double calculateRatio(int numerator, int denominator) {
        return denominator == 0 ? numerator : (double) numerator / denominator;
    }
} 