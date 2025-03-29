package com.hypestats.controller;

import com.hypestats.util.SettingsManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

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
    private Button saveButton;
    
    @FXML
    private Button cancelButton;
    
    private SettingsManager settingsManager;
    private Runnable onSaveCallback;
    
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
        
        // We'll add other settings loading here when they're implemented
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
        
        // Save settings
        settingsManager.setApiKey(apiKey);
        
        // Other settings would be saved here
        
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
     * Close the dialog
     */
    private void closeDialog() {
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