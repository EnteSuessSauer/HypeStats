package com.hypestats;

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
import java.util.Objects;

@Slf4j
public class HypeStatsApp extends Application {
    private static final String APP_TITLE = "HypeStats - Hypixel Bedwars Companion";
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        
        // Position the window on the right side of the screen by default
        // This is useful as most people would run this alongside Minecraft
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(screenBounds.getWidth() - DEFAULT_WIDTH);
        primaryStage.setY((screenBounds.getHeight() - DEFAULT_HEIGHT) / 2);
        
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());
        
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        
        // Try to load icon if it exists
        try {
            InputStream iconStream = getClass().getResourceAsStream("/images/icon.png");
            if (iconStream != null) {
                primaryStage.getIcons().add(new Image(iconStream));
            }
        } catch (Exception e) {
            log.warn("Could not load application icon", e);
        }
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
} 