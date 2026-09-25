package com.example.study_buddy;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.List;

/**
 * Dedicated modal dialog for creating or editing a Notebook.
 * Supports title, description, and curated theme color selection.
 */
public class NotebookDialog {

    private static final List<String> PALETTE = Arrays.asList(
            "#4f46e5", // Indigo (Default)
            "#059669", // Emerald
            "#7c3aed", // Violet
            "#d97706", // Amber
            "#e11d48", // Rose
            "#0284c7", // Sky Blue
            "#0f766e"  // Teal
    );

    public static void show(Window owner, int userId, Notebook existingNotebook, Runnable onSaved) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        boolean isEdit = (existingNotebook != null);
        dialog.setTitle(isEdit ? "Edit Notebook" : "Create New Notebook");
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(24));
        root.setPrefWidth(460);
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // 1. Header
        Label headerTitle = new Label(isEdit ? "✏ Edit Notebook" : "📘 Create New Notebook");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label headerSub = new Label(isEdit ? "Update notebook title, description, or theme color."
                : "Create a new study notebook to organize topics, lecture slides, and notes.");
        headerSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        headerSub.setWrapText(true);

        // 2. Title Field
        Label titleLabel = new Label("Notebook Title *");
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        TextField titleField = new TextField(isEdit ? existingNotebook.getTitle() : "");
        titleField.setPromptText("e.g. Data Structures & Algorithms, Physics, Biology");
        titleField.setStyle("-fx-padding: 9px 12px; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-border-color: #cbd5e1; -fx-font-size: 13px;");

        // 3. Description Field
        Label descLabel = new Label("Description (Optional)");
        descLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        TextArea descArea = new TextArea(isEdit ? existingNotebook.getDescription() : "");
        descArea.setPromptText("Add course code, semester info, or learning goals...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-padding: 4px; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-border-color: #cbd5e1; -fx-font-size: 13px;");

        // 4. Color Palette
        Label colorLabel = new Label("Theme Color");
        colorLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        HBox colorBox = new HBox(10);
        colorBox.setAlignment(Pos.CENTER_LEFT);

        final String[] selectedColor = {isEdit ? existingNotebook.getColorHex() : PALETTE.get(0)};
        final Button[] colorBtns = new Button[PALETTE.size()];

        for (int i = 0; i < PALETTE.size(); i++) {
            String color = PALETTE.get(i);
            Button cBtn = new Button();
            cBtn.setPrefSize(32, 32);
            cBtn.setMinSize(32, 32);
            cBtn.setMaxSize(32, 32);
            cBtn.setCursor(javafx.scene.Cursor.HAND);

            int btnIndex = i;
            boolean isSelected = color.equalsIgnoreCase(selectedColor[0]);
            updateColorButtonStyle(cBtn, color, isSelected);

            cBtn.setOnAction(e -> {
                selectedColor[0] = color;
                for (int j = 0; j < PALETTE.size(); j++) {
                    updateColorButtonStyle(colorBtns[j], PALETTE.get(j), j == btnIndex);
                }
            });

            colorBtns[i] = cBtn;
            colorBox.getChildren().add(cBtn);
        }

        // 5. Error message label
        Label errorLabel = new Label();
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #dc2626; -fx-font-weight: bold;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // 6. Action Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button(isEdit ? "Save Changes" : "Create Notebook");
        saveBtn.setStyle("-fx-background-color: linear-gradient(to right, #4f46e5, #6366f1); -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 20px; -fx-cursor: hand;");

        saveBtn.setOnAction(e -> {
            String title = titleField.getText() != null ? titleField.getText().trim() : "";
            if (title.isEmpty()) {
                errorLabel.setText("Please enter a notebook title.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                titleField.requestFocus();
                return;
            }

            String desc = descArea.getText() != null ? descArea.getText().trim() : "";
            String color = selectedColor[0];

            if (isEdit) {
                DatabaseHelper.updateNotebook(existingNotebook.getId(), title, desc, color);
            } else {
                DatabaseHelper.createNotebook(userId, title, desc, color);
            }

            dialog.close();
            if (onSaved != null) {
                onSaved.run();
            }
        });

        buttonBar.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(
                headerTitle, headerSub,
                titleLabel, titleField,
                descLabel, descArea,
                colorLabel, colorBox,
                errorLabel,
                buttonBar
        );

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static void updateColorButtonStyle(Button btn, String colorHex, boolean isSelected) {
        String border = isSelected ? "-fx-border-color: #1e293b; -fx-border-width: 3px;" : "-fx-border-color: transparent;";
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 50%; -fx-border-radius: 50%; " + border);
        btn.setText(isSelected ? "✓" : "");
        btn.setTextFill(javafx.scene.paint.Color.WHITE);
        btn.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 50%; -fx-border-radius: 50%; "
                + border + " -fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #ffffff;");
    }
}
