# HypeStats Architecture Standardization Guide

This document outlines the standardization of the stats system in HypeStats, explaining how to migrate from the direct fields approach to the new object-oriented architecture.

## Motivation

The previous implementation had several issues:

1. **Scalability problems**: Adding new game modes required adding many fields directly to the `PlayerStats` class
2. **Code duplication**: Common logic like calculating ratios was repeated for each game mode
3. **Inconsistent naming**: Different game modes used different field naming patterns
4. **Complex maintenance**: Changes to one game mode could affect others
5. **Difficult to extend**: New game-specific stats required modifying PlayerStats class

## New Architecture

The new architecture is based on these principles:

1. **Modular**: Each game mode has its own class
2. **Hierarchical**: Common functionality is in a base class
3. **Standardized**: Consistent naming across game modes
4. **Extensible**: New game modes can be added without modifying existing code
5. **Maintainable**: Game-specific logic is encapsulated

## Core Classes

- **GameStats**: Base class for all game mode stats with common fields and methods
- **BedwarsStats, MegaWallsStats, etc.**: Game-specific subclasses
- **GameStatsFactory**: Creates appropriate stat objects for each game mode
- **PlayerStats**: Contains player info and a map of game stats

## Migration Steps

### 1. API Service Changes

In `HypixelApiService.java`, update the parse methods:

```java
private void parseBedwarsStats(PlayerStats stats, JsonObject statsObj, JsonObject playerData) {
    // Create BedwarsStats object
    BedwarsStats bedwarsStats = (BedwarsStats) GameStatsFactory.createGameStats("BEDWARS");
    
    JsonObject bedwarsData = null;
    if (statsObj.has("Bedwars") && !statsObj.get("Bedwars").isJsonNull()) {
        bedwarsData = statsObj.getAsJsonObject("Bedwars");
    } else {
        bedwarsData = new JsonObject();
    }
    
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
    bedwarsStats.setWins(getIntValue(bedwarsData, "wins_bedwars", 0));
    bedwarsStats.setLosses(getIntValue(bedwarsData, "losses_bedwars", 0));
    bedwarsStats.setKills(getIntValue(bedwarsData, "kills_bedwars", 0));
    bedwarsStats.setDeaths(getIntValue(bedwarsData, "deaths_bedwars", 0));
    
    // Set Bedwars-specific stats
    bedwarsStats.setFinalKills(getIntValue(bedwarsData, "final_kills_bedwars", 0));
    bedwarsStats.setFinalDeaths(getIntValue(bedwarsData, "final_deaths_bedwars", 0));
    bedwarsStats.setBedsBroken(getIntValue(bedwarsData, "beds_broken_bedwars", 0));
    bedwarsStats.setBedsLost(getIntValue(bedwarsData, "beds_lost_bedwars", 0));
    
    // Winstreak might be hidden
    if (bedwarsData.has("winstreak") && !bedwarsData.get("winstreak").isJsonNull()) {
        bedwarsStats.setWinstreak(bedwarsData.get("winstreak").getAsInt());
    }
    
    // Calculate derived stats
    bedwarsStats.calculateDerivedStats();
    
    // Add to player's game stats
    stats.addGameStats("BEDWARS", bedwarsStats);
}
```

Repeat this pattern for each game mode parser, creating the appropriate game stats object and populating its fields.

### 2. Mock Data Generation

Update the mock data generation in `generateMockPlayerStats`:

```java
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
    // ... other network stats ...
    
    // Create and add Bedwars stats
    BedwarsStats bedwarsStats = (BedwarsStats) GameStatsFactory.createGameStats("BEDWARS");
    bedwarsStats.setLevel(random.nextInt(1000) + 1);
    bedwarsStats.setWins(random.nextInt(5000) + 1);
    bedwarsStats.setLosses(random.nextInt(2000) + 1);
    // ... set more Bedwars stats ...
    
    bedwarsStats.calculateDerivedStats();
    stats.addGameStats("BEDWARS", bedwarsStats);
    
    // Create and add other game stats similarly
    
    return stats;
}
```

### 3. Controller Updates

Update `PlayerLookupController.java` to work with the new structure:

