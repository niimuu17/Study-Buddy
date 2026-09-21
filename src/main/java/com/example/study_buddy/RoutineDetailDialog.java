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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Floating modal window that allows a student to enter class details:
 * - Subject name (e.g., CSE2008)
 * - 4-letter teacher code (e.g., SH)
 * - Dynamic list of special activities (CT, assignment deadline, project showcase)
 *   with an interactive calendar DatePicker and Time selector, plus "Add more" button.
 * Built 100% in pure JavaFX without relying on external CSS files.
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

        // 4. Special Activities Section
        Label activitiesHeader = new Label("Special Activities (CT, Deadlines, Showcase):");
        activitiesHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-padding: 6px 0 0 0;");

        Label activitiesHint = new Label("Pick activity type, date from calendar, and deadline time.");
        activitiesHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        VBox activitiesContainer = new VBox(8);

        // Helper class to add activity rows with calendar DatePicker + Time ComboBox
        class ActivityRowHelper {
            static void addRow(VBox container, String type, String existingDeadline) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                // Activity description/type
                TextField typeInput = new TextField(type != null ? type : "");
                typeInput.setPromptText("Activity (e.g. CT 1 / Assignment)");
                typeInput.setStyle("-fx-padding: 6px 8px; -fx-border-radius: 6px; -fx-border-color: #cbd5e1;");
                HBox.setHgrow(typeInput, Priority.ALWAYS);

                // Calendar DatePicker
                DatePicker datePicker = new DatePicker();
                datePicker.setPromptText("Pick Date 📅");
                datePicker.setPrefWidth(140);
                datePicker.setStyle("-fx-border-radius: 6px; -fx-border-color: #cbd5e1;");

                // Time selector
                ComboBox<String> timeCombo = new ComboBox<>();
                timeCombo.setEditable(true);
                timeCombo.getItems().addAll("11:59 PM", "11:30 PM", "05:00 PM", "02:00 PM", "12:00 PM", "10:00 AM", "Class Time");
                timeCombo.setPromptText("Time ⏰");
                timeCombo.setPrefWidth(120);
                timeCombo.setStyle("-fx-border-radius: 6px; -fx-border-color: #cbd5e1;");

                // Parse existing deadline string if available
                if (existingDeadline != null && !existingDeadline.trim().isEmpty()) {
                    String clean = existingDeadline.trim();
                    // Example format: "2026-09-30 (11:59 PM)" or "2026-09-30 11:59 PM" or "2026-09-30"
                    String datePart = clean;
                    String timePart = "";
                    if (clean.contains("(") && clean.contains(")")) {
                        int open = clean.indexOf('(');
                        int close = clean.indexOf(')');
                        datePart = clean.substring(0, open).trim();
                        timePart = clean.substring(open + 1, close).trim();
                    } else if (clean.contains(" ")) {
                        int space = clean.indexOf(' ');
                        datePart = clean.substring(0, space).trim();
                        timePart = clean.substring(space + 1).trim();
                    }

                    try {
                        datePicker.setValue(LocalDate.parse(datePart));
                    } catch (DateTimeParseException ignored) {
                        // If not standard ISO date, user can pick new date
                    }

                    if (!timePart.isEmpty()) {
                        timeCombo.setValue(timePart);
                    }
                }

                Button removeBtn = new Button("✕");
                removeBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
                removeBtn.setOnAction(e -> container.getChildren().remove(row));

                row.getChildren().addAll(typeInput, datePicker, timeCombo, removeBtn);
                container.getChildren().add(row);
            }
        }

        // Populate existing activities if any
        if (existingSlot != null && existingSlot.getActivities() != null) {
            for (SpecialActivity act : existingSlot.getActivities()) {
                ActivityRowHelper.addRow(activitiesContainer, act.getActivityType(), act.getDeadlineInfo());
            }
        }

        // "+ Add More Activity" button
        Button addMoreBtn = new Button("+ Add More Activity");
        addMoreBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #4f46e5; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 6px 12px;");
        addMoreBtn.setOnAction(e -> ActivityRowHelper.addRow(activitiesContainer, "", ""));

        // ScrollPane for activities
        ScrollPane activitiesScroll = new ScrollPane(activitiesContainer);
        activitiesScroll.setFitToWidth(true);
        activitiesScroll.setPrefHeight(150);
        activitiesScroll.setStyle("-fx-background-color: transparent; -fx-border-color: #e2e8f0; -fx-border-radius: 6px; -fx-padding: 4px;");

        // 5. Action Buttons
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

            if (subject.isEmpty() && teacher.isEmpty() && activitiesContainer.getChildren().isEmpty()) {
                // If everything is cleared, delete the slot
                DatabaseHelper.deleteRoutineSlot(userId, dayOfWeek, timeSlot);
            } else {
                // Collect activities with DatePicker and Time values
                List<SpecialActivity> activities = new ArrayList<>();
                for (var node : activitiesContainer.getChildren()) {
                    if (node instanceof HBox row) {
                        TextField typeIn = (TextField) row.getChildren().get(0);
                        DatePicker dp = (DatePicker) row.getChildren().get(1);
                        @SuppressWarnings("unchecked")
                        ComboBox<String> tc = (ComboBox<String>) row.getChildren().get(2);

                        String t = typeIn.getText().trim();
                        LocalDate pickedDate = dp.getValue();
                        String pickedTime = tc.getValue() != null ? tc.getValue().trim() : "";

                        // Construct deadline string e.g. "2026-09-30 (11:59 PM)"
                        StringBuilder deadlineBuilder = new StringBuilder();
                        if (pickedDate != null) {
                            deadlineBuilder.append(pickedDate.toString());
                        }
                        if (!pickedTime.isEmpty()) {
                            if (deadlineBuilder.length() > 0) deadlineBuilder.append(" ");
                            deadlineBuilder.append("(").append(pickedTime).append(")");
                        }

                        String deadlineStr = deadlineBuilder.toString();
                        if (!t.isEmpty() || !deadlineStr.isEmpty()) {
                            activities.add(new SpecialActivity(t, deadlineStr));
                        }
                    }
                }

                RoutineSlot slot = new RoutineSlot(userId, dayOfWeek, timeSlot, subject, teacher);
                slot.setActivities(activities);
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
                activitiesHeader, activitiesHint, activitiesScroll, addMoreBtn,
                new Separator(),
                buttonBar
        );

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
