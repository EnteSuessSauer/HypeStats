package com.hypestats.model.events;

import com.hypestats.model.GameState;
import lombok.Getter;

/**
 * Event fired when the game state changes.
 */
@Getter
public class GameStateChangedEvent implements LobbyEvent {
    private final GameState newState;
    
    public GameStateChangedEvent(GameState newState) {
        this.newState = newState;
    }
    
    @Override
    public String toString() {
        return "GameStateChangedEvent{" +
                "newState=" + newState +
                '}';
    }
} 