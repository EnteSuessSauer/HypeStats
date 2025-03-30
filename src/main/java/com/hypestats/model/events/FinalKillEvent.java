package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when a player gets a final kill in Bed Wars.
 */
@Getter
public class FinalKillEvent implements LobbyEvent {
    private final String victimName;
    private final String killerName;
    private final int killCount;
    
    public FinalKillEvent(String victimName, String killerName, int killCount) {
        this.victimName = victimName;
        this.killerName = killerName;
        this.killCount = killCount;
    }
    
    @Override
    public String toString() {
        return "FinalKillEvent{" +
                "victimName='" + victimName + '\'' +
                ", killerName='" + killerName + '\'' +
                ", killCount=" + killCount +
                '}';
    }
} 