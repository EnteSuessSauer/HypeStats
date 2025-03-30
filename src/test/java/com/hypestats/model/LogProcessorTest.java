package com.hypestats.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

class LogProcessorTest {
    
    private static final String TEST_LOG_DIR = "src/test/resources/logs";
    
    @BeforeAll
    static void setUpTestLogs() throws IOException {
        // Ensure test log directory exists
        Path logDir = Paths.get(TEST_LOG_DIR);
        Files.createDirectories(logDir);
        
        // Create a sample test log file if it doesn't exist
        Path sampleLogFile = logDir.resolve("sample_bedwars.log");
        if (!Files.exists(sampleLogFile)) {
            List<String> sampleLines = Arrays.asList(
                "[20:50:26] [Client thread/INFO]: Connecting to mc.hypixel.net., 25565",
                "[20:50:27] [Client thread/INFO]: [CHAT] To leave Bed Wars, type /lobby",
                "[20:50:27] [Client thread/INFO]: [CHAT] Your bed was destroyed so you are a spectator!",
                "[20:50:28] [Client thread/INFO]: [CHAT] -----------------------------------------------------",
                "[20:50:28] [Client thread/INFO]: [CHAT] You are not currently in a party.",
                "[20:50:28] [Client thread/INFO]: [CHAT] -----------------------------------------------------",
                "[20:50:29] [Client thread/INFO]: [CHAT] LeonidM was knocked into the void by Naturaldisastr.",
                "[20:50:31] [Client thread/INFO]: [CHAT]                          ",
                "[20:50:31] [Client thread/INFO]: [CHAT]  b[MVP f+ b] EELLLLLLLLLPRIMO f  6joined the lobby!",
                "[20:50:44] [Client thread/INFO]: [CHAT]  7[3?]  a[VIP 6+ a] hollikat f: does any1 know if u can make hypixel smp in 1.21.5??",
                "[20:50:58] [Client thread/INFO]: [CHAT] Sending you to mini66CK!",
                "[20:50:58] [Client thread/INFO]: [CHAT] Ent3SuessSauer r a has joined (14/16)!",
                "[20:50:59] [Client thread/INFO]: [CHAT] The game starts in 10 seconds!",
                "[20:50:59] [Client thread/INFO]: [CHAT] GbtRnnbYF has joined (15/16)!",
                "[20:51:01] [Client thread/INFO]: [CHAT] yFLOe has joined (16/16)!",
                "[20:51:09] [Client thread/INFO]: [CHAT] ████████████████████████████",
                "[20:51:09] [Client thread/INFO]: [CHAT]                                   Bed Wars",
                "[20:51:09] [Client thread/INFO]: [CHAT] ",
                "[20:51:09] [Client thread/INFO]: [CHAT]      Protect your bed and destroy the enemy beds.",
                "[20:51:09] [Client thread/INFO]: [CHAT]       Upgrade yourself and your team by collecting",
                "[20:51:09] [Client thread/INFO]: [CHAT]     Iron, Gold, Emerald and Diamond from generators",
                "[20:51:09] [Client thread/INFO]: [CHAT]                   to access powerful upgrades.",
                "[20:51:09] [Client thread/INFO]: [CHAT] ",
                "[20:51:09] [Client thread/INFO]: [CHAT] ████████████████████████████",
                "[20:51:12] [Client thread/INFO]: [CHAT] ONLINE: frogigog, tuumopi, c1utch_craft, Dani313, Inmorrtal300, Xatrok, PlushLuigi26, 8l0ckhead, 1xkhu, AceDart, IdkWhoWaterIs, Adryan206, Rassu2014, Ent3SuessSauer, skeertskeert, GALBINSKY1",
                "[20:51:22] [Client thread/INFO]: [CHAT] [SHOUT] [RED] Inmorrtal300: are these snake or frog",
                "[20:51:26] [Client thread/INFO]: [CHAT]  9 7[43?]  9[BLUE]  7tuumopi 7: frog",
                "[20:51:32] [Client thread/INFO]: [CHAT] 8l0ckhead fell into the void.",
                "[20:51:34] [Client thread/INFO]: [CHAT] ONLINE: frogigog, tuumopi, c1utch_craft, Dani313, Inmorrtal300, Xatrok, PlushLuigi26, 1xkhu, AceDart, IdkWhoWaterIs, Adryan206, Rassu2014, Ent3SuessSauer, skeertskeert, GALBINSKY1",
                "[20:51:37] [Client thread/INFO]: [CHAT] skeertskeert fell into the void.",
                "[20:59:51] [Client thread/INFO]: [CHAT] You purchased Fireball (+1 Silver Coin [500])",
                "[20:59:51] [Client thread/INFO]: [CHAT] GALBINSKY1 fell into the void.",
                "[20:59:56] [Client thread/INFO]: [CHAT] +25 Bed Wars Experience (Time Played)",
                "[20:59:56] [Client thread/INFO]: [CHAT] +12 tokens! (Time Played)",
                "[21:00:41] [Client thread/INFO]: [CHAT] ",
                "[21:00:41] [Client thread/INFO]: [CHAT] BED DESTRUCTION > Yellow Bed was bed #1,306 destroyed by 8l0ckhead!",
                "[21:00:41] [Client thread/INFO]: [CHAT] ",
                "[21:00:41] [Client thread/INFO]: [CHAT] skeertskeert was 8l0ckhead's final #1,780. FINAL KILL!",
                "[21:00:41] [Client thread/INFO]: [CHAT] 1xkhu was 8l0ckhead's final #1,781. FINAL KILL!",
                "[21:00:41] [Client thread/INFO]: [CHAT] Inmorrtal300 slipped in BBQ sauce off the edge spilled by IdkWhoWaterIs.",
                "[21:00:46] [Client thread/INFO]: [CHAT] GALBINSKY1 was glazed in BBQ sauce by IdkWhoWaterIs.",
                "[21:00:49] [Client thread/INFO]: [CHAT] Rassu2014 was 8l0ckhead's final #1,782. FINAL KILL!",
                "[21:01:13] [Client thread/INFO]: [CHAT] Dani313 was crushed into moon dust by Ent3SuessSauer r 9.",
                "[21:01:13] [Client thread/INFO]: [CHAT] +1 Iron",
                "[21:01:13] [Client thread/INFO]: [CHAT] +2 Slumber Tickets! (Kill)",
                "[21:01:47] [Client thread/INFO]: [CHAT] Ent3SuessSauer r 9 was stomped by AceDart.",
                "[21:01:47] [Client thread/INFO]: [CHAT] You will respawn in 5 seconds!",
                "[21:01:48] [Client thread/INFO]: [CHAT] You will respawn in 4 seconds!",
                "[21:01:49] [Client thread/INFO]: [CHAT] You will respawn in 3 seconds!",
                "[21:01:50] [Client thread/INFO]: [CHAT] You will respawn in 2 seconds!",
                "[21:01:51] [Client thread/INFO]: [CHAT] You will respawn in 1 second!",
                "[21:01:52] [Client thread/INFO]: [CHAT] You have respawned!",
                "[21:02:08] [Client thread/INFO]: [CHAT] IdkWhoWaterIs was ripped and thrown by c1utch_craft. FINAL KILL!",
                "[21:02:08] [Client thread/INFO]: [CHAT] ",
                "[21:02:08] [Client thread/INFO]: [CHAT] TEAM ELIMINATED > Yellow Team has been eliminated!",
                "[21:02:08] [Client thread/INFO]: [CHAT] ",
                "[21:02:12] [Client thread/INFO]: [CHAT] c1utch_craft was stomped by 8l0ckhead.",
                "[21:03:46] [Client thread/INFO]: [CHAT] ",
                "[21:03:46] [Client thread/INFO]: [CHAT] BED DESTRUCTION > Green Bed was bed #1,307 destroyed by 8l0ckhead!",
                "[21:03:46] [Client thread/INFO]: [CHAT] ",
                "[21:03:52] [Client thread/INFO]: [CHAT] [65?] [BLUE] [VIP] Ent3SuessSauer: incvsi",
                "[21:04:11] [Client thread/INFO]: [CHAT] Dani313 was 8l0ckhead's final #1,783. FINAL KILL!",
                "[21:04:12] [Client thread/INFO]: [CHAT] c1utch_craft was 8l0ckhead's final #1,784. FINAL KILL!",
                "[21:04:15] [Client thread/INFO]: [CHAT] 8l0ckhead was shot by AceDart.",
                "[21:04:25] [Client thread/INFO]: [CHAT]                          ",
                "[21:04:25] [Client thread/INFO]: [CHAT] ? You earned 10 Mystery Dust!",
                "[21:04:26] [Client thread/INFO]: [CHAT]   b> c> a> r  6[MVP 4++ 6] oTrobo f  6joined the lobby!  a< c< b<",
                "[21:04:26] [Client thread/INFO]: Update connection state: '{\"state\":1}'",
                "[21:04:26] [Client thread/ERROR]: Can't ping Hypixel.net: Disconnected"
            );
            Files.write(sampleLogFile, sampleLines);
        }
    }
    
