package com.hypestats.model;

/**
 * Factory for creating appropriate game stats objects
 */
public class GameStatsFactory {
    
    /**
     * Create a GameStats object appropriate for the given game type
     * @param gameType The game type identifier
     * @return Appropriate GameStats implementation
     */
    public static GameStats createGameStats(String gameType) {
        switch (gameType) {
            case "BEDWARS":
                return new BedwarsStats();
            case "MEGA_WALLS":
                return new MegaWallsStats();
            case "TNTGAMES":
                return new TNTGamesStats();
            case "SKYBLOCK":
                return new SkyBlockStats();
            default:
                return new GameStats();
        }
    }
    
    /**
     * Get standardized game type identifier
     * @param apiGameType Game type from API
     * @return Standardized game type identifier
     */
    public static String standardizeGameType(String apiGameType) {
        if (apiGameType == null) return null;
        
        switch (apiGameType) {
            case "BEDWARS":
            case "Bedwars":
                return "BEDWARS";
                
            case "SKYWARS":
            case "SkyWars":
                return "SKYWARS";
                
            case "DUELS":
            case "Duels":
                return "DUELS";
                
            case "MURDER_MYSTERY":
            case "MurderMystery":
                return "MURDER_MYSTERY";
                
            case "TNTGAMES":
            case "TNTGames":
                return "TNTGAMES";
                
            case "UHC":
            case "UHCChampions":
                return "UHC";
                
            case "BUILD_BATTLE":
            case "BuildBattle":
                return "BUILD_BATTLE";
                
            case "MEGA_WALLS":
            case "Walls3":
                return "MEGA_WALLS";
                
            case "SKYBLOCK":
            case "SkyBlock":
                return "SKYBLOCK";
                
            case "ARCADE":
            case "Arcade":
                return "ARCADE";
                
            default:
                return apiGameType.toUpperCase().replace(" ", "_");
        }
    }
} 