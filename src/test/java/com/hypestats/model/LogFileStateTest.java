package com.hypestats.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Integration test for the LobbyTracker using real-world log scenarios
 */
class LogFileStateTest {
    
    @BeforeAll
    static void setUp() throws IOException {
        // Create test log directories if they don't exist
        Files.createDirectories(Paths.get("src/test/resources/logs"));
        
        // We'll create more complex test scenarios
        createBedWarsGameLog();
        createLobbyTransitionLog();
        createPartyWarpLog();
    }
    
    @Test
    void testBedWarsGameStateTracking() {
        // Process the BedWars game log
        LogProcessor.TrackerStateRecorder recorder = new LogProcessor.TrackerStateRecorder();
        LobbyTracker tracker = new LobbyTracker();
        tracker.addListener(recorder);
        
        List<String> logLines = LogProcessor.loadLogFile("bedwars_game.log");
        for (String line : logLines) {
            tracker.processLogLine(line);
        }
        
        // Verify the game state is properly detected
        assertTrue(tracker.isInGame(), "Should detect being in a game");
        assertEquals("Bed Wars", recorder.getGameMode(), "Should detect Bed Wars game mode");
        
        // Verify team assignment was detected
        assertFalse(recorder.getTeam().isEmpty(), "Should have detected team assignment");
        
        // Verify bed destruction was tracked
        assertTrue(recorder.getEvents().stream()
                .anyMatch(e -> e.contains("Bed Destruction")), 
                "Should have detected a bed destruction event");
        
        // Verify final kills were tracked
        assertTrue(recorder.getEvents().stream()
                .anyMatch(e -> e.contains("Final Kill")), 
                "Should have detected final kill events");
        
        // Verify the number of players tracked
        assertTrue(recorder.getPlayers().size() > 5, 
                "Should have tracked multiple players");
    }
    
    @Test
    void testLobbyTransitions() {
        // Process the lobby transition log
        LogProcessor.TrackerStateRecorder recorder = new LogProcessor.TrackerStateRecorder();
        LobbyTracker tracker = new LobbyTracker();
        tracker.addListener(recorder);
        
        List<String> logLines = LogProcessor.loadLogFile("lobby_transition.log");
        for (String line : logLines) {
            tracker.processLogLine(line);
        }
        
        // Verify that we detect leaving a game and returning to lobby
        assertFalse(tracker.isInGame(), "Should detect being in lobby at end");
        
        // Verify that the lobby change events were tracked
        long lobbyChangeCount = recorder.getEvents().stream()
                .filter(e -> e.startsWith("Lobby Change:"))
                .count();
        assertTrue(lobbyChangeCount >= 2, 
                "Should have detected at least 2 lobby changes");
        
        // Verify we detected the return to lobby event
        assertTrue(recorder.getEvents().stream()
                .anyMatch(e -> e.contains("Return to Lobby")), 
                "Should have detected return to lobby event");
    }
    
    @Test
    void testPartyWarpBehavior() {
        // Process the party warp log
        LogProcessor.TrackerStateRecorder recorder = new LogProcessor.TrackerStateRecorder();
        LobbyTracker tracker = new LobbyTracker();
        tracker.addListener(recorder);
        
        List<String> logLines = LogProcessor.loadLogFile("party_warp.log");
        for (String line : logLines) {
            tracker.processLogLine(line);
        }
        
        // Verify party warp was detected
        assertTrue(recorder.getEvents().stream()
                .anyMatch(e -> e.contains("Party Warp")), 
                "Should have detected party warp event");
        
        // Verify that after party warp we detect being in a game
        assertTrue(tracker.isInGame(), "Should detect being in game after party warp");
    }
    
