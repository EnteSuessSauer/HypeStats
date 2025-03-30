package com.hypestats.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a player's statistics from Hypixel
 */
@Data
public class PlayerStats {
    // Player identification
    private String uuid;
    private String username;
    private String rank;
    private String rankColor;
    
    // Network stats
    private int networkLevel;
    private long firstLogin;
    private long lastLogin;
    private long accountAge; // Time between first and last login (in milliseconds)
    private long playtime; // Actual gameplay time (in milliseconds)
    private int playtimeSourceCount; // Number of sources that contributed to playtime calculation
    private boolean playtimeEstimated; // Whether playtime was estimated due to incomplete data
    private int karma;
    private int achievementPoints;
    private int totalRewards;
    private int totalDailyRewards;
    private int rewardStreak;
    private String mostRecentGameType;
    private String recentlyPlayedGame;
    
    // Game-specific statistics
    private Map<String, GameStats> gameStats = new HashMap<>();
    
    // List of highlighted stats for this player
    private List<HighlightedStat> topStats = new ArrayList<>();
    
    // For storing averages to compare against
    private static final Map<String, Double> STAT_AVERAGES = new HashMap<>();
    
    // Guild information
    private String guildName;
    private String guildRank;
    private String guildTag;
    private String guildColor;
    
    // Player status
    private String currentStatus; // Online/Offline with current game
    private int friendsCount;
    private int achievementCompletionPercent;
    
    // Social media links
    private Map<String, String> socialLinks = new HashMap<>();
    
    // Additional game stats (for future expansion)
    private Map<String, Double> additionalStats = new HashMap<>();
    
    static {
        // Initialize average values for different stats
        // These are approximate values for comparison
        // PvP stats (higher value = harder to achieve)
        STAT_AVERAGES.put("bedwarsLevel", 35.0);
        STAT_AVERAGES.put("bedwarsWLRatio", 1.2);
        STAT_AVERAGES.put("bedwarsKDRatio", 1.2);
        STAT_AVERAGES.put("bedwarsFinalKDRatio", 1.0);
        
        STAT_AVERAGES.put("skywarsLevel", 10.0);
        STAT_AVERAGES.put("skywarsWLRatio", 0.7);
        STAT_AVERAGES.put("skywarsKDRatio", 1.2);
        
        STAT_AVERAGES.put("duelsWLRatio", 1.2);
        STAT_AVERAGES.put("duelsKDRatio", 1.2);
        
        STAT_AVERAGES.put("mmKDRatio", 1.0);
        
        // New game mode averages
        STAT_AVERAGES.put("uhcKDRatio", 1.0);
        STAT_AVERAGES.put("megaWallsKDRatio", 1.0);
        STAT_AVERAGES.put("megaWallsFinalKDRatio", 0.9);
        STAT_AVERAGES.put("tntRunRecord", 250.0);
        STAT_AVERAGES.put("buildBattleScore", 1000.0);
        
        // Network stats (lower values to give less weight)
        STAT_AVERAGES.put("networkLevel", 100.0);
        STAT_AVERAGES.put("karma", 10000.0);
        STAT_AVERAGES.put("achievementPoints", 2000.0);
        
        // Account age (in days) - 365 days = 1 year
        STAT_AVERAGES.put("accountAge", 500.0);
    }
    
    /**
     * Add game-specific stats to the player
     * @param gameType Game type identifier
     * @param stats GameStats object
     */
    public void addGameStats(String gameType, GameStats stats) {
        stats.setGameType(gameType);
        gameStats.put(gameType, stats);
    }
    
    /**
     * Get game-specific stats
     * @param gameType Game type identifier
     * @return GameStats object or null if not found
     */
    public GameStats getGameStats(String gameType) {
        return gameStats.get(gameType);
    }
    
    /**
     * Get Bedwars stats
     * @return BedwarsStats or null if not found
     */
    public BedwarsStats getBedwarsStats() {
        GameStats stats = getGameStats("BEDWARS");
        return stats instanceof BedwarsStats ? (BedwarsStats) stats : null;
    }
    
    /**
     * Get Skywars stats
     * @return GameStats or null if not found
     */
    public GameStats getSkywarsStats() {
        return getGameStats("SKYWARS");
    }
    
    /**
     * Get Duels stats
     * @return GameStats or null if not found
     */
    public GameStats getDuelsStats() {
        return getGameStats("DUELS");
    }
    
    /**
     * Get Murder Mystery stats
     * @return GameStats or null if not found
     */
    public GameStats getMurderMysteryStats() {
        return getGameStats("MURDER_MYSTERY");
    }
    
    /**
     * Get TNT Games stats
     * @return TNTGamesStats or null if not found
     */
    public TNTGamesStats getTNTGamesStats() {
        GameStats stats = getGameStats("TNTGAMES");
        return stats instanceof TNTGamesStats ? (TNTGamesStats) stats : null;
    }
    
