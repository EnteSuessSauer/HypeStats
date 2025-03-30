package com.hypestats.util;

import com.hypestats.model.PlayerStats;
import com.hypestats.model.BedwarsStats;
import com.hypestats.model.GameStats;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for checking the authenticity of player accounts
 * Provides methods to detect if a player is using a nickname (nick) or if they are likely an alt account
 */
@Slf4j
public class PlayerAuthenticityChecker {
    
    // Cache for player nick/alt probability calculations to avoid repeated API calls
    private final Map<String, Double> nickProbabilityCache = new HashMap<>();
    private final Map<String, Double> altProbabilityCache = new HashMap<>();
    
    /**
     * Calculates the probability that a player is using a nick based on their stats
     * @param stats PlayerStats object containing the player's statistics
     * @return A value between 0.0 (certainly real) and 1.0 (certainly nicked)
     */
    public double calculateNickProbability(PlayerStats stats) {
        String username = stats.getUsername();
        
        // Check cache first
        if (nickProbabilityCache.containsKey(username)) {
            return nickProbabilityCache.get(username);
        }
        
        double probability = 0.0;
        
        // Check for common nick indicators
        
        // 1. Network level vs skill discrepancy
        int networkLevel = stats.getNetworkLevel();
        if (networkLevel < 25) {  // Low network level threshold
            // Check if Bedwars stats are suspiciously high for this level
            BedwarsStats bedwarsStats = stats.getBedwarsStats();
            if (bedwarsStats != null) {
                double kdRatio = bedwarsStats.getKdRatio();
                double finalKdRatio = bedwarsStats.getFinalKDRatio();
                double wlRatio = bedwarsStats.getWlRatio();
                
                if (kdRatio > 5.0 && networkLevel < 15) {
                    probability += 0.25;
                }
                
                if (finalKdRatio > 3.0 && networkLevel < 15) {
                    probability += 0.25;
                }
                
                if (wlRatio > 3.0 && networkLevel < 15) {
                    probability += 0.2;
                }
            }
            
            // Check other game modes for consistency
            GameStats skywarsStats = stats.getSkywarsStats();
            if (skywarsStats != null) {
                double kdRatio = skywarsStats.getKdRatio();
                double wlRatio = skywarsStats.getWlRatio();
                
                if (kdRatio > 4.0 && networkLevel < 15) {
                    probability += 0.15;
                }
                
                if (wlRatio > 2.5 && networkLevel < 15) {
                    probability += 0.15;
                }
            }
        }
        
        // 2. Check achievement count
        int achievementPoints = stats.getAchievementPoints();
        int achievementCompletionPercent = stats.getAchievementCompletionPercent();
        
        if (achievementPoints < 500 && networkLevel > 15) {
            probability += 0.2;
        }
        
        if (achievementCompletionPercent < 10 && networkLevel > 20) {
            probability += 0.2;
        }
        
        // 3. Check guild membership
        if (!stats.hasGuild() && networkLevel > 30) {
            probability += 0.15;
        }
        
        // 4. Check account age vs skill level
        long accountAge = stats.getAccountAge();
        if (accountAge < 2592000000L) { // Less than 30 days
            // Calculate overall skill based on available game stats
            double overallSkill = calculateOverallSkill(stats);
            if (overallSkill > 0.7) { // Highly skilled
                probability += 0.3;
            }
        }
        
        // Cap at 1.0
        probability = Math.min(probability, 1.0);
        
        // Cache result
        nickProbabilityCache.put(username, probability);
        
        return probability;
    }
    
    /**
     * Calculates the probability that a player is an alt account based on their stats
     * @param stats PlayerStats object containing the player's statistics
     * @return A value between 0.0 (certainly main account) and 1.0 (certainly alt account)
     */
    public double calculateAltProbability(PlayerStats stats) {
        String username = stats.getUsername();
        
        // Check cache first
        if (altProbabilityCache.containsKey(username)) {
            return altProbabilityCache.get(username);
        }
        
        double probability = 0.0;
        
        // 1. Account age check
        long accountAge = stats.getAccountAge();
        if (accountAge < 7776000000L) { // Less than 90 days
            probability += 0.2;
        }
        
        // 2. Friend count check (using a placeholder since the actual field isn't in the model)
        int friendCount = stats.getFriendsCount();
        if (friendCount < 5) {
            probability += 0.2;
        }
        
        // 3. Social media links
        if (!stats.hasSocialLinks()) {
            probability += 0.1;
        }
        
        // 4. Skill vs level check
        int networkLevel = stats.getNetworkLevel();
        double skillLevel = calculateOverallSkill(stats);
        
        if (networkLevel < 30 && skillLevel > 0.7) {
            probability += 0.3;
        }
        
        // 5. Specific game focus check
        // Check if the player only has good stats in one game mode and terrible in others
        if (stats.getGameStats().size() > 1) {
            int goodGameModes = 0;
            for (GameStats gameStats : stats.getGameStats().values()) {
                if (gameStats.getWlRatio() > 1.5 || gameStats.getKdRatio() > 2.0) {
                    goodGameModes++;
                }
            }
            
            if (goodGameModes == 1 && stats.getGameStats().size() >= 3) {
                probability += 0.25;
            }
        }
        
        // Cap at 1.0
        probability = Math.min(probability, 1.0);
        
        // Cache result
        altProbabilityCache.put(username, probability);
        
        return probability;
    }
    
