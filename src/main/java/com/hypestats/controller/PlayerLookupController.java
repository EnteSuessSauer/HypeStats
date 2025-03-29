package com.hypestats.controller;

import com.hypestats.model.PlayerStats;
import com.hypestats.util.HypixelApiService;
import com.hypestats.util.SettingsManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * Controller for the player lookup view
 */
@Slf4j
public class PlayerLookupController {
    
    @FXML
    private TextField usernameField;
    
    @FXML
    private Button searchButton;
    
    @FXML
    private Button clearButton;
    
    @FXML
    private ProgressBar progressBar;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private ScrollPane resultsPane;
    
    @FXML
    private ImageView playerHead;
    
    @FXML
    private Label rankLabel;
    
    @FXML
    private Label usernameLabel;
    
    @FXML
    private Label levelLabel;
    
    @FXML
    private Label winsLabel;
    
    @FXML
    private Label lossesLabel;
    
    @FXML
    private Label wlRatioLabel;
    
    @FXML
    private Label winstreakLabel;
    
    @FXML
    private Label killsLabel;
    
    @FXML
    private Label deathsLabel;
    
    @FXML
    private Label kdRatioLabel;
    
    @FXML
    private Label gamesPlayedLabel;
    
    @FXML
    private Label finalKillsLabel;
    
    @FXML
    private Label finalDeathsLabel;
    
    @FXML
    private Label finalKdRatioLabel;
    
    @FXML
    private Label bedsBrokenLabel;
    
    @FXML
    private Label bedsLostLabel;
    
    @FXML
    private Label bedRatioLabel;
    
    private SettingsManager settingsManager;
    private HypixelApiService apiService;
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        settingsManager = SettingsManager.getInstance();
        apiService = new HypixelApiService();
        
        // Enter key should trigger search
        usernameField.setOnAction(this::searchPlayer);
    }
    
    /**
     * Search for a player
     * @param event ActionEvent
     */
    @FXML
    private void searchPlayer(ActionEvent event) {
        String username = usernameField.getText().trim();
        
        if (username.isEmpty()) {
            showError("Please enter a Minecraft username");
            return;
        }
        
        String apiKey = settingsManager.getApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            showError("Please set your Hypixel API key in Settings first");
            return;
        }
        
        // Clear any previous errors
        hideError();
        
        // Show progress
        progressBar.setVisible(true);
        searchButton.setDisable(true);
        resultsPane.setVisible(false);
        
        // Create background task for API call
        Task<PlayerStats> task = new Task<>() {
            @Override
            protected PlayerStats call() throws Exception {
                return apiService.getPlayerStats(username, apiKey);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    PlayerStats stats = getValue();
                    displayPlayerStats(stats);
                    progressBar.setVisible(false);
                    searchButton.setDisable(false);
                    resultsPane.setVisible(true);
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable exception = getException();
                    if (exception instanceof HypixelApiService.ApiException) {
                        showError(exception.getMessage());
                    } else if (exception instanceof IOException) {
                        showError("Network error. Please check your internet connection.");
                    } else {
                        showError("An unexpected error occurred: " + exception.getMessage());
                    }
                    log.error("Error fetching player stats", exception);
                    progressBar.setVisible(false);
                    searchButton.setDisable(false);
                });
            }
        };
        
        // Start the task
        new Thread(task).start();
    }
    
    /**
     * Clear the search results
     * @param event ActionEvent
     */
    @FXML
    private void clearResults(ActionEvent event) {
        usernameField.clear();
        hideError();
        resultsPane.setVisible(false);
    }
    
    /**
     * Display player stats
     * @param stats PlayerStats to display
     */
    private void displayPlayerStats(PlayerStats stats) {
        // Player info
        usernameLabel.setText(stats.getUsername());
        levelLabel.setText(stats.getFormattedLevel());
        
        // Player head image
        String imageUrl = "https://crafatar.com/avatars/" + stats.getUuid() + "?overlay=true&size=64";
        playerHead.setImage(new Image(imageUrl));
        
        // Player rank
        if (stats.getRank() != null) {
            rankLabel.setText("[" + stats.getRank() + "]");
            rankLabel.getStyleClass().clear();
            rankLabel.getStyleClass().add("text-" + stats.getRankColor());
            rankLabel.setVisible(true);
        } else {
            rankLabel.setVisible(false);
        }
        
        // Stats
        winsLabel.setText(String.valueOf(stats.getWins()));
        lossesLabel.setText(String.valueOf(stats.getLosses()));
        wlRatioLabel.setText(stats.getFormattedWLRatio());
        winstreakLabel.setText(stats.getWinstreakDisplay());
        
        killsLabel.setText(String.valueOf(stats.getKills()));
        deathsLabel.setText(String.valueOf(stats.getDeaths()));
        kdRatioLabel.setText(stats.getFormattedKDRatio());
        gamesPlayedLabel.setText(String.valueOf(stats.getGamesPlayed()));
        
        finalKillsLabel.setText(String.valueOf(stats.getFinalKills()));
        finalDeathsLabel.setText(String.valueOf(stats.getFinalDeaths()));
        finalKdRatioLabel.setText(stats.getFormattedFinalKDRatio());
        
        bedsBrokenLabel.setText(String.valueOf(stats.getBedsBroken()));
        bedsLostLabel.setText(String.valueOf(stats.getBedsLost()));
        
        // Calculate and set bed ratio
        double bedRatio = stats.getBedsLost() > 0 
            ? (double) stats.getBedsBroken() / stats.getBedsLost() 
            : stats.getBedsBroken();
        bedRatioLabel.setText(String.format("%.2f", bedRatio));
    }
    
    /**
     * Show an error message
     * @param message Error message to display
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        progressBar.setVisible(false);
        searchButton.setDisable(false);
    }
    
    /**
     * Hide the error message
     */
    private void hideError() {
        errorLabel.setVisible(false);
    }
} 