    /**
     * Get UHC stats
     * @return GameStats or null if not found
     */
    public GameStats getUHCStats() {
        return getGameStats("UHC");
    }
    
    /**
     * Get Build Battle stats
     * @return GameStats or null if not found
     */
    public GameStats getBuildBattleStats() {
        return getGameStats("BUILD_BATTLE");
    }
    
    /**
     * Get Mega Walls stats
     * @return MegaWallsStats or null if not found
     */
    public MegaWallsStats getMegaWallsStats() {
        GameStats stats = getGameStats("MEGA_WALLS");
        return stats instanceof MegaWallsStats ? (MegaWallsStats) stats : null;
    }
    
    /**
     * Get SkyBlock stats
     * @return SkyBlockStats or null if not found
     */
    public SkyBlockStats getSkyBlockStats() {
        GameStats stats = getGameStats("SKYBLOCK");
        return stats instanceof SkyBlockStats ? (SkyBlockStats) stats : null;
    }
    
    /**
     * Calculate the top stats for this player and populate the topStats list
     */
    public void calculateTopStats() {
        topStats.clear();
        
        // Check account age (prioritize experienced players)
        if (firstLogin > 0) {
            long currentTime = System.currentTimeMillis();
            long ageMillis = currentTime - firstLogin;
            long days = ageMillis / (1000 * 60 * 60 * 24);
            addStatIfSignificant("Account Age", days, "accountAge", "%d days");
        }
        
        // Add stats from each game mode
        for (Map.Entry<String, GameStats> entry : gameStats.entrySet()) {
            String gameType = entry.getKey();
            GameStats stats = entry.getValue();
            
            // Skip if no meaningful stats
            if (!stats.hasStats()) continue;
            
            // Add common stats
            if (stats.getLevel() > 0) {
                String gamePrefix = getGamePrefix(gameType);
                addStatIfSignificant(gamePrefix + " Level", stats.getLevel(), gameType.toLowerCase() + "Level", "%.0f");
            }
            
            if (stats.getWlRatio() > 0) {
                String gamePrefix = getGamePrefix(gameType);
                addStatIfSignificant(gamePrefix + " W/L", stats.getWlRatio(), gameType.toLowerCase() + "WLRatio", "%.2f");
            }
            
            if (stats.getKdRatio() > 0) {
                String gamePrefix = getGamePrefix(gameType);
                addStatIfSignificant(gamePrefix + " K/D", stats.getKdRatio(), gameType.toLowerCase() + "KDRatio", "%.2f");
            }
            
            // Game-specific special stats
            if (stats instanceof BedwarsStats) {
                BedwarsStats bedwarsStats = (BedwarsStats) stats;
                if (bedwarsStats.getFinalKDRatio() > 0) {
                    addStatIfSignificant("Bedwars Final K/D", bedwarsStats.getFinalKDRatio(), "bedwarsFinalKDRatio", "%.2f");
                }
            } else if (stats instanceof MegaWallsStats) {
                MegaWallsStats megaWallsStats = (MegaWallsStats) stats;
                if (megaWallsStats.getFinalKDRatio() > 0) {
                    addStatIfSignificant("Mega Walls Final K/D", megaWallsStats.getFinalKDRatio(), "megaWallsFinalKDRatio", "%.2f");
                }
            } else if (stats instanceof TNTGamesStats) {
                TNTGamesStats tntStats = (TNTGamesStats) stats;
                if (tntStats.getTntRunRecord() > 0) {
                    addStatIfSignificant("TNT Run Record", tntStats.getTntRunRecord(), "tntRunRecord", "%d seconds");
                }
            }
            
            // Add score if applicable
            if (stats.getScore() > 0) {
                String gamePrefix = getGamePrefix(gameType);
                addStatIfSignificant(gamePrefix + " Score", stats.getScore(), gameType.toLowerCase() + "Score", null);
            }
        }
        
        // Network stats (lower priority)
        addStatIfSignificant("Network Level", networkLevel, "networkLevel", null);
        addStatIfSignificant("Achievement Points", achievementPoints, "achievementPoints", null);
        addStatIfSignificant("Karma", karma, "karma", null);
        
        // Sort by significance and take the top 3
        topStats.sort(Comparator.comparing(HighlightedStat::getSignificance).reversed());
        while (topStats.size() > 3) {
            topStats.remove(topStats.size() - 1);
        }
    }
    
    /**
     * Get formatted game prefix for display
     */
    private String getGamePrefix(String gameType) {
        switch (gameType) {
            case "BEDWARS": return "Bedwars";
            case "SKYWARS": return "Skywars";
            case "DUELS": return "Duels";
            case "MURDER_MYSTERY": return "Murder Mystery";
            case "TNTGAMES": return "TNT Games";
            case "UHC": return "UHC";
            case "BUILD_BATTLE": return "Build Battle";
            case "MEGA_WALLS": return "Mega Walls";
            case "SKYBLOCK": return "SkyBlock";
            default: return gameType;
        }
    }
    
