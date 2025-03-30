package com.hypestats.model.events;

/**
 * Event fired when a game ends.
 */
public class GameEndedEvent implements LobbyEvent {
    
    @Override
    public String toString() {
        return "GameEndedEvent{}";
    }
} 