    @Test
    void testLogFileLoading() {
        List<String> logLines = LogProcessor.loadLogFile("sample_bedwars.log");
        assertFalse(logLines.isEmpty(), "Log file should not be empty");
        assertTrue(logLines.size() > 10, "Log file should have multiple lines");
        
        // Verify a few specific lines
        assertTrue(
            logLines.stream().anyMatch(line -> line.contains("Connecting to mc.hypixel.net")),
            "Log should contain connection line"
        );
        assertTrue(
            logLines.stream().anyMatch(line -> line.contains("Bed Wars")),
            "Log should contain game mode name"
        );
    }
    
    @Test
    void testLogProcessing() {
        LobbyTracker tracker = LogProcessor.processLogFile("sample_bedwars.log");
        
        // Expect the game mode to be Bed Wars or Duels (depending on the last detected mode)
        assertTrue(tracker.getGameMode() != null && 
                  (tracker.getGameMode().equals("Bed Wars") || tracker.getGameMode().equals("Duels")), 
                  "Game mode should be detected");
        
        // Verify players were tracked
        Set<String> players = tracker.getRawPlayerNames();
        assertFalse(players.isEmpty(), "Player list should not be empty");
    }
    
    @Test
    void testSummaryGeneration() {
        boolean success = LogProcessor.processAndGenerateSummary("sample_bedwars.log", "sample_bedwars_summary.txt");
        assertTrue(success, "Summary generation should succeed");
        
        // Verify the summary file exists
        Path summaryFile = Paths.get("target", "test-output", "sample_bedwars_summary.txt");
        assertTrue(Files.exists(summaryFile), "Summary file should exist");
        
        try {
            List<String> summaryLines = Files.readAllLines(summaryFile);
            assertFalse(summaryLines.isEmpty(), "Summary file should not be empty");
            
            // Verify summary contains expected sections
            assertTrue(
                summaryLines.stream().anyMatch(line -> line.contains("Lobby Tracker State Summary")),
                "Summary should include state header"
            );
            assertTrue(
                summaryLines.stream().anyMatch(line -> line.contains("Event Log")),
                "Summary should include event log header"
            );
            
            // Check for game-related events
            assertTrue(
                summaryLines.stream().anyMatch(line -> line.contains("Game Mode Detected") || 
                                              line.contains("Game Started") ||
                                              line.contains("Game Ended")),
                "Summary should include game state events"
            );
            
            // Verify events were recorded
            assertTrue(
                summaryLines.stream().anyMatch(line -> line.contains("Bed Destruction")),
                "Summary should include bed destruction event"
            );
            assertTrue(
                summaryLines.stream().anyMatch(line -> line.contains("Final Kill")),
                "Summary should include final kill event"
            );
            
        } catch (IOException e) {
            fail("Failed to read summary file: " + e.getMessage());
        }
    }
    