    /**
     * Add a stat to the highlighted stats list if it's significantly better than average
     * @param name Stat name
     * @param value Stat value
     * @param key Key to lookup average
     * @param format Optional format string
     */
    private void addStatIfSignificant(String name, double value, String key, String format) {
        if (value <= 0) return; // Skip if not available
        
        Double average = STAT_AVERAGES.get(key);
        if (average == null) return;
        
        double significance = value / average;
        if (significance >= 1.5) { // At least 50% better than average
            HighlightedStat stat = new HighlightedStat();
            stat.setName(name);
            stat.setValue(value);
            stat.setSignificance(significance);
            stat.setFormat(format);
            topStats.add(stat);
        }
    }
    
    // Formatted getters for display
    public String getFormattedLevel() {
        // Return the highest level from all game modes with indicator
        BedwarsStats bedwarsStats = getBedwarsStats();
        GameStats skywarsStats = getSkywarsStats();
        
        double bedwarsLevel = bedwarsStats != null ? bedwarsStats.getLevel() : 0;
        double skywarsLevel = skywarsStats != null ? skywarsStats.getLevel() : 0;
        
        if (bedwarsLevel > 0 && bedwarsLevel >= skywarsLevel) {
            return String.format("%.0f✫ (Bedwars)", bedwarsLevel);
        } else if (skywarsLevel > 0) {
            return String.format("%.0f⚔ (Skywars)", skywarsLevel);
        } else if (networkLevel > 0) {
            return String.format("%d (Network)", networkLevel);
        }
        return "0";
    }
    
    public String getNetworkLevelFormatted() {
        return String.format("%d", networkLevel);
    }
    
    /**
     * Get the account age formatted in a human-readable way
     * @return Formatted account age
     */
    public String getFormattedAccountAge() {
        // Return account age
        if (firstLogin <= 0) {
            return "Unknown";
        }
        
        long currentTime = System.currentTimeMillis();
        long ageMillis = currentTime - firstLogin;
        
        // Convert to days
        long days = ageMillis / (1000 * 60 * 60 * 24);
        
        if (days > 365) {
            long years = days / 365;
            long remainingDays = days % 365;
            return String.format("%d year%s, %d day%s", 
                years, years != 1 ? "s" : "", 
                remainingDays, remainingDays != 1 ? "s" : "");
        } else {
            return String.format("%d day%s", days, days != 1 ? "s" : "");
        }
    }
    
    /**
     * Get the playtime formatted in a human-readable way
     * @return Formatted playtime
     */
    public String getFormattedPlaytime() {
        if (playtime <= 0) {
            return "Unknown";
        }
        
        // Convert milliseconds to hours
        long hours = playtime / (1000 * 60 * 60);
        
        if (hours > 24) {
            long days = hours / 24;
            long remainingHours = hours % 24;
            return String.format("%d day%s, %d hour%s", 
                days, days != 1 ? "s" : "", 
                remainingHours, remainingHours != 1 ? "s" : "");
        } else {
            return String.format("%d hour%s", hours, hours != 1 ? "s" : "");
        }
    }
    
    /**
     * Returns the significance color for a stat compared to average
     * @param value Stat value
     * @param key Stat key for average lookup
     * @return CSS class for the stat
     */
    public String getStatSignificanceClass(double value, String key) {
        if (value <= 0) return "stat-average";
        
        Double average = STAT_AVERAGES.get(key);
        if (average == null) return "stat-average";
        
        double significance = value / average;
        
        if (significance >= 2.0) return "stat-exceptional";
        if (significance >= 1.5) return "stat-great";
        if (significance >= 1.2) return "stat-good";
        if (significance >= 0.8) return "stat-average";
        if (significance >= 0.5) return "stat-below-average";
        return "stat-poor";
    }
    
    /**
     * Class to represent a highlighted stat
     */
    @Data
    public static class HighlightedStat {
        private String name;
        private double value;
        private double significance;
        private String format;
        
        public String getFormattedValue() {
            if (format != null) {
                if (name.equals("Account Age") && value > 365) {
                    // Format account age in years and days for better readability
                    long days = (long) value;
                    long years = days / 365;
                    long remainingDays = days % 365;
                    return String.format("%d year%s, %d day%s", 
                        years, years != 1 ? "s" : "", 
                        remainingDays, remainingDays != 1 ? "s" : "");
                }
                return String.format(format, value);
            }
            return value % 1 == 0 ? String.format("%.0f", value) : String.format("%.2f", value);
        }
    }
    
    // Helper methods
    public boolean hasGuild() {
        return guildName != null && !guildName.isEmpty();
    }
    
    public boolean hasSocialLinks() {
        return !socialLinks.isEmpty();
    }

    /**
     * Gets the number of friends the player has
     * @return The number of friends, or 0 if not set
     */
    public int getFriendsCount() {
        return friendsCount;
    }
} 