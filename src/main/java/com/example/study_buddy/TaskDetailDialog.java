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

import java.util.Optional;

/**
 * Floating read-only detail dialog for viewing task information:
 * - Subject and Task / Activity Name
 * - Due Date, Weekday, and Time
 * - Real-time Countdown and Status
 * - Notes
 * - Remove Task option
 */
public class TaskDetailDialog {

    public static void show(Window owner, RoutineTaskItem task, Runnable onAction) {
        show(owner, 0, task, onAction);
    }

    public static void show(Window owner, int userId, RoutineTaskItem task, Runnable onAction) {
        if (task == null) return;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Task Details: " + task.getActivityType());
        dialog.setResizable(false);

        VBox root = new VBox(14);
        root.setPadding(new Insets(22));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #ffffff; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        // 1. Header Row
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label headerTitle = new Label("Task Details 📋");
        headerTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(headerTitle, Priority.ALWAYS);

        Button closeTopBtn = new Button("✕");
        closeTopBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 4px;");
        closeTopBtn.setOnAction(e -> dialog.close());

        headerRow.getChildren().addAll(headerTitle, closeTopBtn);

        // 2. Title & Subject Banner
        HBox banner = new HBox(8);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setStyle("-fx-background-color: #f8fafc; -fx-padding: 10px 12px; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");

        Label subjectBadge = new Label(task.getSubjectName());
        subjectBadge.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 3px 8px;");

        Label taskTitle = new Label(task.getActivityType());
        taskTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(taskTitle, Priority.ALWAYS);

        banner.getChildren().addAll(subjectBadge, taskTitle);

        // 3. Details Card
        VBox detailsBox = new VBox(10);
        detailsBox.setStyle("-fx-background-color: #ffffff; -fx-padding: 6px 4px;");

        // Date Row
        HBox dateRow = createInfoRow("📅 Due Date", task.getFormattedDate());

        // Weekday Row
        HBox weekdayRow = createInfoRow("🗓️ Weekday", task.getWeekdayDisplay());

        // Time Row
        HBox timeRow = createInfoRow("⏰ Time", task.getFormattedTime());

        // Countdown / Status Row
        HBox countdownRow = new HBox(8);
        countdownRow.setAlignment(Pos.CENTER_LEFT);
        Label countdownLabelName = new Label("⏳ Status");
        countdownLabelName.setPrefWidth(110);
        countdownLabelName.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        RoutineTaskItem.TaskColorTheme theme = task.getColorTheme();
        Label countdownPill = new Label(task.getFormattedClockCountdown());
        countdownPill.setStyle("-fx-background-color: " + theme.getBadgeBg() + "; -fx-text-fill: " + theme.getBadgeText() + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 4px 10px;");
        countdownRow.getChildren().addAll(countdownLabelName, countdownPill);

        detailsBox.getChildren().addAll(dateRow, weekdayRow, timeRow, countdownRow);

        // Notes section if available
        if (task.getNotes() != null && !task.getNotes().trim().isEmpty()) {
            VBox notesBox = new VBox(4);
            Label notesTitle = new Label("📝 Notes");
            notesTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
            Label notesContent = new Label(task.getNotes());
            notesContent.setWrapText(true);
            notesContent.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-background-color: #f8fafc; -fx-padding: 8px 10px; -fx-background-radius: 6px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px;");
            notesBox.getChildren().addAll(notesTitle, notesContent);
            detailsBox.getChildren().add(notesBox);
        }

        // 4. Action Buttons: Remove Task, Edit Task, Close
        HBox buttonBar = new HBox(10);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(12, 0, 0, 0));

        Button removeBtn = new Button("🗑 Remove Task");
        removeBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 14px; -fx-cursor: hand;");
        removeBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Remove Task");
            confirm.setHeaderText("Remove task \"" + task.getActivityType() + "\"?");
            confirm.setContentText("Subject: " + task.getSubjectName() + "\nDate: " + task.getFormattedDate() + " (" + task.getFormattedTime() + ")");
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.OK) {
                DatabaseHelper.deleteCalendarTask(task.getActivityId());
                DatabaseHelper.deleteRoutineActivity(task.getActivityId());
                if (onAction != null) onAction.run();
                dialog.close();
            }
        });

        Button editBtn = new Button("✏ Edit Task");
        editBtn.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 14px; -fx-cursor: hand;");
        editBtn.setOnAction(e -> {
            dialog.close();
            CalendarTaskDialog.show(owner, userId, task.getDeadlineDate(), task, onAction);
        });

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 8px 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        buttonBar.getChildren().addAll(removeBtn, spacer, editBtn, closeBtn);

        root.getChildren().addAll(headerRow, banner, detailsBox, new Separator(), buttonBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static HBox createInfoRow(String labelName, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelName);
        label.setPrefWidth(110);
        label.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label valLabel = new Label(value != null && !value.isEmpty() ? value : "—");
        valLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: #1e293b;");
        HBox.setHgrow(valLabel, Priority.ALWAYS);

        row.getChildren().addAll(label, valLabel);
        return row;
    }
}
