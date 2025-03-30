package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when a team is eliminated.
 */
@Getter
public class TeamEliminatedEvent implements LobbyEvent {
    private final String teamName;
    
    public TeamEliminatedEvent(String teamName) {
        this.teamName = teamName;
    }
    
    @Override
    public String toString() {
        return "TeamEliminatedEvent{" +
                "teamName='" + teamName + '\'' +
                '}';
    }
} 