    /**
     * Calculates an overall skill score for a player based on their stats
     * @param stats PlayerStats object containing the player's statistics
     * @return A value between 0.0 (low skill) and 1.0 (high skill)
     */
    private double calculateOverallSkill(PlayerStats stats) {
        if (stats == null || stats.getGameStats().isEmpty()) {
            return 0.0;
        }
        
        double totalSkill = 0.0;
        int gameCount = 0;
        
        // Check Bedwars
        BedwarsStats bedwarsStats = stats.getBedwarsStats();
        if (bedwarsStats != null) {
            double bedwarsSkill = 0.0;
            
            if (bedwarsStats.getWlRatio() > 1.0) bedwarsSkill += 0.2;
            if (bedwarsStats.getWlRatio() > 2.0) bedwarsSkill += 0.2;
            if (bedwarsStats.getWlRatio() > 4.0) bedwarsSkill += 0.1;
            
            if (bedwarsStats.getKdRatio() > 1.5) bedwarsSkill += 0.15;
            if (bedwarsStats.getKdRatio() > 3.0) bedwarsSkill += 0.15;
            
            if (bedwarsStats.getFinalKDRatio() > 1.2) bedwarsSkill += 0.15;
            if (bedwarsStats.getFinalKDRatio() > 2.5) bedwarsSkill += 0.15;
            
            totalSkill += Math.min(bedwarsSkill, 1.0);
            gameCount++;
        }
        
        // Check Skywars
        GameStats skywarsStats = stats.getSkywarsStats();
        if (skywarsStats != null) {
            double skywarsSkill = 0.0;
            
            if (skywarsStats.getWlRatio() > 0.8) skywarsSkill += 0.25;
            if (skywarsStats.getWlRatio() > 1.5) skywarsSkill += 0.25;
            
            if (skywarsStats.getKdRatio() > 1.2) skywarsSkill += 0.25;
            if (skywarsStats.getKdRatio() > 2.5) skywarsSkill += 0.25;
            
            totalSkill += Math.min(skywarsSkill, 1.0);
            gameCount++;
        }
        
        // Check Duels
        GameStats duelsStats = stats.getDuelsStats();
        if (duelsStats != null) {
            double duelsSkill = 0.0;
            
            if (duelsStats.getWlRatio() > 1.0) duelsSkill += 0.33;
            if (duelsStats.getWlRatio() > 2.0) duelsSkill += 0.33;
            
            if (duelsStats.getKdRatio() > 1.2) duelsSkill += 0.34;
            
            totalSkill += Math.min(duelsSkill, 1.0);
            gameCount++;
        }
        
        // If no games have stats, return 0
        if (gameCount == 0) {
            return 0.0;
        }
        
        return totalSkill / gameCount;
    }
    
    /**
     * Gets a textual representation of the nick probability
     * @param probability The nick probability value
     * @return A string representing the probability
     */
    public String getNickProbabilityText(double probability) {
        if (probability < 0.2) {
            return "Likely Real";
        } else if (probability < 0.5) {
            return "Possibly Nicked";
        } else if (probability < 0.8) {
            return "Probably Nicked";
        } else {
            return "Almost Certainly Nicked";
        }
    }
    
    /**
     * Gets a textual representation of the alt probability
     * @param probability The alt probability value
     * @return A string representing the probability
     */
    public String getAltProbabilityText(double probability) {
        if (probability < 0.2) {
            return "Likely Main Account";
        } else if (probability < 0.5) {
            return "Possibly Alt";
        } else if (probability < 0.8) {
            return "Probably Alt";
        } else {
            return "Almost Certainly Alt";
        }
    }
    
    /**
     * Gets a CSS class name based on the probability level
     * @param probability The probability value
     * @return A CSS class name
     */
    public String getProbabilityStyleClass(double probability) {
        if (probability < 0.2) {
            return "auth-low";
        } else if (probability < 0.5) {
            return "auth-medium-low";
        } else if (probability < 0.8) {
            return "auth-medium-high";
        } else {
            return "auth-high";
        }
    }
    
    /**
     * Clear the cache for a specific player
     * @param username The player's username
     */
    public void clearCache(String username) {
        nickProbabilityCache.remove(username);
        altProbabilityCache.remove(username);
    }
    
    /**
     * Clear the entire cache
     */
    public void clearAllCache() {
        nickProbabilityCache.clear();
        altProbabilityCache.clear();
    }
} 