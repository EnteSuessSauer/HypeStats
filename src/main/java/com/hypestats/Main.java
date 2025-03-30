package com.hypestats;

import com.hypestats.controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

/**
 * Main application class
 */
@Slf4j
public class Main extends Application {
    /**
     * JavaFX start method
     * @param primaryStage The primary stage
     * @throws Exception If an error occurs during initialization
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        log.info("Starting HypeStats application");
        
        // Load the main FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Parent root = loader.load();
        
        // Get the controller and pass the HostServices reference
        MainController controller = loader.getController();
        controller.setHostServices(getHostServices());
        
        // Setup the stage
        controller.setupStage(primaryStage);
        
        // Show the window
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    /**
     * Main method
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
} 