package com.example.study_buddy;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

/**
 * Controller for the Study Buddy Home Screen and Weekly Class Routine.
 * Supports:
 * - Empty routine start (0 weekdays, 0 time slots)
 * - Editable weekday names (with automatic database sync)
 * - Adding time slots to the left/right of any slot via right-click
 * - Time duration conflict and overlap detection
 * - Opening the floating cell details dialog with calendar DatePicker & time selector
 */
public class HelloController {

    @FXML private Label welcomeText;
    @FXML private Label userDetailText;
    @FXML private Button logoutButton;
    @FXML private GridPane routineGrid;

    private User currentUser;
    private List<String> weekdays = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();

    private static final List<String> DEFAULT_WEEKDAY_NAMES = Arrays.asList(
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

            // Load user's routine settings from SQLite (can be empty if user starts from zero)
            this.weekdays = DatabaseHelper.getUserWeekdays(user.getId());
            this.timeSlots = DatabaseHelper.getUserTimeSlots(user.getId());

            buildRoutineGrid();
        }
    }

    /**
     * Dynamically constructs the Class Routine grid or displays an empty state prompt.
     */
    private void buildRoutineGrid() {
        routineGrid.getChildren().clear();
        routineGrid.getColumnConstraints().clear();
        routineGrid.getRowConstraints().clear();

        if (currentUser == null) return;

        // 1. Check if user has zero weekdays or zero time slots (Start from nothing)
        if (weekdays.isEmpty() || timeSlots.isEmpty()) {
            VBox emptyPrompt = new VBox(12);
            emptyPrompt.setAlignment(Pos.CENTER);
            emptyPrompt.setPadding(new Insets(40));
            emptyPrompt.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");

            Label emptyTitle = new Label("📅 Your Routine is Empty");
            emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label emptySubtitle = new Label("You can build your schedule from scratch. Click '+ Add Weekday' or '+ Add Time Slot' above to begin!");
            emptySubtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Button quickSetupBtn = new Button("Load Starter Template (Mon-Fri, 5 Slots)");
            quickSetupBtn.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8px 16px;");
            quickSetupBtn.setOnAction(e -> {
                weekdays = new ArrayList<>(Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday"));
                timeSlots = new ArrayList<>(Arrays.asList("08:30 - 09:50", "10:00 - 11:20", "11:30 - 12:50", "01:30 - 02:50", "03:00 - 04:20"));
                DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
                buildRoutineGrid();
            });

            emptyPrompt.getChildren().addAll(emptyTitle, emptySubtitle, quickSetupBtn);
            routineGrid.add(emptyPrompt, 0, 0);
            return;
        }

        // Fetch all saved slots for this user
        Map<String, RoutineSlot> savedSlots = DatabaseHelper.getAllRoutineSlots(currentUser.getId());

        // 2. Top-Left Corner Cell: "Day \ Time"
        Label cornerLabel = new Label("Day \\ Time");
        cornerLabel.setAlignment(Pos.CENTER);
        cornerLabel.setPrefSize(130, 48);
        cornerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #475569; "
                + "-fx-background-color: #f1f5f9; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px;");
        routineGrid.add(cornerLabel, 0, 0);

        // 3. Top Header Row: Time Slots with Right-Click Context Menu ("Add to Left", "Add to Right")
        for (int col = 0; col < timeSlots.size(); col++) {
            int slotIndex = col;
            String slotTime = timeSlots.get(col);

            Label timeHeader = new Label(slotTime + "\n⚙");
            timeHeader.setAlignment(Pos.CENTER);
            timeHeader.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            timeHeader.setPrefSize(160, 48);
            timeHeader.setTooltip(new Tooltip("Right-click to add slot to left/right, edit, or delete"));
            timeHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #312e81; "
                    + "-fx-background-color: #e0e7ff; -fx-background-radius: 8px; -fx-border-color: #c7d2fe; -fx-border-radius: 8px; -fx-cursor: hand;");

            // Context menu for time slots
            ContextMenu menu = createTimeSlotContextMenu(slotIndex, slotTime);

            timeHeader.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY || e.getButton() == MouseButton.PRIMARY) {
                    menu.show(timeHeader, e.getScreenX(), e.getScreenY());
                }
            });

            routineGrid.add(timeHeader, col + 1, 0);
        }

        // 4. Left Column: Weekdays (Editable on click) & Routine Data Cells
        for (int row = 0; row < weekdays.size(); row++) {
            int dayIndex = row;
            String day = weekdays.get(row);

            // Weekday Header Cell (Editable)
            Label dayHeader = new Label(day + " ✏");
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setPrefSize(130, 85);
            dayHeader.setTooltip(new Tooltip("Click to rename this weekday"));
            dayHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b; "
                    + "-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-cursor: hand;");

            // Renaming weekday on click
            dayHeader.setOnMouseClicked(e -> handleRenameWeekday(dayIndex, day));

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
     * Creates context menu for time slots allowing addition to Left or Right with conflict validation.
     */
    private ContextMenu createTimeSlotContextMenu(int index, String currentSlot) {
        ContextMenu menu = new ContextMenu();

        MenuItem addLeftItem = new MenuItem("⬅ Add Slot to Left");
        addLeftItem.setOnAction(e -> promptAddSlotWithConflictCheck(index, "Left"));

        MenuItem addRightItem = new MenuItem("➡ Add Slot to Right");
        addRightItem.setOnAction(e -> promptAddSlotWithConflictCheck(index + 1, "Right"));

        MenuItem editItem = new MenuItem("✏ Edit Duration");
        editItem.setOnAction(e -> promptEditSlotDuration(index, currentSlot));

        MenuItem deleteItem = new MenuItem("🗑 Delete Slot");
        deleteItem.setOnAction(e -> {
            timeSlots.remove(index);
            DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
            buildRoutineGrid();
        });

        menu.getItems().addAll(addLeftItem, addRightItem, new SeparatorMenuItem(), editItem, deleteItem);
        return menu;
    }

    /**
     * Prompts for new slot duration and prevents addition if there is a conflict.
     */
    private void promptAddSlotWithConflictCheck(int insertIndex, String position) {
        TextInputDialog dialog = new TextInputDialog("09:00 - 10:00");
        dialog.setTitle("Add Time Slot (" + position + ")");
        dialog.setHeaderText("Enter class duration (e.g. 08:30 - 09:50 or 01:30 PM - 02:50 PM):");
        dialog.setContentText("Duration:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(slotInput -> {
            String newSlot = slotInput.trim();
            if (newSlot.isEmpty()) return;

            // Check for duration conflict
            String conflict = TimeSlotHelper.findConflict(newSlot, timeSlots, null);
            if (conflict != null) {
                showError("Duration Conflict!",
                        "The time slot \"" + newSlot + "\" conflicts with existing slot \"" + conflict + "\".\n\n"
                        + "You must fix the duration to avoid overlapping classes before adding.");
                return; // Block addition
            }

            if (insertIndex >= 0 && insertIndex <= timeSlots.size()) {
                timeSlots.add(insertIndex, newSlot);
            } else {
                timeSlots.add(newSlot);
            }

            DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
            buildRoutineGrid();
        });
    }

    /**
     * Prompts to edit existing slot duration with conflict validation.
     */
    private void promptEditSlotDuration(int index, String oldSlot) {
        TextInputDialog dialog = new TextInputDialog(oldSlot);
        dialog.setTitle("Edit Time Slot Duration");
        dialog.setHeaderText("Update duration for \"" + oldSlot + "\":");
        dialog.setContentText("New Duration:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(slotInput -> {
            String newSlot = slotInput.trim();
            if (newSlot.isEmpty() || newSlot.equalsIgnoreCase(oldSlot)) return;

            // Check conflict against other slots (ignoring the slot being edited)
            String conflict = TimeSlotHelper.findConflict(newSlot, timeSlots, oldSlot);
            if (conflict != null) {
                showError("Duration Conflict!",
                        "The duration \"" + newSlot + "\" conflicts with existing slot \"" + conflict + "\".\n\n"
                        + "You must fix the duration to avoid overlapping classes.");
                return; // Block update
            }

            DatabaseHelper.renameTimeSlot(currentUser.getId(), oldSlot, newSlot);
            timeSlots.set(index, newSlot);
            buildRoutineGrid();
        });
    }

    /**
     * Renames a weekday and updates database records.
     */
    private void handleRenameWeekday(int dayIndex, String oldDay) {
        TextInputDialog dialog = new TextInputDialog(oldDay);
        dialog.setTitle("Rename Weekday");
        dialog.setHeaderText("Enter new name for weekday (currently \"" + oldDay + "\"):");
        dialog.setContentText("Weekday Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newNameInput -> {
            String newDay = newNameInput.trim();
            if (!newDay.isEmpty() && !newDay.equalsIgnoreCase(oldDay)) {
                DatabaseHelper.renameWeekday(currentUser.getId(), oldDay, newDay);
                weekdays.set(dayIndex, newDay);
                buildRoutineGrid();
            }
        });
    }

    /**
     * Creates an interactive card for each schedule cell.
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

            // Special Activities Badge (e.g. 📌 1 Activity)
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
            showAlert("Weekdays Limit", "You already have 7 weekdays in your routine.");
            return;
        }

        // Find next unused day name or let user input
        String candidate = null;
        for (String day : DEFAULT_WEEKDAY_NAMES) {
            if (!weekdays.contains(day)) {
                candidate = day;
                break;
            }
        }
        if (candidate == null) candidate = "Day " + (weekdays.size() + 1);

        TextInputDialog dialog = new TextInputDialog(candidate);
        dialog.setTitle("Add Weekday");
        dialog.setHeaderText("Enter weekday name (e.g. Sunday, Monday, Theory Day):");
        dialog.setContentText("Weekday:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(inputDay -> {
            String clean = inputDay.trim();
            if (!clean.isEmpty() && !weekdays.contains(clean)) {
                weekdays.add(clean);
                DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
                buildRoutineGrid();
            }
        });
    }

    /**
     * Removes the last weekday from the routine.
     */
    @FXML
    public void handleRemoveWeekday() {
        if (currentUser == null || weekdays.isEmpty()) return;

        weekdays.remove(weekdays.size() - 1);
        DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
        buildRoutineGrid();
    }

    /**
     * Adds an editable time slot with conflict check.
     */
    @FXML
    public void handleAddTimeSlot() {
        if (currentUser == null) return;
        promptAddSlotWithConflictCheck(timeSlots.size(), "End");
    }

    /**
     * Removes the last time slot.
     */
    @FXML
    public void handleRemoveTimeSlot() {
        if (currentUser == null || timeSlots.isEmpty()) return;

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

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
