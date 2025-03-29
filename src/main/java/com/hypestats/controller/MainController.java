package com.hypestats.controller;

import com.hypestats.HypeStatsApp;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Controller for the main application window
 */
@Slf4j
public class MainController {
    private static final Logger log = LoggerFactory.getLogger(MainController.class);

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
                // Refresh player lookup controller if needed
                if (playerLookupController != null) {
                    playerLookupController.initialize();
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
        alert.setContentText("A Hypixel Bedwars stats companion app\n\n" +
                "Version: 1.0.0\n" +
                "Created by: Your Name\n\n" +
                "This application uses the Hypixel API to retrieve player statistics.");
        
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
    }

    /**
     * Show a dialog for first-time run
     */
    private void showFirstRunDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(HypeStatsApp.class.getResource("/fxml/SettingsDialog.fxml"));
            Parent root = loader.load();
            
            SettingsController controller = loader.getController();
            
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
                    "You can get your API key by logging into Hypixel and using the command: /api new");
            
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
} 