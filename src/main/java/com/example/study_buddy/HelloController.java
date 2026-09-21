package com.example.study_buddy;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

/**
 * Controller for the Study Buddy Home Screen and Weekly Class Routine.
 * Manages dynamic weekdays (up to 7), customizable time slots, and interactive class cells.
 */
public class HelloController {

    @FXML private Label welcomeText;
    @FXML private Label userDetailText;
    @FXML private Button logoutButton;
    @FXML private GridPane routineGrid;

    private User currentUser;
    private List<String> weekdays = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();

    private static final List<String> ALL_WEEKDAYS = Arrays.asList(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    );

    /**
     * Initializes the dashboard with the authenticated user and loads their routine.
     */
    public void initUser(User user) {
        this.currentUser = user;
        if (user != null) {
            welcomeText.setText("Welcome back, " + user.getUsername() + "! 📚");
            userDetailText.setText("Logged in as: " + user.getEmail());

            // Load user's routine settings from SQLite
            this.weekdays = DatabaseHelper.getUserWeekdays(user.getId());
            this.timeSlots = DatabaseHelper.getUserTimeSlots(user.getId());

            buildRoutineGrid();
        }
    }

    /**
     * Dynamically constructs the Class Routine grid.
     */
    private void buildRoutineGrid() {
        routineGrid.getChildren().clear();
        routineGrid.getColumnConstraints().clear();
        routineGrid.getRowConstraints().clear();

        if (currentUser == null) return;

        // Fetch all saved slots for this user
        Map<String, RoutineSlot> savedSlots = DatabaseHelper.getAllRoutineSlots(currentUser.getId());

        // 1. Top-Left Corner Cell: "Days \ Times"
        Label cornerLabel = new Label("Day \\ Time");
        cornerLabel.setAlignment(Pos.CENTER);
        cornerLabel.setPrefSize(130, 45);
        cornerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #475569; "
                + "-fx-background-color: #f1f5f9; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");
        routineGrid.add(cornerLabel, 0, 0);

