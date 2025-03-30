package com.hypestats.model;

/**
 * Represents the state of the player's game session.
 */
public enum GameState {
    /**
     * The player is not currently in a game (e.g., in a hub or main menu).
     */
    NOT_IN_GAME,
    
    /**
     * The player is waiting in a game lobby (before the game starts).
     */
    WAITING,
    
    /**
     * The player is currently in an active game.
     */
    IN_GAME,
    
    /**
     * The state is not yet determined.
     */
    UNKNOWN
} 