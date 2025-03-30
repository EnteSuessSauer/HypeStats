package com.hypestats.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// This is a test class, unused imports may be needed for test setup
@SuppressWarnings("unused")
public class LobbyPatternsTest {

    @Test
    void testLobbyNavigationPatterns() {
        // Test LOBBY_CHANGE pattern
        String lobbyChangeText = "[CHAT] Sending you to mini42UDB!";
        assertTrue(LobbyPatterns.LobbyNavigation.LOBBY_CHANGE.matcher(lobbyChangeText).find());
        Matcher matcher = LobbyPatterns.LobbyNavigation.LOBBY_CHANGE.matcher(lobbyChangeText);
        assertTrue(matcher.find());
        assertEquals("mini42UDB", matcher.group(1));
        
        // Test MAIN_LOBBY pattern
        assertTrue(LobbyPatterns.LobbyNavigation.MAIN_LOBBY.matcher("[CHAT] Welcome to Hypixel").find());
        assertTrue(LobbyPatterns.LobbyNavigation.MAIN_LOBBY.matcher("[CHAT] You are currently connected to server Lobby").find());
        
        // Test RECONNECT pattern
        String reconnectText = "[CHAT] You have reconnected to Bed Wars!";
        matcher = LobbyPatterns.LobbyNavigation.RECONNECT.matcher(reconnectText);
        assertTrue(matcher.find());
        assertNull(matcher.group(1)); // No player name in this case
        assertEquals("Bed Wars", matcher.group(2)); // Destination
        
        // Test PARTY_WARP pattern
        String partyWarpText = "[CHAT] [MVP+] PlayerName has warped the party to SkyWars!";
        matcher = LobbyPatterns.LobbyNavigation.PARTY_WARP.matcher(partyWarpText);
        assertTrue(matcher.find());
        assertEquals("PlayerName", matcher.group(1));
        assertEquals("SkyWars", matcher.group(2));
    }
    
    @Test
    void testPlayerTrackingPatterns() {
        // Test PLAYER_JOIN pattern
        String playerJoinText = "[CHAT] SomePlayer has joined (8/16)!";
        Matcher matcher = LobbyPatterns.PlayerTracking.PLAYER_JOIN.matcher(playerJoinText);
        assertTrue(matcher.find());
        assertEquals("SomePlayer", matcher.group(1));
        assertEquals("8", matcher.group(2));
        assertEquals("16", matcher.group(3));
        
        // Test PLAYER_LIST pattern
        String playerListText = "[CHAT] ONLINE: player1, player2, player3, player4";
        matcher = LobbyPatterns.PlayerTracking.PLAYER_LIST.matcher(playerListText);
        assertTrue(matcher.find());
        assertEquals("player1, player2, player3, player4", matcher.group(1));
        
        // Test PLAYER_DEATH_VOID pattern
        String playerVoidText = "[CHAT] PlayerName fell into the void";
        matcher = LobbyPatterns.PlayerTracking.PLAYER_DEATH_VOID.matcher(playerVoidText);
        assertTrue(matcher.find());
        assertEquals("PlayerName", matcher.group(1));
    }
    
    @Test
    void testGameStatePatterns() {
        // Test GAME_START_COUNTDOWN pattern
        String countdownText = "[CHAT] The game starts in 10 seconds!";
        Matcher matcher = LobbyPatterns.GameState.GAME_START_COUNTDOWN.matcher(countdownText);
        assertTrue(matcher.find());
        assertEquals("10", matcher.group(1));
        
        // Test TEAM_ASSIGNMENT pattern
        String teamText = "[CHAT] You have joined Red Team!";
        matcher = LobbyPatterns.GameState.TEAM_ASSIGNMENT.matcher(teamText);
        assertTrue(matcher.find());
        assertEquals("Red", matcher.group(1));
        
        // Test VICTORY pattern
        assertTrue(LobbyPatterns.GameState.VICTORY.matcher("[CHAT] VICTORY!").find());
    }
    
    @Test
    void testGameBannerPatterns() {
        // Test BANNER_TOP and BANNER_FOOTER patterns
        assertTrue(LobbyPatterns.GameBanners.BANNER_TOP.matcher("[CHAT] ████████████████████████████").find());
        assertTrue(LobbyPatterns.GameBanners.BANNER_FOOTER.matcher("[CHAT] ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬").find());
        
        // Test BANNER_TITLE pattern
        String bannerTitle = "[CHAT]                                   Bed Wars";
        Matcher matcher = LobbyPatterns.GameBanners.BANNER_TITLE.matcher(bannerTitle);
        assertTrue(matcher.find());
        assertEquals("Bed Wars", matcher.group(2));
    }
    
    @Test
    void testBedWarsPatterns() {
        // Test BED_DESTRUCTION pattern
        String bedDestruction = "[CHAT] BED DESTRUCTION > Red Bed was bed #1,306 destroyed by PlayerName!";
        Matcher matcher = LobbyPatterns.BedWars.BED_DESTRUCTION.matcher(bedDestruction);
        assertTrue(matcher.find());
        assertEquals("Red", matcher.group(1));
        assertEquals("1,306", matcher.group(2));
        assertEquals("PlayerName", matcher.group(3));
        
        // Test FINAL_KILL_COUNTER pattern
        String finalKill = "[CHAT] VictimName was KillerName's final #1,784. FINAL KILL!";
        matcher = LobbyPatterns.BedWars.FINAL_KILL_COUNTER.matcher(finalKill);
        assertTrue(matcher.find());
        assertEquals("VictimName", matcher.group(1));
        assertEquals("KillerName", matcher.group(2));
        assertEquals("1,784", matcher.group(3));
        
        // Test PLAYER_RESPAWN_COUNTDOWN pattern
        String respawnText = "[CHAT] You will respawn in 5 seconds!";
        matcher = LobbyPatterns.BedWars.PLAYER_RESPAWN_COUNTDOWN.matcher(respawnText);
        assertTrue(matcher.find());
        assertEquals("5", matcher.group(1));
    }
    
    @Test
    void testEconomyPatterns() {
        // Test ITEM_PURCHASE pattern
        String purchaseText = "[CHAT] You purchased Golden Apple (+1 Silver Coin [500])";
        Matcher matcher = LobbyPatterns.Economy.ITEM_PURCHASE.matcher(purchaseText);
        assertTrue(matcher.find());
        assertEquals("Golden Apple", matcher.group(1));
        assertEquals("Silver Coin [500]", matcher.group(2));
        
        // Test UPGRADE_PURCHASE pattern
        String upgradeText = "[CHAT] PlayerName purchased Reinforced Armor I";
        matcher = LobbyPatterns.Economy.UPGRADE_PURCHASE.matcher(upgradeText);
        assertTrue(matcher.find());
        assertEquals("PlayerName", matcher.group(1));
        assertEquals("Reinforced Armor I", matcher.group(2));
        
        // Test RESOURCE_GAIN pattern
        String resourceText = "[CHAT] +40 Iron";
        matcher = LobbyPatterns.Economy.RESOURCE_GAIN.matcher(resourceText);
        assertTrue(matcher.find());
        assertEquals("40", matcher.group(1));
        assertEquals("Iron", matcher.group(2));
    }
} 