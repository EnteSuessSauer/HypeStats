package com.hypestats.util;

import com.google.gson.JsonObject;
import com.hypestats.model.*;

/**
 * Example implementation of the parse methods using the new stats architecture.
 * This is NOT a complete class, just examples of the parse methods.
 */
public class HypixelApiService_Example {

    /**
     * Parse Bedwars stats from API response
     */
    private void parseBedwarsStats(PlayerStats stats, JsonObject statsObj, JsonObject playerData) {
        // Use StatsUtils to safely get the Bedwars data
        JsonObject bedwarsData = StatsUtils.getJsonObjectSafely(statsObj, "Bedwars");
        
        // If no data, just return
        if (bedwarsData == null) {
            return;
        }
        
        // Create BedwarsStats object using factory
        BedwarsStats bedwarsStats = (BedwarsStats) GameStatsFactory.createGameStats("BEDWARS");
        
        // Calculate Bedwars level (star)
        double bedwarsExp = 0;
        if (playerData.has("achievements") && !playerData.get("achievements").isJsonNull()) {
            JsonObject achievements = playerData.getAsJsonObject("achievements");
            if (achievements.has("bedwars_level")) {
                bedwarsExp = achievements.get("bedwars_level").getAsDouble();
            }
        }
        
        // Set basic stats
        bedwarsStats.setLevel(bedwarsExp);
        bedwarsStats.setWins(StatsUtils.getIntValue(bedwarsData, "wins_bedwars", 0));
        bedwarsStats.setLosses(StatsUtils.getIntValue(bedwarsData, "losses_bedwars", 0));
        bedwarsStats.setKills(StatsUtils.getIntValue(bedwarsData, "kills_bedwars", 0));
        bedwarsStats.setDeaths(StatsUtils.getIntValue(bedwarsData, "deaths_bedwars", 0));
        bedwarsStats.setCoins(StatsUtils.getIntValue(bedwarsData, "coins", 0));
        
        // Set Bedwars-specific stats
        bedwarsStats.setFinalKills(StatsUtils.getIntValue(bedwarsData, "final_kills_bedwars", 0));
        bedwarsStats.setFinalDeaths(StatsUtils.getIntValue(bedwarsData, "final_deaths_bedwars", 0));
        bedwarsStats.setBedsBroken(StatsUtils.getIntValue(bedwarsData, "beds_broken_bedwars", 0));
        bedwarsStats.setBedsLost(StatsUtils.getIntValue(bedwarsData, "beds_lost_bedwars", 0));
        
        // Winstreak might be hidden
        if (bedwarsData.has("winstreak") && !bedwarsData.get("winstreak").isJsonNull()) {
            bedwarsStats.setWinstreak(bedwarsData.get("winstreak").getAsInt());
        }
        
        // Calculate derived stats like W/L and K/D ratios
        bedwarsStats.calculateDerivedStats();
        
        // Add to player's game stats
        stats.addGameStats("BEDWARS", bedwarsStats);
    }
    
    /**
     * Parse SkyBlock stats from API response
     */
    private void parseSkyBlockStats(PlayerStats stats, JsonObject statsObj, JsonObject playerData) {
        // SkyBlock stats are structured differently with profiles
        JsonObject skyblockProfiles = null;
        
        // Try to get SkyBlock profiles
        if (playerData.has("stats") && 
            playerData.getAsJsonObject("stats").has("SkyBlock") && 
            !playerData.getAsJsonObject("stats").get("SkyBlock").isJsonNull()) {
            
            skyblockProfiles = playerData.getAsJsonObject("stats").getAsJsonObject("SkyBlock");
        }
        
        if (skyblockProfiles == null) {
            return; // No SkyBlock profiles found
        }
        
        // Create SkyBlockStats using factory
        SkyBlockStats skyblockStats = (SkyBlockStats) GameStatsFactory.createGameStats("SKYBLOCK");
        
        // Find the active profile
        String profileId = findActiveProfile(skyblockProfiles);
        
        if (profileId != null && skyblockProfiles.has(profileId)) {
            JsonObject profile = skyblockProfiles.getAsJsonObject(profileId);
            
            // Extract profile data
            skyblockStats.setProfileName(StatsUtils.getStringValue(profile, "cute_name", "Unknown"));
            skyblockStats.setPurse(StatsUtils.getIntValue(profile, "purse", 0));
            skyblockStats.setBank(StatsUtils.getIntValue(profile, "bank", 0));
            
            // Extract and set skill levels if available
            if (profile.has("skills")) {
                JsonObject skills = profile.getAsJsonObject("skills");
                skyblockStats.setFarmingLevel(StatsUtils.getIntValue(skills, "farming", 0));
                skyblockStats.setMiningLevel(StatsUtils.getIntValue(skills, "mining", 0));
                skyblockStats.setCombatLevel(StatsUtils.getIntValue(skills, "combat", 0));
                skyblockStats.setForagingLevel(StatsUtils.getIntValue(skills, "foraging", 0));
                skyblockStats.setFishingLevel(StatsUtils.getIntValue(skills, "fishing", 0));
                skyblockStats.setEnchantingLevel(StatsUtils.getIntValue(skills, "enchanting", 0));
                skyblockStats.setAlchemyLevel(StatsUtils.getIntValue(skills, "alchemy", 0));
                skyblockStats.setTamingLevel(StatsUtils.getIntValue(skills, "taming", 0));
            }
        }
        
        // Add to player's game stats
        stats.addGameStats("SKYBLOCK", skyblockStats);
    }
    
