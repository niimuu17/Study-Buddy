package com.example.study_buddy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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

        stage.setTitle("Study Buddy - Login & Sign Up");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMaximized(true);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
