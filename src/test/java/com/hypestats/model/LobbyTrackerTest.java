package com.hypestats.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class LobbyTrackerTest {
    
    private LobbyTracker lobbyTracker;
    private TestLobbyEventListener eventListener;
    
    @BeforeEach
    void setUp() {
        lobbyTracker = new LobbyTracker();
        eventListener = new TestLobbyEventListener();
        lobbyTracker.addListener(eventListener);
    }
    
    @Test
    void testLobbyChange() {
        // Process lobby change log line
        lobbyTracker.processLogLine("[CHAT] Sending you to mini123AB!");
        
        // Verify the event was captured
        assertEquals(1, eventListener.lobbyChangeCount);
        assertEquals("", eventListener.oldLobby);
        assertEquals("mini123AB", eventListener.newLobby);
    }
    
    @Test
    void testPlayerJoinAndQuit() {
        // Process player join log line
        lobbyTracker.processLogLine("[CHAT] PlayerOne has joined (1/16)!");
        
        // Verify the event was captured
        assertEquals(1, eventListener.playerJoinCount);
        assertEquals("PlayerOne", eventListener.lastJoinedPlayer);
        
        // Process another player join
        lobbyTracker.processLogLine("[CHAT] PlayerTwo has joined (2/16)!");
        assertEquals(2, eventListener.playerJoinCount);
        
        // Process player quit
        lobbyTracker.processLogLine("[CHAT] PlayerOne has quit!");
        assertEquals(1, eventListener.playerQuitCount);
        assertEquals("PlayerOne", eventListener.lastQuitPlayer);
    }
    
    @Test
    void testGameStart() {
        // Process game start countdown
        lobbyTracker.processLogLine("[CHAT] The game starts in 10 seconds!");
        assertTrue(lobbyTracker.isInGame());
        assertEquals(1, eventListener.gameCountdownCount);
        
        // Process game start
        lobbyTracker.processLogLine("[CHAT] The game starts in 1 second!");
        lobbyTracker.processLogLine("[CHAT] The game has started!");
        assertEquals(1, eventListener.gameStartCount);
    }
    
    @Test
    void testBedDestruction() {
        // Process bed destruction
        lobbyTracker.processLogLine("[CHAT] BED DESTRUCTION > Red Bed was bed #1,306 destroyed by PlayerName!");
        
        assertEquals(1, eventListener.bedDestructionCount);
        assertEquals("Red", eventListener.bedDestroyedTeam);
        assertEquals("PlayerName", eventListener.bedDestroyedBy);
        assertEquals(1306, eventListener.bedNumber);
    }
    
    @Test
    void testReturnToLobby() {
        // First establish that we're in a game
        lobbyTracker.processLogLine("[CHAT] The game starts in 1 second!");
        assertTrue(lobbyTracker.isInGame());
        
        // Now return to lobby
        lobbyTracker.processLogLine("[CHAT] Welcome to Hypixel");
        
        // Verify we're no longer in a game
        assertFalse(lobbyTracker.isInGame());
        assertEquals(1, eventListener.returnToLobbyCount);
    }
    
    @Test
    void testPlayerList() {
        // Process player list from /who command
        lobbyTracker.processLogLine("[CHAT] ONLINE: PlayerOne, PlayerTwo, PlayerThree");
        
        // Verify the players were added
        assertEquals(1, eventListener.playerListUpdateCount);
        assertEquals(3, eventListener.playerList.size());
        assertTrue(eventListener.playerList.contains("PlayerOne"));
        assertTrue(eventListener.playerList.contains("PlayerTwo"));
        assertTrue(eventListener.playerList.contains("PlayerThree"));
    }
    
    @Test
    void testGameBanner() {
        // Process game banner
        lobbyTracker.processLogLine("[CHAT] ████████████████████████████");
        lobbyTracker.processLogLine("[CHAT]                                   Bed Wars");
        lobbyTracker.processLogLine("[CHAT] ████████████████████████████");
        
        // Verify we're in a game and the game mode was detected
        assertTrue(lobbyTracker.isInGame());
        assertEquals(1, eventListener.gameModeDetectedCount);
        assertEquals("Bed Wars", eventListener.detectedGameMode);
    }
    
    /**
     * Test implementation of the LobbyEventListener interface
     * to capture and verify events
     */
    private static class TestLobbyEventListener implements LobbyTracker.LobbyEventListener {
        int lobbyChangeCount = 0;
        String oldLobby = "";
        String newLobby = "";
        
        int playerJoinCount = 0;
        String lastJoinedPlayer = "";
        
        int playerQuitCount = 0;
        String lastQuitPlayer = "";
        
        int playerListUpdateCount = 0;
        List<String> playerList = new ArrayList<>();
        
        int gameStartCount = 0;
        int gameEndCount = 0;
        int gameCountdownCount = 0;
        
        int teamEliminatedCount = 0;
        int userEliminatedCount = 0;
        
        int returnToLobbyCount = 0;
        int queueJoinCount = 0;
        
        int gameModeDetectedCount = 0;
        String detectedGameMode = "";
        
        int teamAssignmentCount = 0;
        String assignedTeam = "";
        
        int bedDestructionCount = 0;
        String bedDestroyedTeam = "";
        String bedDestroyedBy = "";
        int bedNumber = 0;

        @Override
        public void onLobbyChange(String oldLobby, String newLobby) {
            lobbyChangeCount++;
            this.oldLobby = oldLobby;
            this.newLobby = newLobby;
        }

        @Override
        public void onPlayerJoin(String playerName) {
            playerJoinCount++;
            this.lastJoinedPlayer = playerName;
        }

        @Override
        public void onPlayerQuit(String playerName) {
            playerQuitCount++;
            this.lastQuitPlayer = playerName;
        }

        @Override
        public void onPlayerEliminated(String playerName) {
            // Not tracking for this test
        }

        @Override
        public void onPlayerListUpdate(List<String> players) {
            playerListUpdateCount++;
            this.playerList = new ArrayList<>(players);
        }

        @Override
        public void onGameStart() {
            gameStartCount++;
        }

        @Override
        public void onTeamEliminated() {
            teamEliminatedCount++;
        }

        @Override
        public void onUserEliminated() {
            userEliminatedCount++;
        }
        
        @Override
        public void onReturnToLobby() {
            returnToLobbyCount++;
        }
        
        @Override
        public void onQueueJoin(String gameType) {
            queueJoinCount++;
        }
        
        @Override
        public void onGameModeDetected(String gameMode) {
            gameModeDetectedCount++;
            this.detectedGameMode = gameMode;
        }
        
        @Override
        public void onTeamAssignment(String team) {
            teamAssignmentCount++;
            this.assignedTeam = team;
        }
        
        @Override
        public void onGameCountdown(int seconds) {
            gameCountdownCount++;
        }
        
        @Override
        public void onGameEnd() {
            gameEndCount++;
        }
        
        @Override
        public void onBedDestruction(String teamColor, String destroyer, int bedNumber) {
            bedDestructionCount++;
            this.bedDestroyedTeam = teamColor;
            this.bedDestroyedBy = destroyer;
            this.bedNumber = bedNumber;
        }
    }
} 