package com.example.study_buddy;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main JavaFX Application class for Study Buddy.
 * Initializes the SQLite database and presents the Login & Sign-Up portal.
 */
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Initialize SQLite database and tables
        DatabaseHelper.initDatabase();

        // Load the Login & Sign-Up portal view
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        // Pre-fit window to primary screen visual bounds to prevent floating window appearance
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        stage.setTitle("Study Buddy - Login & Sign Up");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        // Maximize after show() to ensure native window manager respects maximized state
        stage.setMaximized(true);
        Platform.runLater(() -> stage.setMaximized(true));

        // Start background Producer-Consumer thread pool
        NotebookJobQueue.getInstance().startConsumers(2);
    }

    @Override
    public void stop() {
        // Gracefully shutdown background thread pool on application exit
        NotebookJobQueue.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
