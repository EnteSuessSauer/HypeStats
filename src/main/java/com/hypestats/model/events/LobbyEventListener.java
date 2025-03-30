package com.hypestats.model.events;

/**
 * Interface for listening to lobby events.
 */
public interface LobbyEventListener {
    /**
     * Called when the game state changes.
     * @param event The event
     */
    default void onGameStateChanged(GameStateChangedEvent event) {}

    /**
     * Called when the game mode is detected.
     * @param event The event
     */
    default void onGameModeDetected(GameModeDetectedEvent event) {}
    
    /**
     * Called when the lobby changes.
     * @param event The event
     */
    default void onLobbyChanged(LobbyChangedEvent event) {}
    
    /**
     * Called when a player joins the lobby.
     * @param event The event
     */
    default void onPlayerJoined(PlayerJoinedEvent event) {}
    
    /**
     * Called when a player quits the lobby.
     * @param event The event
     */
    default void onPlayerQuit(PlayerQuitEvent event) {}
    
    /**
     * Called when a player is assigned to a team.
     * @param event The event
     */
    default void onTeamAssignment(TeamAssignmentEvent event) {}
    
    /**
     * Called when a game starts.
     * @param event The event
     */
    default void onGameStarted(GameStartedEvent event) {}
    
    /**
     * Called when a game ends.
     * @param event The event
     */
    default void onGameEnded(GameEndedEvent event) {}
    
    /**
     * Called when a bed is destroyed.
     * @param event The event
     */
    default void onBedDestroyed(BedDestroyedEvent event) {}
    
    /**
     * Called when a player gets a final kill.
     * @param event The event
     */
    default void onFinalKill(FinalKillEvent event) {}
    
    /**
     * Called when a team is eliminated.
     * @param event The event
     */
    default void onTeamEliminated(TeamEliminatedEvent event) {}
    
    /**
     * Called when multiple players are detected at once (e.g. from /who command)
     * @param event The event
     */
    default void onPlayersDetected(PlayersDetectedEvent event) {}
} 