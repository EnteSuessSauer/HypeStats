package com.hypestats.model.events;

import lombok.Getter;

/**
 * Event fired when a bed is destroyed in Bed Wars.
 */
@Getter
public class BedDestroyedEvent implements LobbyEvent {
    private final String teamName;
    private final String destroyerName;
    private final int bedNumber;
    
    public BedDestroyedEvent(String teamName, String destroyerName, int bedNumber) {
        this.teamName = teamName;
        this.destroyerName = destroyerName;
        this.bedNumber = bedNumber;
    }
    
    @Override
    public String toString() {
        return "BedDestroyedEvent{" +
                "teamName='" + teamName + '\'' +
                ", destroyerName='" + destroyerName + '\'' +
                ", bedNumber=" + bedNumber +
                '}';
    }
} 