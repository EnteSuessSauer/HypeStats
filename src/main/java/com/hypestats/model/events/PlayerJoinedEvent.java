package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when a player joins the lobby.
 */
@Getter
public class PlayerJoinedEvent implements LobbyEvent {
    private final String playerName;
    
    public PlayerJoinedEvent(String playerName) {
        this.playerName = playerName;
    }
    
    @Override
    public String toString() {
        return "PlayerJoinedEvent{" +
                "playerName='" + playerName + '\'' +
                '}';
    }
} 