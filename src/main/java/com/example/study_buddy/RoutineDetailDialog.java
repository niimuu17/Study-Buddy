package com.example.study_buddy;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Floating modal window that allows a student to enter class details:
 * - Subject name (e.g., CSE2008)
 * - 4-letter teacher code (e.g., SH)
 * Built in pure JavaFX.
 */
public class RoutineDetailDialog {

    public static void show(Window owner, int userId, String dayOfWeek, String timeSlot,
                            RoutineSlot existingSlot, Runnable onSaved) {

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Class Details: " + dayOfWeek + " (" + timeSlot + ")");
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', Arial, sans-serif;");
        root.setPrefWidth(560);

        // 1. Header
        Label headerTitle = new Label("Class Details 🎓");
        headerTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label headerSubtitle = new Label(dayOfWeek + " | " + timeSlot);
        headerSubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6366f1; -fx-font-weight: bold;");

        // 2. Subject Name field
        Label subjectLabel = new Label("Subject Name:");
        subjectLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField subjectField = new TextField();
        subjectField.setPromptText("e.g. CSE2008");
        subjectField.setStyle("-fx-padding: 8px 10px; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-border-color: #cbd5e1;");
        if (existingSlot != null && existingSlot.getSubjectName() != null) {
            subjectField.setText(existingSlot.getSubjectName());
        }

        // 3. Teacher Code field
        Label teacherLabel = new Label("Teacher Code (up to 4 letters):");
        teacherLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField teacherField = new TextField();
        teacherField.setPromptText("e.g. SH");
        teacherField.setStyle("-fx-padding: 8px 10px; -fx-background-radius: 6px; -fx-border-radius: 6px; -fx-border-color: #cbd5e1;");
        if (existingSlot != null && existingSlot.getTeacherCode() != null) {
            teacherField.setText(existingSlot.getTeacherCode());
        }

        // 4. Action Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button clearBtn = new Button("Clear Class");
        clearBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            DatabaseHelper.deleteRoutineSlot(userId, dayOfWeek, timeSlot);
            if (onSaved != null) onSaved.run();
            dialog.close();
        });

        Button saveBtn = new Button("Save Class Details");
        saveBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 18px; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            String subject = subjectField.getText().trim();
            String teacher = teacherField.getText().trim();

            if (subject.isEmpty() && teacher.isEmpty()) {
                DatabaseHelper.deleteRoutineSlot(userId, dayOfWeek, timeSlot);
            } else {
                RoutineSlot slot = new RoutineSlot(userId, dayOfWeek, timeSlot, subject, teacher);
                DatabaseHelper.saveRoutineSlot(slot);
            }

            if (onSaved != null) onSaved.run();
            dialog.close();
        });

        buttonBar.getChildren().addAll(clearBtn, cancelBtn, saveBtn);

        root.getChildren().addAll(
                headerTitle, headerSubtitle,
                subjectLabel, subjectField,
                teacherLabel, teacherField,
                new Separator(),
                buttonBar
        );

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
