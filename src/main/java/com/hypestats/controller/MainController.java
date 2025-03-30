package com.hypestats.controller;

import com.hypestats.HypeStatsApp;
import com.hypestats.util.HotkeyManager;
import com.hypestats.util.SettingsManager;
import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import javafx.scene.image.Image;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Controller for the main application window
 */
@Slf4j
public class MainController {
    @FXML
    private Label statusLabel;

    @FXML
    private Button settingsButton;

    @FXML
    private TabPane mainTabPane;
    
    @FXML
    private Tab playerLookupTab;
    
    @FXML
    private Tab lobbyTrackerTab;
    
    @FXML
    private MenuItem settingsMenuItem;
    
    @FXML
    private MenuItem aboutMenuItem;
    
    @FXML
    private MenuItem exitMenuItem;

    private SettingsManager settingsManager;
    private HostServices hostServices;
    private PlayerLookupController playerLookupController;
    private LobbyTrackerController lobbyTrackerController;
    private Stage primaryStage;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        settingsManager = SettingsManager.getInstance();
        
        // Check for API key on startup
        if (settingsManager.getApiKey() == null || settingsManager.getApiKey().isEmpty()) {
            log.info("No API key found, prompting user");
            showFirstRunDialog();
        }
    }
    
    /**
     * Set up the hotkeys
     * This should be called after setting the primary stage
     */
    public void setupHotkeys() {
        if (primaryStage == null) {
            log.error("Cannot set up hotkeys, primary stage is null");
            return;
        }
        
        // Add key event filter to the scene for global hotkey support
        // We need to wait until the scene is available before adding event filters
        if (primaryStage.getScene() == null) {
            log.warn("Scene is not available yet, hotkeys will be set up when the scene is available");
            
            // Listen for scene changes and set up hotkeys when the scene becomes available
            primaryStage.sceneProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null) {
                    log.info("Scene is now available, setting up hotkeys");
                    newValue.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
                    registerHotkeys();
                }
            });
        } else {
            // Scene is already available, set up hotkeys now
            primaryStage.getScene().addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
            registerHotkeys();
        }
    }
    
    /**
     * Register all application hotkeys
     */
    private void registerHotkeys() {
        // Get hotkey settings
        String refreshLogHotkey = settingsManager.getRefreshLogHotkey();
        
        // Register refresh log hotkey
        if (refreshLogHotkey != null && !refreshLogHotkey.isEmpty()) {
            HotkeyManager.registerHotkey(refreshLogHotkey, this::triggerLogRefresh);
            log.info("Registered refresh log hotkey: {}", refreshLogHotkey);
        }
    }
    
    /**
     * Handle key press events for global hotkeys
     * @param event KeyEvent
     */
    private void handleKeyPress(KeyEvent event) {
        // Let the hotkey manager handle the event
        boolean handled = HotkeyManager.handleKeyEvent(event);
        
        if (handled) {
            log.debug("Hotkey triggered: {}", event);
        }
    }
    
    /**
     * Triggered by the refresh log hotkey
     */
    private void triggerLogRefresh() {
        // Switch to the lobby tracker tab if not already there
        mainTabPane.getSelectionModel().select(lobbyTrackerTab);
        
        // Trigger the refresh action if the controller is available
        if (lobbyTrackerController != null) {
            lobbyTrackerController.refreshLogFile(null);
        }
    }

    /**
     * Set the status message
     * @param message Status message to display
     * @param styleClass CSS style class for the status (optional)
     */
    public void setStatus(String message, String styleClass) {
        statusLabel.setText(message);
        
        // Clear existing style classes except base ones
        statusLabel.getStyleClass().removeAll(
            "status-running", "status-stopped", "status-warning"
        );
        
        if (styleClass != null) {
            statusLabel.getStyleClass().add(styleClass);
        }
    }

    /**
     * Show the settings dialog
     */
    @FXML
    private void showSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(HypeStatsApp.class.getResource("/fxml/SettingsDialog.fxml"));
            Parent root = loader.load();
            
            SettingsController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Settings");
            stage.setScene(new Scene(root));
            
            // Set callback for when settings are saved
            controller.setOnSaveCallback(() -> {
                // Re-register hotkeys with updated settings
                HotkeyManager.clearHotkeys();
                registerHotkeys();
                
                // Refresh player lookup controller if needed
                if (playerLookupController != null) {
                    playerLookupController.initialize();
                }
                
                // Refresh lobby tracker controller if needed
                if (lobbyTrackerController != null) {
                    lobbyTrackerController.initialize();
                }
            });
            
            stage.showAndWait();
        } catch (IOException e) {
            log.error("Error loading settings dialog", e);
            showError("Error", "Could not load settings dialog", e.getMessage());
        }
    }

    /**
     * Show the about dialog
     */
    @FXML
    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About HypeStats");
        alert.setHeaderText("HypeStats");
        alert.setContentText("A comprehensive Hypixel statistics companion app\n\n" +
                "Version: 1.0.0\n" +
                "Created by: Your Name\n\n" +
                "This application uses the Hypixel API to retrieve player statistics across multiple game modes.");
        
        alert.showAndWait();
    }

    /**
     * Exit the application
     */
    @FXML
    private void exitApplication() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.close();
    }

    /**
     * Set the controller for the player lookup tab
     * @param controller PlayerLookupController instance
     */
    public void setPlayerLookupController(PlayerLookupController controller) {
        this.playerLookupController = controller;
    }
    
    /**
     * Set the controller for the lobby tracker tab
     * @param controller LobbyTrackerController instance
     */
    public void setLobbyTrackerController(LobbyTrackerController controller) {
        this.lobbyTrackerController = controller;
        
        // Initialize the lobby tracker controller with the API service if not already done
        if (controller != null) {
            // Create and inject the HypixelAPIService
            com.hypestats.services.HypixelAPIService apiService = 
                new com.hypestats.services.HypixelAPIService(settingsManager.getApiKey());
            controller.setApiService(apiService);
        }
    }

    /**
     * Show a dialog for first-time run
     */
    private void showFirstRunDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(HypeStatsApp.class.getResource("/fxml/SettingsDialog.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Welcome to HypeStats");
            stage.setScene(new Scene(root));
            
            // Show first-run message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Welcome to HypeStats");
            alert.setHeaderText("Welcome to HypeStats!");
            alert.setContentText("It looks like this is your first time using HypeStats.\n\n" +
                    "To get started, please enter your Hypixel API key in the settings.\n\n" +
                    "You can get your API key by visiting the Hypixel Developer Dashboard at developer.hypixel.net");
            
            alert.showAndWait();
            
            // Show settings dialog
            stage.showAndWait();
        } catch (IOException e) {
            log.error("Error loading first-run dialog", e);
        }
    }
    
    /**
     * Show an error dialog
     * @param title Dialog title
     * @param header Dialog header
     * @param content Error message
     */
    private void showError(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Open the Hypixel API website
     * @param event ActionEvent
     */
    @FXML
    private void openHypixelApi(ActionEvent event) {
        String url = "https://api.hypixel.net/";
        
        try {
            if (hostServices != null) {
                hostServices.showDocument(url);
            } else {
                // Fallback method if HostServices is not available
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (IOException | URISyntaxException e) {
            log.error("Failed to open URL: {}", url, e);
            setStatus("Failed to open URL", "status-stopped");
        }
    }

    /**
     * Set the host services
     * @param hostServices HostServices
     */
    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    /**
     * Set up the primary stage
     * @param stage JavaFX primary stage
     */
    public void setupStage(Stage stage) {
        // Store the primary stage
        this.primaryStage = stage;
        
        // Set window title and icon
        stage.setTitle("HypeStats - Hypixel Statistics Companion");
        
        // Try to load icon safely with null check
        try {
            InputStream iconStream = getClass().getResourceAsStream("/images/icon.png");
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            } else {
                log.warn("Icon resource not found: /images/icon.png");
            }
        } catch (Exception e) {
            log.warn("Could not load application icon", e);
        }
        
        // Set minimum window size
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        
        // Automatically save settings on window close
        stage.setOnCloseRequest(event -> SettingsManager.getInstance().saveSettings());
        
        // Set up hotkeys after the scene is available
        setupHotkeys();
    }

    /**
     * Set the primary stage reference
     * @param stage Primary stage
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
} 