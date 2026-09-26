package com.example.study_buddy;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;

/**
 * Modern modal dialog allowing the user to view and update their Google Gemini API key.
 */
public class ApiKeyDialog {

    public static void show(Stage parentStage, Runnable onKeyUpdated) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Gemini API Configuration");
        dialog.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', system-ui, sans-serif;");
        root.setPrefWidth(460);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🔑");
        icon.setStyle("-fx-font-size: 24px;");
        VBox titleBox = new VBox(3);
        Label title = new Label("Gemini API Key Settings");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label subtitle = new Label("Required for AI quiz generation and automated short answer grading.");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, titleBox);

        // Current status banner
        HBox statusBox = new HBox(8);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        statusBox.setPadding(new Insets(8, 12, 8, 12));
        boolean hasKey = ApiKeyManager.hasApiKey();
        statusBox.setStyle(hasKey
                ? "-fx-background-color: #ecfdf5; -fx-background-radius: 6px; -fx-border-color: #a7f3d0; -fx-border-radius: 6px;"
                : "-fx-background-color: #fffbeb; -fx-background-radius: 6px; -fx-border-color: #fde68a; -fx-border-radius: 6px;");

        Label statusDot = new Label(hasKey ? "● Active:" : "● Missing:");
        statusDot.setStyle(hasKey ? "-fx-text-fill: #059669; -fx-font-weight: bold;" : "-fx-text-fill: #d97706; -fx-font-weight: bold;");
        Label statusText = new Label(hasKey ? ApiKeyManager.getMaskedApiKey() : "No Gemini API key detected");
        statusText.setStyle(hasKey ? "-fx-text-fill: #065f46; -fx-font-size: 12px;" : "-fx-text-fill: #92400e; -fx-font-size: 12px;");
        statusBox.getChildren().addAll(statusDot, statusText);

        // Input field
        VBox inputSection = new VBox(6);
        Label inputLabel = new Label("Enter or Update API Key:");
        inputLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        PasswordField keyInput = new PasswordField();
        keyInput.setPromptText("Paste your AI Studio key here (AIzaSy...)");
        keyInput.setText(ApiKeyManager.getApiKey());
        keyInput.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 8px 12px; -fx-font-size: 13px;");

        // Reveal toggle button
        CheckBox revealCheck = new CheckBox("Show key text");
        revealCheck.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        TextField plainInput = new TextField();
        plainInput.setStyle(keyInput.getStyle());
        plainInput.setManaged(false);
        plainInput.setVisible(false);

        revealCheck.selectedProperty().addListener((obs, oldV, isSelected) -> {
            if (isSelected) {
                plainInput.setText(keyInput.getText());
                keyInput.setVisible(false);
                keyInput.setManaged(false);
                plainInput.setVisible(true);
                plainInput.setManaged(true);
            } else {
                keyInput.setText(plainInput.getText());
                plainInput.setVisible(false);
                plainInput.setManaged(false);
                keyInput.setVisible(true);
                keyInput.setManaged(true);
            }
        });

        // Link to AI Studio
        Hyperlink aiStudioLink = new Hyperlink("Get a Free Gemini API Key from Google AI Studio ↗");
        aiStudioLink.setStyle("-fx-font-size: 11px; -fx-text-fill: #4f46e5;");
        aiStudioLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI("https://aistudio.google.com/app/apikey"));
                }
            } catch (Exception ex) {
                System.err.println("Could not open browser: " + ex.getMessage());
            }
        });

        inputSection.getChildren().addAll(inputLabel, keyInput, plainInput, revealCheck, aiStudioLink);

        // Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Save API Key");
        saveBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 20px; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            String enteredKey = revealCheck.isSelected() ? plainInput.getText() : keyInput.getText();
            ApiKeyManager.setApiKey(enteredKey != null ? enteredKey.trim() : "");
            if (onKeyUpdated != null) {
                onKeyUpdated.run();
            }
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        buttonBar.getChildren().addAll(spacer, cancelBtn, saveBtn);

        root.getChildren().addAll(header, statusBox, inputSection, buttonBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
