package com.hypestats.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypestats.HypeStatsApp;
import com.hypestats.model.PlayerStats;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map;

/**
 * Service for interacting with the Hypixel API
 */
@Slf4j
public class HypixelApiService {
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String HYPIXEL_API_URL = "https://api.hypixel.net/player";
    private static final int MAX_REQUESTS_PER_MINUTE = 110; // Hypixel allows 120/min but we use 110 to be safe
    
    private final OkHttpClient client;
    private final Gson gson;
    private final List<Long> requestTimestamps;
    private final Random random = new Random();
    
    public HypixelApiService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.requestTimestamps = new ArrayList<>();
    }
    
    /**
     * Get player stats from Hypixel API
     * @param username Minecraft username
     * @param apiKey Hypixel API key
     * @return PlayerStats object
     * @throws IOException if API request fails
     * @throws ApiException if there's an API-level error (rate limit, invalid key, etc.)
     */
    public PlayerStats getPlayerStats(String username, String apiKey) throws IOException, ApiException {
        // Special handling for test mode
        if (HypeStatsApp.isTestMode()) {
            try {
                DevLogger.log("API: getPlayerStats called for username: " + username);
                if (apiKey == null || apiKey.isEmpty()) {
                    DevLogger.log("API: No API key provided");
                    throw new ApiException("No API key provided");
                }
            } catch (Exception e) {
                DevLogger.log("API: Error in test logging", e);
            }
            
            // In test mode, randomly decide whether to succeed or fail
            if (random.nextInt(10) < 3) { // 30% chance of failure for testing
                String errorMsg = "Simulated API failure in test mode";
                DevLogger.log("API: " + errorMsg);
                throw new ApiException(errorMsg);
            }
            
            // In test mode with success case, generate mock data
            if (username.equals("TESTFAIL")) {
                DevLogger.log("API: Forced failure for test user");
                throw new ApiException("Test failure triggered by username");
            }
            
            DevLogger.log("API: Returning mock data for " + username);
            return generateMockPlayerStats(username);
        }
        
        // Normal operation
        try {
            // Implement rate limiting
            enforceRateLimit();
            
            // First, get the UUID from the username via Mojang API
            String uuid = getPlayerUuid(username);
            if (uuid == null) {
                throw new ApiException("Player not found");
            }
            
            // Then, get the player data from Hypixel API
            Request request = new Request.Builder()
                    .url(HYPIXEL_API_URL + "?uuid=" + uuid)
                    .header("API-Key", apiKey)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    if (response.code() == 403) {
                        throw new ApiException("Invalid API key");
                    } else if (response.code() == 429) {
                        throw new ApiException("Rate limit exceeded. Please try again later.");
                    } else {
                        throw new ApiException("API error: " + response.code());
                    }
                }
                
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                if (!jsonResponse.get("success").getAsBoolean()) {
                    String cause = jsonResponse.has("cause") ? jsonResponse.get("cause").getAsString() : "Unknown error";
                    throw new ApiException("API error: " + cause);
                }
                
                if (!jsonResponse.has("player") || jsonResponse.get("player").isJsonNull()) {
                    throw new ApiException("Player has never played on Hypixel");
                }
                
                return parsePlayerStats(jsonResponse.getAsJsonObject("player"), username, uuid);
            }
        } catch (IOException | ApiException e) {
            log.error("API Request failed: {}", e.getMessage());
            if (HypeStatsApp.isTestMode()) {
                DevLogger.log("API: Request failed: " + e.getMessage(), e);
            }
            throw e;
        }
    }
    
    /**
     * Generate mock player stats for testing
     * @param username Username to generate stats for
     * @return Mock PlayerStats object
     */
    private PlayerStats generateMockPlayerStats(String username) {
        PlayerStats stats = new PlayerStats();
        stats.setUuid("test-" + username.toLowerCase());
        stats.setUsername(username);
        
        // Network stats
        stats.setNetworkLevel(random.nextInt(250) + 1);
        stats.setKarma(random.nextInt(100000) + 1);
        stats.setAchievementPoints(random.nextInt(10000) + 1);
        
        // Generate believable test data for Bedwars
        stats.setBedwarsLevel(random.nextInt(1000) + 1);
        stats.setBedwarsWins(random.nextInt(5000) + 1);
        stats.setBedwarsLosses(random.nextInt(2000) + 1);
        stats.setBedwarsWLRatio((double) stats.getBedwarsWins() / Math.max(1, stats.getBedwarsLosses()));
        
        stats.setBedwarsKills(random.nextInt(15000) + 1);
        stats.setBedwarsDeaths(random.nextInt(10000) + 1);
        stats.setBedwarsKDRatio((double) stats.getBedwarsKills() / Math.max(1, stats.getBedwarsDeaths()));
        
        stats.setBedwarsFinalKills(random.nextInt(10000) + 1);
        stats.setBedwarsFinalDeaths(random.nextInt(5000) + 1);
        stats.setBedwarsFinalKDRatio((double) stats.getBedwarsFinalKills() / Math.max(1, stats.getBedwarsFinalDeaths()));
        
        stats.setBedwarsBedsBroken(random.nextInt(5000) + 1);
        stats.setBedwarsBedsLost(random.nextInt(3000) + 1);
        stats.setBedwarsGamesPlayed(stats.getBedwarsWins() + stats.getBedwarsLosses());
        
        // Generate mock Skywars stats
        stats.setSkywarsLevel(random.nextInt(50) + 1);
        stats.setSkywarsWins(random.nextInt(2000) + 1);
        stats.setSkywarsLosses(random.nextInt(3000) + 1);
        stats.setSkywarsWLRatio((double) stats.getSkywarsWins() / Math.max(1, stats.getSkywarsLosses()));
        stats.setSkywarsKills(random.nextInt(8000) + 1);
        stats.setSkywarsDeaths(random.nextInt(7000) + 1);
        stats.setSkywarsKDRatio((double) stats.getSkywarsKills() / Math.max(1, stats.getSkywarsDeaths()));
        stats.setSkywarsCoins(random.nextInt(100000) + 1);
        stats.setSkywarsGamesPlayed(stats.getSkywarsWins() + stats.getSkywarsLosses());
        
        // Generate mock Duels stats
        stats.setDuelsWins(random.nextInt(3000) + 1);
        stats.setDuelsLosses(random.nextInt(2000) + 1);
        stats.setDuelsWLRatio((double) stats.getDuelsWins() / Math.max(1, stats.getDuelsLosses()));
        stats.setDuelsKills(random.nextInt(6000) + 1);
        stats.setDuelsDeaths(random.nextInt(4000) + 1);
        stats.setDuelsKDRatio((double) stats.getDuelsKills() / Math.max(1, stats.getDuelsDeaths()));
        stats.setDuelsCoins(random.nextInt(50000) + 1);
        stats.setDuelsGamesPlayed(stats.getDuelsWins() + stats.getDuelsLosses());
        
        // Generate mock Murder Mystery stats
        stats.setMmWins(random.nextInt(1000) + 1);
        stats.setMmGamesPlayed(random.nextInt(3000) + 1);
        stats.setMmKills(random.nextInt(5000) + 1);
        stats.setMmDeaths(random.nextInt(4000) + 1);
        stats.setMmKDRatio((double) stats.getMmKills() / Math.max(1, stats.getMmDeaths()));
        stats.setMmCoins(random.nextInt(30000) + 1);
        
        // Generate mock TNT Games stats
        stats.setTntgamesWins(random.nextInt(1500) + 1);
        stats.setTntgamesCoins(random.nextInt(40000) + 1);
        stats.setTntRunWins(random.nextInt(800) + 1);
        stats.setTntRunRecord(random.nextInt(500) + 100);
        stats.setBowSpleefWins(random.nextInt(400) + 1);
        stats.setWizardsWins(random.nextInt(200) + 1);
        stats.setPvpRunWins(random.nextInt(300) + 1);
        
        // Generate mock UHC stats
        stats.setUhcWins(random.nextInt(300) + 1);
        stats.setUhcKills(random.nextInt(1500) + 1);
        stats.setUhcDeaths(random.nextInt(1000) + 1);
        stats.setUhcKDRatio((double) stats.getUhcKills() / Math.max(1, stats.getUhcDeaths()));
        stats.setUhcCoins(random.nextInt(20000) + 1);
        stats.setUhcScore(random.nextInt(5000) + 1);
        
        // Generate mock Build Battle stats
        stats.setBuildBattleWins(random.nextInt(500) + 1);
        stats.setBuildBattleGamesPlayed(random.nextInt(1500) + 1);
        stats.setBuildBattleScore(random.nextInt(3000) + 500);
        stats.setBuildBattleCoins(random.nextInt(25000) + 1);
        
        // Generate mock Mega Walls stats
        stats.setMegaWallsWins(random.nextInt(400) + 1);
        stats.setMegaWallsKills(random.nextInt(3000) + 1);
        stats.setMegaWallsDeaths(random.nextInt(2500) + 1);
        stats.setMegaWallsKDRatio((double) stats.getMegaWallsKills() / Math.max(1, stats.getMegaWallsDeaths()));
        stats.setMegaWallsAssists(random.nextInt(1000) + 1);
        stats.setMegaWallsFinalKills(random.nextInt(1500) + 1);
        stats.setMegaWallsFinalDeaths(random.nextInt(1000) + 1);
        stats.setMegaWallsFinalKDRatio((double) stats.getMegaWallsFinalKills() / Math.max(1, stats.getMegaWallsFinalDeaths()));
        
        // Generate mock SkyBlock stats
        String[] profileNames = {"Banana", "Pineapple", "Apple", "Strawberry", "Grape", "Mango", "Coconut"};
        stats.setSkyblockProfile(profileNames[random.nextInt(profileNames.length)]);
        stats.setSkyblockBank(random.nextInt(100000000) + 1);
        stats.setSkyblockPurse(random.nextInt(10000000) + 1);
        stats.setSkyblockCoins(stats.getSkyblockBank() + stats.getSkyblockPurse());
        
        // Randomly assign rank
        String[] possibleRanks = {"DEFAULT", "VIP", "VIP+", "MVP", "MVP+", "MVP++"};
        String[] rankColors = {"GRAY", "GREEN", "GREEN", "AQUA", "AQUA", "GOLD"};
        int rankIndex = random.nextInt(possibleRanks.length);
        
        stats.setRank(possibleRanks[rankIndex]);
        stats.setRankColor(rankColors[rankIndex]);
        
        // 70% chance to have a visible winstreak
        if (random.nextInt(10) < 7) {
            stats.setBedwarsWinstreak(random.nextInt(50) + 1);
        }
        
        // Set account age and playtime
        long now = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000L;
        long randomDaysAgo = random.nextInt(1000) + 30; // 30 days to 1030 days (~3 years) ago
        
        stats.setFirstLogin(now - (randomDaysAgo * dayInMillis));
        stats.setLastLogin(now - random.nextInt(10) * dayInMillis); // 0-10 days ago
        stats.setAccountAge(stats.getLastLogin() - stats.getFirstLogin());
        
        // Set random playtime (min 5% of account age, max 20%)
        double playTimePercent = random.nextDouble() * 0.15 + 0.05; // 5% to 20%
        stats.setPlaytime((long) (stats.getAccountAge() * playTimePercent));
        
        // Calculate top stats
        stats.calculateTopStats();
        
        return stats;
    }
    
    /**
     * Get player UUID from Mojang API
     * @param username Minecraft username
     * @return UUID of the player, or null if player not found
     * @throws IOException if API request fails
     */
    private String getPlayerUuid(String username) throws IOException {
        Request request = new Request.Builder()
                .url(MOJANG_API_URL + username)
                .build();
        
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    return null; // Player not found
                }
                throw new IOException("Mojang API error: " + response.code());
            }
            
            String responseBody = response.body().string();
            JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
            
            return jsonResponse.get("id").getAsString();
        }
    }
    
    /**
     * Enforce Hypixel API rate limit
     * @throws ApiException if rate limit is exceeded
     */
    private synchronized void enforceRateLimit() throws ApiException {
        long now = System.currentTimeMillis();
        
        // Remove timestamps older than 1 minute
        requestTimestamps.removeIf(timestamp -> now - timestamp > 60000);
        
        // Check if we've exceeded the rate limit
        if (requestTimestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            throw new ApiException("Rate limit exceeded. Please wait before making more requests.");
        }
        
        // Add the current timestamp
        requestTimestamps.add(now);
    }
    
    /**
     * Parse player stats from Hypixel API response
     */
    private PlayerStats parsePlayerStats(JsonObject playerData, String displayName, String uuid) {
        PlayerStats stats = new PlayerStats();
        stats.setUuid(uuid);
        stats.setUsername(displayName);
        
        // Network stats
        if (playerData.has("networkExp") && !playerData.get("networkExp").isJsonNull()) {
            double networkExp = playerData.get("networkExp").getAsDouble();
            // Formula for Hypixel Network Level calculation
            double networkLevel = (Math.sqrt(networkExp + 15312.5) - 125 / Math.sqrt(2)) / (25 * Math.sqrt(2));
            stats.setNetworkLevel((int) Math.floor(networkLevel));
        }
        
        if (playerData.has("karma") && !playerData.get("karma").isJsonNull()) {
            stats.setKarma(playerData.get("karma").getAsInt());
        }
        
        if (playerData.has("achievementPoints") && !playerData.get("achievementPoints").isJsonNull()) {
            stats.setAchievementPoints(playerData.get("achievementPoints").getAsInt());
        }
        
        if (playerData.has("firstLogin") && !playerData.get("firstLogin").isJsonNull()) {
            stats.setFirstLogin(playerData.get("firstLogin").getAsLong());
        }
        
        if (playerData.has("lastLogin") && !playerData.get("lastLogin").isJsonNull()) {
            stats.setLastLogin(playerData.get("lastLogin").getAsLong());
        }
        
        // Calculate basic estimated playtime if both logins are available
        if (stats.getFirstLogin() > 0 && stats.getLastLogin() > 0) {
            stats.setAccountAge(stats.getLastLogin() - stats.getFirstLogin());
        }
        
        // Parse player status
        parsePlayerStatus(stats, playerData);
        
        // Parse social media links
        parseSocialMedia(stats, playerData);
        
        // Parse achievements data
        parseAchievements(stats, playerData);
        
        // Try to get more accurate playtime by looking at played games data
        // Some players have this statistic, which gives a better measure of actual time spent playing
        long playtimeMillis = 0;
        int playtimeSourceCount = 0;
        
        // Check for various game playtime stats
        if (playerData.has("stats")) {
            JsonObject allStats = playerData.getAsJsonObject("stats");
            
            // Bedwars playtime (in minutes)
            if (allStats.has("Bedwars") && !allStats.get("Bedwars").isJsonNull()) {
                JsonObject bedwarsStats = allStats.getAsJsonObject("Bedwars");
                if (bedwarsStats.has("time_played_bedwars") && !bedwarsStats.get("time_played_bedwars").isJsonNull()) {
                    playtimeMillis += bedwarsStats.get("time_played_bedwars").getAsLong() * 60L * 1000L;
                    playtimeSourceCount++;
                }
            }
            
            // Skywars playtime (in seconds)
            if (allStats.has("SkyWars") && !allStats.get("SkyWars").isJsonNull()) {
                JsonObject skywarsStats = allStats.getAsJsonObject("SkyWars");
                if (skywarsStats.has("time_played") && !skywarsStats.get("time_played").isJsonNull()) {
                    playtimeMillis += skywarsStats.get("time_played").getAsLong() * 1000L;
                    playtimeSourceCount++;
                }
            }
            
            // Murder Mystery playtime (in seconds)
            if (allStats.has("MurderMystery") && !allStats.get("MurderMystery").isJsonNull()) {
                JsonObject mmStats = allStats.getAsJsonObject("MurderMystery");
                if (mmStats.has("time_played") && !mmStats.get("time_played").isJsonNull()) {
                    playtimeMillis += mmStats.get("time_played").getAsLong() * 1000L;
                    playtimeSourceCount++;
                } else if (mmStats.has("games") && !mmStats.get("games").isJsonNull()) {
                    // Estimate: average MM game is ~5 minutes
                    playtimeMillis += mmStats.get("games").getAsLong() * 5L * 60L * 1000L;
                    playtimeSourceCount++;
                }
            }
            
            // Duels playtime (in seconds)
            if (allStats.has("Duels") && !allStats.get("Duels").isJsonNull()) {
                JsonObject duelsStats = allStats.getAsJsonObject("Duels");
                if (duelsStats.has("games_played_duels") && !duelsStats.get("games_played_duels").isJsonNull()) {
                    // Estimate: average duel is ~3 minutes
                    playtimeMillis += duelsStats.get("games_played_duels").getAsLong() * 3L * 60L * 1000L;
                    playtimeSourceCount++;
                }
            }
            
            // Set the playtime if we found any
            if (playtimeMillis > 0) {
                stats.setPlaytime(playtimeMillis);
                stats.setPlaytimeSourceCount(playtimeSourceCount);
            }
            
            // Estimate playtime from account age if we have very little data
            // Players with active accounts typically spend at least 1% of their account age playing
            if (playtimeSourceCount < 2 && stats.getAccountAge() > 0) {
                long estimatedMinPlaytime = stats.getAccountAge() / 100;
                if (playtimeMillis < estimatedMinPlaytime) {
                    log.info("Playtime data incomplete for {}, supplementing with estimate", stats.getUsername());
                    stats.setPlaytime(estimatedMinPlaytime);
                    stats.setPlaytimeEstimated(true);
                }
            }
        }
        
        if (playerData.has("mostRecentGameType") && !playerData.get("mostRecentGameType").isJsonNull()) {
            stats.setMostRecentGameType(playerData.get("mostRecentGameType").getAsString());
        }
        
        // Extract game stats
        JsonObject statsObj = null;
        if (playerData.has("stats") && !playerData.get("stats").isJsonNull()) {
            statsObj = playerData.getAsJsonObject("stats");
        } else {
            statsObj = new JsonObject();
        }
        
        // Parse Bedwars stats
        parseBedwarsStats(stats, statsObj, playerData);
        
        // Parse Skywars stats
        parseSkywarsStats(stats, statsObj);
        
        // Parse Duels stats
        parseDuelsStats(stats, statsObj);
        
        // Parse Murder Mystery stats
        parseMurderMysteryStats(stats, statsObj);
        
        // Parse Arcade stats
        parseArcadeStats(stats, statsObj);
        
        // Parse TNT Games stats
        parseTNTGamesStats(stats, statsObj);
        
        // Parse UHC stats
        parseUHCStats(stats, statsObj);
        
        // Parse Build Battle stats
        parseBuildBattleStats(stats, statsObj);
        
        // Parse Mega Walls stats
        parseMegaWallsStats(stats, statsObj);
        
        // Parse SkyBlock stats
        parseSkyBlockStats(stats, statsObj, playerData);
        
        // Extract player rank
        parsePlayerRank(stats, playerData);
        
        // Attempt to fetch guild information
        fetchGuildInfo(stats);
        
        // Calculate the top stats
        stats.calculateTopStats();
        
        return stats;
    }
    
    /**
     * Parse Bedwars stats from API response
     */
    private void parseBedwarsStats(PlayerStats stats, JsonObject statsObj, JsonObject playerData) {
        JsonObject bedwarsStats = null;
        if (statsObj.has("Bedwars") && !statsObj.get("Bedwars").isJsonNull()) {
            bedwarsStats = statsObj.getAsJsonObject("Bedwars");
        } else {
            bedwarsStats = new JsonObject();
        }
        
        // Calculate Bedwars level (star)
        double bedwarsExp = 0;
        if (playerData.has("achievements") && !playerData.get("achievements").isJsonNull()) {
            JsonObject achievements = playerData.getAsJsonObject("achievements");
            if (achievements.has("bedwars_level")) {
                bedwarsExp = achievements.get("bedwars_level").getAsDouble();
            }
        }
        stats.setBedwarsLevel(bedwarsExp);
        
        // Extract stats from bedwars data
        int wins = getIntValue(bedwarsStats, "wins_bedwars", 0);
        int losses = getIntValue(bedwarsStats, "losses_bedwars", 0);
        int kills = getIntValue(bedwarsStats, "kills_bedwars", 0);
        int deaths = getIntValue(bedwarsStats, "deaths_bedwars", 0);
        int finalKills = getIntValue(bedwarsStats, "final_kills_bedwars", 0);
        int finalDeaths = getIntValue(bedwarsStats, "final_deaths_bedwars", 0);
        int bedsBroken = getIntValue(bedwarsStats, "beds_broken_bedwars", 0);
        int bedsLost = getIntValue(bedwarsStats, "beds_lost_bedwars", 0);
        
        // Winstreak might be hidden
        Integer winstreak = null;
        if (bedwarsStats.has("winstreak") && !bedwarsStats.get("winstreak").isJsonNull()) {
            winstreak = bedwarsStats.get("winstreak").getAsInt();
        }
        
        // Calculate derived stats
        double wlRatio = losses > 0 ? (double) wins / losses : wins;
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        double finalKdRatio = finalDeaths > 0 ? (double) finalKills / finalDeaths : finalKills;
        int gamesPlayed = wins + losses;
        
        // Set all the stats
        stats.setBedwarsWins(wins);
        stats.setBedwarsLosses(losses);
        stats.setBedwarsWLRatio(wlRatio);
        stats.setBedwarsKills(kills);
        stats.setBedwarsDeaths(deaths);
        stats.setBedwarsKDRatio(kdRatio);
        stats.setBedwarsFinalKills(finalKills);
        stats.setBedwarsFinalDeaths(finalDeaths);
        stats.setBedwarsFinalKDRatio(finalKdRatio);
        stats.setBedwarsWinstreak(winstreak);
        stats.setBedwarsBedsBroken(bedsBroken);
        stats.setBedwarsBedsLost(bedsLost);
        stats.setBedwarsGamesPlayed(gamesPlayed);
    }
    
    /**
     * Parse Skywars stats from API response
     */
    private void parseSkywarsStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject skywarsStats = null;
        if (statsObj.has("SkyWars") && !statsObj.get("SkyWars").isJsonNull()) {
            skywarsStats = statsObj.getAsJsonObject("SkyWars");
        } else {
            return; // No Skywars stats
        }
        
        // Extract basic stats
        int wins = getIntValue(skywarsStats, "wins", 0);
        int losses = getIntValue(skywarsStats, "losses", 0);
        int kills = getIntValue(skywarsStats, "kills", 0);
        int deaths = getIntValue(skywarsStats, "deaths", 0);
        int coins = getIntValue(skywarsStats, "coins", 0);
        
        // Skywars level/experience
        double skywarsExp = 0;
        if (skywarsStats.has("skywars_experience") && !skywarsStats.get("skywars_experience").isJsonNull()) {
            skywarsExp = skywarsStats.get("skywars_experience").getAsDouble();
            // Formula for Skywars level calculation (approximation)
            stats.setSkywarsLevel(Math.floor(skywarsExp / 10000) + 1);
        }
        
        // Calculate derived stats
        double wlRatio = losses > 0 ? (double) wins / losses : wins;
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        int gamesPlayed = wins + losses;
        
        // Set all the stats
        stats.setSkywarsWins(wins);
        stats.setSkywarsLosses(losses);
        stats.setSkywarsWLRatio(wlRatio);
        stats.setSkywarsKills(kills);
        stats.setSkywarsDeaths(deaths);
        stats.setSkywarsKDRatio(kdRatio);
        stats.setSkywarsCoins(coins);
        stats.setSkywarsGamesPlayed(gamesPlayed);
    }
    
    /**
     * Parse Duels stats from API response
     */
    private void parseDuelsStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject duelsStats = null;
        if (statsObj.has("Duels") && !statsObj.get("Duels").isJsonNull()) {
            duelsStats = statsObj.getAsJsonObject("Duels");
        } else {
            return; // No Duels stats
        }
        
        // Extract basic stats
        int wins = getIntValue(duelsStats, "wins", 0);
        int losses = getIntValue(duelsStats, "losses", 0);
        int kills = getIntValue(duelsStats, "kills", 0);
        int deaths = getIntValue(duelsStats, "deaths", 0);
        int coins = getIntValue(duelsStats, "coins", 0);
        
        // Calculate derived stats
        double wlRatio = losses > 0 ? (double) wins / losses : wins;
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        int gamesPlayed = wins + losses;
        
        // Set all the stats
        stats.setDuelsWins(wins);
        stats.setDuelsLosses(losses);
        stats.setDuelsWLRatio(wlRatio);
        stats.setDuelsKills(kills);
        stats.setDuelsDeaths(deaths);
        stats.setDuelsKDRatio(kdRatio);
        stats.setDuelsCoins(coins);
        stats.setDuelsGamesPlayed(gamesPlayed);
    }
    
    /**
     * Parse Murder Mystery stats from API response
     */
    private void parseMurderMysteryStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject mmStats = null;
        if (statsObj.has("MurderMystery") && !statsObj.get("MurderMystery").isJsonNull()) {
            mmStats = statsObj.getAsJsonObject("MurderMystery");
        } else {
            return; // No Murder Mystery stats
        }
        
        // Extract basic stats
        int wins = getIntValue(mmStats, "wins", 0);
        int gamesPlayed = getIntValue(mmStats, "games", 0);
        int kills = getIntValue(mmStats, "kills", 0);
        int deaths = getIntValue(mmStats, "deaths", 0);
        int coins = getIntValue(mmStats, "coins", 0);
        
        // Calculate KD ratio
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        
        // Set all the stats
        stats.setMmWins(wins);
        stats.setMmGamesPlayed(gamesPlayed);
        stats.setMmKills(kills);
        stats.setMmDeaths(deaths);
        stats.setMmKDRatio(kdRatio);
        stats.setMmCoins(coins);
    }
    
    /**
     * Parse Arcade stats from API response
     */
    private void parseArcadeStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject arcadeStats = null;
        if (statsObj.has("Arcade") && !statsObj.get("Arcade").isJsonNull()) {
            arcadeStats = statsObj.getAsJsonObject("Arcade");
        } else {
            return; // No Arcade stats
        }
        
        // Extract basic stats
        int wins = getIntValue(arcadeStats, "wins", 0);
        int coins = getIntValue(arcadeStats, "coins", 0);
        
        // Set all the stats
        stats.setArcadeWins(wins);
        stats.setArcadeCoins(coins);
    }
    
    /**
     * Parse TNT Games stats from API response
     */
    private void parseTNTGamesStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject tntGamesStats = null;
        if (statsObj.has("TNTGames") && !statsObj.get("TNTGames").isJsonNull()) {
            tntGamesStats = statsObj.getAsJsonObject("TNTGames");
        } else {
            return; // No TNT Games stats
        }
        
        // Extract basic stats
        int wins = getIntValue(tntGamesStats, "wins", 0);
        int coins = getIntValue(tntGamesStats, "coins", 0);
        
        // Game mode specific wins
        int tntRunWins = getIntValue(tntGamesStats, "wins_tntrun", 0);
        int tntRunRecord = getIntValue(tntGamesStats, "record_tntrun", 0);
        int bowSpleefWins = getIntValue(tntGamesStats, "wins_bowspleef", 0);
        int wizardsWins = getIntValue(tntGamesStats, "wins_capture", 0);
        int pvpRunWins = getIntValue(tntGamesStats, "wins_pvprun", 0);
        
        // Set all the stats
        stats.setTntgamesWins(wins);
        stats.setTntgamesCoins(coins);
        stats.setTntRunWins(tntRunWins);
        stats.setTntRunRecord(tntRunRecord);
        stats.setBowSpleefWins(bowSpleefWins);
        stats.setWizardsWins(wizardsWins);
        stats.setPvpRunWins(pvpRunWins);
    }
    
    /**
     * Parse UHC stats from API response
     */
    private void parseUHCStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject uhcStats = null;
        if (statsObj.has("UHC") && !statsObj.get("UHC").isJsonNull()) {
            uhcStats = statsObj.getAsJsonObject("UHC");
        } else {
            return; // No UHC stats
        }
        
        // Extract basic stats
        int wins = getIntValue(uhcStats, "wins", 0);
        int kills = getIntValue(uhcStats, "kills", 0);
        int deaths = getIntValue(uhcStats, "deaths", 0);
        int coins = getIntValue(uhcStats, "coins", 0);
        int score = getIntValue(uhcStats, "score", 0);
        
        // Calculate KD ratio
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        
        // Set all the stats
        stats.setUhcWins(wins);
        stats.setUhcKills(kills);
        stats.setUhcDeaths(deaths);
        stats.setUhcKDRatio(kdRatio);
        stats.setUhcCoins(coins);
        stats.setUhcScore(score);
    }
    
    /**
     * Parse Build Battle stats from API response
     */
    private void parseBuildBattleStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject buildStats = null;
        if (statsObj.has("BuildBattle") && !statsObj.get("BuildBattle").isJsonNull()) {
            buildStats = statsObj.getAsJsonObject("BuildBattle");
        } else {
            return; // No Build Battle stats
        }
        
        // Extract basic stats
        int wins = getIntValue(buildStats, "wins", 0);
        int gamesPlayed = getIntValue(buildStats, "games_played", 0);
        int score = getIntValue(buildStats, "score", 0);
        int coins = getIntValue(buildStats, "coins", 0);
        
        // Set all the stats
        stats.setBuildBattleWins(wins);
        stats.setBuildBattleGamesPlayed(gamesPlayed);
        stats.setBuildBattleScore(score);
        stats.setBuildBattleCoins(coins);
    }
    
    /**
     * Parse Mega Walls stats from API response
     */
    private void parseMegaWallsStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject megaWallsStats = null;
        if (statsObj.has("Walls3") && !statsObj.get("Walls3").isJsonNull()) {
            megaWallsStats = statsObj.getAsJsonObject("Walls3");
        } else {
            return; // No Mega Walls stats
        }
        
        // Extract basic stats
        int wins = getIntValue(megaWallsStats, "wins", 0);
        int kills = getIntValue(megaWallsStats, "kills", 0);
        int deaths = getIntValue(megaWallsStats, "deaths", 0);
        int assists = getIntValue(megaWallsStats, "assists", 0);
        int finalKills = getIntValue(megaWallsStats, "final_kills", 0);
        int finalDeaths = getIntValue(megaWallsStats, "final_deaths", 0);
        
        // Calculate derived stats
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        double finalKdRatio = finalDeaths > 0 ? (double) finalKills / finalDeaths : finalKills;
        
        // Set all the stats
        stats.setMegaWallsWins(wins);
        stats.setMegaWallsKills(kills);
        stats.setMegaWallsDeaths(deaths);
        stats.setMegaWallsKDRatio(kdRatio);
        stats.setMegaWallsAssists(assists);
        stats.setMegaWallsFinalKills(finalKills);
        stats.setMegaWallsFinalDeaths(finalDeaths);
        stats.setMegaWallsFinalKDRatio(finalKdRatio);
    }
    
    /**
     * Parse SkyBlock stats from API response
     */
    private void parseSkyBlockStats(PlayerStats stats, JsonObject statsObj, JsonObject playerData) {
        // SkyBlock stats are more complex and stored in separate API endpoints
        // We'll extract the profile name and basic coin data if available
        if (statsObj.has("SkyBlock") && !statsObj.get("SkyBlock").isJsonNull()) {
            JsonObject skyBlockStats = statsObj.getAsJsonObject("SkyBlock");
            
            if (skyBlockStats.has("profiles")) {
                JsonElement profiles = skyBlockStats.get("profiles");
                
                // Handle different profile structures - can be either object or array
                if (profiles.isJsonObject()) {
                    // Handle as object - likely the new API format
                    JsonObject profilesObj = profiles.getAsJsonObject();
                    
                    // Just take the first profile we find
                    for (Map.Entry<String, JsonElement> entry : profilesObj.entrySet()) {
                        if (entry.getValue().isJsonObject()) {
                            JsonObject profile = entry.getValue().getAsJsonObject();
                            setProfileData(stats, profile);
                            break; // Just use the first profile
                        }
                    }
                } else if (profiles.isJsonArray() && profiles.getAsJsonArray().size() > 0) {
                    // Handle as array - older API format
                    JsonObject profile = profiles.getAsJsonArray().get(0).getAsJsonObject();
                    setProfileData(stats, profile);
                }
            }
        }
        
        // If we didn't get any SkyBlock data, set defaults
        if (stats.getSkyblockProfile() == null) {
            stats.setSkyblockProfile("None");
            stats.setSkyblockBank(0);
            stats.setSkyblockPurse(0);
            stats.setSkyblockCoins(0);
            
            // Set default skill levels
            stats.setSkyblockFarmingLevel(1);
            stats.setSkyblockMiningLevel(1);
            stats.setSkyblockCombatLevel(1);
            stats.setSkyblockForagingLevel(1);
            stats.setSkyblockFishingLevel(1);
            stats.setSkyblockEnchantingLevel(1);
            stats.setSkyblockAlchemyLevel(1);
            stats.setSkyblockTamingLevel(1);
        }
    }
    
    /**
     * Helper method to set profile data from a profile object
     */
    private void setProfileData(PlayerStats stats, JsonObject profile) {
        // Get profile name
        if (profile.has("cute_name")) {
            stats.setSkyblockProfile(profile.get("cute_name").getAsString());
        } else if (profile.has("profile_name")) {
            stats.setSkyblockProfile(profile.get("profile_name").getAsString());  
        }
        
        // Get basic economy stats
        if (profile.has("banking") && !profile.get("banking").isJsonNull()) {
            JsonObject banking = profile.getAsJsonObject("banking");
            if (banking.has("balance")) {
                stats.setSkyblockBank((int) banking.get("balance").getAsDouble());
            }
        }
        
        if (profile.has("coin_purse")) {
            stats.setSkyblockPurse((int) profile.get("coin_purse").getAsDouble());
        }
        
        // Total coins estimation
        stats.setSkyblockCoins(stats.getSkyblockBank() + stats.getSkyblockPurse());
        
        // We would need separate API calls for skill levels
        // For now, setting default values
        stats.setSkyblockFarmingLevel(1);
        stats.setSkyblockMiningLevel(1);
        stats.setSkyblockCombatLevel(1);
        stats.setSkyblockForagingLevel(1);
        stats.setSkyblockFishingLevel(1);
        stats.setSkyblockEnchantingLevel(1);
        stats.setSkyblockAlchemyLevel(1);
        stats.setSkyblockTamingLevel(1);
    }
    
    /**
     * Extract player rank information
     */
    private void parsePlayerRank(PlayerStats stats, JsonObject playerData) {
        String rank = "DEFAULT";
        String rankColor = "GRAY";
        
        if (playerData.has("rank") && !playerData.get("rank").isJsonNull()) {
            rank = playerData.get("rank").getAsString();
        } else if (playerData.has("monthlyPackageRank") && !playerData.get("monthlyPackageRank").isJsonNull() 
                && !playerData.get("monthlyPackageRank").getAsString().equals("NONE")) {
            rank = playerData.get("monthlyPackageRank").getAsString();
        } else if (playerData.has("newPackageRank") && !playerData.get("newPackageRank").isJsonNull()) {
            rank = playerData.get("newPackageRank").getAsString();
        } else if (playerData.has("packageRank") && !playerData.get("packageRank").isJsonNull()) {
            rank = playerData.get("packageRank").getAsString();
        }
        
        // Assign color based on rank
        if (rank.contains("MVP+")) {
            rankColor = "AQUA";
        } else if (rank.contains("MVP")) {
            rankColor = "AQUA";
        } else if (rank.contains("VIP+")) {
            rankColor = "GREEN";
        } else if (rank.contains("VIP")) {
            rankColor = "GREEN";
        } else if (rank.equals("YOUTUBER")) {
            rankColor = "RED";
        }
        
        // Format the rank for display
        if (!rank.equals("DEFAULT")) {
            rank = rank.replace("_PLUS", "+").replace("SUPERSTAR", "MVP++");
            stats.setRank(rank);
            stats.setRankColor(rankColor);
        }
    }
    
    /**
     * Parse player social media links
     */
    private void parseSocialMedia(PlayerStats stats, JsonObject playerData) {
        if (playerData.has("socialMedia") && !playerData.get("socialMedia").isJsonNull()) {
            JsonObject socialMedia = playerData.getAsJsonObject("socialMedia");
            
            if (socialMedia.has("links") && !socialMedia.get("links").isJsonNull()) {
                JsonObject links = socialMedia.getAsJsonObject("links");
                
                for (Map.Entry<String, JsonElement> entry : links.entrySet()) {
                    if (!entry.getValue().isJsonNull()) {
                        stats.getSocialLinks().put(entry.getKey().toUpperCase(), entry.getValue().getAsString());
                    }
                }
            }
        }
    }
    
    /**
     * Parse player achievements
     */
    private void parseAchievements(PlayerStats stats, JsonObject playerData) {
        if (playerData.has("achievementsOneTime") && !playerData.get("achievementsOneTime").isJsonNull()) {
            try {
                JsonArray achievements = playerData.getAsJsonArray("achievementsOneTime");
                int completedCount = achievements.size();
                
                // Estimate total achievements (adjust this number based on current Hypixel data)
                int totalAchievements = 250;
                int percent = (int)Math.round((completedCount / (double)totalAchievements) * 100);
                stats.setAchievementCompletionPercent(percent);
            } catch (Exception e) {
                log.error("Error parsing achievements: {}", e.getMessage());
            }
        }
    }
    
    /**
     * Parse player status (online/offline)
     */
    private void parsePlayerStatus(PlayerStats stats, JsonObject playerData) {
        if (playerData.has("lastLogin") && playerData.has("lastLogout")) {
            long lastLogin = playerData.get("lastLogin").getAsLong();
            long lastLogout = playerData.get("lastLogout").getAsLong();
            
            if (lastLogin > lastLogout) {
                stats.setCurrentStatus("Online");
                
                if (playerData.has("mostRecentGameType") && !playerData.get("mostRecentGameType").isJsonNull()) {
                    String gameType = playerData.get("mostRecentGameType").getAsString();
                    stats.setCurrentStatus("Online • " + formatGameType(gameType));
                }
            } else {
                stats.setCurrentStatus("Offline");
            }
        } else {
            stats.setCurrentStatus("Unknown");
        }
    }
    
    /**
     * Format game type name for display
     */
    private String formatGameType(String gameType) {
        switch (gameType) {
            case "BEDWARS": return "Bedwars";
            case "SKYWARS": return "Skywars";
            case "MURDER_MYSTERY": return "Murder Mystery";
            case "SKYBLOCK": return "SkyBlock";
            case "DUELS": return "Duels";
            case "ARCADE": return "Arcade";
            case "TNTGAMES": return "TNT Games";
            case "HOUSING": return "Housing";
            case "UHC": return "UHC";
            case "MCGO": return "Cops & Crims";
            case "BATTLEGROUND": return "Warlords";
            default: return gameType;
        }
    }
    
    /**
     * Fetch guild information for a player
     */
    private void fetchGuildInfo(PlayerStats stats) {
        if (stats.getUuid() == null) {
            return;
        }
        
        String apiKey = SettingsManager.getInstance().getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }
        
        try {
            String url = "https://api.hypixel.net/guild?player=" + stats.getUuid();
            Request request = new Request.Builder()
                    .url(url)
                    .header("API-Key", apiKey)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return;
                }
                
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                
                if (!jsonResponse.get("success").getAsBoolean() || jsonResponse.get("guild").isJsonNull()) {
                    return;
                }
                
                JsonObject guild = jsonResponse.getAsJsonObject("guild");
                
                if (guild.has("name")) {
                    stats.setGuildName(guild.get("name").getAsString());
                }
                
                if (guild.has("tag")) {
                    stats.setGuildTag(guild.get("tag").getAsString());
                }
                
                if (guild.has("tagColor")) {
                    stats.setGuildColor(guild.get("tagColor").getAsString());
                }
                
                // Find player's rank
                if (guild.has("members") && guild.get("members").isJsonArray()) {
                    JsonArray members = guild.getAsJsonArray("members");
                    
                    for (JsonElement member : members) {
                        JsonObject memberObj = member.getAsJsonObject();
                        
                        if (memberObj.has("uuid") && memberObj.get("uuid").getAsString().equals(stats.getUuid())) {
                            if (memberObj.has("rank")) {
                                stats.setGuildRank(memberObj.get("rank").getAsString());
                            } else {
                                stats.setGuildRank("Member");
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error fetching guild info: {}", e.getMessage());
        }
    }
    
    private int getIntValue(JsonObject jsonObject, String key, int defaultValue) {
        if (jsonObject.has(key) && !jsonObject.get(key).isJsonNull()) {
            return jsonObject.get(key).getAsInt();
        }
        return defaultValue;
    }
    
    /**
     * Exception thrown when there's an API-level error
     */
    public static class ApiException extends Exception {
        public ApiException(String message) {
            super(message);
        }
    }
} 