package com.hypestats.util;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages application-wide hotkeys
 */
@Slf4j
public class HotkeyManager {
    private static final Map<KeyCombination, Runnable> hotkeys = new HashMap<>();
    
    /**
     * Register a global hotkey
     * @param hotkeyString Hotkey string (e.g., "ctrl+r")
     * @param action Action to perform when hotkey is pressed
     * @return true if registered successfully, false otherwise
     */
    public static boolean registerHotkey(String hotkeyString, Runnable action) {
        try {
            KeyCombination keyCombination = parseHotkeyString(hotkeyString);
            hotkeys.put(keyCombination, action);
            log.debug("Registered hotkey: {}", hotkeyString);
            return true;
        } catch (IllegalArgumentException e) {
            log.error("Failed to register hotkey: {}", hotkeyString, e);
            return false;
        }
    }
    
    /**
     * Unregister a global hotkey
     * @param hotkeyString Hotkey string (e.g., "ctrl+r")
     */
    public static void unregisterHotkey(String hotkeyString) {
        try {
            KeyCombination keyCombination = parseHotkeyString(hotkeyString);
            hotkeys.remove(keyCombination);
            log.debug("Unregistered hotkey: {}", hotkeyString);
        } catch (IllegalArgumentException e) {
            log.error("Failed to unregister hotkey: {}", hotkeyString, e);
        }
    }
    
    /**
     * Clear all registered hotkeys
     */
    public static void clearHotkeys() {
        hotkeys.clear();
        log.debug("Cleared all hotkeys");
    }
    
    /**
     * Handle a key event and execute the corresponding action if a matching hotkey is found
     * @param event KeyEvent
     * @return true if a hotkey was triggered, false otherwise
     */
    public static boolean handleKeyEvent(KeyEvent event) {
        for (Map.Entry<KeyCombination, Runnable> entry : hotkeys.entrySet()) {
            if (entry.getKey().match(event)) {
                entry.getValue().run();
                event.consume();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Parse a hotkey string into a KeyCombination
     * @param hotkeyString Hotkey string (e.g., "ctrl+r", "shift+f5")
     * @return KeyCombination
     * @throws IllegalArgumentException if the hotkey string is invalid
     */
    public static KeyCombination parseHotkeyString(String hotkeyString) {
        if (hotkeyString == null || hotkeyString.isEmpty()) {
            throw new IllegalArgumentException("Hotkey string cannot be empty");
        }
        
        // Split the string by + symbol
        String[] parts = hotkeyString.split("\\+");
        if (parts.length == 0) {
            throw new IllegalArgumentException("Invalid hotkey format: " + hotkeyString);
        }
        
        // The last part is the main key
        String keyPart = parts[parts.length - 1].toUpperCase();
        KeyCode keyCode;
        try {
            keyCode = KeyCode.valueOf(keyPart);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid key code: " + keyPart, e);
        }
        
        // Check for modifiers
        KeyCombination.Modifier[] modifiers = new KeyCombination.Modifier[parts.length - 1];
        for (int i = 0; i < parts.length - 1; i++) {
            String modPart = parts[i].toLowerCase();
            switch (modPart) {
                case "ctrl":
                case "control":
                    modifiers[i] = KeyCombination.CONTROL_DOWN;
                    break;
                case "alt":
                    modifiers[i] = KeyCombination.ALT_DOWN;
                    break;
                case "shift":
                    modifiers[i] = KeyCombination.SHIFT_DOWN;
                    break;
                case "meta":
                case "cmd":
                case "command":
                    modifiers[i] = KeyCombination.META_DOWN;
                    break;
                default:
                    throw new IllegalArgumentException("Invalid modifier: " + modPart);
            }
        }
        
        return new KeyCodeCombination(keyCode, modifiers);
    }
    
    /**
     * Convert a KeyCombination to a human-readable string
     * @param keyCombination KeyCombination
     * @return Human-readable string (e.g., "Ctrl+R")
     */
    public static String keyCombinationToString(KeyCombination keyCombination) {
        return keyCombination.toString()
                .replace("Shortcut", "Ctrl")
                .replace(" ", "+");
    }
} 