package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when a game mode is detected.
 */
@Getter
public class GameModeDetectedEvent implements LobbyEvent {
    private final String gameMode;
    
    public GameModeDetectedEvent(String gameMode) {
        this.gameMode = gameMode;
    }
    
    @Override
    public String toString() {
        return "GameModeDetectedEvent{" +
                "gameMode='" + gameMode + '\'' +
                '}';
    }
} 