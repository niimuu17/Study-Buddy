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
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

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

    // Sidebar components
    @FXML private Button leftToggleBtn;
    @FXML private Button rightToggleBtn;
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;

    private boolean isLeftSidebarOpen = false;
    private boolean isRightSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 230.0;

    private User currentUser;
    private List<String> weekdays = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();

    private static final List<String> DEFAULT_WEEKDAY_NAMES = Arrays.asList(
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    );

    @FXML
    public void initialize() {
        if (leftSidebar != null) {
            leftSidebar.setVisible(false);
            leftSidebar.setManaged(false);
            leftSidebar.setPrefWidth(0);
            leftSidebar.setMinWidth(0);
            leftSidebar.setMaxWidth(0);
        }
        if (rightSidebar != null) {
            rightSidebar.setVisible(false);
            rightSidebar.setManaged(false);
            rightSidebar.setPrefWidth(0);
            rightSidebar.setMinWidth(0);
            rightSidebar.setMaxWidth(0);
        }
        if (leftToggleBtn != null) {
            leftToggleBtn.setText("☰ Sidebar");
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setText("Sidebar ▤");
        }
    }

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
                timeSlots = new ArrayList<>(Arrays.asList("08:30 - 09:50", "10:00 - 11:20", "11:30 - 12:50", "01:30 PM - 02:50 PM", "03:00 PM - 04:20 PM"));
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

        // 4. Left Column: Weekdays (Editable on click / right-click context menu) & Routine Data Cells
        for (int row = 0; row < weekdays.size(); row++) {
            int dayIndex = row;
            String day = weekdays.get(row);

            // Weekday Header Cell (Right-click for options, click to rename)
            Label dayHeader = new Label(day + "\n⚙");
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            dayHeader.setPrefSize(130, 85);
            dayHeader.setTooltip(new Tooltip("Right-click to add day above/below, rename, or delete"));
            dayHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b; "
                    + "-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-cursor: hand;");

            // Context menu for weekday operations (Add Above, Add Below, Rename, Delete)
            ContextMenu weekdayMenu = createWeekdayContextMenu(dayIndex, day);

            dayHeader.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    weekdayMenu.show(dayHeader, e.getScreenX(), e.getScreenY());
                } else if (e.getButton() == MouseButton.PRIMARY) {
                    handleRenameWeekday(dayIndex, day);
                }
            });

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
     * Uses the modal TimeSlotDialog which stays open until the user resolves any duration conflict.
     */
    private void promptAddSlotWithConflictCheck(int insertIndex, String position) {
        String defaultDuration = "09:00 - 10:00";
        TimeSlotDialog.show(
                routineGrid.getScene().getWindow(),
                "Add Time Slot (" + position + ")",
                "Enter class duration (e.g. 08:30 - 09:50 or 01:30 PM - 02:50 PM):",
                defaultDuration,
                timeSlots,
                null,
                newSlot -> {
                    if (insertIndex >= 0 && insertIndex <= timeSlots.size()) {
                        timeSlots.add(insertIndex, newSlot);
                    } else {
                        timeSlots.add(newSlot);
                    }

                    DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
                    buildRoutineGrid();
                }
        );
    }

    /**
     * Prompts to edit existing slot duration with conflict validation.
     */
    private void promptEditSlotDuration(int index, String oldSlot) {
        TimeSlotDialog.show(
                routineGrid.getScene().getWindow(),
                "Edit Time Slot Duration",
                "Update duration for \"" + oldSlot + "\":",
                oldSlot,
                timeSlots,
                oldSlot, // ignore self when checking conflicts
                newSlot -> {
                    DatabaseHelper.renameTimeSlot(currentUser.getId(), oldSlot, newSlot);
                    timeSlots.set(index, newSlot);
                    buildRoutineGrid();
                }
        );
    }

    /**
     * Creates context menu for weekdays allowing addition above or below, renaming, or deletion.
     */
    private ContextMenu createWeekdayContextMenu(int index, String currentDay) {
        ContextMenu menu = new ContextMenu();

        MenuItem addAboveItem = new MenuItem("⬆ Add Day Above");
        addAboveItem.setOnAction(e -> promptAddWeekdayAt(index, "Above"));

        MenuItem addBelowItem = new MenuItem("⬇ Add Day Below");
        addBelowItem.setOnAction(e -> promptAddWeekdayAt(index + 1, "Below"));

        MenuItem renameItem = new MenuItem("✏ Rename Day");
        renameItem.setOnAction(e -> handleRenameWeekday(index, currentDay));

        MenuItem deleteItem = new MenuItem("🗑 Delete Day");
        deleteItem.setOnAction(e -> {
            weekdays.remove(index);
            DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
            buildRoutineGrid();
        });

        menu.getItems().addAll(addAboveItem, addBelowItem, new SeparatorMenuItem(), renameItem, deleteItem);
        return menu;
    }

    /**
     * Prompts the user to enter a new weekday name and inserts it at the specified index.
     */
    private void promptAddWeekdayAt(int insertIndex, String position) {
        if (weekdays.size() >= 7) {
            showAlert("Weekdays Limit", "You already have 7 weekdays in your routine.");
            return;
        }

        String candidate = null;
        for (String day : DEFAULT_WEEKDAY_NAMES) {
            if (!weekdays.contains(day)) {
                candidate = day;
                break;
            }
        }
        if (candidate == null) candidate = "Day " + (weekdays.size() + 1);

        TextInputDialog dialog = new TextInputDialog(candidate);
        dialog.setTitle("Add Weekday (" + position + ")");
        dialog.setHeaderText("Enter weekday name (e.g. Sunday, Monday, Theory Day):");
        dialog.setContentText("Weekday:");

        Optional<String> res = dialog.showAndWait();
        res.ifPresent(inputDay -> {
            String clean = inputDay.trim();
            if (clean.isEmpty()) {
                showError("Invalid Weekday", "Weekday name cannot be empty.");
                return;
            }
            if (weekdays.contains(clean)) {
                showError("Duplicate Weekday", "A weekday named \"" + clean + "\" already exists in your routine.");
                return;
            }

            if (insertIndex >= 0 && insertIndex <= weekdays.size()) {
                weekdays.add(insertIndex, clean);
            } else {
                weekdays.add(clean);
            }

            DatabaseHelper.saveUserRoutineConfig(currentUser.getId(), weekdays, timeSlots);
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
     * Adds a new weekday to the routine.
     */
    @FXML
    public void handleAddWeekday() {
        if (currentUser == null) return;
        promptAddWeekdayAt(weekdays.size(), "End");
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

    /**
     * Toggles the Left Sidebar with ultra-smooth animation.
     */
    @FXML
    public void handleToggleLeft() {
        isLeftSidebarOpen = !isLeftSidebarOpen;
        animateSidebar(leftSidebar, isLeftSidebarOpen, SIDEBAR_WIDTH);
        if (leftToggleBtn != null) {
            leftToggleBtn.setText(isLeftSidebarOpen ? "✕ Sidebar" : "☰ Sidebar");
        }
    }

    /**
     * Toggles the Right Sidebar with ultra-smooth animation.
     */
    @FXML
    public void handleToggleRight() {
        isRightSidebarOpen = !isRightSidebarOpen;
        animateSidebar(rightSidebar, isRightSidebarOpen, SIDEBAR_WIDTH);
        if (rightToggleBtn != null) {
            rightToggleBtn.setText(isRightSidebarOpen ? "Sidebar ✕" : "Sidebar ▤");
        }
    }

    /**
     * Performs an ultra-smooth cubic ease-in-out transition on sidebar width
     * with dynamic geometric clipping to eliminate any text reflow or jitter.
     */
    private void animateSidebar(VBox sidebar, boolean expand, double targetWidth) {
        if (sidebar == null) return;

        sidebar.setVisible(true);
        sidebar.setManaged(true);

        Rectangle clip = new Rectangle();
        double currentHeight = sidebar.getHeight() > 0 ? sidebar.getHeight() : 800;
        clip.setHeight(currentHeight);
        sidebar.setClip(clip);

        double startWidth = sidebar.getWidth();
        if (expand && startWidth <= 0) startWidth = 0.0;
        double endWidth = expand ? targetWidth : 0.0;

        double finalStartWidth = startWidth;
        Transition transition = new Transition() {
            {
                setCycleDuration(Duration.millis(260));
                setInterpolator(Interpolator.EASE_BOTH);
            }

            @Override
            protected void interpolate(double frac) {
                double current = finalStartWidth + (endWidth - finalStartWidth) * frac;
                sidebar.setPrefWidth(current);
                sidebar.setMinWidth(current);
                sidebar.setMaxWidth(current);
                clip.setWidth(current);
                clip.setHeight(sidebar.getHeight() > 0 ? sidebar.getHeight() : currentHeight);
            }
        };

        transition.setOnFinished(e -> {
            if (!expand) {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
            }
            sidebar.setClip(null); // restore clean rendering after animation
        });

        transition.play();
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
