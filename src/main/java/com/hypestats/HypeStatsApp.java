package com.hypestats;

import com.hypestats.util.DevLogger;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Screen;
import javafx.geometry.Rectangle2D;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class HypeStatsApp extends Application {
    private static final String APP_TITLE = "HypeStats - Hypixel Statistics Companion";
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 700;
    
    private static boolean testMode = false;
    
    public static void main(String[] args) {
        // Check for test mode flag
        for (String arg : args) {
            if (arg.equals("--test") || arg.equals("-t")) {
                testMode = true;
                DevLogger.init();
                DevLogger.log("Starting application in TEST MODE");
                break;
            }
        }
        
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        try {
            // Create the scene first
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
            
            // Position the window on the right side of the screen by default
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            primaryStage.setX(screenBounds.getWidth() - DEFAULT_WIDTH);
            primaryStage.setY((screenBounds.getHeight() - DEFAULT_HEIGHT) / 2);
            
            String title = APP_TITLE;
            if (testMode) {
                title += " [TEST MODE]";
            }
            primaryStage.setTitle(title);
            
            // Set the scene on the stage first
            primaryStage.setScene(scene);
            
            // Now get the controller and initialize it with host services and stage
            com.hypestats.controller.MainController controller = loader.getController();
            controller.setHostServices(getHostServices());
            controller.setupStage(primaryStage);
            
            // Try to load icon if it exists
            try {
                InputStream iconStream = getClass().getResourceAsStream("/images/icon.png");
                if (iconStream != null) {
                    primaryStage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception e) {
                String msg = "Could not load application icon";
                log.warn(msg, e);
                if (testMode) DevLogger.log(msg + ": " + e.getMessage());
            }
            
            primaryStage.show();
            
            if (testMode) {
                DevLogger.log("Application UI loaded successfully");
            }
        } catch (Exception e) {
            String msg = "Error starting application";
            log.error(msg, e);
            if (testMode) DevLogger.log(msg + ": " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Check if the application is running in test mode
     * @return true if in test mode, false otherwise
     */
    public static boolean isTestMode() {
        return testMode;
    }
} 