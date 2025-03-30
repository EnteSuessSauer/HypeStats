package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when the player changes lobby.
 */
@Getter
public class LobbyChangedEvent implements LobbyEvent {
    private final String lobbyId;
    private final boolean isGameLobby;
    
    public LobbyChangedEvent(String lobbyId, boolean isGameLobby) {
        this.lobbyId = lobbyId;
        this.isGameLobby = isGameLobby;
    }
    
    @Override
    public String toString() {
        return "LobbyChangedEvent{" +
                "lobbyId='" + lobbyId + '\'' +
                ", isGameLobby=" + isGameLobby +
                '}';
    }
} 