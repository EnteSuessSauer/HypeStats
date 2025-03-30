package com.hypestats.model.events;

import lombok.Getter;

import java.util.Set;

/**
 * Event fired when multiple players are detected at once (e.g. from /who command)
 */
@Getter
public class PlayersDetectedEvent implements LobbyEvent {
    private final Set<String> playerNames;
    
    public PlayersDetectedEvent(Set<String> playerNames) {
        this.playerNames = playerNames;
    }
    
    @Override
    public String toString() {
        return "PlayersDetectedEvent{" +
                "playerCount=" + playerNames.size() +
                '}';
    }
} 