    /**
     * Create a BedWars game log file for testing
     */
    private static void createBedWarsGameLog() throws IOException {
        List<String> logLines = new ArrayList<>();
        
        // Connection and lobby
        logLines.add("[20:50:26] [Client thread/INFO]: Connecting to mc.hypixel.net., 25565");
        logLines.add("[20:50:27] [Client thread/INFO]: [CHAT] To leave Bed Wars, type /lobby");
        
        // Game banner
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT] ████████████████████████████");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT]                                   Bed Wars");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT] ");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT]      Protect your bed and destroy the enemy beds.");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT]       Upgrade yourself and your team by collecting");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT]     Iron, Gold, Emerald and Diamond from generators");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT]                   to access powerful upgrades.");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT] ");
        logLines.add("[20:51:09] [Client thread/INFO]: [CHAT] ████████████████████████████");
        
        // Game starting
        logLines.add("[20:51:10] [Client thread/INFO]: [CHAT] The game starts in 10 seconds!");
        logLines.add("[20:51:15] [Client thread/INFO]: [CHAT] The game starts in 5 seconds!");
        logLines.add("[20:51:20] [Client thread/INFO]: [CHAT] The game has started!");
        
        // Player list
        logLines.add("[20:51:12] [Client thread/INFO]: [CHAT] ONLINE: frogigog, tuumopi, c1utch_craft, Dani313, Inmorrtal300, Xatrok, PlushLuigi26, 8l0ckhead, 1xkhu, AceDart, IdkWhoWaterIs, Adryan206, Rassu2014, Ent3SuessSauer, skeertskeert, GALBINSKY1");
        
        // Team assignment
        logLines.add("[20:51:15] [Client thread/INFO]: [CHAT] You have joined Blue Team!");
        
        // Game interactions
        logLines.add("[20:51:32] [Client thread/INFO]: [CHAT] 8l0ckhead fell into the void.");
        logLines.add("[20:59:51] [Client thread/INFO]: [CHAT] You purchased Fireball (+1 Silver Coin [500])");
        logLines.add("[20:59:56] [Client thread/INFO]: [CHAT] +25 Bed Wars Experience (Time Played)");
        
        // Bed destruction
        logLines.add("[21:00:41] [Client thread/INFO]: [CHAT] ");
        logLines.add("[21:00:41] [Client thread/INFO]: [CHAT] BED DESTRUCTION > Yellow Bed was bed #1,306 destroyed by 8l0ckhead!");
        logLines.add("[21:00:41] [Client thread/INFO]: [CHAT] ");
        
        // Final kill
        logLines.add("[21:00:41] [Client thread/INFO]: [CHAT] skeertskeert was 8l0ckhead's final #1,780. FINAL KILL!");
        
        // Player death and elimination
        logLines.add("[21:01:13] [Client thread/INFO]: [CHAT] Dani313 was crushed into moon dust by Ent3SuessSauer r 9.");
        logLines.add("[21:01:13] [Client thread/INFO]: [CHAT] +2 Slumber Tickets! (Kill)");
        
        // Team elimination
        logLines.add("[21:02:08] [Client thread/INFO]: [CHAT] ");
        logLines.add("[21:02:08] [Client thread/INFO]: [CHAT] TEAM ELIMINATED > Yellow Team has been eliminated!");
        logLines.add("[21:02:08] [Client thread/INFO]: [CHAT] ");
        
        // Player death and respawn
        logLines.add("[21:01:47] [Client thread/INFO]: [CHAT] Ent3SuessSauer r 9 was stomped by AceDart.");
        logLines.add("[21:01:47] [Client thread/INFO]: [CHAT] You will respawn in 5 seconds!");
        logLines.add("[21:01:52] [Client thread/INFO]: [CHAT] You have respawned!");
        
        // Write the log to file
        Path logFile = Paths.get("src/test/resources/logs/bedwars_game.log");
        Files.write(logFile, logLines);
    }
    
    /**
     * Create a log file with lobby transitions for testing
     */
    private static void createLobbyTransitionLog() throws IOException {
        List<String> logLines = new ArrayList<>();
        
        // Start in main lobby
        logLines.add("[19:30:00] [Client thread/INFO]: [CHAT] Welcome to Hypixel!");
        logLines.add("[19:30:05] [Client thread/INFO]: [CHAT] Player1 joined the lobby!");
        
        // Join a queue
        logLines.add("[19:30:10] [Client thread/INFO]: [CHAT] You have joined the queue for Bed Wars!");
        
        // Transition to a game lobby
        logLines.add("[19:30:15] [Client thread/INFO]: [CHAT] Sending you to mini123AB!");
        logLines.add("[19:30:20] [Client thread/INFO]: [CHAT] Player2 has joined (1/16)!");
        logLines.add("[19:30:25] [Client thread/INFO]: [CHAT] The game starts in 10 seconds!");
        logLines.add("[19:30:28] [Client thread/INFO]: [CHAT] The game starts in 5 seconds!");
        logLines.add("[19:30:30] [Client thread/INFO]: [CHAT] The game has started!");
        
        // Game banner
        logLines.add("[19:30:31] [Client thread/INFO]: [CHAT] ████████████████████████████");
        logLines.add("[19:30:31] [Client thread/INFO]: [CHAT]                                   Bed Wars");
        logLines.add("[19:30:31] [Client thread/INFO]: [CHAT] ████████████████████████████");
        
        // Game ended
        logLines.add("[19:40:00] [Client thread/INFO]: [CHAT] VICTORY!");
        
        // Return to lobby
        logLines.add("[19:40:05] [Client thread/INFO]: [CHAT] Sending you to limbo1!");
        logLines.add("[19:40:10] [Client thread/INFO]: [CHAT] Welcome to Hypixel!");
        
        // Write the log to file
        Path logFile = Paths.get("src/test/resources/logs/lobby_transition.log");
        Files.write(logFile, logLines);
    }
    
    /**
     * Create a log file with party warp behavior for testing
     */
    private static void createPartyWarpLog() throws IOException {
        List<String> logLines = new ArrayList<>();
        
        // Start in main lobby
        logLines.add("[18:00:00] [Client thread/INFO]: [CHAT] Welcome to Hypixel!");
        
        // Party formed
        logLines.add("[18:00:10] [Client thread/INFO]: [CHAT] You have joined PartyLeader's party!");
        logLines.add("[18:00:15] [Client thread/INFO]: [CHAT] Player1 has joined the party!");
        
        // Party warp
        logLines.add("[18:00:20] [Client thread/INFO]: [CHAT] [MVP+] PartyLeader has warped the party to SkyWars!");
        
        // Lobby reached
        logLines.add("[18:00:25] [Client thread/INFO]: [CHAT] Sending you to mini789XY!");
        logLines.add("[18:00:30] [Client thread/INFO]: [CHAT] PartyLeader has joined (1/8)!");
        logLines.add("[18:00:35] [Client thread/INFO]: [CHAT] You have joined (2/8)!");
        logLines.add("[18:00:40] [Client thread/INFO]: [CHAT] Player1 has joined (3/8)!");
        
        // Game start
        logLines.add("[18:00:45] [Client thread/INFO]: [CHAT] The game starts in 10 seconds!");
        logLines.add("[18:00:50] [Client thread/INFO]: [CHAT] The game starts in 5 seconds!");
        logLines.add("[18:00:55] [Client thread/INFO]: [CHAT] The game has started!");
        
        // Game banner
        logLines.add("[18:00:56] [Client thread/INFO]: [CHAT] ████████████████████████████");
        logLines.add("[18:00:56] [Client thread/INFO]: [CHAT]                                   SkyWars");
        logLines.add("[18:00:56] [Client thread/INFO]: [CHAT] ████████████████████████████");
        
        // Write the log to file
        Path logFile = Paths.get("src/test/resources/logs/party_warp.log");
        Files.write(logFile, logLines);
    }
} 