package com.hypestats.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Service for interacting with the Hypixel API.
 */
@Slf4j
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class HypixelAPIService {
    private static final String HYPIXEL_API_URL = "https://api.hypixel.net";
    private static final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    private final String apiKey;
    private final Map<String, UUID> uuidCache = new HashMap<>();
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    
    @Getter
    private int remainingLimit = 300; // Default Hypixel API limit is 300 requests per 5 minutes
    
    private Instant lastRateLimitReset = Instant.now();
    private Instant nextAllowedRequest = Instant.MIN;
    
    private Consumer<String> statusCallback;
    
    public HypixelAPIService(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public void setStatusCallback(Consumer<String> statusCallback) {
        this.statusCallback = statusCallback;
    }
    
    private void updateStatus(String status) {
        if (statusCallback != null) {
            Platform.runLater(() -> statusCallback.accept(status));
        }
    }
    
    /**
     * Get UUID for a Minecraft username
     * @param username The username to look up
     * @return The UUID or null if not found
     */
    public UUID getUUID(String username) throws IOException, InterruptedException {
        if (username == null || username.isEmpty() || username.equalsIgnoreCase("you")) {
            return null;
        }
        
        // Check cache first
        if (uuidCache.containsKey(username.toLowerCase())) {
            return uuidCache.get(username.toLowerCase());
        }
        
        // Call Mojang API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MOJANG_API_URL + username))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode jsonNode = objectMapper.readTree(response.body());
            String uuidStr = jsonNode.get("id").asText();
            
            // Add hyphens to UUID format
            String formattedUuid = uuidStr.replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"
            );
            
            UUID uuid = UUID.fromString(formattedUuid);
            uuidCache.put(username.toLowerCase(), uuid);
            return uuid;
        } else if (response.statusCode() == 429) {
            log.warn("Rate limited by Mojang API");
            updateStatus("Rate limited by Mojang API");
            throw new IOException("Rate limited by Mojang API");
        } else {
            log.warn("Failed to get UUID for {}: {}", username, response.statusCode());
            return null;
        }
    }
    
    /**
     * Get player data from Hypixel API
     * @param playerName The player name to look up
     * @return PlayerData object or null if not found
     */
    public PlayerData getPlayerData(String playerName) throws IOException, InterruptedException {
        if (playerName == null || playerName.isEmpty() || playerName.equalsIgnoreCase("you")) {
            return null;
        }
        
        // Check rate limit
        if (remainingLimit <= 0) {
            Duration timeSinceReset = Duration.between(lastRateLimitReset, Instant.now());
            if (timeSinceReset.toMinutes() >= 5) {
                // Reset rate limit after 5 minutes
                remainingLimit = 300;
                lastRateLimitReset = Instant.now();
            } else {
                log.warn("API rate limit reached. Waiting for reset.");
                updateStatus("API rate limit reached. Waiting for reset.");
                return null;
            }
        }
        
        // Respect the "next allowed request" time to prevent hammering
        if (Instant.now().isBefore(nextAllowedRequest)) {
            Duration waitTime = Duration.between(Instant.now(), nextAllowedRequest);
            if (waitTime.toMillis() > 100) { // Only wait if it's a significant amount of time
                log.debug("Waiting {} ms before next API request", waitTime.toMillis());
                Thread.sleep(waitTime.toMillis());
            }
        }
        
        // Get UUID first
        UUID uuid;
        try {
            uuid = getUUID(playerName);
            if (uuid == null) {
                log.warn("Could not find UUID for player: {}", playerName);
                return null;
            }
            
            // Check cache for this UUID
            if (playerDataCache.containsKey(uuid)) {
                return playerDataCache.get(uuid);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Error getting UUID for player: {}", playerName, e);
            return null;
        }
        
        // Call Hypixel API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HYPIXEL_API_URL + "/player?uuid=" + uuid.toString()))
                .header("API-Key", apiKey)
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        // Set the next allowed request time to be at least 250ms from now (to prevent hammering)
        nextAllowedRequest = Instant.now().plusMillis(250);
        
        // Update rate limit based on headers
        if (response.headers().firstValue("RateLimit-Remaining").isPresent()) {
            try {
                remainingLimit = Integer.parseInt(response.headers().firstValue("RateLimit-Remaining").get());
            } catch (NumberFormatException e) {
                log.warn("Could not parse rate limit header", e);
            }
        } else {
            // If no header, just decrement our counter
            remainingLimit--;
        }
        
        if (response.statusCode() == 200) {
            JsonNode rootNode = objectMapper.readTree(response.body());
            
            if (!rootNode.get("success").asBoolean()) {
                log.warn("API returned error: {}", rootNode.get("cause").asText());
                return null;
            }
            
            JsonNode playerNode = rootNode.get("player");
            if (playerNode == null || playerNode.isNull()) {
                log.warn("Player not found: {}", playerName);
                return null;
            }
            
            // Process the player data
            PlayerData playerData = processPlayerData(playerNode, playerName, uuid);
            playerDataCache.put(uuid, playerData);
            
            return playerData;
        } else if (response.statusCode() == 429) {
            log.warn("Rate limited by Hypixel API");
            updateStatus("Rate limited by Hypixel API");
            remainingLimit = 0;
            return null;
        } else {
            log.warn("Failed to get player data for {}: {}", playerName, response.statusCode());
            return null;
        }
    }
    
    /**
     * Process player data from the API response
     */
    @SuppressWarnings({"unused", "UnusedLocalVariable"})
    private PlayerData processPlayerData(JsonNode playerNode, String playerName, UUID uuid) {
        PlayerData.PlayerDataBuilder builder = PlayerData.builder()
                .playerName(playerName)
                .uuid(uuid.toString());
        
        // Extract rank information
        String rank = "Default";
        if (playerNode.has("newPackageRank")) {
            rank = playerNode.get("newPackageRank").asText().replace("_PLUS", "+");
        }
        if (playerNode.has("monthlyPackageRank") && !playerNode.get("monthlyPackageRank").asText().equals("NONE")) {
            rank = playerNode.get("monthlyPackageRank").asText();
        }
        builder.rank(rank);
        
        // Extract general stats
        builder.level(playerNode.has("networkExp") ? calculateNetworkLevel(playerNode.get("networkExp").asDouble()) : 0);
        builder.achievementPoints(playerNode.has("achievementPoints") ? playerNode.get("achievementPoints").asInt() : 0);
        builder.firstLogin(playerNode.has("firstLogin") ? formatTimestamp(playerNode.get("firstLogin").asLong()) : "Unknown");
        builder.lastLogin(playerNode.has("lastLogin") ? formatTimestamp(playerNode.get("lastLogin").asLong()) : "Unknown");
        
        // Extract game-specific stats
        JsonNode statsNode = playerNode.get("stats");
        if (statsNode != null) {
            // BedWars stats
            if (statsNode.has("Bedwars")) {
                JsonNode bedwarsNode = statsNode.get("Bedwars");
                int wins = getIntValue(bedwarsNode, "wins_bedwars", 0);
                int losses = getIntValue(bedwarsNode, "losses_bedwars", 0);
                int kills = getIntValue(bedwarsNode, "kills_bedwars", 0);
                int deaths = getIntValue(bedwarsNode, "deaths_bedwars", 0);
                int finalKills = getIntValue(bedwarsNode, "final_kills_bedwars", 0);
                int finalDeaths = getIntValue(bedwarsNode, "final_deaths_bedwars", 0);
                int bedsBroken = getIntValue(bedwarsNode, "beds_broken_bedwars", 0);
                int bedsLost = getIntValue(bedwarsNode, "beds_lost_bedwars", 0);
                
                builder.bedwarsLevel(calculateBedwarsLevel(getIntValue(bedwarsNode, "Experience", 0)))
                        .bedwarsWins(wins)
                        .bedwarsLosses(losses)
                        .bedwarsKills(kills)
                        .bedwarsDeaths(deaths)
                        .bedwarsFinalKills(finalKills)
                        .bedwarsFinalDeaths(finalDeaths)
                        .bedwarsBedsBroken(bedsBroken)
                        .bedwarsBedsLost(bedsLost)
                        .bedwarsWinLossRatio(PlayerData.calculateRatio(wins, losses))
                        .bedwarsKillDeathRatio(PlayerData.calculateRatio(kills, deaths))
                        .bedwarsFinalKillDeathRatio(PlayerData.calculateRatio(finalKills, finalDeaths));
            }
            
            // SkyWars stats
            if (statsNode.has("SkyWars")) {
                JsonNode skywarsNode = statsNode.get("SkyWars");
                int wins = getIntValue(skywarsNode, "wins", 0);
                int losses = getIntValue(skywarsNode, "losses", 0);
                int kills = getIntValue(skywarsNode, "kills", 0);
                int deaths = getIntValue(skywarsNode, "deaths", 0);
                
                builder.skywarsLevel(calculateSkywarsLevel(getIntValue(skywarsNode, "skywars_experience", 0)))
                        .skywarsWins(wins)
                        .skywarsLosses(losses)
                        .skywarsKills(kills)
                        .skywarsDeaths(deaths)
                        .skywarsWinLossRatio(PlayerData.calculateRatio(wins, losses))
                        .skywarsKillDeathRatio(PlayerData.calculateRatio(kills, deaths));
            }
            
            // Duels stats
            if (statsNode.has("Duels")) {
                JsonNode duelsNode = statsNode.get("Duels");
                int wins = getIntValue(duelsNode, "wins", 0);
                int losses = getIntValue(duelsNode, "losses", 0);
                int kills = getIntValue(duelsNode, "kills", 0);
                int deaths = getIntValue(duelsNode, "deaths", 0);
                
                builder.duelsWins(wins)
                        .duelsLosses(losses)
                        .duelsKills(kills)
                        .duelsDeaths(deaths)
                        .duelsWinLossRatio(PlayerData.calculateRatio(wins, losses))
                        .duelsKillDeathRatio(PlayerData.calculateRatio(kills, deaths));
            }
        }
        
        return builder.build();
    }
    
    private int getIntValue(JsonNode node, String fieldName, int defaultValue) {
        return node.has(fieldName) ? node.get(fieldName).asInt() : defaultValue;
    }
    
    private String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(timestamp));
    }
    
    /**
     * Calculate Hypixel network level from XP
     */
    private int calculateNetworkLevel(double exp) {
        return exp <= 0 ? 1 : (int) Math.floor(1 + (-8750.0 + Math.sqrt(8750 * 8750 + 5000 * exp)) / 2500);
    }
    
    /**
     * Calculate BedWars level from XP
     */
    private int calculateBedwarsLevel(int exp) {
        if (exp < 500) return 0;
        
        int level = 0;
        int expRemaining = exp;
        
        // First level requires 500 XP
        expRemaining -= 500;
        level++;
        
        // Levels 1-99 require level*500 XP
        for (int i = 1; i < 100; i++) {
            int expRequired = i * 500;
            if (expRemaining < expRequired) break;
            expRemaining -= expRequired;
            level++;
        }
        
        return level;
    }
    
    /**
     * Calculate SkyWars level from XP
     */
    private int calculateSkywarsLevel(int exp) {
        int xp = exp;
        int level = 0;
        int[] xpPerLevel = {0, 20, 50, 80, 100, 250, 500, 1000, 1500, 2500, 4000, 5000};
        
        if (xp >= 15000) {
            level = (xp - 15000) / 10000 + 12;
        } else {
            for (int i = 0; i < xpPerLevel.length; i++) {
                if (xp < xpPerLevel[i]) {
                    level = i;
                    break;
                }
            }
        }
        
        return level;
    }
    
    /**
     * Asynchronously fetch player data
     */
    public CompletableFuture<PlayerData> getPlayerDataAsync(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getPlayerData(playerName);
            } catch (IOException | InterruptedException e) {
                log.error("Error fetching player data for {}", playerName, e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        });
    }
} 