    @Test
    void testProcessingCustomLogLines() {
        List<String> customLines = Arrays.asList(
            "[12:34:56] [Client thread/INFO]: [CHAT] Sending you to mini123AB!",
            "[12:34:57] [Client thread/INFO]: [CHAT] Player1 has joined (1/8)!",
            "[12:34:58] [Client thread/INFO]: [CHAT] Player2 has joined (2/8)!",
            "[12:34:59] [Client thread/INFO]: [CHAT] The game starts in 10 seconds!",
            "[12:35:00] [Client thread/INFO]: [CHAT] ████████████████████████████",
            "[12:35:01] [Client thread/INFO]: [CHAT]                                   SkyWars",
            "[12:35:02] [Client thread/INFO]: [CHAT] ████████████████████████████",
            "[12:35:03] [Client thread/INFO]: [CHAT] Player1 was pushed off a cliff by Player2."
        );
        
        LobbyTracker tracker = LogProcessor.processLogLines(customLines);
        
        // Verify the game mode
        assertEquals("SkyWars", tracker.getGameMode(), "Game mode should be SkyWars");
        
        // Test generating a summary from custom lines
        boolean success = LogProcessor.processAndGenerateSummary(customLines, "custom_summary.txt");
        assertTrue(success, "Custom summary generation should succeed");
    }
} 