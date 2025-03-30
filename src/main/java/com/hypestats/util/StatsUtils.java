package com.hypestats.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Utility methods for stats handling
 */
public class StatsUtils {
    
    /**
     * Safely get an integer value from a JsonObject, with a default value if not found
     * @param jsonObject The JsonObject to extract from
     * @param key The key to look for
     * @param defaultValue The default value to return if not found
     * @return The integer value or the default
     */
    public static int getIntValue(JsonObject jsonObject, String key, int defaultValue) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return jsonObject.get(key).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely get a double value from a JsonObject, with a default value if not found
     * @param jsonObject The JsonObject to extract from
     * @param key The key to look for
     * @param defaultValue The default value to return if not found
     * @return The double value or the default
     */
    public static double getDoubleValue(JsonObject jsonObject, String key, double defaultValue) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return jsonObject.get(key).getAsDouble();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely get a long value from a JsonObject, with a default value if not found
     * @param jsonObject The JsonObject to extract from
     * @param key The key to look for
     * @param defaultValue The default value to return if not found
     * @return The long value or the default
     */
    public static long getLongValue(JsonObject jsonObject, String key, long defaultValue) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return jsonObject.get(key).getAsLong();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely get a string value from a JsonObject, with a default value if not found
     * @param jsonObject The JsonObject to extract from
     * @param key The key to look for
     * @param defaultValue The default value to return if not found
     * @return The string value or the default
     */
    public static String getStringValue(JsonObject jsonObject, String key, String defaultValue) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return jsonObject.get(key).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Safely get a JsonObject from another JsonObject, returning null if not found
     * @param jsonObject The JsonObject to extract from
     * @param key The key to look for
     * @return The JsonObject or null
     */
    public static JsonObject getJsonObjectSafely(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key) || jsonObject.get(key).isJsonNull()) {
            return null;
        }
        try {
            JsonElement element = jsonObject.get(key);
            if (element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (Exception e) {
            // Do nothing, will return null
        }
        return null;
    }
    
    /**
     * Format a value for display based on a format pattern
     * @param value The value to format
     * @param formatPattern The format pattern (null for default)
     * @return Formatted string
     */
    public static String formatValue(double value, String formatPattern) {
        if (formatPattern != null) {
            return String.format(formatPattern, value);
        }
        
        // Default formatting: whole numbers as integers, decimal numbers with two places
        return value % 1 == 0 ? String.format("%.0f", value) : String.format("%.2f", value);
    }
    
    /**
     * Format a time duration in milliseconds to a human-readable string
     * @param durationMs Duration in milliseconds
     * @return Formatted string (e.g., "2 days, 3 hours")
     */
    public static String formatTimeDuration(long durationMs) {
        if (durationMs <= 0) {
            return "Unknown";
        }
        
        // Convert milliseconds to hours
        long hours = durationMs / (1000 * 60 * 60);
        
        if (hours > 24) {
            long days = hours / 24;
            long remainingHours = hours % 24;
            return String.format("%d day%s, %d hour%s", 
                days, days != 1 ? "s" : "", 
                remainingHours, remainingHours != 1 ? "s" : "");
        } else {
            return String.format("%d hour%s", hours, hours != 1 ? "s" : "");
        }
    }
    
    /**
     * Calculate a ratio with protection against division by zero
     * @param numerator The numerator
     * @param denominator The denominator
     * @return The calculated ratio
     */
    public static double calculateRatio(double numerator, double denominator) {
        if (denominator == 0) {
            return numerator; // If denominator is 0, return numerator
        }
        return numerator / denominator;
    }
} 