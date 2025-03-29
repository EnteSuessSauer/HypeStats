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
    
    // Bedwars stats
    private double bedwarsLevel;
    private int bedwarsWins;
    private int bedwarsLosses;
    private double bedwarsWLRatio;
    private int bedwarsKills;
    private int bedwarsDeaths;
    private double bedwarsKDRatio;
    private int bedwarsFinalKills;
    private int bedwarsFinalDeaths;
    private double bedwarsFinalKDRatio;
    private Integer bedwarsWinstreak; // May be null if hidden
    private int bedwarsBedsBroken;
    private int bedwarsBedsLost;
    private int bedwarsGamesPlayed;
    
    // Skywars stats
    private double skywarsLevel;
    private int skywarsWins;
    private int skywarsLosses;
    private double skywarsWLRatio;
    private int skywarsKills;
    private int skywarsDeaths;
    private double skywarsKDRatio;
    private int skywarsCoins;
    private int skywarsGamesPlayed;
    
    // Duels stats
    private int duelsWins;
    private int duelsLosses;
    private double duelsWLRatio;
    private int duelsKills;
    private int duelsDeaths;
    private double duelsKDRatio;
    private int duelsCoins;
    private int duelsGamesPlayed;
    
    // Murder Mystery stats
    private int mmWins;
    private int mmGamesPlayed;
    private int mmKills;
    private int mmDeaths;
    private double mmKDRatio;
    private int mmCoins;
    
    // Arcade stats
    private int arcadeWins;
    private int arcadeCoins;
    
    // SkyBlock stats
    private int skyblockCoins;
    private int skyblockPurse;
    private int skyblockBank;
    private int skyblockFarmingLevel;
    private int skyblockMiningLevel;
    private int skyblockCombatLevel;
    private int skyblockForagingLevel;
    private int skyblockFishingLevel;
    private int skyblockEnchantingLevel;
    private int skyblockAlchemyLevel;
    private int skyblockTamingLevel;
    private String skyblockProfile;
    
    // TNT Games stats
    private int tntgamesWins;
    private int tntgamesCoins;
    private int tntRunWins;
    private int tntRunRecord;
    private int bowSpleefWins;
    private int wizardsWins;
    private int pvpRunWins;
    
    // UHC stats
    private int uhcWins;
    private int uhcKills;
    private int uhcDeaths;
    private double uhcKDRatio;
    private int uhcCoins;
    private int uhcScore;
    
    // Build Battle stats
    private int buildBattleWins;
    private int buildBattleGamesPlayed;
    private int buildBattleScore;
    private int buildBattleCoins;
    
    // Mega Walls stats
    private int megaWallsWins;
    private int megaWallsKills;
    private int megaWallsDeaths;
    private double megaWallsKDRatio;
    private int megaWallsAssists;
    private int megaWallsFinalKills;
    private int megaWallsFinalDeaths;
    private double megaWallsFinalKDRatio;
    
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
        
        // PvP stats (high priority)
        // Bedwars stats
        addStatIfSignificant("Bedwars Final K/D", bedwarsFinalKDRatio, "bedwarsFinalKDRatio", "%.2f");
        addStatIfSignificant("Bedwars K/D", bedwarsKDRatio, "bedwarsKDRatio", "%.2f");
        addStatIfSignificant("Bedwars W/L", bedwarsWLRatio, "bedwarsWLRatio", "%.2f");
        addStatIfSignificant("Bedwars Level", bedwarsLevel, "bedwarsLevel", "%.0f✫");
        
        // Skywars stats
        addStatIfSignificant("Skywars K/D", skywarsKDRatio, "skywarsKDRatio", "%.2f");
        addStatIfSignificant("Skywars W/L", skywarsWLRatio, "skywarsWLRatio", "%.2f");
        addStatIfSignificant("Skywars Level", skywarsLevel, "skywarsLevel", "%.0f");
        
        // Duels stats
        addStatIfSignificant("Duels K/D", duelsKDRatio, "duelsKDRatio", "%.2f");
        addStatIfSignificant("Duels W/L", duelsWLRatio, "duelsWLRatio", "%.2f");
        
        // Murder Mystery stats
        addStatIfSignificant("Murder Mystery K/D", mmKDRatio, "mmKDRatio", "%.2f");
        
        // UHC stats
        addStatIfSignificant("UHC K/D", uhcKDRatio, "uhcKDRatio", "%.2f");
        
        // Mega Walls stats
        addStatIfSignificant("Mega Walls K/D", megaWallsKDRatio, "megaWallsKDRatio", "%.2f");
        addStatIfSignificant("Mega Walls Final K/D", megaWallsFinalKDRatio, "megaWallsFinalKDRatio", "%.2f");
        
        // TNT Games stats
        addStatIfSignificant("TNT Run Record", tntRunRecord, "tntRunRecord", "%d seconds");
        
        // Build Battle stats
        addStatIfSignificant("Build Battle Score", buildBattleScore, "buildBattleScore", null);
        
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
    
    // Compatibility methods for older code
    public double getLevel() {
        return bedwarsLevel;
    }
    
    public int getWins() {
        return bedwarsWins;
    }
    
    public int getLosses() {
        return bedwarsLosses;
    }
    
    public double getWlRatio() {
        return bedwarsWLRatio;
    }
    
    public int getKills() {
        return bedwarsKills;
    }
    
    public int getDeaths() {
        return bedwarsDeaths;
    }
    
    public double getKdRatio() {
        return bedwarsKDRatio;
    }
    
    public int getFinalKills() {
        return bedwarsFinalKills;
    }
    
    public int getFinalDeaths() {
        return bedwarsFinalDeaths;
    }
    
    public double getFinalKdRatio() {
        return bedwarsFinalKDRatio;
    }
    
    public Integer getWinstreak() {
        return bedwarsWinstreak;
    }
    
    public int getBedsBroken() {
        return bedwarsBedsBroken;
    }
    
    public int getBedsLost() {
        return bedwarsBedsLost;
    }
    
    public int getGamesPlayed() {
        return bedwarsGamesPlayed;
    }
    
    // Formatted getters for display
    public String getFormattedWLRatio() {
        return String.format("%.2f", bedwarsWLRatio);
    }
    
    public String getFormattedKDRatio() {
        return String.format("%.2f", bedwarsKDRatio);
    }
    
    public String getFormattedFinalKDRatio() {
        return String.format("%.2f", bedwarsFinalKDRatio);
    }
    
    public String getFormattedLevel() {
        // Return the highest level from all game modes with indicator
        if (bedwarsLevel > 0 && bedwarsLevel >= skywarsLevel) {
            return String.format("%.0f✫ (Bedwars)", bedwarsLevel);
        } else if (skywarsLevel > 0) {
            return String.format("%.0f⚔ (Skywars)", skywarsLevel);
        } else if (networkLevel > 0) {
            return String.format("%d (Network)", networkLevel);
        }
        return "0";
    }
    
    public String getWinstreakDisplay() {
        return bedwarsWinstreak != null ? bedwarsWinstreak.toString() : "Hidden";
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
    
    // Getters and setters for new fields
    public String getGuildName() {
        return guildName;
    }
    
    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }
    
    public String getGuildRank() {
        return guildRank;
    }
    
    public void setGuildRank(String guildRank) {
        this.guildRank = guildRank;
    }
    
    public String getGuildTag() {
        return guildTag;
    }
    
    public void setGuildTag(String guildTag) {
        this.guildTag = guildTag;
    }
    
    public String getGuildColor() {
        return guildColor;
    }
    
    public void setGuildColor(String guildColor) {
        this.guildColor = guildColor;
    }
    
    public String getCurrentStatus() {
        return currentStatus;
    }
    
    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }
    
    public int getFriendsCount() {
        return friendsCount;
    }
    
    public void setFriendsCount(int friendsCount) {
        this.friendsCount = friendsCount;
    }
    
    public int getAchievementCompletionPercent() {
        return achievementCompletionPercent;
    }
    
    public void setAchievementCompletionPercent(int achievementCompletionPercent) {
        this.achievementCompletionPercent = achievementCompletionPercent;
    }
    
    public Map<String, String> getSocialLinks() {
        return socialLinks;
    }
    
    public void setSocialLinks(Map<String, String> socialLinks) {
        this.socialLinks = socialLinks;
    }
    
    public Map<String, Double> getAdditionalStats() {
        return additionalStats;
    }
    
    public void setAdditionalStats(Map<String, Double> additionalStats) {
        this.additionalStats = additionalStats;
    }
    
    public void addAdditionalStat(String key, double value) {
        this.additionalStats.put(key, value);
    }
    
    public int getPlaytimeSourceCount() {
        return playtimeSourceCount;
    }
    
    public void setPlaytimeSourceCount(int playtimeSourceCount) {
        this.playtimeSourceCount = playtimeSourceCount;
    }
    
    public boolean isPlaytimeEstimated() {
        return playtimeEstimated;
    }
    
    public void setPlaytimeEstimated(boolean playtimeEstimated) {
        this.playtimeEstimated = playtimeEstimated;
    }
    
    // Helper methods
    public boolean hasGuild() {
        return guildName != null && !guildName.isEmpty();
    }
    
    public boolean hasSocialLinks() {
        return !socialLinks.isEmpty();
    }
} 