    /**
     * Parse TNT Games stats from API response
     */
    private void parseTNTGamesStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject tntGamesData = StatsUtils.getJsonObjectSafely(statsObj, "TNTGames");
        
        // If no data, just return
        if (tntGamesData == null) {
            return;
        }
        
        // Create TNTGamesStats using factory
        TNTGamesStats tntGamesStats = (TNTGamesStats) GameStatsFactory.createGameStats("TNTGAMES");
        
        // Extract basic stats
        tntGamesStats.setWins(StatsUtils.getIntValue(tntGamesData, "wins", 0));
        tntGamesStats.setCoins(StatsUtils.getIntValue(tntGamesData, "coins", 0));
        
        // Game mode specific wins
        tntGamesStats.setTntRunWins(StatsUtils.getIntValue(tntGamesData, "wins_tntrun", 0));
        tntGamesStats.setTntRunRecord(StatsUtils.getIntValue(tntGamesData, "record_tntrun", 0));
        tntGamesStats.setBowSpleefWins(StatsUtils.getIntValue(tntGamesData, "wins_bowspleef", 0));
        tntGamesStats.setWizardsWins(StatsUtils.getIntValue(tntGamesData, "wins_capture", 0));
        tntGamesStats.setPvpRunWins(StatsUtils.getIntValue(tntGamesData, "wins_pvprun", 0));
        
        // Calculate derived stats
        tntGamesStats.calculateDerivedStats();
        
        // Add to player's game stats
        stats.addGameStats("TNTGAMES", tntGamesStats);
    }
    
    // Helper method to find active SkyBlock profile
    private String findActiveProfile(JsonObject skyblockProfiles) {
        // Implementation to find the active profile
        // For example, you could look for the profile with the most recent activity
        return null; // Placeholder
    }
    
    /**
     * Generate mock player stats for testing
     * This shows how to create mock data with the new approach
     */
    private PlayerStats generateMockPlayerStats(String username) {
        PlayerStats stats = new PlayerStats();
        stats.setUuid("test-" + username.toLowerCase());
        stats.setUsername(username);
        
        // Set a random seed based on username for consistent results
        long seed = 0;
        for (char c : username.toCharArray()) {
            seed = 31 * seed + c;
        }
        java.util.Random random = new java.util.Random(seed);
        
        // Network stats
        stats.setNetworkLevel(random.nextInt(250) + 1);
        stats.setKarma(random.nextInt(100000) + 1);
        stats.setAchievementPoints(random.nextInt(10000) + 1);
        stats.setAchievementCompletionPercent(random.nextInt(100) + 1);
        
        // Create Bedwars stats
        BedwarsStats bedwarsStats = (BedwarsStats) GameStatsFactory.createGameStats("BEDWARS");
        bedwarsStats.setLevel(random.nextInt(1000) + 1);
        bedwarsStats.setWins(random.nextInt(5000) + 1);
        bedwarsStats.setLosses(random.nextInt(2000) + 1);
        bedwarsStats.setKills(random.nextInt(15000) + 1);
        bedwarsStats.setDeaths(random.nextInt(10000) + 1);
        bedwarsStats.setFinalKills(random.nextInt(10000) + 1);
        bedwarsStats.setFinalDeaths(random.nextInt(5000) + 1);
        bedwarsStats.setBedsBroken(random.nextInt(5000) + 1);
        bedwarsStats.setBedsLost(random.nextInt(3000) + 1);
        
        // 70% chance to have a visible winstreak
        if (random.nextInt(10) < 7) {
            bedwarsStats.setWinstreak(random.nextInt(50) + 1);
        }
        
        // Calculate derived stats
        bedwarsStats.calculateDerivedStats();
        
        // Add to player's game stats
        stats.addGameStats("BEDWARS", bedwarsStats);
        
        // Create TNT Games stats
        TNTGamesStats tntGamesStats = (TNTGamesStats) GameStatsFactory.createGameStats("TNTGAMES");
        tntGamesStats.setWins(150 + random.nextInt(1350));
        tntGamesStats.setCoins(6000 + random.nextInt(34000));
        tntGamesStats.setTntRunWins(50 + random.nextInt(750));
        tntGamesStats.setTntRunRecord(200 + random.nextInt(300));
        tntGamesStats.setBowSpleefWins(75 + random.nextInt(325));
        tntGamesStats.setWizardsWins(25 + random.nextInt(175));
        tntGamesStats.setPvpRunWins(50 + random.nextInt(250));
        
        // Calculate derived stats
        tntGamesStats.calculateDerivedStats();
        
        // Add to player's game stats
        stats.addGameStats("TNTGAMES", tntGamesStats);
        
        // More game modes would be added here...
        
        // Calculate top stats
        stats.calculateTopStats();
        
        return stats;
    }
} 