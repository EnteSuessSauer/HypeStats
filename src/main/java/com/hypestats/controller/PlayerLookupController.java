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
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.Tooltip;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller for the player lookup tab
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
    private Label errorLabel;
    
    @FXML
    private ProgressBar progressBar;
    
    @FXML
    private ScrollPane resultsPane;
    
    @FXML
    private ImageView playerHead;
    
    @FXML
    private Label usernameLabel;
    
    @FXML
    private Label rankLabel;
    
    @FXML
    private Label levelLabel;

    @FXML
    private VBox topStatsContainer;
    
    // Network Stats
    @FXML
    private Label networkLevelLabel;

    @FXML
    private Label karmaLabel;

    @FXML
    private Label achievementPointsLabel;

    @FXML
    private Label achievementCompletionLabel;

    @FXML
    private Label accountAgeLabel;
    
    @FXML
    private Label playtimeLabel;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private Label guildTagLabel;
    
    @FXML
    private TitledPane guildPane;
    
    @FXML
    private Label guildNameLabel;
    
    @FXML
    private Label guildRankLabel;
    
    @FXML
    private HBox socialLinksContainer;
    
    // Bedwars Stats
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

    // Skywars Stats
    @FXML
    private Label skywarsLevelLabel;
    
    @FXML
    private Label skywarsWinsLabel;
    
    @FXML
    private Label skywarsLossesLabel;
    
    @FXML
    private Label skywarsWlRatioLabel;
    
    @FXML
    private Label skywarsKillsLabel;
    
    @FXML
    private Label skywarsDeathsLabel;
    
    @FXML
    private Label skywarsKdRatioLabel;
    
    @FXML
    private Label skywarsGamesPlayedLabel;

    // Duels Stats
    @FXML
    private Label duelsWinsLabel;
    
    @FXML
    private Label duelsLossesLabel;
    
    @FXML
    private Label duelsWlRatioLabel;
    
    @FXML
    private Label duelsKillsLabel;
    
    @FXML
    private Label duelsDeathsLabel;
    
    @FXML
    private Label duelsKdRatioLabel;
    
    @FXML
    private Label duelsGamesPlayedLabel;
    
    // Murder Mystery Stats
    @FXML
    private Label mmWinsLabel;
    
    @FXML
    private Label mmGamesPlayedLabel;
    
    @FXML
    private Label mmWinRateLabel;
    
    @FXML
    private Label mmCoinsLabel;
    
    @FXML
    private Label mmKillsLabel;
    
    @FXML
    private Label mmDeathsLabel;
    
    @FXML
    private Label mmKdRatioLabel;
    
    // TNT Games Stats
    @FXML
    private Label tntgamesWinsLabel;
    
    @FXML
    private Label tntgamesCoinsLabel;
    
    @FXML
    private Label tntRunWinsLabel;
    
    @FXML
    private Label tntRunRecordLabel;
    
    @FXML
    private Label bowSpleefWinsLabel;
    
    @FXML
    private Label wizardsWinsLabel;
    
    @FXML
    private Label pvpRunWinsLabel;
    
    // UHC Stats
    @FXML
    private Label uhcWinsLabel;
    
    @FXML
    private Label uhcCoinsLabel;
    
    @FXML
    private Label uhcScoreLabel;
    
    @FXML
    private Label uhcKillsLabel;
    
    @FXML
    private Label uhcDeathsLabel;
    
    @FXML
    private Label uhcKdRatioLabel;
    
    // Build Battle Stats
    @FXML
    private Label buildBattleWinsLabel;
    
    @FXML
    private Label buildBattleGamesPlayedLabel;
    
    @FXML
    private Label buildBattleWinRateLabel;
    
    @FXML
    private Label buildBattleCoinsLabel;
    
    @FXML
    private Label buildBattleScoreLabel;
    
    // Mega Walls Stats
    @FXML
    private Label megaWallsWinsLabel;
    
    @FXML
    private Label megaWallsAssistsLabel;
    
    @FXML
    private Label megaWallsKillsLabel;
    
    @FXML
    private Label megaWallsDeathsLabel;
    
    @FXML
    private Label megaWallsKdRatioLabel;
    
    @FXML
    private Label megaWallsFinalKillsLabel;
    
    @FXML
    private Label megaWallsFinalDeathsLabel;
    
    @FXML
    private Label megaWallsFinalKdRatioLabel;
    
    // SkyBlock Stats
    @FXML
    private Label skyblockProfileLabel;
    
    @FXML
    private Label skyblockCoinsLabel;
    
    @FXML
    private Label skyblockBankLabel;
    
    @FXML
    private Label skyblockPurseLabel;
    
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
        
        // Display status
        if (stats.getCurrentStatus() != null) {
            statusLabel.setText(stats.getCurrentStatus());
            statusLabel.getStyleClass().clear();
            statusLabel.getStyleClass().add("status-label");
            
            if (stats.getCurrentStatus().startsWith("Online")) {
                statusLabel.getStyleClass().add("status-online");
            } else if (stats.getCurrentStatus().startsWith("Offline")) {
                statusLabel.getStyleClass().add("status-offline");
            }
            
            statusLabel.setVisible(true);
        } else {
            statusLabel.setVisible(false);
        }
        
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
        
        // Guild tag if available
        displayGuildTag(stats);
        
        // Display social links
        displaySocialLinks(stats);

        // Display top stats
        displayTopStats(stats);
        
        // Network stats
        displayNetworkStats(stats);
        
        // Guild info if available
        displayGuildInfo(stats);
        
        // Bedwars stats
        displayBedwarsStats(stats);
        
        // Skywars stats
        displaySkywarsStats(stats);
        
        // Duels stats
        displayDuelsStats(stats);
        
        // Murder Mystery stats
        displayMurderMysteryStats(stats);
        
        // TNT Games stats
        displayTNTGamesStats(stats);
        
        // UHC stats
        displayUHCStats(stats);
        
        // Build Battle stats
        displayBuildBattleStats(stats);
        
        // Mega Walls stats
        displayMegaWallsStats(stats);
        
        // SkyBlock stats
        displaySkyBlockStats(stats);
    }

    /**
     * Display the player's top stats
     */
    private void displayTopStats(PlayerStats stats) {
        topStatsContainer.getChildren().clear();
        
        for (PlayerStats.HighlightedStat topStat : stats.getTopStats()) {
            Label statLabel = new Label(topStat.getName() + ": " + topStat.getFormattedValue());
            statLabel.getStyleClass().add("top-stat");
            
            // Add style based on significance
            double significance = topStat.getSignificance();
            if (significance >= 3.0) {
                statLabel.getStyleClass().add("stat-exceptional");
            } else if (significance >= 2.0) {
                statLabel.getStyleClass().add("stat-great");
            } else if (significance >= 1.5) {
                statLabel.getStyleClass().add("stat-good");
            } else {
                statLabel.getStyleClass().add("stat-average");
            }
            
            // Add icon based on stat type
            if (topStat.getName().contains("K/D") || topStat.getName().contains("W/L")) {
                statLabel.getStyleClass().add("combat-stat");
            } else if (topStat.getName().contains("Level")) {
                statLabel.getStyleClass().add("level-stat");
            } else if (topStat.getName().equals("Account Age")) {
                statLabel.getStyleClass().add("experience-stat");
            }
            
            topStatsContainer.getChildren().add(statLabel);
        }
        
        if (topStatsContainer.getChildren().isEmpty()) {
            Label noTopStatsLabel = new Label("No exceptional stats to display");
            noTopStatsLabel.getStyleClass().add("stat-average");
            topStatsContainer.getChildren().add(noTopStatsLabel);
        }
    }
    
    /**
     * Display guild tag next to username
     */
    private void displayGuildTag(PlayerStats stats) {
        if (stats.hasGuild() && stats.getGuildTag() != null) {
            guildTagLabel.setText("[" + stats.getGuildTag() + "]");
            
            // Apply color if available
            if (stats.getGuildColor() != null) {
                guildTagLabel.getStyleClass().clear();
                guildTagLabel.getStyleClass().add("guild-tag");
                guildTagLabel.getStyleClass().add("text-" + stats.getGuildColor());
            }
            
            guildTagLabel.setVisible(true);
        } else {
            guildTagLabel.setVisible(false);
        }
    }
    
    /**
     * Display detailed guild information
     */
    private void displayGuildInfo(PlayerStats stats) {
        if (stats.hasGuild()) {
            guildPane.setVisible(true);
            guildNameLabel.setText(stats.getGuildName());
            guildRankLabel.setText(stats.getGuildRank() != null ? stats.getGuildRank() : "Member");
        } else {
            guildPane.setVisible(false);
        }
    }
    
    /**
     * Display social media links
     */
    private void displaySocialLinks(PlayerStats stats) {
        socialLinksContainer.getChildren().clear();
        
        if (!stats.hasSocialLinks()) {
            return;
        }
        
        // Process each social link
        stats.getSocialLinks().forEach((platform, link) -> {
            try {
                // Create button for each social platform
                javafx.scene.control.Button socialButton = new javafx.scene.control.Button();
                socialButton.getStyleClass().add("social-button");
                socialButton.getStyleClass().add(platform.toLowerCase() + "-icon");
                
                // Format platform name nicely
                String platformName = platform.substring(0, 1).toUpperCase() + platform.substring(1).toLowerCase();
                
                // Add tooltip with platform name
                Tooltip tooltip = new Tooltip(platformName);
                tooltip.setStyle(
                    "-fx-font-size: 12px; " +
                    "-fx-background-color: rgba(60, 60, 60, 0.8); " +
                    "-fx-text-fill: white;"
                );
                Tooltip.install(socialButton, tooltip);
                
                // Add click action to copy to clipboard
                socialButton.setOnAction(e -> {
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent content = new ClipboardContent();
                    content.putString(link);
                    clipboard.setContent(content);
                    
                    // Show copied notification
                    tooltip.setText("✓ " + platformName + " copied!");
                    tooltip.setStyle(
                        "-fx-font-size: 12px; " +
                        "-fx-background-color: rgba(40, 100, 40, 0.9); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold;"
                    );
                    
                    // Reset tooltip after delay
                    PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                    delay.setOnFinished(event -> {
                        tooltip.setText(platformName);
                        tooltip.setStyle(
                            "-fx-font-size: 12px; " +
                            "-fx-background-color: rgba(60, 60, 60, 0.8); " +
                            "-fx-text-fill: white;"
                        );
                    });
                    delay.play();
                });
                
                socialLinksContainer.getChildren().add(socialButton);
            } catch (Exception e) {
                log.error("Error creating social button: {}", e.getMessage());
            }
        });
    }
    
    /**
     * Display network stats
     */
    private void displayNetworkStats(PlayerStats stats) {
        networkLevelLabel.setText(stats.getNetworkLevelFormatted());
        applyStatStyle(networkLevelLabel, stats.getNetworkLevel(), "networkLevel");
        
        karmaLabel.setText(String.valueOf(stats.getKarma()));
        applyStatStyle(karmaLabel, stats.getKarma(), "karma");
        
        achievementPointsLabel.setText(String.valueOf(stats.getAchievementPoints()));
        applyStatStyle(achievementPointsLabel, stats.getAchievementPoints(), "achievementPoints");
        
        // Display achievement completion percentage
        achievementCompletionLabel.setText(stats.getAchievementCompletionPercent() + "%");
        
        // Set account age
        accountAgeLabel.setText(stats.getFormattedAccountAge());
        
        // Set playtime
        playtimeLabel.setText(stats.getFormattedPlaytime());
    }
    
    /**
     * Display Bedwars stats
     */
    private void displayBedwarsStats(PlayerStats stats) {
        // Bedwars Stats
        winsLabel.setText(String.valueOf(stats.getBedwarsWins()));
        lossesLabel.setText(String.valueOf(stats.getBedwarsLosses()));
        
        wlRatioLabel.setText(String.format("%.2f", stats.getBedwarsWLRatio()));
        applyStatStyle(wlRatioLabel, stats.getBedwarsWLRatio(), "bedwarsWLRatio");
        
        winstreakLabel.setText(stats.getWinstreakDisplay());
        
        killsLabel.setText(String.valueOf(stats.getBedwarsKills()));
        deathsLabel.setText(String.valueOf(stats.getBedwarsDeaths()));
        
        kdRatioLabel.setText(String.format("%.2f", stats.getBedwarsKDRatio()));
        applyStatStyle(kdRatioLabel, stats.getBedwarsKDRatio(), "bedwarsKDRatio");
        
        gamesPlayedLabel.setText(String.valueOf(stats.getBedwarsGamesPlayed()));
        
        finalKillsLabel.setText(String.valueOf(stats.getBedwarsFinalKills()));
        finalDeathsLabel.setText(String.valueOf(stats.getBedwarsFinalDeaths()));
        
        finalKdRatioLabel.setText(String.format("%.2f", stats.getBedwarsFinalKDRatio()));
        applyStatStyle(finalKdRatioLabel, stats.getBedwarsFinalKDRatio(), "bedwarsFinalKDRatio");
        
        bedsBrokenLabel.setText(String.valueOf(stats.getBedwarsBedsBroken()));
        bedsLostLabel.setText(String.valueOf(stats.getBedwarsBedsLost()));
        
        // Calculate and set bed ratio
        double bedRatio = stats.getBedwarsBedsLost() > 0 
            ? (double) stats.getBedwarsBedsBroken() / stats.getBedwarsBedsLost() 
            : stats.getBedwarsBedsBroken();
        bedRatioLabel.setText(String.format("%.2f", bedRatio));
    }
    
    /**
     * Display Skywars stats
     */
    private void displaySkywarsStats(PlayerStats stats) {
        if (stats.getSkywarsGamesPlayed() == 0) {
            return; // No Skywars stats
        }
        
        skywarsLevelLabel.setText(String.format("%.0f", stats.getSkywarsLevel()));
        applyStatStyle(skywarsLevelLabel, stats.getSkywarsLevel(), "skywarsLevel");
        
        skywarsWinsLabel.setText(String.valueOf(stats.getSkywarsWins()));
        skywarsLossesLabel.setText(String.valueOf(stats.getSkywarsLosses()));
        
        skywarsWlRatioLabel.setText(String.format("%.2f", stats.getSkywarsWLRatio()));
        applyStatStyle(skywarsWlRatioLabel, stats.getSkywarsWLRatio(), "skywarsWLRatio");
        
        skywarsKillsLabel.setText(String.valueOf(stats.getSkywarsKills()));
        skywarsDeathsLabel.setText(String.valueOf(stats.getSkywarsDeaths()));
        
        skywarsKdRatioLabel.setText(String.format("%.2f", stats.getSkywarsKDRatio()));
        applyStatStyle(skywarsKdRatioLabel, stats.getSkywarsKDRatio(), "skywarsKDRatio");
        
        skywarsGamesPlayedLabel.setText(String.valueOf(stats.getSkywarsGamesPlayed()));
    }
    
    /**
     * Display Duels stats
     */
    private void displayDuelsStats(PlayerStats stats) {
        if (stats.getDuelsGamesPlayed() == 0) {
            return; // No Duels stats
        }
        
        duelsWinsLabel.setText(String.valueOf(stats.getDuelsWins()));
        duelsLossesLabel.setText(String.valueOf(stats.getDuelsLosses()));
        
        duelsWlRatioLabel.setText(String.format("%.2f", stats.getDuelsWLRatio()));
        applyStatStyle(duelsWlRatioLabel, stats.getDuelsWLRatio(), "duelsWLRatio");
        
        duelsKillsLabel.setText(String.valueOf(stats.getDuelsKills()));
        duelsDeathsLabel.setText(String.valueOf(stats.getDuelsDeaths()));
        
        duelsKdRatioLabel.setText(String.format("%.2f", stats.getDuelsKDRatio()));
        applyStatStyle(duelsKdRatioLabel, stats.getDuelsKDRatio(), "duelsKDRatio");
        
        duelsGamesPlayedLabel.setText(String.valueOf(stats.getDuelsGamesPlayed()));
    }
    
    /**
     * Display Murder Mystery stats
     */
    private void displayMurderMysteryStats(PlayerStats stats) {
        mmWinsLabel.setText(String.valueOf(stats.getMmWins()));
        mmGamesPlayedLabel.setText(String.valueOf(stats.getMmGamesPlayed()));
        
        // Calculate win rate
        double winRate = stats.getMmGamesPlayed() > 0 
            ? (double) stats.getMmWins() / stats.getMmGamesPlayed() * 100 
            : 0;
        mmWinRateLabel.setText(String.format("%.2f%%", winRate));
        
        mmCoinsLabel.setText(String.valueOf(stats.getMmCoins()));
        mmKillsLabel.setText(String.valueOf(stats.getMmKills()));
        mmDeathsLabel.setText(String.valueOf(stats.getMmDeaths()));
        
        mmKdRatioLabel.setText(String.format("%.2f", stats.getMmKDRatio()));
        applyStatStyle(mmKdRatioLabel, stats.getMmKDRatio(), "mmKDRatio");
    }
    
    /**
     * Display TNT Games stats
     */
    private void displayTNTGamesStats(PlayerStats stats) {
        tntgamesWinsLabel.setText(String.valueOf(stats.getTntgamesWins()));
        tntgamesCoinsLabel.setText(String.valueOf(stats.getTntgamesCoins()));
        tntRunWinsLabel.setText(String.valueOf(stats.getTntRunWins()));
        tntRunRecordLabel.setText(String.valueOf(stats.getTntRunRecord()) + "s");
        bowSpleefWinsLabel.setText(String.valueOf(stats.getBowSpleefWins()));
        wizardsWinsLabel.setText(String.valueOf(stats.getWizardsWins()));
        pvpRunWinsLabel.setText(String.valueOf(stats.getPvpRunWins()));
        
        // Apply styling to TNT Run record
        applyStatStyle(tntRunRecordLabel, stats.getTntRunRecord(), "tntRunRecord");
    }
    
    /**
     * Display UHC stats
     */
    private void displayUHCStats(PlayerStats stats) {
        uhcWinsLabel.setText(String.valueOf(stats.getUhcWins()));
        uhcCoinsLabel.setText(String.valueOf(stats.getUhcCoins()));
        uhcScoreLabel.setText(String.valueOf(stats.getUhcScore()));
        uhcKillsLabel.setText(String.valueOf(stats.getUhcKills()));
        uhcDeathsLabel.setText(String.valueOf(stats.getUhcDeaths()));
        
        uhcKdRatioLabel.setText(String.format("%.2f", stats.getUhcKDRatio()));
        applyStatStyle(uhcKdRatioLabel, stats.getUhcKDRatio(), "uhcKDRatio");
    }
    
    /**
     * Display Build Battle stats
     */
    private void displayBuildBattleStats(PlayerStats stats) {
        buildBattleWinsLabel.setText(String.valueOf(stats.getBuildBattleWins()));
        buildBattleGamesPlayedLabel.setText(String.valueOf(stats.getBuildBattleGamesPlayed()));
        
        // Calculate win rate
        double winRate = stats.getBuildBattleGamesPlayed() > 0 
            ? (double) stats.getBuildBattleWins() / stats.getBuildBattleGamesPlayed() * 100 
            : 0;
        buildBattleWinRateLabel.setText(String.format("%.2f%%", winRate));
        
        buildBattleCoinsLabel.setText(String.valueOf(stats.getBuildBattleCoins()));
        buildBattleScoreLabel.setText(String.valueOf(stats.getBuildBattleScore()));
        
        // Apply styling to Build Battle score
        applyStatStyle(buildBattleScoreLabel, stats.getBuildBattleScore(), "buildBattleScore");
    }
    
    /**
     * Display Mega Walls stats
     */
    private void displayMegaWallsStats(PlayerStats stats) {
        megaWallsWinsLabel.setText(String.valueOf(stats.getMegaWallsWins()));
        megaWallsAssistsLabel.setText(String.valueOf(stats.getMegaWallsAssists()));
        megaWallsKillsLabel.setText(String.valueOf(stats.getMegaWallsKills()));
        megaWallsDeathsLabel.setText(String.valueOf(stats.getMegaWallsDeaths()));
        
        megaWallsKdRatioLabel.setText(String.format("%.2f", stats.getMegaWallsKDRatio()));
        applyStatStyle(megaWallsKdRatioLabel, stats.getMegaWallsKDRatio(), "megaWallsKDRatio");
        
        megaWallsFinalKillsLabel.setText(String.valueOf(stats.getMegaWallsFinalKills()));
        megaWallsFinalDeathsLabel.setText(String.valueOf(stats.getMegaWallsFinalDeaths()));
        
        megaWallsFinalKdRatioLabel.setText(String.format("%.2f", stats.getMegaWallsFinalKDRatio()));
        applyStatStyle(megaWallsFinalKdRatioLabel, stats.getMegaWallsFinalKDRatio(), "megaWallsFinalKDRatio");
    }
    
    /**
     * Display SkyBlock stats
     */
    private void displaySkyBlockStats(PlayerStats stats) {
        skyblockProfileLabel.setText(stats.getSkyblockProfile() != null ? stats.getSkyblockProfile() : "None");
        skyblockCoinsLabel.setText(String.format("%,d", stats.getSkyblockCoins()));
        skyblockBankLabel.setText(String.format("%,d", stats.getSkyblockBank()));
        skyblockPurseLabel.setText(String.format("%,d", stats.getSkyblockPurse()));
    }
    
    /**
     * Apply style to a stat label based on its significance
     */
    private void applyStatStyle(Label label, double value, String key) {
        // Clear existing style classes
        label.getStyleClass().removeAll(
            "stat-exceptional", "stat-great", "stat-good", 
            "stat-average", "stat-below-average", "stat-poor"
        );
        
        // Add new style class
        label.getStyleClass().add(getStatStyleClass(value, key));
    }
    
    /**
     * Get the style class for a stat based on its significance
     */
    private String getStatStyleClass(double value, String key) {
        if (value <= 0) return "stat-average";
        
        // Use the PlayerStats utility method
        PlayerStats dummyStats = new PlayerStats();
        return dummyStats.getStatSignificanceClass(value, key);
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