package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when a player quits the lobby.
 */
@Getter
public class PlayerQuitEvent implements LobbyEvent {
    private final String playerName;
    
    public PlayerQuitEvent(String playerName) {
        this.playerName = playerName;
    }
    
    @Override
    public String toString() {
        return "PlayerQuitEvent{" +
                "playerName='" + playerName + '\'' +
                '}';
    }
} 