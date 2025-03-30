package com.hypestats.model;

import java.util.regex.Pattern;

/**
 * Immutable collection of regex patterns used for Hypixel lobby tracking
 * Organized by category to improve maintainability
 */
public record LobbyPatterns() {

    /**
     * Patterns for basic lobby navigation
     */
    public static final class LobbyNavigation {
        public static final Pattern LOBBY_CHANGE = Pattern.compile("\\[CHAT\\] Sending you to (\\w+)!");
        public static final Pattern MAIN_LOBBY = Pattern.compile("\\[CHAT\\] (?:Welcome to|You are currently connected to server) (?:the )?(Hypixel|Lobby|limbo)");
        public static final Pattern QUEUE_JOIN = Pattern.compile("\\[CHAT\\] You have joined the queue for (.+)!");
        public static final Pattern CONNECTING = Pattern.compile("\\[Client thread/INFO\\]: Connecting to (.+?), (\\d+)");
        public static final Pattern RECONNECT = Pattern.compile("\\[CHAT\\] (?:\\[.+?\\] )?(.+?) has reconnected!|\\[CHAT\\] (?:You have reconnected to (?:the )?|Reconnecting you to )(.+?)!");
        public static final Pattern PARTY_WARP = Pattern.compile("\\[CHAT\\] (?:\\[.+?\\] )?(.+?) has warped the party to (.+?)!|\\[CHAT\\] (.+?) warped you to (.+?)!");
    }
    
    /**
     * Patterns for player tracking
     */
    public static final class PlayerTracking {
        public static final Pattern PLAYER_JOIN = Pattern.compile("\\[CHAT\\] (.+?) has joined \\((\\d+)/(\\d+)\\)!");
        public static final Pattern PLAYER_QUIT = Pattern.compile("\\[CHAT\\] (.+?) has quit!");
        public static final Pattern PLAYER_LIST = Pattern.compile("\\[CHAT\\] ONLINE: (.+)");
        public static final Pattern PLAYER_DEATH_VOID = Pattern.compile("\\[CHAT\\] (.+?) fell into the void");
        public static final Pattern PLAYER_DEATH_BY_PLAYER = Pattern.compile("\\[CHAT\\] (.+?) was (.+?) by (.+?)\\.");
        public static final Pattern TEAM_CHAT = Pattern.compile("\\[CHAT\\] \\[(\\d+)\\?\\] \\[(\\w+)\\] (.+)");
    }
    
    /**
     * Patterns for game state detection
     */
    public static final class GameState {
        public static final Pattern GAME_START_COUNTDOWN = Pattern.compile("\\[CHAT\\] The game starts in (\\d+) seconds?!");
        public static final Pattern GAME_START_FINAL = Pattern.compile("\\[CHAT\\] The game starts in 1 second!");
        public static final Pattern GAME_STARTED = Pattern.compile("\\[CHAT\\] The game has started!");
        public static final Pattern GAME_MODE = Pattern.compile("\\[CHAT\\] You are playing on (\\w+)");
        public static final Pattern TEAM_ASSIGNMENT = Pattern.compile("\\[CHAT\\] You have joined (\\w+) Team!");
        public static final Pattern VICTORY = Pattern.compile("\\[CHAT\\] VICTORY!");
        public static final Pattern GAME_END = Pattern.compile("\\[CHAT\\] .+ team wins!");
        public static final Pattern PLAYER_ELIMINATED = Pattern.compile("\\[CHAT\\] You have been eliminated!");
        public static final Pattern CROSS_TEAMING_WARNING = Pattern.compile("\\[CHAT\\] Cross-teaming is not allowed! Report cross-teamers using /report\\.");
    }
    
    /**
     * Patterns for game banners
     */
    public static final class GameBanners {
        public static final Pattern BANNER_TOP = Pattern.compile("\\[CHAT\\] (?:▬+|=+|\\?+|▒+|█+){2,}");
        public static final Pattern BANNER_TITLE = Pattern.compile("\\[CHAT\\]\\s+(.+?)(Bed Wars|SkyWars|Duels|UHC|Mega Walls|Murder Mystery|Arcade|Build Battle|TNT Games|Blitz SG)(.*)");
        public static final Pattern BANNER_FOOTER = Pattern.compile("\\[CHAT\\] (?:▬+|=+|\\?+|▒+|█+){2,}");
        public static final Pattern GAME_START_BEDWARS = Pattern.compile("\\[CHAT\\] Bed Wars");
    }
    
    /**
     * Patterns for Bed Wars specific events
     */
    public static final class BedWars {
        public static final Pattern BED_DESTRUCTION = Pattern.compile("\\[CHAT\\] BED DESTRUCTION > (.+?) Bed was bed #([\\d,]+) destroyed by (.+?)!");
        public static final Pattern BED_DESTROYED_SPECTATOR = Pattern.compile("\\[CHAT\\] Your bed was destroyed so you are a spectator!");
        public static final Pattern FINAL_KILL = Pattern.compile("\\[CHAT\\] (.+?) was .+ by .+?\\. FINAL KILL!");
        public static final Pattern FINAL_KILL_COUNTER = Pattern.compile("\\[CHAT\\] (.+?) was (.+?)'s final #([\\d,]+)\\. FINAL KILL!");
        public static final Pattern TEAM_ELIMINATED = Pattern.compile("\\[CHAT\\] TEAM ELIMINATED > (.+?) Team has been eliminated!");
        public static final Pattern PLAYER_RESPAWN_COUNTDOWN = Pattern.compile("\\[CHAT\\] You will respawn in (\\d+) seconds?!");
        public static final Pattern PLAYER_RESPAWNED = Pattern.compile("\\[CHAT\\] You have respawned!");
    }
    
    /**
     * Patterns for in-game economy and rewards
     */
    public static final class Economy {
        public static final Pattern ITEM_PURCHASE = Pattern.compile("\\[CHAT\\] You purchased (.+?) \\(\\+\\d+ (.+?)\\)");
        public static final Pattern UPGRADE_PURCHASE = Pattern.compile("\\[CHAT\\] (.+?) purchased (.+)");
        public static final Pattern EXPERIENCE_GAIN = Pattern.compile("\\[CHAT\\] \\+(\\d+) (.+?) Experience \\((.+?)\\)");
        public static final Pattern TOKENS_GAIN = Pattern.compile("\\[CHAT\\] \\+(\\d+) tokens! \\((.+?)\\)");
        public static final Pattern ITEM_DEPOSIT = Pattern.compile("\\[CHAT\\] You deposited (.+?) into your Team Chest!");
        public static final Pattern RESOURCE_GAIN = Pattern.compile("\\[CHAT\\] \\+(\\d+) (Iron|Gold|Diamond|Emerald)");
        public static final Pattern TICKET_GAIN = Pattern.compile("\\[CHAT\\] \\+(\\d+) (.+?) Tickets! \\((.+?)\\)");
    }
} 