package com.hypestats.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SkyBlock specific statistics
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SkyBlockStats extends GameStats {
    private String profileName;
    private int purse;
    private int bank;
    private int farmingLevel;
    private int miningLevel;
    private int combatLevel;
    private int foragingLevel;
    private int fishingLevel;
    private int enchantingLevel;
    private int alchemyLevel;
    private int tamingLevel;
    
    /**
     * Calculate total coins (purse + bank)
     */
    public int getTotalCoins() {
        return purse + bank;
    }
    
    /**
     * Get formatted total coins
     */
    public String getFormattedTotalCoins() {
        return String.format("%,d", getTotalCoins());
    }
    
    /**
     * Get formatted bank balance
     */
    public String getFormattedBank() {
        return String.format("%,d", bank);
    }
    
    /**
     * Get formatted purse
     */
    public String getFormattedPurse() {
        return String.format("%,d", purse);
    }
    
    /**
     * Check if there are meaningful SkyBlock stats to display
     */
    @Override
    public boolean hasStats() {
        return profileName != null || purse > 0 || bank > 0;
    }
} 