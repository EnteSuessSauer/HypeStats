package com.hypestats.controller;

import com.hypestats.model.BedwarsStats;
import com.hypestats.model.GameStats;
import com.hypestats.model.MegaWallsStats;
import com.hypestats.model.PlayerStats;
import com.hypestats.model.SkyBlockStats;
import com.hypestats.model.TNTGamesStats;
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
    
    // Additional game mode container panes
    @FXML
    private TitledPane murderMysteryPane;
    
    @FXML
    private TitledPane tntGamesPane;
    
    @FXML
    private TitledPane uhcPane;
    
    @FXML
    private TitledPane buildBattlePane;
    
    @FXML
    private TitledPane megaWallsPane;
    
    @FXML
    private TitledPane skyblockPane;
    
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
        
        // Display achievement count instead of percentage
        if (stats.getAchievementCompletionPercent() > 0) {
            achievementCompletionLabel.setText("(" + stats.getAchievementCompletionPercent() + " completed)");
        } else {
            achievementCompletionLabel.setText("");
        }
        
        // Set account age
        accountAgeLabel.setText(stats.getFormattedAccountAge());
        
        // Set playtime
        playtimeLabel.setText(stats.getFormattedPlaytime());
    }
    
    /**
     * Display Bedwars stats
     */
    private void displayBedwarsStats(PlayerStats stats) {
        BedwarsStats bedwarsStats = stats.getBedwarsStats();
        
        // If no Bedwars stats available, don't display anything
        if (bedwarsStats == null || !bedwarsStats.hasStats()) {
            return;
        }
        
        // Bedwars Stats
        winsLabel.setText(String.valueOf(bedwarsStats.getWins()));
        lossesLabel.setText(String.valueOf(bedwarsStats.getLosses()));
        
        wlRatioLabel.setText(bedwarsStats.getFormattedWLRatio());
        applyStatStyle(wlRatioLabel, bedwarsStats.getWlRatio(), "bedwarsWLRatio");
        
        winstreakLabel.setText(bedwarsStats.getWinstreakDisplay());
        
        killsLabel.setText(String.valueOf(bedwarsStats.getKills()));
        deathsLabel.setText(String.valueOf(bedwarsStats.getDeaths()));
        
        kdRatioLabel.setText(bedwarsStats.getFormattedKDRatio());
        applyStatStyle(kdRatioLabel, bedwarsStats.getKdRatio(), "bedwarsKDRatio");
        
        gamesPlayedLabel.setText(String.valueOf(bedwarsStats.getGamesPlayed()));
        
        finalKillsLabel.setText(String.valueOf(bedwarsStats.getFinalKills()));
        finalDeathsLabel.setText(String.valueOf(bedwarsStats.getFinalDeaths()));
        
        finalKdRatioLabel.setText(bedwarsStats.getFormattedFinalKDRatio());
        applyStatStyle(finalKdRatioLabel, bedwarsStats.getFinalKDRatio(), "bedwarsFinalKDRatio");
        
        bedsBrokenLabel.setText(String.valueOf(bedwarsStats.getBedsBroken()));
        bedsLostLabel.setText(String.valueOf(bedwarsStats.getBedsLost()));
        bedRatioLabel.setText(bedwarsStats.getFormattedBedRatio());
    }
    
    /**
     * Display Skywars stats
     */
    private void displaySkywarsStats(PlayerStats stats) {
        GameStats skywarsStats = stats.getSkywarsStats();
        
        // If no Skywars stats available, don't display anything
        if (skywarsStats == null || !skywarsStats.hasStats()) {
            return;
        }
        
        skywarsLevelLabel.setText(skywarsStats.getFormattedLevel());
        applyStatStyle(skywarsLevelLabel, skywarsStats.getLevel(), "skywarsLevel");
        
        skywarsWinsLabel.setText(String.valueOf(skywarsStats.getWins()));
        skywarsLossesLabel.setText(String.valueOf(skywarsStats.getLosses()));
        
        skywarsWlRatioLabel.setText(skywarsStats.getFormattedWLRatio());
        applyStatStyle(skywarsWlRatioLabel, skywarsStats.getWlRatio(), "skywarsWLRatio");
        
        skywarsKillsLabel.setText(String.valueOf(skywarsStats.getKills()));
        skywarsDeathsLabel.setText(String.valueOf(skywarsStats.getDeaths()));
        
        skywarsKdRatioLabel.setText(skywarsStats.getFormattedKDRatio());
        applyStatStyle(skywarsKdRatioLabel, skywarsStats.getKdRatio(), "skywarsKDRatio");
        
        skywarsGamesPlayedLabel.setText(String.valueOf(skywarsStats.getGamesPlayed()));
    }
    
    /**
     * Display Duels stats
     */
    private void displayDuelsStats(PlayerStats stats) {
        GameStats duelsStats = stats.getDuelsStats();
        
        // If no Duels stats available, don't display anything
        if (duelsStats == null || !duelsStats.hasStats()) {
            return;
        }
        
        duelsWinsLabel.setText(String.valueOf(duelsStats.getWins()));
        duelsLossesLabel.setText(String.valueOf(duelsStats.getLosses()));
        
        duelsWlRatioLabel.setText(duelsStats.getFormattedWLRatio());
        applyStatStyle(duelsWlRatioLabel, duelsStats.getWlRatio(), "duelsWLRatio");
        
        duelsKillsLabel.setText(String.valueOf(duelsStats.getKills()));
        duelsDeathsLabel.setText(String.valueOf(duelsStats.getDeaths()));
        
        duelsKdRatioLabel.setText(duelsStats.getFormattedKDRatio());
        applyStatStyle(duelsKdRatioLabel, duelsStats.getKdRatio(), "duelsKDRatio");
        
        duelsGamesPlayedLabel.setText(String.valueOf(duelsStats.getGamesPlayed()));
    }
    
    /**
     * Display Murder Mystery stats
     */
    private void displayMurderMysteryStats(PlayerStats stats) {
        GameStats mmStats = stats.getMurderMysteryStats();
        
        // Check if stats exist
        if (mmStats == null || !mmStats.hasStats()) {
            murderMysteryPane.setVisible(false);
            return; // No Murder Mystery stats available
        }
        
        // Show the pane since we have data
        murderMysteryPane.setVisible(true);
        
        mmWinsLabel.setText(String.valueOf(mmStats.getWins()));
        mmGamesPlayedLabel.setText(String.valueOf(mmStats.getGamesPlayed()));
        
        // Calculate win rate
        double winRate = mmStats.getGamesPlayed() > 0 
            ? (double) mmStats.getWins() / mmStats.getGamesPlayed() * 100 
            : 0;
        mmWinRateLabel.setText(String.format("%.2f%%", winRate));
        
        mmCoinsLabel.setText(String.valueOf(mmStats.getCoins()));
        mmKillsLabel.setText(String.valueOf(mmStats.getKills()));
        mmDeathsLabel.setText(String.valueOf(mmStats.getDeaths()));
        
        mmKdRatioLabel.setText(mmStats.getFormattedKDRatio());
        applyStatStyle(mmKdRatioLabel, mmStats.getKdRatio(), "mmKDRatio");
    }
    
    /**
     * Display TNT Games stats
     */
    private void displayTNTGamesStats(PlayerStats stats) {
        // Get TNT Games stats
        TNTGamesStats tntStats = stats.getTNTGamesStats();
        
        // Check if stats exist
        if (tntStats == null || !tntStats.hasStats()) {
            tntGamesPane.setVisible(false);
            return; // No TNT Games stats available
        }
        
        // Show the pane since we have data
        tntGamesPane.setVisible(true);
        
        tntgamesWinsLabel.setText(String.valueOf(tntStats.getWins()));
        tntgamesCoinsLabel.setText(String.valueOf(tntStats.getCoins()));
        tntRunWinsLabel.setText(String.valueOf(tntStats.getTntRunWins()));
        tntRunRecordLabel.setText(tntStats.getFormattedTNTRunRecord());
        bowSpleefWinsLabel.setText(String.valueOf(tntStats.getBowSpleefWins()));
        wizardsWinsLabel.setText(String.valueOf(tntStats.getWizardsWins()));
        pvpRunWinsLabel.setText(String.valueOf(tntStats.getPvpRunWins()));
        
        // Apply styling to TNT Run record
        applyStatStyle(tntRunRecordLabel, tntStats.getTntRunRecord(), "tntRunRecord");
    }
    
    /**
     * Display UHC stats
     */
    private void displayUHCStats(PlayerStats stats) {
        GameStats uhcStats = stats.getUHCStats();
        
        // Check if stats exist
        if (uhcStats == null || !uhcStats.hasStats()) {
            uhcPane.setVisible(false);
            return; // No UHC stats available
        }
        
        // Show the pane since we have data
        uhcPane.setVisible(true);
        
        uhcWinsLabel.setText(String.valueOf(uhcStats.getWins()));
        uhcCoinsLabel.setText(String.valueOf(uhcStats.getCoins()));
        uhcScoreLabel.setText(String.valueOf(uhcStats.getScore()));
        uhcKillsLabel.setText(String.valueOf(uhcStats.getKills()));
        uhcDeathsLabel.setText(String.valueOf(uhcStats.getDeaths()));
        
        uhcKdRatioLabel.setText(uhcStats.getFormattedKDRatio());
        applyStatStyle(uhcKdRatioLabel, uhcStats.getKdRatio(), "uhcKDRatio");
    }
    
    /**
     * Display Build Battle stats
     */
    private void displayBuildBattleStats(PlayerStats stats) {
        GameStats buildBattleStats = stats.getBuildBattleStats();
        
        // Check if stats exist
        if (buildBattleStats == null || !buildBattleStats.hasStats()) {
            buildBattlePane.setVisible(false);
            return; // No Build Battle stats available
        }
        
        // Show the pane since we have data
        buildBattlePane.setVisible(true);
        
        buildBattleWinsLabel.setText(String.valueOf(buildBattleStats.getWins()));
        buildBattleGamesPlayedLabel.setText(String.valueOf(buildBattleStats.getGamesPlayed()));
        
        // Calculate win rate
        double winRate = buildBattleStats.getGamesPlayed() > 0 
            ? (double) buildBattleStats.getWins() / buildBattleStats.getGamesPlayed() * 100 
            : 0;
        buildBattleWinRateLabel.setText(String.format("%.2f%%", winRate));
        
        buildBattleCoinsLabel.setText(String.valueOf(buildBattleStats.getCoins()));
        buildBattleScoreLabel.setText(String.valueOf(buildBattleStats.getScore()));
        
        // Apply styling to Build Battle score
        applyStatStyle(buildBattleScoreLabel, buildBattleStats.getScore(), "buildBattleScore");
    }
    
    /**
     * Display Mega Walls stats
     */
    private void displayMegaWallsStats(PlayerStats stats) {
        MegaWallsStats megaWallsStats = stats.getMegaWallsStats();
        
        // Check if stats exist
        if (megaWallsStats == null || !megaWallsStats.hasStats()) {
            megaWallsPane.setVisible(false);
            return; // No Mega Walls stats available
        }
        
        // Show the pane since we have data
        megaWallsPane.setVisible(true);
        
        megaWallsWinsLabel.setText(String.valueOf(megaWallsStats.getWins()));
        megaWallsAssistsLabel.setText(String.valueOf(megaWallsStats.getAssists()));
        megaWallsKillsLabel.setText(String.valueOf(megaWallsStats.getKills()));
        megaWallsDeathsLabel.setText(String.valueOf(megaWallsStats.getDeaths()));
        
        megaWallsKdRatioLabel.setText(megaWallsStats.getFormattedKDRatio());
        applyStatStyle(megaWallsKdRatioLabel, megaWallsStats.getKdRatio(), "megaWallsKDRatio");
        
        megaWallsFinalKillsLabel.setText(String.valueOf(megaWallsStats.getFinalKills()));
        megaWallsFinalDeathsLabel.setText(String.valueOf(megaWallsStats.getFinalDeaths()));
        
        megaWallsFinalKdRatioLabel.setText(megaWallsStats.getFormattedFinalKDRatio());
        applyStatStyle(megaWallsFinalKdRatioLabel, megaWallsStats.getFinalKDRatio(), "megaWallsFinalKDRatio");
    }
    
    /**
     * Display SkyBlock stats
     */
    private void displaySkyBlockStats(PlayerStats stats) {
        SkyBlockStats skyblockStats = stats.getSkyBlockStats();
        
        // Check if stats exist
        if (skyblockStats == null || !skyblockStats.hasStats()) {
            skyblockPane.setVisible(false);
            return; // No SkyBlock stats available
        }
        
        // Show the pane since we have data
        skyblockPane.setVisible(true);
        
        skyblockProfileLabel.setText(skyblockStats.getProfileName() != null ? skyblockStats.getProfileName() : "Unknown");
        skyblockCoinsLabel.setText(skyblockStats.getFormattedTotalCoins());
        skyblockBankLabel.setText(skyblockStats.getFormattedBank());
        skyblockPurseLabel.setText(skyblockStats.getFormattedPurse());
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