        // 2. Top Header Row: Time Slots
        for (int col = 0; col < timeSlots.size(); col++) {
            String slotTime = timeSlots.get(col);
            Label timeHeader = new Label(slotTime);
            timeHeader.setAlignment(Pos.CENTER);
            timeHeader.setPrefSize(160, 45);
            timeHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #312e81; "
                    + "-fx-background-color: #e0e7ff; -fx-background-radius: 8px; -fx-border-color: #c7d2fe; -fx-border-radius: 8px;");
            routineGrid.add(timeHeader, col + 1, 0);
        }

        // 3. Left Column: Weekdays & Routine Data Cells
        for (int row = 0; row < weekdays.size(); row++) {
            String day = weekdays.get(row);

            // Weekday Header Cell
            Label dayHeader = new Label(day);
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setPrefSize(130, 85);
            dayHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b; "
                    + "-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px;");
            routineGrid.add(dayHeader, 0, row + 1);

            // Data Cells for this day
            for (int col = 0; col < timeSlots.size(); col++) {
                String time = timeSlots.get(col);
                String key = day + "|||" + time;
                RoutineSlot slot = savedSlots.get(key);

                VBox cellCard = createCellCard(day, time, slot);
                routineGrid.add(cellCard, col + 1, row + 1);
            }
        }
    }

    /**
     * Creates an interactive card for each schedule cell.
     * Displays Subject Name (top), 4-letter Teacher Code (bottom), and activity badge if any.
     */
    private VBox createCellCard(String day, String time, RoutineSlot slot) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(160, 85);
        card.setPadding(new Insets(8));

        boolean hasClass = (slot != null && !slot.isEmpty());

        if (hasClass) {
            // Subject Name (e.g. CSE2008)
            Label subjectLabel = new Label(slot.getSubjectName());
            subjectLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e1b4b;");

            // Teacher Code (e.g. SH)
            Label teacherLabel = new Label(slot.getTeacherCode() != null && !slot.getTeacherCode().isEmpty()
                    ? slot.getTeacherCode() : "-");
            teacherLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4338ca;");

            card.getChildren().addAll(subjectLabel, teacherLabel);

            // Special Activities Badge (e.g. 📌 CT / Deadline)
            if (slot.getActivities() != null && !slot.getActivities().isEmpty()) {
                int count = slot.getActivities().size();
                String badgeText = "📌 " + count + (count == 1 ? " Activity" : " Activities");
                Label activityBadge = new Label(badgeText);
                activityBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #b45309; "
                        + "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
                card.getChildren().add(activityBadge);
            }

            card.setStyle("-fx-background-color: #eef2ff; -fx-background-radius: 8px; "
                    + "-fx-border-color: #c7d2fe; -fx-border-radius: 8px; -fx-cursor: hand;");
        } else {
            // Empty Cell
            Label addLabel = new Label("+ Add Class");
            addLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold;");
            card.getChildren().add(addLabel);
            card.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8px; "
                    + "-fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-style: dashed; -fx-cursor: hand;");
        }

        // Hover Effect
        card.setOnMouseEntered(e -> card.setStyle(
                (hasClass ? "-fx-background-color: #e0e7ff; -fx-border-color: #818cf8;"
                          : "-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1;")
                + "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                (hasClass ? "-fx-background-color: #eef2ff; -fx-border-color: #c7d2fe;"
                          : "-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-style: dashed;")
                + "-fx-background-radius: 8px; -fx-border-radius: 8px; -fx-cursor: hand;"
        ));

        // Click Event: opens the floating modal dialog
        card.setOnMouseClicked(e -> {
            RoutineDetailDialog.show(
                    routineGrid.getScene().getWindow(),
                    currentUser.getId(),
                    day,
                    time,
                    slot,
                    this::buildRoutineGrid
            );
        });

        return card;
    }

    /**
     * Adds a new weekday to the routine (up to 7 days).
     */
    @FXML
    public void handleAddWeekday() {
        if (currentUser == null) return;

        if (weekdays.size() >= 7) {
            showAlert("Weekdays Limit", "You have already added all 7 weekdays to your routine.");
            return;
        }

        // Find the next day not currently in the routine
        String nextDayToAdd = null;
        for (String day : ALL_WEEKDAYS) {
            if (!weekdays.contains(day)) {
                nextDayToAdd = day;
                break;
            }
        }

        if (nextDayToAdd != null) {
            weekdays.add(nextDayToAdd);
            DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
            buildRoutineGrid();
        }
    }

    /**
     * Removes the last weekday from the routine.
     */
    @FXML
    public void handleRemoveWeekday() {
        if (currentUser == null) return;

        if (weekdays.size() <= 1) {
            showAlert("Minimum Days", "Your routine must contain at least 1 weekday.");
            return;
        }

        weekdays.remove(weekdays.size() - 1);
        DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
        buildRoutineGrid();
    }

    /**
     * Adds an editable time slot.
     */
    @FXML
    public void handleAddTimeSlot() {
        if (currentUser == null) return;

        TextInputDialog dialog = new TextInputDialog("04:30 - 05:50");
        dialog.setTitle("Add Class Time Slot");
        dialog.setHeaderText("Enter class duration / time slot (e.g. 04:30 - 05:50):");
        dialog.setContentText("Time Slot:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(slot -> {
            String cleanSlot = slot.trim();
            if (!cleanSlot.isEmpty() && !timeSlots.contains(cleanSlot)) {
                timeSlots.add(cleanSlot);
                DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
                buildRoutineGrid();
            }
        });
    }

    /**
     * Removes the last time slot.
     */
    @FXML
    public void handleRemoveTimeSlot() {
        if (currentUser == null) return;

        if (timeSlots.size() <= 1) {
            showAlert("Minimum Slots", "Your routine must contain at least 1 time slot.");
            return;
        }

        timeSlots.remove(timeSlots.size() - 1);
        DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
        buildRoutineGrid();
    }

    /**
     * Logs out the user and redirects back to the login portal.
     * Ensures the window remains maximized.
     */
    @FXML
    protected void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Study Buddy - Login & Sign Up");
            stage.setMaximized(true);
            stage.setResizable(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
