package com.hypestats.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypestats.HypeStatsApp;
import com.hypestats.model.PlayerStats;
import com.hypestats.model.GameStats;
import com.hypestats.model.BedwarsStats;
import com.hypestats.model.TNTGamesStats;
import com.hypestats.model.MegaWallsStats;
import com.hypestats.model.SkyBlockStats;
import com.hypestats.model.UHCStats;
import com.hypestats.model.BuildBattleStats;
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
import java.util.HashMap;

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
    private Random random = new Random();
    
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
        
        // Set a random seed based on username for consistent results
        long seed = 0;
        for (char c : username.toCharArray()) {
            seed = 31 * seed + c;
        }
        random = new Random(seed);
        
        // Network stats
        stats.setNetworkLevel(random.nextInt(250) + 1);
        stats.setKarma(random.nextInt(100000) + 1);
        stats.setAchievementPoints(random.nextInt(10000) + 1);
        stats.setAchievementCompletionPercent(random.nextInt(100) + 1);
        
        // Generate believable test data for Bedwars
        BedwarsStats bedwarsStats = new BedwarsStats();
        bedwarsStats.setLevel(random.nextInt(500) + 1); // More realistic range of Bedwars levels
        bedwarsStats.setWins(random.nextInt(5000) + 1);
        bedwarsStats.setLosses(random.nextInt(2000) + 1);
        bedwarsStats.setKills(random.nextInt(15000) + 1);
        bedwarsStats.setDeaths(random.nextInt(10000) + 1);
        bedwarsStats.setFinalKills(random.nextInt(10000) + 1);
        bedwarsStats.setFinalDeaths(random.nextInt(5000) + 1);
        bedwarsStats.setBedsBroken(random.nextInt(5000) + 1);
        bedwarsStats.setBedsLost(random.nextInt(3000) + 1);
        bedwarsStats.setGamesPlayed(bedwarsStats.getWins() + bedwarsStats.getLosses());
        bedwarsStats.setWinstreak(random.nextInt(20) + 1);
        bedwarsStats.calculateDerivedStats();
        stats.addGameStats("BEDWARS", bedwarsStats);
        
        // Generate mock Skywars stats
        GameStats skywarsStats = new GameStats();
        skywarsStats.setLevel(random.nextInt(50) + 1);
        skywarsStats.setWins(random.nextInt(2000) + 1);
        skywarsStats.setLosses(random.nextInt(3000) + 1);
        skywarsStats.setKills(random.nextInt(8000) + 1);
        skywarsStats.setDeaths(random.nextInt(7000) + 1);
        skywarsStats.setCoins(random.nextInt(100000) + 1);
        skywarsStats.setGamesPlayed(skywarsStats.getWins() + skywarsStats.getLosses());
        skywarsStats.calculateDerivedStats();
        stats.addGameStats("SKYWARS", skywarsStats);
        
        // Generate mock Duels stats
        GameStats duelsStats = new GameStats();
        duelsStats.setWins(random.nextInt(3000) + 1);
        duelsStats.setLosses(random.nextInt(2000) + 1);
        duelsStats.setKills(random.nextInt(6000) + 1);
        duelsStats.setDeaths(random.nextInt(4000) + 1);
        duelsStats.setCoins(random.nextInt(50000) + 1);
        duelsStats.setGamesPlayed(duelsStats.getWins() + duelsStats.getLosses());
        duelsStats.calculateDerivedStats();
        stats.addGameStats("DUELS", duelsStats);
        
        // ----- NEW GAME MODES WITH CLEAR DISTINCT VALUES -----
        
        // Generate mock Murder Mystery stats
        GameStats mmStats = new GameStats();
        mmStats.setWins(100 + random.nextInt(900));
        mmStats.setGamesPlayed(500 + random.nextInt(2500));
        mmStats.setKills(200 + random.nextInt(4800));
        mmStats.setDeaths(100 + random.nextInt(3900));
        mmStats.setCoins(5000 + random.nextInt(25000));
        mmStats.calculateDerivedStats();
        stats.addGameStats("MURDER_MYSTERY", mmStats);
        
        // Generate mock TNT Games stats
        TNTGamesStats tntStats = new TNTGamesStats();
        tntStats.setWins(150 + random.nextInt(1350));
        tntStats.setCoins(6000 + random.nextInt(34000));
        tntStats.setTntRunWins(50 + random.nextInt(750));
        tntStats.setTntRunRecord(200 + random.nextInt(300));
        tntStats.setBowSpleefWins(75 + random.nextInt(325));
        tntStats.setWizardsWins(25 + random.nextInt(175));
        tntStats.setPvpRunWins(50 + random.nextInt(250));
        tntStats.calculateDerivedStats();
        stats.addGameStats("TNTGAMES", tntStats);
        
        // Generate mock UHC stats
        GameStats uhcStats = new GameStats();
        uhcStats.setWins(50 + random.nextInt(250));
        uhcStats.setKills(300 + random.nextInt(1200));
        uhcStats.setDeaths(200 + random.nextInt(800));
        uhcStats.setCoins(4000 + random.nextInt(16000));
        uhcStats.setScore(1000 + random.nextInt(4000));
        uhcStats.calculateDerivedStats();
        stats.addGameStats("UHC", uhcStats);
        
        // Generate mock Build Battle stats
        BuildBattleStats buildBattleStats = new BuildBattleStats();
        buildBattleStats.setWins(75 + random.nextInt(425));
        buildBattleStats.setGamesPlayed(300 + random.nextInt(1200));
        buildBattleStats.setScore(800 + random.nextInt(2200));
        buildBattleStats.setCoins(3000 + random.nextInt(22000));
        buildBattleStats.calculateDerivedStats();
        stats.addGameStats("BUILDBATTLE", buildBattleStats);
        
        // Generate mock Mega Walls stats
        MegaWallsStats megaWallsStats = new MegaWallsStats();
        megaWallsStats.setWins(80 + random.nextInt(320));
        megaWallsStats.setKills(500 + random.nextInt(2500));
        megaWallsStats.setDeaths(400 + random.nextInt(2100));
        megaWallsStats.setAssists(200 + random.nextInt(800));
        megaWallsStats.setFinalKills(300 + random.nextInt(1200));
        megaWallsStats.setFinalDeaths(200 + random.nextInt(800));
        megaWallsStats.calculateDerivedStats();
        stats.addGameStats("MEGA_WALLS", megaWallsStats);
        
        // Generate mock SkyBlock stats
        SkyBlockStats skyBlockStats = new SkyBlockStats();
        String[] profileNames = {"Banana", "Pineapple", "Apple", "Strawberry", "Grape", "Mango", "Coconut"};
        skyBlockStats.setProfileName(profileNames[random.nextInt(profileNames.length)]);
        skyBlockStats.setBank(10000000 + random.nextInt(90000000));
        skyBlockStats.setPurse(1000000 + random.nextInt(9000000));
        skyBlockStats.setCoins(skyBlockStats.getBank() + skyBlockStats.getPurse());
        stats.addGameStats("SKYBLOCK", skyBlockStats);
        
        // Randomly assign rank
        String[] possibleRanks = {"DEFAULT", "VIP", "VIP+", "MVP", "MVP+", "MVP++"};
        String[] rankColors = {"GRAY", "GREEN", "GREEN", "AQUA", "AQUA", "GOLD"};
        int rankIndex = random.nextInt(possibleRanks.length);
        
        stats.setRank(possibleRanks[rankIndex]);
        stats.setRankColor(rankColors[rankIndex]);
        
        // 70% chance to have a visible winstreak
        if (random.nextInt(10) < 7) {
            // Get the BedwarsStats from PlayerStats
            BedwarsStats bwStats = (BedwarsStats) stats.getGameStats("BEDWARS");
            if (bwStats != null) {
                bwStats.setWinstreak(random.nextInt(50) + 1);
            }
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
        
        // Set current status
        String[] possibleGames = {"Lobby", "Bedwars", "Skywars", "Murder Mystery", "Duels", "Build Battle", "UHC"};
        boolean isOnline = random.nextInt(10) < 7; // 70% chance to be online
        
        if (isOnline) {
            stats.setCurrentStatus("Online in " + possibleGames[random.nextInt(possibleGames.length)]);
        } else {
            stats.setCurrentStatus("Offline");
        }
        
        // Add some social media links
        Map<String, String> socialLinks = new HashMap<>();
        if (random.nextInt(10) < 7) {
            // 70% chance to have Discord
            socialLinks.put("discord", username + "#" + (1000 + random.nextInt(9000)));
        }
        if (random.nextInt(10) < 5) {
            // 50% chance to have YouTube
            socialLinks.put("youtube", "https://youtube.com/c/" + username);
        }
        if (random.nextInt(10) < 3) {
            // 30% chance to have Twitch
            socialLinks.put("twitch", "https://twitch.tv/" + username);
        }
        stats.setSocialLinks(socialLinks);
        
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
        parseSkyBlockStats(stats, statsObj);
        
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
            return; // No Bedwars stats
        }
        
        // Extract basic stats
        int wins = getIntValue(bedwarsStats, "wins_bedwars", 0);
        int losses = getIntValue(bedwarsStats, "losses_bedwars", 0);
        int kills = getIntValue(bedwarsStats, "kills_bedwars", 0);
        int deaths = getIntValue(bedwarsStats, "deaths_bedwars", 0);
        int finalKills = getIntValue(bedwarsStats, "final_kills_bedwars", 0);
        int finalDeaths = getIntValue(bedwarsStats, "final_deaths_bedwars", 0);
        int bedsBroken = getIntValue(bedwarsStats, "beds_broken_bedwars", 0);
        int bedsLost = getIntValue(bedwarsStats, "beds_lost_bedwars", 0);
        int gamesPlayed = wins + losses;
        Integer winstreak = null;
        
        // Winstreak might be hidden for privacy
        if (bedwarsStats.has("winstreak") && !bedwarsStats.get("winstreak").isJsonNull()) {
            winstreak = bedwarsStats.get("winstreak").getAsInt();
        }
        
        // Calculate ratios
        double wlRatio = losses > 0 ? (double) wins / losses : wins;
        double kdRatio = deaths > 0 ? (double) kills / deaths : kills;
        double finalKdRatio = finalDeaths > 0 ? (double) finalKills / finalDeaths : finalKills;
        
        // Calculate experience & level
        double bedwarsExp = 0;
        if (bedwarsStats.has("Experience") && !bedwarsStats.get("Experience").isJsonNull()) {
            bedwarsExp = bedwarsStats.get("Experience").getAsDouble();
        }
        
        // Level calculation based on prestige & level
        int prestige = 0;
        int level = 0;
        
        // Check player achievement data for stars_bedwars
        if (playerData.has("achievements") && !playerData.get("achievements").isJsonNull()) {
            JsonObject achievements = playerData.getAsJsonObject("achievements");
            if (achievements.has("bedwars_level") && !achievements.get("bedwars_level").isJsonNull()) {
                level = achievements.get("bedwars_level").getAsInt();
            }
        }
        
        // If level is still 0 but we have experience, calculate level from experience
        if (level == 0 && bedwarsExp > 0) {
            // Bedwars level calculation formula:
            // First level requires 500 XP, second level requires 1000 XP
            // All other levels require 5000 XP
            if (bedwarsExp < 500) {
                level = 0;
            } else if (bedwarsExp < 1500) {
                level = 1;
            } else {
                level = (int) Math.floor((bedwarsExp - 1500) / 5000) + 2;
            }
        }
        
        // Create BedwarsStats object
        BedwarsStats bwStats = new BedwarsStats();
        bwStats.setLevel(level);
        bwStats.setWins(wins);
        bwStats.setLosses(losses);
        bwStats.setKills(kills);
        bwStats.setDeaths(deaths);
        bwStats.setFinalKills(finalKills);
        bwStats.setFinalDeaths(finalDeaths);
        bwStats.setBedsBroken(bedsBroken);
        bwStats.setBedsLost(bedsLost);
        bwStats.setGamesPlayed(gamesPlayed);
        bwStats.setWinstreak(winstreak);
        
        // Calculate derived stats
        bwStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("BEDWARS", bwStats);
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
        int gamesPlayed = getIntValue(skywarsStats, "games_played", 0);
        if (gamesPlayed == 0) {
            gamesPlayed = wins + losses; // If games_played not available, estimate from wins/losses
        }
        
        // Skywars specific stats
        double skywarsExp = 0;
        if (skywarsStats.has("skywars_experience") && !skywarsStats.get("skywars_experience").isJsonNull()) {
            skywarsExp = skywarsStats.get("skywars_experience").getAsDouble();
            // Formula for Skywars level calculation (approximation)
            skywarsExp = Math.floor(skywarsExp / 10000) + 1;
        }
        
        // Create GameStats object for Skywars
        GameStats swStats = new GameStats();
        swStats.setLevel(skywarsExp);
        swStats.setWins(wins);
        swStats.setLosses(losses);
        swStats.setKills(kills);
        swStats.setDeaths(deaths);
        swStats.setCoins(coins);
        swStats.setGamesPlayed(gamesPlayed);
        
        // Calculate derived stats
        swStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("SKYWARS", swStats);
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
        int gamesPlayed = getIntValue(duelsStats, "games_played_duels", 0);
        if (gamesPlayed == 0) {
            gamesPlayed = wins + losses; // If games_played not available, estimate from wins/losses
        }
        
        // Create GameStats object for Duels
        GameStats duelsGameStats = new GameStats();
        duelsGameStats.setWins(wins);
        duelsGameStats.setLosses(losses);
        duelsGameStats.setKills(kills);
        duelsGameStats.setDeaths(deaths);
        duelsGameStats.setCoins(coins);
        duelsGameStats.setGamesPlayed(gamesPlayed);
        
        // Calculate derived stats
        duelsGameStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("DUELS", duelsGameStats);
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
        
        // Create GameStats object for Murder Mystery
        GameStats mmGameStats = new GameStats();
        mmGameStats.setWins(wins);
        mmGameStats.setGamesPlayed(gamesPlayed);
        mmGameStats.setKills(kills);
        mmGameStats.setDeaths(deaths);
        mmGameStats.setCoins(coins);
        
        // Calculate derived stats
        mmGameStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("MURDER_MYSTERY", mmGameStats);
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
        
        // Create GameStats object for Arcade
        GameStats arcadeGameStats = new GameStats();
        arcadeGameStats.setWins(wins);
        arcadeGameStats.setCoins(coins);
        
        // Calculate derived stats
        arcadeGameStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("ARCADE", arcadeGameStats);
    }
    
    /**
     * Parse TNT Games stats from API response
     */
    private void parseTNTGamesStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject tntStats = null;
        if (statsObj.has("TNTGames") && !statsObj.get("TNTGames").isJsonNull()) {
            tntStats = statsObj.getAsJsonObject("TNTGames");
        } else {
            return; // No TNT Games stats
        }
        
        // Extract basic stats
        int wins = getIntValue(tntStats, "wins", 0);
        int coins = getIntValue(tntStats, "coins", 0);
        
        // Extract TNT-specific stats
        int tntRunWins = getIntValue(tntStats, "wins_tntrun", 0);
        int tntRunRecord = getIntValue(tntStats, "record_tntrun", 0);
        int bowSpleefWins = getIntValue(tntStats, "wins_bowspleef", 0);
        int wizardsWins = getIntValue(tntStats, "wins_capture", 0);
        int pvpRunWins = getIntValue(tntStats, "wins_pvprun", 0);
        
        // Create TNTGamesStats object
        TNTGamesStats tntGameStats = new TNTGamesStats();
        tntGameStats.setWins(wins);
        tntGameStats.setCoins(coins);
        tntGameStats.setTntRunWins(tntRunWins);
        tntGameStats.setTntRunRecord(tntRunRecord);
        tntGameStats.setBowSpleefWins(bowSpleefWins);
        tntGameStats.setWizardsWins(wizardsWins);
        tntGameStats.setPvpRunWins(pvpRunWins);
        
        // Calculate derived stats
        tntGameStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("TNTGAMES", tntGameStats);
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
        
        // Create UHCStats object
        UHCStats uhcGameStats = new UHCStats();
        uhcGameStats.setWins(wins);
        uhcGameStats.setKills(kills);
        uhcGameStats.setDeaths(deaths);
        uhcGameStats.setCoins(coins);
        uhcGameStats.setScore(score);
        
        // Calculate derived stats
        uhcGameStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("UHC", uhcGameStats);
    }
    
    /**
     * Parse Build Battle stats from API response
     */
    private void parseBuildBattleStats(PlayerStats stats, JsonObject statsObj) {
        JsonObject buildBattleStats = null;
        if (statsObj.has("BuildBattle") && !statsObj.get("BuildBattle").isJsonNull()) {
            buildBattleStats = statsObj.getAsJsonObject("BuildBattle");
        } else {
            return; // No Build Battle stats
        }
        
        // Extract basic stats
        int wins = getIntValue(buildBattleStats, "wins", 0);
        int gamesPlayed = getIntValue(buildBattleStats, "games_played", 0);
        int score = getIntValue(buildBattleStats, "score", 0);
        int coins = getIntValue(buildBattleStats, "coins", 0);
        
        // Create BuildBattleStats object
        BuildBattleStats buildBattleGameStats = new BuildBattleStats();
        buildBattleGameStats.setWins(wins);
        buildBattleGameStats.setGamesPlayed(gamesPlayed);
        buildBattleGameStats.setScore(score);
        buildBattleGameStats.setCoins(coins);
        
        // Calculate derived stats
        buildBattleGameStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("BUILDBATTLE", buildBattleGameStats);
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
        int finalAssists = getIntValue(megaWallsStats, "final_assists", 0);
        
        // Create MegaWallsStats object
        MegaWallsStats megaWallsGameStats = new MegaWallsStats();
        megaWallsGameStats.setWins(wins);
        megaWallsGameStats.setKills(kills);
        megaWallsGameStats.setDeaths(deaths);
        megaWallsGameStats.setAssists(assists);
        megaWallsGameStats.setFinalKills(finalKills);
        megaWallsGameStats.setFinalAssists(finalAssists);
        
        // Calculate derived stats
        megaWallsGameStats.calculateDerivedStats();
        
        // Add to PlayerStats
        stats.addGameStats("MEGA_WALLS", megaWallsGameStats);
    }
    
    /**
     * Parse SkyBlock stats from API response
     */
    private void parseSkyBlockStats(PlayerStats stats, JsonObject playerObj) {
        // Check if SkyBlock profiles exist
        if (!playerObj.has("stats") || !playerObj.getAsJsonObject("stats").has("SkyBlock")) {
            return; // No SkyBlock stats
        }

        // Create SkyBlockStats object
        SkyBlockStats skyBlockStats = new SkyBlockStats();
        
        // Get player's UUID
        String playerUuid = playerObj.has("uuid") ? playerObj.get("uuid").getAsString() : "";
        if (playerUuid.isEmpty()) {
            return; // No player UUID found
        }
        
        // Get the player's SkyBlock profiles
        JsonObject skyblockProfiles = null;
        try {
            skyblockProfiles = fetchSkyBlockProfiles(playerUuid);
        } catch (Exception e) {
            System.err.println("Error fetching SkyBlock profiles: " + e.getMessage());
            return;
        }
        
        if (skyblockProfiles == null || !skyblockProfiles.has("profiles") || skyblockProfiles.get("profiles").isJsonNull()) {
            return; // No SkyBlock profiles found
        }
        
        // Find the active profile (either the one marked as selected or the first one)
        String activeProfileId = "";
        String profileName = "";
        JsonObject activeProfile = null;
        JsonArray profiles = skyblockProfiles.getAsJsonArray("profiles");
        
        for (int i = 0; i < profiles.size(); i++) {
            JsonObject profile = profiles.get(i).getAsJsonObject();
            if (profile.has("selected") && profile.get("selected").getAsBoolean()) {
                activeProfileId = profile.has("profile_id") ? profile.get("profile_id").getAsString() : "";
                profileName = profile.has("cute_name") ? profile.get("cute_name").getAsString() : "";
                activeProfile = profile;
                break;
            }
        }
        
        // If no selected profile found, use the first one
        if (activeProfile == null && profiles.size() > 0) {
            JsonObject profile = profiles.get(0).getAsJsonObject();
            activeProfileId = profile.has("profile_id") ? profile.get("profile_id").getAsString() : "";
            profileName = profile.has("cute_name") ? profile.get("cute_name").getAsString() : "";
            activeProfile = profile;
        }
        
        if (activeProfile == null) {
            return; // No profile found
        }
        
        // Set the profile name
        skyBlockStats.setProfileName(profileName);
        
        // Get member data for the player
        JsonObject memberData = null;
        if (activeProfile.has("members") && activeProfile.getAsJsonObject("members").has(playerUuid)) {
            memberData = activeProfile.getAsJsonObject("members").getAsJsonObject(playerUuid);
        }
        
        if (memberData == null) {
            return; // No member data found
        }
        
        // Extract coin-related stats
        int purseCoins = 0;
        int bankCoins = 0;
        
        if (memberData.has("coin_purse")) {
            try {
                purseCoins = (int) memberData.get("coin_purse").getAsDouble();
            } catch (Exception e) {
                // Failed to parse coin purse
            }
        }
        
        // Bank is in the profile shared data
        if (activeProfile.has("banking") && activeProfile.getAsJsonObject("banking").has("balance")) {
            try {
                bankCoins = (int) activeProfile.getAsJsonObject("banking").get("balance").getAsDouble();
            } catch (Exception e) {
                // Failed to parse bank balance
            }
        }
        
        // Set coin-related stats
        skyBlockStats.setPurse(purseCoins);
        skyBlockStats.setBank(bankCoins);
        skyBlockStats.setCoins(purseCoins + bankCoins);
        
        // Extract skill levels if available
        if (memberData.has("experience_skill_farming")) {
            int farmingLevel = calculateSkillLevel(memberData.get("experience_skill_farming").getAsDouble(), "farming");
            skyBlockStats.setFarmingLevel(farmingLevel);
        }
        
        if (memberData.has("experience_skill_mining")) {
            int miningLevel = calculateSkillLevel(memberData.get("experience_skill_mining").getAsDouble(), "mining");
            skyBlockStats.setMiningLevel(miningLevel);
        }
        
        if (memberData.has("experience_skill_combat")) {
            int combatLevel = calculateSkillLevel(memberData.get("experience_skill_combat").getAsDouble(), "combat");
            skyBlockStats.setCombatLevel(combatLevel);
        }
        
        if (memberData.has("experience_skill_foraging")) {
            int foragingLevel = calculateSkillLevel(memberData.get("experience_skill_foraging").getAsDouble(), "foraging");
            skyBlockStats.setForagingLevel(foragingLevel);
        }
        
        // Add to PlayerStats
        stats.addGameStats("SKYBLOCK", skyBlockStats);
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
                
                // Just set the count of completed achievements
                stats.setAchievementCompletionPercent(completedCount);
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
     * Fetch SkyBlock profiles for a player
     */
    private JsonObject fetchSkyBlockProfiles(String playerUuid) throws IOException {
        String apiKey = SettingsManager.getInstance().getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IOException("API key is not set");
        }
        
        String url = HYPIXEL_API_URL + "/skyblock/profiles?uuid=" + playerUuid;
        Request request = new Request.Builder()
                .url(url)
                .header("API-Key", apiKey)
                .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch SkyBlock profiles: " + response.code());
            }

            String jsonData = response.body().string();
            JsonObject responseObj = new Gson().fromJson(jsonData, JsonObject.class);
            
            if (!responseObj.has("success") || !responseObj.get("success").getAsBoolean()) {
                String message = responseObj.has("cause") ? responseObj.get("cause").getAsString() : "Unknown error";
                throw new IOException("API error: " + message);
            }
            
            return responseObj;
        }
    }
    
    /**
     * Calculate skill level from experience points
     */
    private int calculateSkillLevel(double experience, String skillType) {
        // SkyBlock skill experience requirements
        // These are approximate values for general skills
        double[] skillExperienceRequirements = {
            50, 175, 375, 675, 1175, 1925, 2925, 4425, 6425, 9925, 
            14925, 22425, 32425, 47425, 67425, 97425, 147425, 222425, 322425, 
            472425, 672425, 922425, 1222425, 1622425, 2022425
        };
        
        // Find the level based on experience
        int level = 0;
        while (level < skillExperienceRequirements.length && experience >= skillExperienceRequirements[level]) {
            level++;
        }
        
        return level + 1; // Levels are 1-based
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