```java
private void displayBedwarsStats(PlayerStats stats) {
    BedwarsStats bedwarsStats = stats.getBedwarsStats();
    
    // If no stats available, don't display anything
    if (bedwarsStats == null || !bedwarsStats.hasStats()) {
        return;
    }
    
    // Bedwars Stats
    winsLabel.setText(String.valueOf(bedwarsStats.getWins()));
    lossesLabel.setText(String.valueOf(bedwarsStats.getLosses()));
    wlRatioLabel.setText(bedwarsStats.getFormattedWLRatio());
    applyStatStyle(wlRatioLabel, bedwarsStats.getWlRatio(), "bedwarsWLRatio");
    
    winstreakLabel.setText(bedwarsStats.getWinstreakDisplay());
    
    killsLabel.setText(String.valueOf(bedwarsStats.getKills()));
    deathsLabel.setText(String.valueOf(bedwarsStats.getDeaths()));
    
    kdRatioLabel.setText(bedwarsStats.getFormattedKDRatio());
    applyStatStyle(kdRatioLabel, bedwarsStats.getKdRatio(), "bedwarsKDRatio");
    
    gamesPlayedLabel.setText(String.valueOf(bedwarsStats.getGamesPlayed()));
    
    finalKillsLabel.setText(String.valueOf(bedwarsStats.getFinalKills()));
    finalDeathsLabel.setText(String.valueOf(bedwarsStats.getFinalDeaths()));
    
    finalKdRatioLabel.setText(bedwarsStats.getFormattedFinalKDRatio());
    applyStatStyle(finalKdRatioLabel, bedwarsStats.getFinalKDRatio(), "bedwarsFinalKDRatio");
    
    bedsBrokenLabel.setText(String.valueOf(bedwarsStats.getBedsBroken()));
    bedsLostLabel.setText(String.valueOf(bedwarsStats.getBedsLost()));
    bedRatioLabel.setText(bedwarsStats.getFormattedBedRatio());
}
```

Apply the same pattern to all other display methods in the controller.

### 4. Adding New Game Modes

To add a new game mode:

1. Create a new class extending `GameStats` with game-specific fields
2. Update `GameStatsFactory` to return your new class for the appropriate game type
3. Implement a parser method in `HypixelApiService`
4. Add the game mode to the mock data generator if needed
5. Update the controller to display the new game stats

## Benefits

1. **Consistency**: All game modes follow the same structure
2. **Modularity**: Each game mode is self-contained
3. **Extensibility**: Easy to add new game modes
4. **Maintainability**: Easier to understand and modify
5. **Reduced redundancy**: Common code is in the base class

## Example: Adding a new Paintball game mode

1. Create `PaintballStats.java`:

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class PaintballStats extends GameStats {
    private int shots;
    private int hits;
    private double accuracy;
    
    @Override
    public void calculateDerivedStats() {
        super.calculateDerivedStats();
        
        // Calculate accuracy
        accuracy = shots > 0 ? (double) hits / shots : 0;
    }
    
    public String getFormattedAccuracy() {
        return String.format("%.2f%%", accuracy * 100);
    }
}
```

2. Update `GameStatsFactory`:

```java
public static GameStats createGameStats(String gameType) {
    switch (gameType) {
        case "BEDWARS":
            return new BedwarsStats();
        // ... other cases ...
        case "PAINTBALL":
            return new PaintballStats();
        default:
            return new GameStats();
    }
}
```

3. Add a parser method in `HypixelApiService`:

```java
private void parsePaintballStats(PlayerStats stats, JsonObject statsObj) {
    JsonObject paintballData = getJsonObjectSafely(statsObj, "Paintball");
    if (paintballData == null) return;
    
    PaintballStats paintballStats = (PaintballStats) GameStatsFactory.createGameStats("PAINTBALL");
    
    paintballStats.setWins(getIntValue(paintballData, "wins", 0));
    paintballStats.setKills(getIntValue(paintballData, "kills", 0));
    paintballStats.setDeaths(getIntValue(paintballData, "deaths", 0));
    paintballStats.setCoins(getIntValue(paintballData, "coins", 0));
    
    // Paintball-specific
    paintballStats.setShots(getIntValue(paintballData, "shots_fired", 0));
    paintballStats.setHits(getIntValue(paintballData, "kills", 0)); // Assuming hits = kills
    
    paintballStats.calculateDerivedStats();
    stats.addGameStats("PAINTBALL", paintballStats);
}
```

4. Add to mock data generator if needed
5. Create a display method in the controller

## Conclusion

This standardized approach will make the codebase more maintainable and extensible, allowing for easier addition of new game modes and ensuring consistent behavior across all parts of the application. 