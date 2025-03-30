package com.hypestats.controller;

import com.hypestats.util.SettingsManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Controller for the settings dialog
 */
@Slf4j
public class SettingsController {
    
    @FXML
    private TextField apiKeyField;
    
    @FXML
    private CheckBox startMaximizedCheckbox;
    
    @FXML
    private CheckBox autoCheckUpdatesCheckbox;
    
    @FXML
    private TextField refreshLogHotkeyField;
    
    @FXML
    private Button refreshLogHotkeyButton;
    
    @FXML
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    private SettingsManager settingsManager;
    private Runnable onSaveCallback;
    
    // Hotkey capture variables
    private boolean isCapturingHotkey = false;
    private TextField activeHotkeyField;
    private final Set<KeyCode> pressedKeys = new HashSet<>();
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        settingsManager = SettingsManager.getInstance();
        
        // Load current settings
        String apiKey = settingsManager.getApiKey();
        if (apiKey != null) {
            apiKeyField.setText(apiKey);
        }
        
        // Load hotkey settings
        refreshLogHotkeyField.setText(settingsManager.getRefreshLogHotkey());
        
        // Set up hotkey fields to be non-editable directly
        refreshLogHotkeyField.setEditable(false);
    }
    
    /**
     * Handle save button click
     * @param event ActionEvent
     */
    @FXML
    private void handleSave(ActionEvent event) {
        String apiKey = apiKeyField.getText().trim();
        
        // Very basic validation for API key format (UUID format)
        if (!apiKey.isEmpty() && !apiKey.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            log.warn("Invalid API key format: {}", apiKey);
            // Show error here if needed
            return;
        }
        
        // Save API key
        settingsManager.setApiKey(apiKey);
        
        // Save hotkey settings
        String refreshLogHotkey = refreshLogHotkeyField.getText().trim();
        if (!refreshLogHotkey.isEmpty()) {
            settingsManager.setRefreshLogHotkey(refreshLogHotkey);
        }
        
        // Close the dialog
        closeDialog();
        
        // Execute callback if provided
        if (onSaveCallback != null) {
            onSaveCallback.run();
        }
    }
    
    /**
     * Handle cancel button click
     * @param event ActionEvent
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        closeDialog();
    }
    
    /**
     * Handle setting the refresh log hotkey
     * @param event ActionEvent
     */
    @FXML
    private void handleSetRefreshLogHotkey(ActionEvent event) {
        startHotkeyCapture(refreshLogHotkeyField, refreshLogHotkeyButton);
    }
    
    /**
     * Start capturing a hotkey for a specific field
     * @param hotkeyField TextField to set the hotkey in
     * @param button Button that was clicked to start the capture
     */
    private void startHotkeyCapture(TextField hotkeyField, Button button) {
        if (isCapturingHotkey) {
            // Cancel any active capture
            stopHotkeyCapture();
        }
        
        // Start a new capture
        isCapturingHotkey = true;
        activeHotkeyField = hotkeyField;
        pressedKeys.clear();
        
        // Update UI
        hotkeyField.setText("Press keys...");
        button.setText("Cancel");
        
        // Set up key listeners on the stage
        Stage stage = (Stage) button.getScene().getWindow();
        
        stage.addEventFilter(KeyEvent.KEY_PRESSED, this::handleHotkeyKeyPressed);
        stage.addEventFilter(KeyEvent.KEY_RELEASED, this::handleHotkeyKeyReleased);
        
        // Update button action to cancel capture
        button.setOnAction(e -> stopHotkeyCapture());
    }
    
    /**
     * Stop capturing a hotkey
     */
    private void stopHotkeyCapture() {
        if (!isCapturingHotkey) {
            return;
        }
        
        isCapturingHotkey = false;
        
        // Reset UI for the active button
        if (activeHotkeyField == refreshLogHotkeyField) {
            refreshLogHotkeyButton.setText("Set");
            refreshLogHotkeyButton.setOnAction(this::handleSetRefreshLogHotkey);
        }
        
        // Remove key listeners
        Stage stage = (Stage) activeHotkeyField.getScene().getWindow();
        stage.removeEventFilter(KeyEvent.KEY_PRESSED, this::handleHotkeyKeyPressed);
        stage.removeEventFilter(KeyEvent.KEY_RELEASED, this::handleHotkeyKeyReleased);
        
        // If the field text is still "Press keys...", restore the original value
        if ("Press keys...".equals(activeHotkeyField.getText())) {
            if (activeHotkeyField == refreshLogHotkeyField) {
                activeHotkeyField.setText(settingsManager.getRefreshLogHotkey());
            }
        }
        
        activeHotkeyField = null;
    }
    
    /**
     * Handle key pressed events during hotkey capture
     * @param event KeyEvent
     */
    private void handleHotkeyKeyPressed(KeyEvent event) {
        if (!isCapturingHotkey) {
            return;
        }
        
        // Don't consume event for escape key (used to cancel)
        if (event.getCode() == KeyCode.ESCAPE) {
            stopHotkeyCapture();
            event.consume();
            return;
        }
        
        // Add the key to pressed keys set
        pressedKeys.add(event.getCode());
        
        // Update field text
        updateHotkeyFieldText();
        
        // Consume the event
        event.consume();
    }
    
    /**
     * Handle key released events during hotkey capture
     * @param event KeyEvent
     */
    private void handleHotkeyKeyReleased(KeyEvent event) {
        if (!isCapturingHotkey) {
            return;
        }
        
        // Remove key from pressed keys
        pressedKeys.remove(event.getCode());
        
        // If ESC was released, we'll already have canceled capture
        if (event.getCode() == KeyCode.ESCAPE) {
            return;
        }
        
        // If no more keys are pressed and we have a valid combination, stop capture
        if (pressedKeys.isEmpty() && !activeHotkeyField.getText().equals("Press keys...")) {
            stopHotkeyCapture();
        }
        
        // Consume the event
        event.consume();
    }
    
    /**
     * Update the hotkey field text based on currently pressed keys
     */
    private void updateHotkeyFieldText() {
        if (!isCapturingHotkey || pressedKeys.isEmpty()) {
            return;
        }
        
        // Build hotkey string
        StringBuilder hotkeyString = new StringBuilder();
        
        // Add modifiers first
        if (pressedKeys.contains(KeyCode.CONTROL)) hotkeyString.append("ctrl+");
        if (pressedKeys.contains(KeyCode.ALT)) hotkeyString.append("alt+");
        if (pressedKeys.contains(KeyCode.SHIFT)) hotkeyString.append("shift+");
        if (pressedKeys.contains(KeyCode.META)) hotkeyString.append("meta+");
        
        // Add the main key (can be any key that's not a modifier)
        for (KeyCode keyCode : pressedKeys) {
            if (keyCode != KeyCode.CONTROL && keyCode != KeyCode.ALT && 
                keyCode != KeyCode.SHIFT && keyCode != KeyCode.META) {
                hotkeyString.append(keyCode.getName().toLowerCase());
                break;
            }
        }
        
        // Only update if we have at least one non-modifier key
        if (hotkeyString.toString().endsWith("+")) {
            activeHotkeyField.setText("Press keys...");
        } else {
            activeHotkeyField.setText(hotkeyString.toString());
        }
    }
    
    /**
     * Close the dialog
     */
    private void closeDialog() {
        // Make sure to stop any active hotkey capture
        if (isCapturingHotkey) {
            stopHotkeyCapture();
        }
        
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Set a callback to run when settings are saved
     * @param callback Runnable to execute when settings are saved
     */
    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }
} 