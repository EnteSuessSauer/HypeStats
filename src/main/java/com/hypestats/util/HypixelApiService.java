package com.hypestats.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.hypestats.model.PlayerStats;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for interacting with the Hypixel API
 */
public class HypixelApiService {
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String HYPIXEL_API_URL = "https://api.hypixel.net/player";
    private static final int MAX_REQUESTS_PER_MINUTE = 110; // Hypixel allows 120/min but we use 110 to be safe
    
    private final OkHttpClient client;
    private final Gson gson;
    private final List<Long> requestTimestamps;
    
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
        
        // Get Bedwars stats
        JsonObject bedwarsStats = null;
        if (playerData.has("stats") && !playerData.get("stats").isJsonNull()) {
            JsonObject statsObj = playerData.getAsJsonObject("stats");
            if (statsObj.has("Bedwars") && !statsObj.get("Bedwars").isJsonNull()) {
                bedwarsStats = statsObj.getAsJsonObject("Bedwars");
            }
        }
        
        if (bedwarsStats == null) {
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
        stats.setLevel(bedwarsExp);
        
        // Get player rank
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
        stats.setWins(wins);
        stats.setLosses(losses);
        stats.setWlRatio(wlRatio);
        stats.setKills(kills);
        stats.setDeaths(deaths);
        stats.setKdRatio(kdRatio);
        stats.setFinalKills(finalKills);
        stats.setFinalDeaths(finalDeaths);
        stats.setFinalKdRatio(finalKdRatio);
        stats.setWinstreak(winstreak);
        stats.setBedsBroken(bedsBroken);
        stats.setBedsLost(bedsLost);
        stats.setGamesPlayed(gamesPlayed);
        
        return stats;
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