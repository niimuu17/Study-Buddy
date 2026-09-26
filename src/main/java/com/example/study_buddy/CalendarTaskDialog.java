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
import javafx.stage.Window;

import java.time.LocalDate;

/**
 * Floating modal dialog for creating a task or academic activity directly from the Calendar:
 * - Activity Type (CT, Assignment, Quiz, Project, Exam, etc.)
 * - Subject Name (e.g. Math 2207, CSE2008)
 * - Task Title / Description
 * - Deadline Date and Time
 * - Notes
 */
public class CalendarTaskDialog {

    public static void show(Window owner, int userId, LocalDate preselectedDate, Runnable onSaved) {
        show(owner, userId, preselectedDate, null, onSaved);
    }

    public static void show(Window owner, int userId, LocalDate preselectedDate, RoutineTaskItem existingTask, Runnable onSaved) {
        boolean isEditing = existingTask != null;
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(isEditing ? "Edit Task / Activity ✏️" : "Add Task / Activity 📅");
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(22));
        root.setPrefWidth(440);
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label headerTitle = new Label(isEditing ? "Edit Task / Activity ✏️" : "Add Task / Activity 📅");
        headerTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4px;");
        closeBtn.setOnAction(e -> dialog.close());

        header.getChildren().addAll(headerTitle, closeBtn);

        // Feedback error message
        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // 1. Activity Type + Subject Row
        HBox typeSubjectRow = new HBox(12);

        VBox typeBox = new VBox(5);
        typeBox.setPrefWidth(180);
        Label typeLabel = new Label("Activity Type:");
        typeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("CT", "Assignment", "Quiz", "Project", "Exam", "Lab Report", "Presentation", "Other");
        typeCombo.setValue(isEditing ? existingTask.getParsedCategory() : "CT");
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setStyle("-fx-padding: 4px; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px;");
        typeBox.getChildren().addAll(typeLabel, typeCombo);

        VBox subjectBox = new VBox(5);
        HBox.setHgrow(subjectBox, Priority.ALWAYS);
        Label subjectLabel = new Label("Subject / Course:");
        subjectLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField subjectField = new TextField();
        subjectField.setPromptText("e.g. Math 2207");
        if (isEditing && existingTask.getSubjectName() != null && !existingTask.getSubjectName().equals("General")) {
            subjectField.setText(existingTask.getSubjectName());
        }
        subjectField.setStyle("-fx-padding: 7px 10px; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px;");
        subjectBox.getChildren().addAll(subjectLabel, subjectField);

        typeSubjectRow.getChildren().addAll(typeBox, subjectBox);

        // 2. Task Title / Details
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Task Title / Details:");
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField titleField = new TextField();
        titleField.setPromptText("e.g. CT 1 (Chapters 1 - 3)");
        if (isEditing) {
            titleField.setText(existingTask.getParsedTitle());
        }
        titleField.setStyle("-fx-padding: 7px 10px; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px;");
        titleBox.getChildren().addAll(titleLabel, titleField);

        // 3. Date & Time Row
        HBox dateTimeRow = new HBox(12);

        LocalDate initialDate = (isEditing && existingTask.getDeadlineDate() != null)
                ? existingTask.getDeadlineDate()
                : (preselectedDate != null ? preselectedDate : LocalDate.now());

        VBox dateBox = new VBox(5);
        dateBox.setPrefWidth(200);
        Label dateLabel = new Label("Deadline Date:");
        dateLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        DatePicker datePicker = new DatePicker(initialDate);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle("-fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px;");
        dateBox.getChildren().addAll(dateLabel, datePicker);

        VBox timeBox = new VBox(5);
        HBox.setHgrow(timeBox, Priority.ALWAYS);
        Label timeLabel = new Label("Time:");
        timeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        ComboBox<String> timeCombo = new ComboBox<>();
        timeCombo.setEditable(true);
        timeCombo.getItems().addAll("11:59 PM", "11:30 PM", "05:00 PM", "02:00 PM", "12:00 PM", "10:00 AM", "08:30 AM");
        timeCombo.setValue(isEditing && existingTask.getFormattedTime() != null ? existingTask.getFormattedTime() : "11:59 PM");
        timeCombo.setMaxWidth(Double.MAX_VALUE);
        timeCombo.setStyle("-fx-padding: 4px; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px;");
        timeBox.getChildren().addAll(timeLabel, timeCombo);

        dateTimeRow.getChildren().addAll(dateBox, timeBox);

        // 4. Notes (Optional)
        VBox notesBox = new VBox(5);
        Label notesLabel = new Label("Notes / Instructions (optional):");
        notesLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        TextField notesField = new TextField();
        notesField.setPromptText("e.g. Bring calculator and graph paper");
        if (isEditing && existingTask.getNotes() != null) {
            notesField.setText(existingTask.getNotes());
        }
        notesField.setStyle("-fx-padding: 7px 10px; -fx-background-radius: 6px; -fx-border-color: #cbd5e1; -fx-border-radius: 6px;");
        notesBox.getChildren().addAll(notesLabel, notesField);

        // 5. Action Buttons
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 16px; -fx-cursor: hand;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button(isEditing ? "Update Task" : "Save Task");
        saveBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 20px; -fx-cursor: hand;");
        saveBtn.setOnAction(e -> {
            String actType = typeCombo.getValue() != null ? typeCombo.getValue().trim() : "Task";
            String subject = subjectField.getText().trim();
            String title = titleField.getText().trim();
            LocalDate date = datePicker.getValue();
            String time = timeCombo.getValue() != null ? timeCombo.getValue().trim() : "11:59 PM";
            String notes = notesField.getText().trim();

            if (title.isEmpty() && subject.isEmpty()) {
                errorLabel.setText("Please enter a subject or task title.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            if (date == null) {
                errorLabel.setText("Please pick a deadline date.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                return;
            }

            String displayTitle = title.isEmpty() ? actType : title;
            String displaySubject = subject.isEmpty() ? "General" : subject;

            if (isEditing) {
                DatabaseHelper.updateAnyTask(existingTask.getActivityId(), displayTitle, displaySubject, actType, date.toString(), time, notes);
            } else {
                DatabaseHelper.createCalendarTask(userId, displayTitle, displaySubject, actType, date.toString(), time, notes);
            }

            if (onSaved != null) onSaved.run();
            dialog.close();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonBar.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(header, errorLabel, typeSubjectRow, titleBox, dateTimeRow, notesBox, new Separator(), buttonBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
