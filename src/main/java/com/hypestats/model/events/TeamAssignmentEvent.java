package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when a player is assigned to a team.
 */
@Getter
public class TeamAssignmentEvent implements LobbyEvent {
    private final String playerName;
    private final String teamName;
    
    public TeamAssignmentEvent(String playerName, String teamName) {
        this.playerName = playerName;
        this.teamName = teamName;
    }
    
    @Override
    public String toString() {
        return "TeamAssignmentEvent{" +
                "playerName='" + playerName + '\'' +
                ", teamName='" + teamName + '\'' +
                '}';
    }
} 