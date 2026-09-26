package com.example.study_buddy;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import java.util.concurrent.CompletableFuture;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.imageio.ImageIO;

import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

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

    // Top Navigation Bar
    @FXML private HBox appTopBar;

    // Center Workspace & Navigation
    @FXML private VBox mainMenuView;
    @FXML private ScrollPane routineView;
    @FXML private VBox calendarView;
    @FXML private ComboBox<String> calendarMonthSelect;
    @FXML private ComboBox<Integer> calendarYearSelect;
    @FXML private GridPane calendarDayHeaders;
    @FXML private GridPane calendarGrid;
    @FXML private Button navHomeBtn;
    @FXML private Button navRoutineBtn;
    @FXML private Button navCalendarBtn;
    @FXML private Button navQuizBtn;
    @FXML private Button navProgressBtn;

    // AI Quiz Components (Built using BorderPane for Teacher Requirement #3)
    @FXML private BorderPane quizView;
    @FXML private Label quizHeaderTitle;
    @FXML private Label quizHeaderSubtitle;
    @FXML private Button quizApiKeyBtn;
    @FXML private Button quizResetBtn;
    @FXML private StackPane quizCenterStack;
    @FXML private ScrollPane quizSetupScroll;
    @FXML private ScrollPane quizActiveScroll;
    @FXML private VBox quizQuestionsContainer;
    @FXML private VBox quizLoadingOverlay;
    @FXML private ProgressIndicator quizProgressIndicator;
    @FXML private Label quizLoadingLabel;
    @FXML private Label quizLoadingSubLabel;
    @FXML private HBox sourceFileChip;
    @FXML private Label sourceFileLabel;
    @FXML private ComboBox<String> quizNotebookPageSelect;
    @FXML private ComboBox<Integer> quizNumQuestionsCombo;
    @FXML private ComboBox<String> quizDifficultyCombo;
    @FXML private ComboBox<String> quizTypeCombo;
    @FXML private TextArea quizCustomPromptArea;
    @FXML private Button generateQuizBtn;
    @FXML private HBox quizBottomBar;
    @FXML private Label quizProgressLabel;
    @FXML private Button quizSubmitBtn;

    // Academic Progress Components
    @FXML private VBox progressView;
    @FXML private VBox progressCoursesGalleryPane;
    @FXML private FlowPane coursesGrid;
    @FXML private Button addCourseBtn;
    @FXML private VBox progressDetailPane;
    @FXML private Label progressCourseTitleHeader;
    @FXML private Label progressCourseSubtitle;
    @FXML private Button uploadSyllabusBtn;
    @FXML private Button setTermExamBtn;
    @FXML private Button addMarksBtn;
    @FXML private Label syllabusProgressSummaryLabel;
    @FXML private ProgressBar syllabusProgressBar;
    @FXML private VBox syllabusChaptersContainer;
    @FXML private Label marksAverageLabel;
    @FXML private VBox marksListContainer;
    @FXML private PieChart syllabusPieChart;
    @FXML private Label examCountdownBadge;
    @FXML private Label examForecastText;
    @FXML private Button examForecastToggleBtn;

    private Course currentSelectedCourse = null;
    private String fullExamForecastText = "";
    private boolean isExamForecastExpanded = false;

    private QuizSession currentQuizSession = null;
    private File uploadedQuizFile = null;
    private String uploadedQuizFileContent = "";
    private final GeminiApiService geminiApiService = new GeminiApiService();
    private final List<Page> availableQuizPages = new ArrayList<>();

    // Notebooks (Main Menu)
    @FXML private Button myNotebooksBtn;
    @FXML private Button newNotebookBtn;
    @FXML private VBox notebooksContentArea;
    @FXML private FlowPane notebooksGrid;

    // Notebook Workspace View
    @FXML private VBox notebookWorkspaceView;
    @FXML private Button toggleTopicsBtn;
    @FXML private Label notebookBreadcrumbLabel;
    @FXML private Label workspaceStatusLabel;
    @FXML private VBox topicsSidebar;
    @FXML private VBox topicsListContainer;
    @FXML private VBox pagePlaygroundContainer;

    private boolean isTopicsSidebarOpen = true;

    private Notebook currentNotebook;
    private Topic currentTopic;
    private Page currentPage;
    private List<PageBlock> currentPageBlocks = new ArrayList<>();
    private VBox blocksContainer;

    // Sidebar components
    @FXML private Button leftToggleBtn;
    @FXML private Button rightToggleBtn;
    @FXML private VBox leftSidebar;
    @FXML private VBox rightSidebar;
    @FXML private Label taskCountBadge;
    @FXML private VBox tasksContainer;

    private boolean isLeftSidebarOpen = false;
    private boolean isRightSidebarOpen = false;
    private static final double SIDEBAR_WIDTH = 290.0;

    private YearMonth currentCalendarMonth = YearMonth.now();
    private boolean isUpdatingCalendarSelectors = false;

    private List<RoutineTaskItem> activeTasks = new ArrayList<>();
    private static class TaskCardNodes {
        final HBox card;
        final Region stripe;
        final Label subjectLabel;
        final Label countdownLabel;
        TaskCardNodes(HBox card, Region stripe, Label subjectLabel, Label countdownLabel) {
            this.card = card;
            this.stripe = stripe;
            this.subjectLabel = subjectLabel;
            this.countdownLabel = countdownLabel;
        }
    }
    private final Map<RoutineTaskItem, TaskCardNodes> taskCardMap = new HashMap<>();
    private Timeline taskCountdownTimeline;

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
            leftToggleBtn.setText("☰");
            leftToggleBtn.setTooltip(new Tooltip("Toggle Sidebar"));
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setText("📋");
            rightToggleBtn.setTooltip(new Tooltip("Toggle Tasks"));
        }
        setupCalendarDayHeaders();
        setupCalendarSelectors();
        if (notebookWorkspaceView != null) {
            notebookWorkspaceView.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.isControlDown() && event.getCode() == KeyCode.V) {
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    if (clipboard.hasImage() && currentPage != null) {
                        event.consume();
                        handlePasteClipboardImage();
                    }
                }
            });
        }
        setupQuizViewDefaults();
        handleOpenMainMenu();
    }

    /**
     * Switches center workspace to the blank Main Menu.
     */
    @FXML
    public void handleOpenMainMenu() {
        if (appTopBar != null) {
            appTopBar.setVisible(true);
            appTopBar.setManaged(true);
        }
        if (mainMenuView != null) {
            mainMenuView.setVisible(true);
            mainMenuView.setManaged(true);
        }
        if (routineView != null) {
            routineView.setVisible(false);
            routineView.setManaged(false);
        }
        if (calendarView != null) {
            calendarView.setVisible(false);
            calendarView.setManaged(false);
        }
        if (notebookWorkspaceView != null) {
            notebookWorkspaceView.setVisible(false);
            notebookWorkspaceView.setManaged(false);
        }
        if (quizView != null) {
            quizView.setVisible(false);
            quizView.setManaged(false);
        }
        if (progressView != null) {
            progressView.setVisible(false);
            progressView.setManaged(false);
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setVisible(true);
            rightToggleBtn.setManaged(true);
        }
        updateNavActiveState(navHomeBtn);
    }

    /**
     * Switches center workspace to the Weekly Class Routine.
     */
    @FXML
    public void handleOpenRoutine() {
        if (appTopBar != null) {
            appTopBar.setVisible(true);
            appTopBar.setManaged(true);
        }
        if (mainMenuView != null) {
            mainMenuView.setVisible(false);
            mainMenuView.setManaged(false);
        }
        if (routineView != null) {
            routineView.setVisible(true);
            routineView.setManaged(true);
        }
        if (calendarView != null) {
            calendarView.setVisible(false);
            calendarView.setManaged(false);
        }
        if (notebookWorkspaceView != null) {
            notebookWorkspaceView.setVisible(false);
            notebookWorkspaceView.setManaged(false);
        }
        if (quizView != null) {
            quizView.setVisible(false);
            quizView.setManaged(false);
        }
        if (progressView != null) {
            progressView.setVisible(false);
            progressView.setManaged(false);
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setVisible(false);
            rightToggleBtn.setManaged(false);
        }
        if (isRightSidebarOpen) {
            handleToggleRight();
        }
        updateNavActiveState(navRoutineBtn);
    }

    /**
     * Switches center workspace to the Google Calendar-style interactive Calendar.
     */
    @FXML
    public void handleOpenCalendar() {
        if (appTopBar != null) {
            appTopBar.setVisible(true);
            appTopBar.setManaged(true);
        }
        if (mainMenuView != null) {
            mainMenuView.setVisible(false);
            mainMenuView.setManaged(false);
        }
        if (routineView != null) {
            routineView.setVisible(false);
            routineView.setManaged(false);
        }
        if (notebookWorkspaceView != null) {
            notebookWorkspaceView.setVisible(false);
            notebookWorkspaceView.setManaged(false);
        }
        if (quizView != null) {
            quizView.setVisible(false);
            quizView.setManaged(false);
        }
        if (progressView != null) {
            progressView.setVisible(false);
            progressView.setManaged(false);
        }
        if (calendarView != null) {
            calendarView.setVisible(true);
            calendarView.setManaged(true);
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setVisible(false);
            rightToggleBtn.setManaged(false);
        }
        if (isRightSidebarOpen) {
            handleToggleRight();
        }
        updateNavActiveState(navCalendarBtn);
        renderCalendar();
    }

    private void updateNavActiveState(Button activeButton) {
        if (navHomeBtn != null) {
            navHomeBtn.getStyleClass().remove("nav-item-active");
        }
        if (navRoutineBtn != null) {
            navRoutineBtn.getStyleClass().remove("nav-item-active");
        }
        if (navCalendarBtn != null) {
            navCalendarBtn.getStyleClass().remove("nav-item-active");
        }
        if (navQuizBtn != null) {
            navQuizBtn.getStyleClass().remove("nav-item-active");
        }
        if (navProgressBtn != null) {
            navProgressBtn.getStyleClass().remove("nav-item-active");
        }
        if (activeButton != null && !activeButton.getStyleClass().contains("nav-item-active")) {
            activeButton.getStyleClass().add("nav-item-active");
        }
    }

    /**
     * Action handler for "My Notebooks".
     */
    @FXML
    public void handleMyNotebooks() {
        loadNotebooks();
    }

    /**
     * Initializes default options and items for the AI Quiz configuration panels.
     */
    private void setupQuizViewDefaults() {
        if (quizNumQuestionsCombo != null) {
            quizNumQuestionsCombo.getItems().setAll(3, 5, 8, 10, 15);
            quizNumQuestionsCombo.setValue(5);
        }
        if (quizDifficultyCombo != null) {
            quizDifficultyCombo.getItems().setAll("Easy", "Medium", "Hard");
            quizDifficultyCombo.setValue("Medium");
        }
        if (quizTypeCombo != null) {
            quizTypeCombo.getItems().setAll("Both (MCQ + Short Answer)", "MCQ Only", "Short Answer Only");
            quizTypeCombo.setValue("Both (MCQ + Short Answer)");
        }
    }

    /**
     * Switches center workspace to the AI Quiz System (BorderPane layout).
     */
    @FXML
    public void handleOpenQuiz() {
        if (appTopBar != null) {
            appTopBar.setVisible(true);
            appTopBar.setManaged(true);
        }
        if (mainMenuView != null) {
            mainMenuView.setVisible(false);
            mainMenuView.setManaged(false);
        }
        if (routineView != null) {
            routineView.setVisible(false);
            routineView.setManaged(false);
        }
        if (calendarView != null) {
            calendarView.setVisible(false);
            calendarView.setManaged(false);
        }
        if (notebookWorkspaceView != null) {
            notebookWorkspaceView.setVisible(false);
            notebookWorkspaceView.setManaged(false);
        }
        if (quizView != null) {
            quizView.setVisible(true);
            quizView.setManaged(true);
        }
        if (progressView != null) {
            progressView.setVisible(false);
            progressView.setManaged(false);
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setVisible(false);
            rightToggleBtn.setManaged(false);
        }
        if (isRightSidebarOpen) {
            handleToggleRight();
        }
        updateNavActiveState(navQuizBtn);
        populateQuizNotebookPages();
    }

    /**
     * Returns from Quiz view to Main Menu dashboard.
     */
    @FXML
    public void handleBackFromQuiz() {
        handleOpenMainMenu();
    }

    // ==========================================
    // Academic Progress Tracker Workflows
    // ==========================================

    @FXML
    public void handleOpenProgress() {
        if (appTopBar != null) {
            appTopBar.setVisible(true);
            appTopBar.setManaged(true);
        }
        if (mainMenuView != null) {
            mainMenuView.setVisible(false);
            mainMenuView.setManaged(false);
        }
        if (routineView != null) {
            routineView.setVisible(false);
            routineView.setManaged(false);
        }
        if (calendarView != null) {
            calendarView.setVisible(false);
            calendarView.setManaged(false);
        }
        if (notebookWorkspaceView != null) {
            notebookWorkspaceView.setVisible(false);
            notebookWorkspaceView.setManaged(false);
        }
        if (quizView != null) {
            quizView.setVisible(false);
            quizView.setManaged(false);
        }
        if (progressView != null) {
            progressView.setVisible(true);
            progressView.setManaged(true);
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setVisible(false);
            rightToggleBtn.setManaged(false);
        }
        if (isRightSidebarOpen) {
            handleToggleRight();
        }
        updateNavActiveState(navProgressBtn);
        handleBackToCoursesGallery();
    }

    @FXML
    public void handleBackToCoursesGallery() {
        if (progressCoursesGalleryPane != null) {
            progressCoursesGalleryPane.setVisible(true);
            progressCoursesGalleryPane.setManaged(true);
        }
        if (progressDetailPane != null) {
            progressDetailPane.setVisible(false);
            progressDetailPane.setManaged(false);
        }
        currentSelectedCourse = null;
        loadAndRenderCourses();
    }

    private void loadAndRenderCourses() {
        if (coursesGrid == null) return;
        coursesGrid.getChildren().clear();
        int userId = (currentUser != null) ? currentUser.getId() : 1;
        List<Course> courses = DatabaseHelper.getCourses(userId);

        for (Course c : courses) {
            VBox card = new VBox(8);
            card.getStyleClass().add("course-card");
            card.setAlignment(Pos.TOP_LEFT);

            HBox topRow = new HBox(8);
            topRow.setAlignment(Pos.CENTER_LEFT);
            Label codeLabel = new Label(c.getCourseCode());
            codeLabel.getStyleClass().add("course-code-badge");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            topRow.getChildren().addAll(codeLabel, spacer);

            Label titleLabel = new Label(c.getCourseTitle());
            titleLabel.getStyleClass().add("course-title-text");
            titleLabel.setWrapText(true);
            VBox.setVgrow(titleLabel, Priority.ALWAYS);

            card.getChildren().addAll(topRow, titleLabel);

            card.setOnMouseClicked(e -> openCourseProgressDetail(c));

            // Right-click context menu to delete course
            ContextMenu cm = new ContextMenu();
            MenuItem delItem = new MenuItem("🗑️ Delete Course");
            delItem.setOnAction(ev -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Delete course '" + c.getCourseCode() + "' and its syllabus data?",
                        ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        DatabaseHelper.deleteCourse(c.getId());
                        loadAndRenderCourses();
                    }
                });
            });
            cm.getItems().add(delItem);
            card.setOnContextMenuRequested(ev -> cm.show(card, ev.getScreenX(), ev.getScreenY()));

            coursesGrid.getChildren().add(card);
        }

        if (courses.isEmpty()) {
            VBox emptyPrompt = new VBox(12);
            emptyPrompt.setAlignment(Pos.CENTER);
            emptyPrompt.setStyle("-fx-padding: 40px; -fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");
            Label emptyLbl = new Label("No courses added yet");
            emptyLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #64748b;");
            Label emptySub = new Label("Click '+ Add Course' above to add your first academic course.");
            emptySub.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
            emptyPrompt.getChildren().addAll(emptyLbl, emptySub);
            coursesGrid.getChildren().add(emptyPrompt);
        }
    }

    private void openCourseProgressDetail(Course course) {
        if (course == null) return;
        this.currentSelectedCourse = course;
        if (progressCoursesGalleryPane != null) {
            progressCoursesGalleryPane.setVisible(false);
            progressCoursesGalleryPane.setManaged(false);
        }
        if (progressDetailPane != null) {
            progressDetailPane.setVisible(true);
            progressDetailPane.setManaged(true);
        }
        if (progressCourseTitleHeader != null) {
            progressCourseTitleHeader.setText(course.getCourseCode() + " - " + course.getCourseTitle());
        }
        loadCourseSyllabusAndStats(course.getId());
        loadCourseMarks(course.getId());
        loadTermExamAndForecast(course);
    }

    private void loadCourseSyllabusAndStats(int courseId) {
        if (syllabusChaptersContainer == null) return;
        syllabusChaptersContainer.getChildren().clear();

        List<SyllabusChapter> chapters = DatabaseHelper.getSyllabusChapters(courseId);
        int totalTopics = 0;
        int completedTopics = 0;

        for (SyllabusChapter ch : chapters) {
            VBox chapterCard = new VBox(8);
            chapterCard.getStyleClass().add("chapter-card");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            Label chapNum = new Label("Chapter " + ch.getChapterNumber() + ": " + ch.getTitle());
            chapNum.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label countBadge = new Label(ch.getCompletedTopicsCount() + "/" + ch.getTotalTopicsCount());
            countBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #4f46e5; -fx-background-color: #e0e7ff; -fx-padding: 2px 7px; -fx-background-radius: 6px;");
            header.getChildren().addAll(chapNum, sp, countBadge);

            VBox topicsList = new VBox(4);
            for (SyllabusTopic topic : ch.getTopics()) {
                totalTopics++;
                if (topic.isCompleted()) completedTopics++;

                HBox topicRow = new HBox(10);
                topicRow.setAlignment(Pos.CENTER_LEFT);
                topicRow.getStyleClass().add("topic-row");

                CheckBox cb = new CheckBox();
                cb.setSelected(topic.isCompleted());
                Label topicLbl = new Label(topic.getTitle());
                topicLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (topic.isCompleted() ? "#64748b; -fx-font-style: italic;" : "#1e293b;"));

                cb.setOnAction(e -> {
                    boolean isChecked = cb.isSelected();
                    DatabaseHelper.setTopicCompleted(topic.getId(), isChecked);
                    topic.setCompleted(isChecked);
                    loadCourseSyllabusAndStats(courseId);
                    if (currentSelectedCourse != null) {
                        loadTermExamAndForecast(currentSelectedCourse);
                    }
                });

                topicRow.getChildren().addAll(cb, topicLbl);
                topicsList.getChildren().add(topicRow);
            }

            chapterCard.getChildren().addAll(header, topicsList);
            syllabusChaptersContainer.getChildren().add(chapterCard);
        }

        double pct = (totalTopics > 0) ? ((double) completedTopics / totalTopics) : 0.0;
        if (syllabusProgressBar != null) {
            syllabusProgressBar.setProgress(pct);
        }
        if (syllabusProgressSummaryLabel != null) {
            syllabusProgressSummaryLabel.setText(completedTopics + "/" + totalTopics + " Topics (" + (int)(pct * 100) + "%)");
        }

        // Update PieChart
        if (syllabusPieChart != null) {
            syllabusPieChart.getData().clear();
            if (totalTopics > 0) {
                PieChart.Data completedData = new PieChart.Data("Done (" + completedTopics + ")", completedTopics);
                PieChart.Data remainingData = new PieChart.Data("Remaining (" + (totalTopics - completedTopics) + ")", (totalTopics - completedTopics));
                syllabusPieChart.getData().addAll(completedData, remainingData);
            }
        }
    }

    private void loadCourseMarks(int courseId) {
        if (marksListContainer == null) return;
        marksListContainer.getChildren().clear();

        List<AcademicMark> marks = DatabaseHelper.getAcademicMarks(courseId);
        double totalPct = 0;
        int count = 0;

        for (AcademicMark m : marks) {
            HBox chip = new HBox(8);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.getStyleClass().add("mark-chip");

            Label typeBadge = new Label(m.getAssessmentType());
            typeBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-padding: 2px 6px; -fx-background-radius: 4px;");

            Label nameLbl = new Label(m.getAssessmentName());
            nameLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label scoreLbl = new Label(String.format("%.1f/%.1f (%.0f%%)", m.getObtainedMarks(), m.getTotalMarks(), m.getPercentage()));
            scoreLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #059669;");

            Button delBtn = new Button("✕");
            delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-cursor: hand; -fx-padding: 0 4px;");
            delBtn.setOnAction(e -> {
                DatabaseHelper.deleteAcademicMark(m.getId());
                loadCourseMarks(courseId);
            });

            chip.getChildren().addAll(typeBadge, nameLbl, sp, scoreLbl, delBtn);
            marksListContainer.getChildren().add(chip);

            totalPct += m.getPercentage();
            count++;
        }

        if (marksAverageLabel != null) {
            if (count > 0) {
                marksAverageLabel.setText(String.format("Avg: %.1f%%", (totalPct / count)));
            } else {
                marksAverageLabel.setText("Avg: --%");
            }
        }

        if (marks.isEmpty()) {
            Label noMarks = new Label("No marks added. Click '➕ Add Marks' above.");
            noMarks.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            marksListContainer.getChildren().add(noMarks);
        }
    }

    private void loadTermExamAndForecast(Course course) {
        if (course == null) return;
        String examDateStr = DatabaseHelper.getTermExamDate(course.getId());
        if (examDateStr == null || examDateStr.trim().isEmpty()) {
            if (examCountdownBadge != null) examCountdownBadge.setText("No Exam Set");
            setExamForecastContent("Set a Term Exam date above to generate your personalized AI study pace forecast.");
            return;
        }

        try {
            LocalDate examDate = LocalDate.parse(examDateStr);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), examDate);
            if (examCountdownBadge != null) {
                if (days < 0) {
                    examCountdownBadge.setText("Exam Passed");
                } else if (days == 0) {
                    examCountdownBadge.setText("Exam Today!");
                } else {
                    examCountdownBadge.setText(days + " Days Left");
                }
            }

            List<SyllabusChapter> chapters = DatabaseHelper.getSyllabusChapters(course.getId());
            int total = 0;
            int done = 0;
            List<String> remaining = new ArrayList<>();
            for (SyllabusChapter ch : chapters) {
                for (SyllabusTopic t : ch.getTopics()) {
                    total++;
                    if (t.isCompleted()) done++;
                    else remaining.add(t.getTitle());
                }
            }

            if (total == 0) {
                setExamForecastContent("Upload your syllabus above so AI can calculate your preparation forecast.");
                return;
            }

            if (days > 0 && geminiApiService != null) {
                setExamForecastContent("🤖 Calculating AI study forecast...");
                final int fTotal = total;
                final int fDone = done;
                final long fDays = days;
                geminiApiService.generateExamForecastAsync(course.getCourseTitle(), fTotal, fDone, remaining, fDays)
                        .thenAccept(advice -> Platform.runLater(() -> {
                            setExamForecastContent(advice);
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                setExamForecastContent("Study ~" + String.format("%.1f", (double)(fTotal - fDone) / Math.max(1, fDays / 7.0)) + " topics per week to finish before your exam.");
                            });
                            return null;
                        });
            }
        } catch (Exception e) {
            if (examCountdownBadge != null) examCountdownBadge.setText(examDateStr);
        }
    }

    private void setExamForecastContent(String text) {
        this.fullExamForecastText = (text != null) ? text : "";
        this.isExamForecastExpanded = false;
        renderExamForecast();
    }

    private void renderExamForecast() {
        if (examForecastText == null) return;
        if (fullExamForecastText == null || fullExamForecastText.length() <= 110) {
            examForecastText.setText(fullExamForecastText != null ? fullExamForecastText : "");
            if (examForecastToggleBtn != null) {
                examForecastToggleBtn.setVisible(false);
                examForecastToggleBtn.setManaged(false);
            }
        } else {
            if (examForecastToggleBtn != null) {
                examForecastToggleBtn.setVisible(true);
                examForecastToggleBtn.setManaged(true);
            }
            if (isExamForecastExpanded) {
                examForecastText.setText(fullExamForecastText);
                if (examForecastToggleBtn != null) examForecastToggleBtn.setText("See Less ▴");
            } else {
                examForecastText.setText(fullExamForecastText.substring(0, 100) + "...");
                if (examForecastToggleBtn != null) examForecastToggleBtn.setText("See More ▾");
            }
        }
    }

    @FXML
    public void handleToggleExamForecastSeeMore() {
        isExamForecastExpanded = !isExamForecastExpanded;
        renderExamForecast();
    }

    @FXML
    public void handleAddNewCourse() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add Academic Course");
        dialog.setHeaderText("Enter Course Code and Course Title");

        ButtonType addBtnType = new ButtonType("Add Course", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 40, 10, 10));

        TextField codeField = new TextField();
        codeField.setPromptText("e.g. CSE 2100");
        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Object-Oriented Programming");

        grid.add(new Label("Course Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Course Title:"), 0, 1);
        grid.add(titleField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(codeField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addBtnType) {
                return new Pair<>(codeField.getText().trim(), titleField.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(pair -> {
            if (!pair.getKey().isEmpty() && !pair.getValue().isEmpty()) {
                int uid = (currentUser != null) ? currentUser.getId() : 1;
                DatabaseHelper.createCourse(uid, pair.getKey(), pair.getValue());
                loadAndRenderCourses();
            }
        });
    }

    @FXML
    public void handleUploadSyllabus() {
        if (currentSelectedCourse == null) return;
        ContextMenu menu = new ContextMenu();
        MenuItem addItem = new MenuItem("➕ Add Syllabus");
        MenuItem deleteItem = new MenuItem("🗑️ Delete Syllabus");

        addItem.setOnAction(e -> handleAddSyllabusFile());
        deleteItem.setOnAction(e -> handleDeleteSyllabus());

        menu.getItems().addAll(addItem, deleteItem);
        menu.show(uploadSyllabusBtn, Side.BOTTOM, 0, 0);
    }

    private void handleAddSyllabusFile() {
        if (currentSelectedCourse == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Course Syllabus (PDF, Doc, Image, PPTX, Text)");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Formats (*.pdf, *.png, *.jpg, *.jpeg, *.docx, *.pptx, *.txt, *.md)",
                        "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.docx", "*.pptx", "*.txt", "*.md"),
                new FileChooser.ExtensionFilter("Pictures (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("Word Documents (*.docx)", "*.docx"),
                new FileChooser.ExtensionFilter("PowerPoint Presentations (*.pptx)", "*.pptx"),
                new FileChooser.ExtensionFilter("Text Files (*.txt, *.md)", "*.txt", "*.md")
        );
        Stage stage = (progressView != null && progressView.getScene() != null)
                ? (Stage) progressView.getScene().getWindow() : null;
        File file = fc.showOpenDialog(stage);
        if (file == null) return;

        if (syllabusProgressSummaryLabel != null) {
            syllabusProgressSummaryLabel.setText("Extracting & parsing syllabus with AI...");
        }

        CompletableFuture<List<SyllabusChapter>> future;
        if (QuizSourceHelper.isImageFile(file)) {
            future = geminiApiService.parseSyllabusHierarchyFromImageAsync(file);
        } else {
            future = CompletableFuture.supplyAsync(() -> {
                try {
                    return QuizSourceHelper.readFileContent(file);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
                }
            }).thenCompose(text -> geminiApiService.parseSyllabusHierarchyAsync(text));
        }

        future.thenAccept(chapters -> Platform.runLater(() -> {
            DatabaseHelper.saveSyllabusChapters(currentSelectedCourse.getId(), chapters);
            loadCourseSyllabusAndStats(currentSelectedCourse.getId());
            loadTermExamAndForecast(currentSelectedCourse);
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Successfully extracted " + chapters.size() + " chapters and topics for " + currentSelectedCourse.getCourseCode() + "!");
            alert.showAndWait();
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                loadCourseSyllabusAndStats(currentSelectedCourse.getId());
                Alert alert = new Alert(Alert.AlertType.ERROR, "Syllabus Extraction Error: " + ex.getMessage());
                alert.showAndWait();
            });
            return null;
        });
    }

    private void handleDeleteSyllabus() {
        if (currentSelectedCourse == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Syllabus");
        confirm.setHeaderText("Delete syllabus for " + currentSelectedCourse.getCourseCode() + "?");
        confirm.setContentText("This will remove all chapters and topics from your checklist. This action cannot be undone.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                DatabaseHelper.saveSyllabusChapters(currentSelectedCourse.getId(), Collections.emptyList());
                loadCourseSyllabusAndStats(currentSelectedCourse.getId());
                loadTermExamAndForecast(currentSelectedCourse);
            }
        });
    }

    @FXML
    public void handleSetTermExamDate() {
        if (currentSelectedCourse == null) return;
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Set Term Final Exam Date");
        dialog.setHeaderText("Select target exam date for " + currentSelectedCourse.getCourseCode());

        ButtonType saveBtn = new ButtonType("Save Date", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        DatePicker dp = new DatePicker(LocalDate.now().plusDays(30));
        VBox box = new VBox(10, new Label("Exam Date:"), dp);
        box.setPadding(new Insets(15));
        dialog.getDialogPane().setContent(box);

        dialog.setResultConverter(btn -> (btn == saveBtn) ? dp.getValue() : null);
        dialog.showAndWait().ifPresent(date -> {
            DatabaseHelper.setTermExamDate(currentSelectedCourse.getId(), date.toString());
            loadTermExamAndForecast(currentSelectedCourse);
        });
    }

    @FXML
    public void handleAddMarksDialog() {
        if (currentSelectedCourse == null) return;
        Dialog<AcademicMark> dialog = new Dialog<>();
        dialog.setTitle("Add Marks");
        dialog.setHeaderText("Enter marks for " + currentSelectedCourse.getCourseCode());

        ButtonType saveBtn = new ButtonType("Save Mark", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("CT", "Assignment", "Spot test", "Lab test", "Lab task", "Lab quiz");
        typeCombo.setValue("CT");
        typeCombo.setEditable(true);

        TextField nameField = new TextField("CT 1");
        TextField obtainedField = new TextField("18.0");
        TextField totalField = new TextField("20.0");

        grid.add(new Label("Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Name:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Obtained Score:"), 0, 2);
        grid.add(obtainedField, 1, 2);
        grid.add(new Label("Total Marks:"), 0, 3);
        grid.add(totalField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                try {
                    double obt = Double.parseDouble(obtainedField.getText().trim());
                    double tot = Double.parseDouble(totalField.getText().trim());
                    String typeVal = typeCombo.getValue() != null ? typeCombo.getValue().trim() : "CT";
                    return new AcademicMark(0, currentSelectedCourse.getId(), typeVal, nameField.getText().trim(), obt, tot, LocalDate.now().toString(), null);
                } catch (Exception ignored) {}
            }
            return null;
        });

        dialog.showAndWait().ifPresent(mark -> {
            DatabaseHelper.addAcademicMark(mark.getCourseId(), mark.getAssessmentType(), mark.getAssessmentName(), mark.getObtainedMarks(), mark.getTotalMarks(), mark.getExamDate());
            loadCourseMarks(currentSelectedCourse.getId());
        });
    }

    /**
     * Opens the Gemini API Key configuration modal.
     */
    @FXML
    public void handleOpenApiKeyDialog() {
        Stage stage = (quizView != null && quizView.getScene() != null)
                ? (Stage) quizView.getScene().getWindow() : null;
        ApiKeyDialog.show(stage, () -> {
            // Callback when key is saved
        });
    }

    /**
     * Populates the notebook page dropdown with pages from user's notebooks.
     */
    private void populateQuizNotebookPages() {
        if (quizNotebookPageSelect == null || currentUser == null) return;
        quizNotebookPageSelect.getItems().clear();
        availableQuizPages.clear();

        List<Notebook> notebooks = DatabaseHelper.getUserNotebooks(currentUser.getId());
        for (Notebook nb : notebooks) {
            List<Topic> topics = DatabaseHelper.getTopicsByNotebook(nb.getId());
            for (Topic tp : topics) {
                List<Page> pages = DatabaseHelper.getPagesByTopic(tp.getId());
                for (Page pg : pages) {
                    availableQuizPages.add(pg);
                    quizNotebookPageSelect.getItems().add(nb.getTitle() + " → " + tp.getTitle() + " → " + pg.getTitle());
                }
            }
        }
    }

    /**
     * Opens file chooser to upload source notes/document.
     */
    @FXML
    public void handleUploadSourceFile() {
        Stage stage = (quizView != null && quizView.getScene() != null)
                ? (Stage) quizView.getScene().getWindow() : null;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Study Notes / Source File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text & Code Files (*.txt, *.md, *.java, *.py, *.json)", "*.txt", "*.md", "*.java", "*.py", "*.json", "*.c", "*.cpp", "*.html", "*.css"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );
        File chosen = fileChooser.showOpenDialog(stage);
        if (chosen != null) {
            try {
                String content = QuizSourceHelper.readFileContent(chosen);
                if (content.trim().isEmpty()) {
                    showError("Empty File", "The selected file contains no readable text.");
                    return;
                }
                this.uploadedQuizFile = chosen;
                this.uploadedQuizFileContent = content;
                if (sourceFileLabel != null) {
                    sourceFileLabel.setText(QuizSourceHelper.formatSourceSummary("📄 " + chosen.getName(), content));
                }
                if (sourceFileChip != null) {
                    sourceFileChip.setVisible(true);
                    sourceFileChip.setManaged(true);
                }
                if (quizNotebookPageSelect != null) {
                    quizNotebookPageSelect.getSelectionModel().clearSelection();
                }
            } catch (IOException e) {
                showError("File Read Error", "Could not read file: " + e.getMessage());
            }
        }
    }

    /**
     * Removes active source file.
     */
    @FXML
    public void handleRemoveSourceFile() {
        this.uploadedQuizFile = null;
        this.uploadedQuizFileContent = "";
        if (sourceFileChip != null) {
            sourceFileChip.setVisible(false);
            sourceFileChip.setManaged(false);
        }
    }

    /**
     * Call #1: Generates quiz questions using Gemini API asynchronously.
     */
    @FXML
    public void handleGenerateQuiz() {
        if (!ApiKeyManager.hasApiKey()) {
            showAlert("API Key Required", "Please configure your Google Gemini API Key first.");
            handleOpenApiKeyDialog();
            if (!ApiKeyManager.hasApiKey()) return;
        }

        // Determine source text
        String sourceText = "";
        if (uploadedQuizFile != null && !uploadedQuizFileContent.trim().isEmpty()) {
            sourceText = uploadedQuizFileContent;
        } else if (quizNotebookPageSelect != null && quizNotebookPageSelect.getSelectionModel().getSelectedIndex() >= 0) {
            int selIdx = quizNotebookPageSelect.getSelectionModel().getSelectedIndex();
            if (selIdx >= 0 && selIdx < availableQuizPages.size()) {
                Page pg = availableQuizPages.get(selIdx);
                sourceText = QuizSourceHelper.extractPageContent(pg);
            }
        }

        String prompt = (quizCustomPromptArea != null) ? quizCustomPromptArea.getText().trim() : "";

        if (sourceText.isEmpty() && prompt.isEmpty()) {
            showError("Missing Information",
                    "Please provide a topic or select/upload source material to generate the quiz.");
            return;
        }

        int numQuestions = (quizNumQuestionsCombo != null && quizNumQuestionsCombo.getValue() != null)
                ? quizNumQuestionsCombo.getValue() : 5;
        String difficulty = (quizDifficultyCombo != null && quizDifficultyCombo.getValue() != null)
                ? quizDifficultyCombo.getValue() : "Medium";
        String typeMode = (quizTypeCombo != null && quizTypeCombo.getValue() != null)
                ? quizTypeCombo.getValue() : "Both";

        if (quizLoadingLabel != null) {
            quizLoadingLabel.setText("AI is crafting your quiz...");
        }
        if (quizLoadingSubLabel != null) {
            quizLoadingSubLabel.setText("Formulating questions and choices via Google Gemini...");
        }
        if (quizLoadingOverlay != null) {
            quizLoadingOverlay.setVisible(true);
            quizLoadingOverlay.setManaged(true);
        }

        final String finalSourceText = sourceText;
        geminiApiService.generateQuizAsync(prompt, finalSourceText, numQuestions, difficulty, typeMode)
                .thenAccept(session -> Platform.runLater(() -> {
                    if (quizLoadingOverlay != null) {
                        quizLoadingOverlay.setVisible(false);
                        quizLoadingOverlay.setManaged(false);
                    }
                    this.currentQuizSession = session;
                    renderQuizQuestions(session);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (quizLoadingOverlay != null) {
                            quizLoadingOverlay.setVisible(false);
                            quizLoadingOverlay.setManaged(false);
                        }
                        String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                        showError("Quiz Generation Error", msg);
                    });
                    return null;
                });
    }

    /**
     * Renders the interactive question cards in quizQuestionsContainer.
     */
    private void renderQuizQuestions(QuizSession session) {
        if (quizQuestionsContainer == null || session == null) return;
        quizQuestionsContainer.getChildren().clear();

        List<QuizQuestion> questions = session.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            final int qIndex = i;
            final QuizQuestion q = questions.get(i);

            VBox card = new VBox(12);
            card.getStyleClass().add("quiz-question-card");

            // Header row: Question Number & Badge
            HBox cardHeader = new HBox(10);
            cardHeader.setAlignment(Pos.CENTER_LEFT);

            Label qNumLabel = new Label("Question " + (qIndex + 1) + " of " + questions.size());
            qNumLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label badge = new Label(q.getType() == QuizQuestion.QuestionType.MCQ ? "Multiple Choice (1 pt)" : "Short Answer (5 pts)");
            badge.getStyleClass().add("quiz-badge");

            cardHeader.getChildren().addAll(qNumLabel, spacer, badge);

            // Question prompt text with See More / See Less for long questions
            String fullQuestionText = q.getQuestionText();
            Label promptLabel = new Label();
            promptLabel.setWrapText(true);
            promptLabel.setMinHeight(Region.USE_PREF_SIZE);
            promptLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #334155;");
            promptLabel.prefWidthProperty().bind(card.widthProperty().subtract(40));

            card.getChildren().addAll(cardHeader, promptLabel);

            if (fullQuestionText.length() > 160) {
                int cutIdx = fullQuestionText.lastIndexOf(' ', 140);
                if (cutIdx <= 0) cutIdx = 140;
                String excerpt = fullQuestionText.substring(0, cutIdx).trim() + "...";
                promptLabel.setText(excerpt);

                Button seeMoreBtn = new Button("See More ▾");
                seeMoreBtn.getStyleClass().add("quiz-see-more-btn");
                boolean[] isExpanded = new boolean[]{false};
                seeMoreBtn.setOnAction(e -> {
                    if (!isExpanded[0]) {
                        promptLabel.setText(fullQuestionText);
                        seeMoreBtn.setText("See Less ▴");
                        isExpanded[0] = true;
                    } else {
                        promptLabel.setText(excerpt);
                        seeMoreBtn.setText("See More ▾");
                        isExpanded[0] = false;
                    }
                });
                card.getChildren().add(seeMoreBtn);
            } else {
                promptLabel.setText(fullQuestionText);
            }

            if (q.getType() == QuizQuestion.QuestionType.MCQ) {
                // 4 Interactive MCQ Cards
                GridPane optionsGrid = new GridPane();
                optionsGrid.setHgap(12);
                optionsGrid.setVgap(10);

                ColumnConstraints col1 = new ColumnConstraints();
                col1.setPercentWidth(50);
                ColumnConstraints col2 = new ColumnConstraints();
                col2.setPercentWidth(50);
                optionsGrid.getColumnConstraints().addAll(col1, col2);

                List<HBox> optionCardBoxes = new ArrayList<>();

                for (int optIdx = 0; optIdx < q.getOptions().size(); optIdx++) {
                    final int currentOptIdx = optIdx;
                    String optionText = q.getOptions().get(optIdx);

                    HBox optCard = new HBox(10);
                    optCard.setAlignment(Pos.CENTER_LEFT);
                    optCard.getStyleClass().add("quiz-mcq-card");

                    Label letter = new Label(QuizQuestion.getOptionLetter(currentOptIdx));
                    letter.getStyleClass().add("quiz-option-letter");

                    Label text = new Label(optionText);
                    text.setWrapText(true);
                    text.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");
                    HBox.setHgrow(text, Priority.ALWAYS);

                    optCard.getChildren().addAll(letter, text);

                    optCard.setOnMouseClicked(e -> {
                        if (session.isSubmitted()) return; // locked after submission
                        q.setUserSelectedOption(currentOptIdx);

                        // Update selection styles
                        for (int k = 0; k < optionCardBoxes.size(); k++) {
                            HBox box = optionCardBoxes.get(k);
                            Label l = (Label) box.getChildren().get(0);
                            if (k == currentOptIdx) {
                                box.getStyleClass().remove("quiz-mcq-card");
                                if (!box.getStyleClass().contains("quiz-mcq-card-selected")) {
                                    box.getStyleClass().add("quiz-mcq-card-selected");
                                }
                                l.getStyleClass().remove("quiz-option-letter");
                                if (!l.getStyleClass().contains("quiz-option-letter-selected")) {
                                    l.getStyleClass().add("quiz-option-letter-selected");
                                }
                            } else {
                                box.getStyleClass().remove("quiz-mcq-card-selected");
                                if (!box.getStyleClass().contains("quiz-mcq-card")) {
                                    box.getStyleClass().add("quiz-mcq-card");
                                }
                                l.getStyleClass().remove("quiz-option-letter-selected");
                                if (!l.getStyleClass().contains("quiz-option-letter")) {
                                    l.getStyleClass().add("quiz-option-letter");
                                }
                            }
                        }
                        updateQuizProgressStatus();
                    });

                    optionCardBoxes.add(optCard);
                    int row = optIdx / 2;
                    int col = optIdx % 2;
                    optionsGrid.add(optCard, col, row);
                }

                card.getChildren().add(optionsGrid);
            } else {
                // Short Answer Input Area
                VBox saBox = new VBox(6);
                Label ansLabel = new Label("Your Response:");
                ansLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

                TextArea saArea = new TextArea();
                saArea.setPromptText("Type your detailed response here...");
                saArea.setPrefRowCount(3);
                saArea.setWrapText(true);
                saArea.setStyle("-fx-background-color: #ffffff; -fx-border-color: #cbd5e1; -fx-border-radius: 6px; -fx-background-radius: 6px;");

                saArea.textProperty().addListener((obs, oldVal, newVal) -> {
                    q.setStudentAnswer(newVal);
                    updateQuizProgressStatus();
                });

                saBox.getChildren().addAll(ansLabel, saArea);
                card.getChildren().add(saBox);
            }

            quizQuestionsContainer.getChildren().add(card);
        }

        // Switch to Active screen
        if (quizSetupScroll != null) {
            quizSetupScroll.setVisible(false);
            quizSetupScroll.setManaged(false);
        }
        if (quizActiveScroll != null) {
            quizActiveScroll.setVisible(true);
            quizActiveScroll.setManaged(true);
            quizActiveScroll.setVvalue(0.0);
        }
        if (quizSubmitBtn != null) {
            quizSubmitBtn.setVisible(true);
            quizSubmitBtn.setManaged(true);
        }
        updateQuizProgressStatus();
    }

    /**
     * Updates the progress label in bottom bar showing answered questions.
     */
    private void updateQuizProgressStatus() {
        if (quizProgressLabel == null || currentQuizSession == null) return;
        int total = currentQuizSession.getTotalQuestions();
        int answered = currentQuizSession.getAnsweredCount();
        quizProgressLabel.setText(String.format("Answered %d of %d questions", answered, total));
    }

    /**
     * Call #2: Submits quiz, grades short answers with Gemini AI, and reveals results.
     */
    @FXML
    public void handleSubmitQuiz() {
        if (currentQuizSession == null) return;

        int total = currentQuizSession.getTotalQuestions();
        int answered = currentQuizSession.getAnsweredCount();
        int unanswered = total - answered;

        if (unanswered > 0) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Submit Quiz");
            confirm.setHeaderText("Incomplete Quiz");
            confirm.setContentText(String.format("You have %d unanswered question(s). Submit now anyway?", unanswered));
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        // If there are short answer questions, Call #2 is executed to grade them via Gemini
        if (currentQuizSession.hasShortAnswerQuestions()) {
            if (quizLoadingLabel != null) {
                quizLoadingLabel.setText("Evaluating your responses...");
            }
            if (quizLoadingSubLabel != null) {
                quizLoadingSubLabel.setText("Gemini AI is reviewing your short answers and preparing personalized feedback...");
            }
            if (quizLoadingOverlay != null) {
                quizLoadingOverlay.setVisible(true);
                quizLoadingOverlay.setManaged(true);
            }

            geminiApiService.gradeShortAnswersAsync(currentQuizSession)
                    .thenRun(() -> Platform.runLater(() -> {
                        if (quizLoadingOverlay != null) {
                            quizLoadingOverlay.setVisible(false);
                            quizLoadingOverlay.setManaged(false);
                        }
                        currentQuizSession.setSubmitted(true);
                        revealQuizResults(currentQuizSession);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            if (quizLoadingOverlay != null) {
                                quizLoadingOverlay.setVisible(false);
                                quizLoadingOverlay.setManaged(false);
                            }
                            String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                            showError("Grading Error", "Could not complete AI grading: " + msg);
                        });
                        return null;
                    });
        } else {
            // MCQ only: graded instantly and locally (Call #2 skipped!)
            currentQuizSession.setSubmitted(true);
            revealQuizResults(currentQuizSession);
        }
    }

    /**
     * Renders score summary banner, highlights right/wrong cards, and displays explanations & feedback.
     */
    private void revealQuizResults(QuizSession session) {
        if (quizQuestionsContainer == null || session == null) return;

        if (quizSubmitBtn != null) {
            quizSubmitBtn.setVisible(false);
            quizSubmitBtn.setManaged(false);
        }

        int totalEarned = session.getTotalScoreEarned();
        int totalMax = session.getTotalMaxScore();
        int pct = session.getPercentageScore();
        String grade = session.getGradeLetter();

        // Banner Card at top of questions container
        VBox banner = new VBox(10);
        banner.getStyleClass().add("quiz-result-banner");

        HBox bannerTop = new HBox(12);
        bannerTop.setAlignment(Pos.CENTER_LEFT);

        Label congratsIcon = new Label(pct >= 80 ? "🏆" : pct >= 60 ? "🎉" : "📚");
        congratsIcon.setStyle("-fx-font-size: 32px;");

        VBox bannerTexts = new VBox(3);
        Label bannerTitle = new Label("Quiz Results: " + pct + "% (Grade " + grade + ")");
        bannerTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        Label bannerSub = new Label(String.format("You scored %d out of %d total points.", totalEarned, totalMax));
        bannerSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #e0e7ff;");
        bannerTexts.getChildren().addAll(bannerTitle, bannerSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statsPills = new HBox(8);
        statsPills.setAlignment(Pos.CENTER_RIGHT);
        if (session.getMcqCount() > 0) {
            Label mcqPill = new Label("MCQ: " + session.getCorrectMcqCount() + "/" + session.getMcqCount());
            mcqPill.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 4px 10px;");
            statsPills.getChildren().add(mcqPill);
        }
        if (session.getShortAnswerCount() > 0) {
            Label saPill = new Label("Short Ans: " + session.getShortAnswerCount() + " evaluated");
            saPill.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-background-radius: 20px; -fx-padding: 4px 10px;");
            statsPills.getChildren().add(saPill);
        }

        bannerTop.getChildren().addAll(congratsIcon, bannerTexts, spacer, statsPills);
        banner.getChildren().add(bannerTop);

        quizQuestionsContainer.getChildren().add(0, banner);

        // Update each question card in review mode
        List<QuizQuestion> questions = session.getQuestions();
        // Since banner is at index 0, question cards start at index 1
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            VBox card = (VBox) quizQuestionsContainer.getChildren().get(i + 1);

            if (q.getType() == QuizQuestion.QuestionType.MCQ) {
                // Highlight options
                GridPane optionsGrid = null;
                for (Node child : card.getChildren()) {
                    if (child instanceof GridPane) {
                        optionsGrid = (GridPane) child;
                        break;
                    }
                }
                if (optionsGrid != null) {
                    for (Node n : optionsGrid.getChildren()) {
                        if (n instanceof HBox) {
                            HBox optBox = (HBox) n;
                            Integer optIdx = GridPane.getRowIndex(n) != null && GridPane.getColumnIndex(n) != null
                                    ? (GridPane.getRowIndex(n) * 2 + GridPane.getColumnIndex(n)) : null;
                            if (optIdx != null) {
                                optBox.getStyleClass().removeAll("quiz-mcq-card-selected", "quiz-mcq-card");
                                if (optIdx == q.getCorrectIndex()) {
                                    optBox.getStyleClass().add("quiz-mcq-card-correct");
                                    Label txt = (Label) optBox.getChildren().get(1);
                                    txt.setText(txt.getText() + "  ✓ (Correct Answer)");
                                    txt.setStyle("-fx-font-size: 13px; -fx-text-fill: #065f46; -fx-font-weight: bold;");
                                } else if (q.getUserSelectedOption() != null && optIdx.equals(q.getUserSelectedOption())) {
                                    optBox.getStyleClass().add("quiz-mcq-card-incorrect");
                                    Label txt = (Label) optBox.getChildren().get(1);
                                    txt.setText(txt.getText() + "  ✕ (Your Answer)");
                                    txt.setStyle("-fx-font-size: 13px; -fx-text-fill: #991b1b;");
                                }
                            }
                        }
                    }
                }

                // Add Explanation box
                if (q.getExplanation() != null && !q.getExplanation().trim().isEmpty()) {
                    VBox explBox = new VBox(4);
                    explBox.getStyleClass().add("quiz-explanation-box");
                    Label explTitle = new Label("💡 Explanation:");
                    explTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4338ca;");
                    Label explText = new Label(q.getExplanation());
                    explText.setWrapText(true);
                    explText.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
                    explBox.getChildren().addAll(explTitle, explText);
                    card.getChildren().add(explBox);
                }
            } else {
                // Short answer review
                VBox saBox = null;
                for (Node child : card.getChildren()) {
                    if (child instanceof VBox) {
                        saBox = (VBox) child;
                        break;
                    }
                }
                if (saBox != null) {
                    for (Node saChild : saBox.getChildren()) {
                        if (saChild instanceof TextArea) {
                            ((TextArea) saChild).setEditable(false);
                        }
                    }
                }

                // AI Feedback Box
                VBox fbBox = new VBox(6);
                fbBox.getStyleClass().add("quiz-feedback-box");

                HBox fbHeader = new HBox(8);
                fbHeader.setAlignment(Pos.CENTER_LEFT);
                Label fbIcon = new Label("🤖 AI Feedback:");
                fbIcon.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #065f46;");
                Region sp = new Region();
                HBox.setHgrow(sp, Priority.ALWAYS);
                Label scorePill = new Label("Score: " + q.getAwardedScore() + " / " + q.getMaxScore() + " pts");
                scorePill.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-font-weight: bold; -fx-background-radius: 10px; -fx-padding: 2px 8px; -fx-font-size: 11px;");
                fbHeader.getChildren().addAll(fbIcon, sp, scorePill);

                Label fbText = new Label(q.getAiFeedback().isEmpty() ? "No feedback returned." : q.getAiFeedback());
                fbText.setWrapText(true);
                fbText.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e293b;");

                Label rubricText = new Label("Expected Key Points: " + q.getRubric());
                rubricText.setWrapText(true);
                rubricText.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");

                fbBox.getChildren().addAll(fbHeader, fbText, rubricText);
                card.getChildren().add(fbBox);
            }
        }

        if (quizActiveScroll != null) {
            quizActiveScroll.setVvalue(0.0);
        }
        if (quizProgressLabel != null) {
            quizProgressLabel.setText(String.format("🎉 Quiz Completed! Final Score: %d / %d (%d%%)", totalEarned, totalMax, pct));
        }
    }

    /**
     * Resets the quiz view back to the setup configuration screen.
     */
    @FXML
    public void handleResetQuiz() {
        this.currentQuizSession = null;
        if (quizQuestionsContainer != null) {
            quizQuestionsContainer.getChildren().clear();
        }
        if (quizActiveScroll != null) {
            quizActiveScroll.setVisible(false);
            quizActiveScroll.setManaged(false);
        }
        if (quizSetupScroll != null) {
            quizSetupScroll.setVisible(true);
            quizSetupScroll.setManaged(true);
        }
        if (quizSubmitBtn != null) {
            quizSubmitBtn.setVisible(false);
            quizSubmitBtn.setManaged(false);
        }
        if (quizProgressLabel != null) {
            quizProgressLabel.setText("Ready to create quiz");
        }
    }

    /**
     * Action handler for "+ New Notebook".
     */
    @FXML
    public void handleNewNotebook() {
        if (currentUser == null) return;
        NotebookDialog.show(
                mainMenuView.getScene().getWindow(),
                currentUser.getId(),
                null,
                this::loadNotebooks
        );
    }

    /**
     * Loads and renders all notebooks belonging to the current user as modern interactive cards.
     */
    public void loadNotebooks() {
        if (notebooksGrid == null || currentUser == null) return;
        notebooksGrid.getChildren().clear();

        List<Notebook> notebooks = DatabaseHelper.getUserNotebooks(currentUser.getId());

        if (notebooks.isEmpty()) {
            VBox emptyPrompt = new VBox(12);
            emptyPrompt.setAlignment(Pos.CENTER);
            emptyPrompt.setPadding(new Insets(36, 40, 36, 40));
            emptyPrompt.setPrefWidth(550);
            emptyPrompt.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px; -fx-border-style: dashed;");

            Label emptyIcon = new Label("📚");
            emptyIcon.setStyle("-fx-font-size: 36px;");

            Label emptyTitle = new Label("No Notebooks Yet");
            emptyTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label emptySub = new Label("Create your first notebook to organize topics, lecture slides, and notes.");
            emptySub.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

            Button createBtn = new Button("+ Create First Notebook");
            createBtn.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px; -fx-padding: 8px 16px;");
            createBtn.setOnAction(e -> handleNewNotebook());

            emptyPrompt.getChildren().addAll(emptyIcon, emptyTitle, emptySub, createBtn);
            notebooksGrid.getChildren().add(emptyPrompt);
            return;
        }

        for (Notebook nb : notebooks) {
            Node card = createNotebookCard(nb);
            notebooksGrid.getChildren().add(card);
        }
    }

    /**
     * Constructs a modern interactive card for a single notebook.
     * The 3-dot options button is pinned to the top-right corner of the card.
     */
    private Node createNotebookCard(Notebook notebook) {
        StackPane card = new StackPane();
        card.setPrefSize(224, 128);
        card.setMinSize(224, 128);
        card.setMaxSize(224, 128);
        card.getStyleClass().add("notebook-card");

        String themeColor = (notebook.getColorHex() != null && !notebook.getColorHex().isEmpty())
                ? notebook.getColorHex() : "#4f46e5";

        // Top Accent Stripe
        Region accentStripe = new Region();
        accentStripe.setPrefHeight(5);
        accentStripe.setMinHeight(5);
        accentStripe.setStyle("-fx-background-color: " + themeColor + "; -fx-background-radius: 10px 10px 0 0;");

        // Card Content Body
        VBox body = new VBox(6);
        body.setPadding(new Insets(9, 11, 9, 11));
        VBox.setVgrow(body, Priority.ALWAYS);

        // Header Row: Color dot + Title (with right padding so it leaves space for the 3-dot button)
        HBox headerRow = new HBox(6);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        headerRow.setPadding(new Insets(0, 24, 0, 0));

        Label dot = new Label("●");
        dot.setStyle("-fx-font-size: 12px; -fx-text-fill: " + themeColor + ";");

        Label titleLabel = new Label(notebook.getTitle());
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        headerRow.getChildren().addAll(dot, titleLabel);

        // Menu button (3 dots) pinned directly to top-right of the card
        Button optionsBtn = new Button("⋮");
        optionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 4px; -fx-cursor: hand;");
        StackPane.setAlignment(optionsBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(optionsBtn, new Insets(8, 8, 0, 0));

        ContextMenu menu = new ContextMenu();
        MenuItem editItem = new MenuItem("✏ Edit Details");
        editItem.setOnAction(e -> NotebookDialog.show(
                mainMenuView.getScene().getWindow(),
                currentUser.getId(),
                notebook,
                this::loadNotebooks
        ));

        MenuItem deleteItem = new MenuItem("🗑 Delete Notebook");
        deleteItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Notebook");
            confirm.setHeaderText("Delete \"" + notebook.getTitle() + "\"?");
            confirm.setContentText("All topics, pages, and attachments inside this notebook will be permanently deleted.");
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                DatabaseHelper.deleteNotebook(notebook.getId());
                loadNotebooks();
            }
        });

        menu.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
        optionsBtn.setOnAction(e -> menu.show(optionsBtn, javafx.geometry.Side.BOTTOM, 0, 0));

        // Description
        Label descLabel = new Label(notebook.getDescription() != null && !notebook.getDescription().isEmpty()
                ? notebook.getDescription() : "No description provided.");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(26);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Footer: Topics & Pages Counts + Open button
        HBox footer = new HBox(6);
        footer.setAlignment(Pos.CENTER_LEFT);

        int topics = notebook.getTopicCount();
        int pages = notebook.getPageCount();
        String statsText = topics + (topics == 1 ? " Topic" : " Topics") + " · "
                         + pages + (pages == 1 ? " Page" : " Pages");
        Label statsLabel = new Label(statsText);
        statsLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #4338ca; "
                + "-fx-background-color: #eef2ff; -fx-padding: 2px 6px; -fx-background-radius: 4px;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Label openArrow = new Label("Open ➜");
        openArrow.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #6366f1;");

        footer.getChildren().addAll(statsLabel, footerSpacer, openArrow);

        body.getChildren().addAll(headerRow, descLabel, spacer, footer);

        VBox contentBox = new VBox();
        contentBox.getChildren().addAll(accentStripe, body);

        card.getChildren().addAll(contentBox, optionsBtn);

        // Card Click opens the notebook
        card.setOnMouseClicked(e -> {
            if (e.getTarget() != optionsBtn && !optionsBtn.isHover()) {
                handleOpenNotebook(notebook);
            }
        });

        return card;
    }

    /**
     * Handles opening a notebook workspace.
     */
    private void handleOpenNotebook(Notebook notebook) {
        openNotebookWorkspace(notebook);
    }

    /**
     * Opens the interactive Notebook Workspace view.
     */
    public void openNotebookWorkspace(Notebook notebook) {
        this.currentNotebook = notebook;
        this.currentTopic = null;
        this.currentPage = null;

        // Hide main app top bar to give maximum vertical space to the notebook
        if (appTopBar != null) {
            appTopBar.setVisible(false);
            appTopBar.setManaged(false);
        }

        if (mainMenuView != null) {
            mainMenuView.setVisible(false);
            mainMenuView.setManaged(false);
        }
        if (routineView != null) {
            routineView.setVisible(false);
            routineView.setManaged(false);
        }
        if (calendarView != null) {
            calendarView.setVisible(false);
            calendarView.setManaged(false);
        }
        if (notebookWorkspaceView != null) {
            notebookWorkspaceView.setVisible(true);
            notebookWorkspaceView.setManaged(true);
        }
        if (quizView != null) {
            quizView.setVisible(false);
            quizView.setManaged(false);
        }
        if (rightToggleBtn != null) {
            rightToggleBtn.setVisible(false);
            rightToggleBtn.setManaged(false);
        }
        if (isRightSidebarOpen) {
            handleToggleRight();
        }

        if (notebookBreadcrumbLabel != null) {
            notebookBreadcrumbLabel.setText("📘 " + notebook.getTitle());
        }
        if (workspaceStatusLabel != null) {
            workspaceStatusLabel.setText("");
        }

        setTopicsSidebarOpen(true);
        loadTopicsExplorer();

        // Producer: dispatch background stats indexing job to consumer thread pool
        NotebookJobQueue.getInstance().submitJob(new NotebookStatsJob(notebook.getId(), notebook.getTitle()));
    }

    /**
     * Navigates back from the Notebook Workspace to the main dashboard.
     */
    @FXML
    public void handleBackToDashboard() {
        if (appTopBar != null) {
            appTopBar.setVisible(true);
            appTopBar.setManaged(true);
        }
        handleOpenMainMenu();
        loadNotebooks();
    }

    /**
     * Toggles the visibility of the left Topics Explorer sidebar.
     */
    @FXML
    public void handleToggleTopicsSidebar() {
        setTopicsSidebarOpen(!isTopicsSidebarOpen);
    }

    /**
     * Closes the Topics Explorer sidebar.
     */
    @FXML
    public void handleCloseTopicsSidebar() {
        setTopicsSidebarOpen(false);
    }

    private void setTopicsSidebarOpen(boolean open) {
        this.isTopicsSidebarOpen = open;
        if (topicsSidebar != null) {
            topicsSidebar.setVisible(open);
            topicsSidebar.setManaged(open);
            if (open) {
                topicsSidebar.setPrefWidth(280);
                topicsSidebar.setMinWidth(240);
            } else {
                topicsSidebar.setPrefWidth(0);
                topicsSidebar.setMinWidth(0);
            }
        }
        if (toggleTopicsBtn != null) {
            if (open) {
                toggleTopicsBtn.setText("📂 Topics");
                toggleTopicsBtn.setStyle("");
            } else {
                toggleTopicsBtn.setText("📂 Show Topics");
                toggleTopicsBtn.setStyle("-fx-border-color: #6366f1; -fx-text-fill: #4338ca; -fx-background-color: #eef2ff;");
            }
        }
    }

    /**
     * Loads the Topics, Pages, and File Attachments into the left explorer pane.
     */
    public void loadTopicsExplorer() {
        if (topicsListContainer == null || currentNotebook == null) return;
        topicsListContainer.getChildren().clear();

        List<Topic> topics = DatabaseHelper.getTopicsByNotebook(currentNotebook.getId());

        if (topics.isEmpty()) {
            VBox emptyPrompt = new VBox(10);
            emptyPrompt.setAlignment(Pos.CENTER);
            emptyPrompt.setPadding(new Insets(24, 12, 24, 12));
            emptyPrompt.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 6px; -fx-border-color: #e2e8f0; -fx-border-radius: 6px; -fx-border-style: dashed;");

            Label emptyLabel = new Label("No topics yet");
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

            Button addTopicBtn = new Button("+ Add First Topic");
            addTopicBtn.setStyle("-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px; -fx-padding: 6px 12px;");
            addTopicBtn.setOnAction(e -> promptAddTopic());

            emptyPrompt.getChildren().addAll(emptyLabel, addTopicBtn);
            topicsListContainer.getChildren().add(emptyPrompt);
            showEmptyPlaygroundState();
            return;
        }

        // Ensure currentTopic is resolved properly
        if (currentTopic == null && !topics.isEmpty()) {
            currentTopic = topics.get(0);
        } else if (currentTopic != null) {
            Topic matched = null;
            for (Topic t : topics) {
                if (t.getId() == currentTopic.getId()) {
                    matched = t;
                    break;
                }
            }
            currentTopic = (matched != null) ? matched : (!topics.isEmpty() ? topics.get(0) : null);
        }

        Page firstPageToSelect = null;
        Topic firstPageTopic = null;

        for (Topic topic : topics) {
            VBox topicSection = new VBox(4);

            boolean isOpened = (currentTopic != null && currentTopic.getId() == topic.getId());

            // Topic Header Bar
            HBox topicHeader = new HBox(6);
            topicHeader.setAlignment(Pos.CENTER_LEFT);
            topicHeader.getStyleClass().add("topic-header");
            if (isOpened) {
                topicHeader.getStyleClass().add("topic-header-active");
            }

            Label folderIcon = new Label(isOpened ? "▼ 📂" : "▶ 📁");
            folderIcon.setStyle("-fx-font-size: " + (isOpened ? "12px;" : "11px;") + " -fx-text-fill: #475569;");

            Label topicTitle = new Label(topic.getTitle());
            topicTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: " + (isOpened ? "bold;" : "normal;") + " -fx-text-fill: #1e293b;");
            HBox.setHgrow(topicTitle, Priority.ALWAYS);

            // Options menu (Rename, Attach, Delete)
            Button optionsBtn = new Button("⋮");
            optionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 0 4px; -fx-cursor: hand;");

            ContextMenu topicMenu = new ContextMenu();
            MenuItem renameItem = new MenuItem("✏ Rename Topic");
            renameItem.setOnAction(e -> promptRenameTopic(topic));

            MenuItem attachFileItem = new MenuItem("📎 Attach File...");
            attachFileItem.setOnAction(e -> attachFileToTopic(topic));

            MenuItem deleteItem = new MenuItem("🗑 Delete Topic");
            deleteItem.setOnAction(e -> deleteTopic(topic));

            topicMenu.getItems().addAll(renameItem, attachFileItem, new SeparatorMenuItem(), deleteItem);
            optionsBtn.setOnAction(e -> topicMenu.show(optionsBtn, javafx.geometry.Side.BOTTOM, 0, 0));

            if (isOpened) {
                // Quick Add Page button
                Button addPageBtn = new Button("+ Page");
                addPageBtn.setTooltip(new Tooltip("Add new page to this topic"));
                addPageBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 3px 6px; -fx-cursor: hand;");
                addPageBtn.setOnAction(e -> promptAddPage(topic));

                // Quick Add File button
                Button addFileBtn = new Button("+ File");
                addFileBtn.setTooltip(new Tooltip("Attach file (PDF, PPTX, Word, etc.) to this topic"));
                addFileBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-size: 10px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 3px 6px; -fx-cursor: hand;");
                addFileBtn.setOnAction(e -> attachFileToTopic(topic));

                topicHeader.getChildren().addAll(folderIcon, topicTitle, addPageBtn, addFileBtn, optionsBtn);
            } else {
                int totalItems = (topic.getPages() != null ? topic.getPages().size() : 0)
                               + (topic.getFiles() != null ? topic.getFiles().size() : 0);
                Label countLabel = new Label(totalItems + (totalItems == 1 ? " item" : " items"));
                countLabel.getStyleClass().add("topic-count-badge");

                topicHeader.getChildren().addAll(folderIcon, topicTitle, countLabel, optionsBtn);

                // Clicking anywhere on collapsed topic header opens this topic
                topicHeader.setOnMouseClicked(e -> {
                    if (e.getTarget() != optionsBtn && !optionsBtn.isHover()) {
                        openTopic(topic);
                    }
                });
            }

            topicSection.getChildren().add(topicHeader);

            if (isOpened) {
                // Pages & Files Container (Indented)
                VBox childrenBox = new VBox(2);
                childrenBox.setPadding(new Insets(2, 0, 4, 16));

                boolean hasContent = false;

                // List Pages
                for (Page page : topic.getPages()) {
                    hasContent = true;
                    if (firstPageToSelect == null) {
                        firstPageToSelect = page;
                        firstPageTopic = topic;
                    }

                    HBox pageRow = new HBox(6);
                    pageRow.setAlignment(Pos.CENTER_LEFT);
                    pageRow.getStyleClass().add("page-item");
                    if (currentPage != null && currentPage.getId() == page.getId()) {
                        pageRow.getStyleClass().add("page-item-active");
                    }

                    Label pageIcon = new Label("📄");
                    pageIcon.setStyle("-fx-font-size: 12px;");

                    Label pageTitleLabel = new Label(page.getTitle());
                    pageTitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155;");
                    HBox.setHgrow(pageTitleLabel, Priority.ALWAYS);

                    Button pageMenuBtn = new Button("⋮");
                    pageMenuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-padding: 0 2px; -fx-cursor: hand;");

                    ContextMenu pageMenu = new ContextMenu();
                    MenuItem renamePageItem = new MenuItem("✏ Rename Page");
                    renamePageItem.setOnAction(me -> promptRenamePage(topic, page));

                    MenuItem deletePageItem = new MenuItem("🗑 Delete Page");
                    deletePageItem.setOnAction(me -> deletePage(topic, page));

                    pageMenu.getItems().addAll(renamePageItem, new SeparatorMenuItem(), deletePageItem);
                    pageMenuBtn.setOnAction(me -> pageMenu.show(pageMenuBtn, javafx.geometry.Side.BOTTOM, 0, 0));

                    pageRow.getChildren().addAll(pageIcon, pageTitleLabel, pageMenuBtn);

                    pageRow.setOnMouseClicked(pe -> {
                        if (pe.getTarget() != pageMenuBtn && !pageMenuBtn.isHover()) {
                            selectPage(topic, page);
                        }
                    });

                    childrenBox.getChildren().add(pageRow);
                }

                // List Attached Files (PDF, PPTX, etc.)
                for (TopicFile file : topic.getFiles()) {
                    hasContent = true;
                    HBox fileRow = new HBox(6);
                    fileRow.setAlignment(Pos.CENTER_LEFT);
                    fileRow.getStyleClass().add("page-item");

                    Label fileIcon = new Label(file.getFileIcon());
                    fileIcon.setStyle("-fx-font-size: 12px;");

                    Label fileNameLabel = new Label(file.getOriginalName());
                    fileNameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
                    HBox.setHgrow(fileNameLabel, Priority.ALWAYS);

                    Label sizeLabel = new Label(file.getFormattedSize());
                    sizeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

                    Button fileMenuBtn = new Button("⋮");
                    fileMenuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-padding: 0 2px; -fx-cursor: hand;");

                    ContextMenu fileMenu = new ContextMenu();
                    MenuItem openItem = new MenuItem("▶ Open (Default App)");
                    openItem.setOnAction(me -> handleOpenFile(file));

                    MenuItem openWithItem = new MenuItem("⚙ Choose App (Open With...)");
                    openWithItem.setOnAction(me -> handleOpenWith(file));

                    MenuItem showInExplorerItem = new MenuItem("📁 Show in Explorer");
                    showInExplorerItem.setOnAction(me -> handleShowInExplorer(file));

                    MenuItem deleteFileItem = new MenuItem("🗑 Remove File");
                    deleteFileItem.setOnAction(me -> {
                        DatabaseHelper.deleteTopicFile(file.getId());
                        loadTopicsExplorer();
                    });

                    fileMenu.getItems().addAll(openItem, openWithItem, showInExplorerItem, new SeparatorMenuItem(), deleteFileItem);
                    fileMenuBtn.setOnAction(me -> fileMenu.show(fileMenuBtn, javafx.geometry.Side.BOTTOM, 0, 0));

                    fileRow.getChildren().addAll(fileIcon, fileNameLabel, sizeLabel, fileMenuBtn);

                    fileRow.setOnContextMenuRequested(me -> fileMenu.show(fileRow, me.getScreenX(), me.getScreenY()));

                    fileRow.setOnMouseClicked(fe -> {
                        if (fe.getTarget() != fileMenuBtn && !fileMenuBtn.isHover()) {
                            handleOpenFile(file);
                        }
                    });

                    childrenBox.getChildren().add(fileRow);
                }

                if (!hasContent) {
                    Label emptyTopicHint = new Label("No pages or files yet. Click + Page to create one.");
                    emptyTopicHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-padding: 4px 8px;");
                    childrenBox.getChildren().add(emptyTopicHint);
                }

                topicSection.getChildren().add(childrenBox);
            }

            topicsListContainer.getChildren().add(topicSection);
        }

        // Maintain or initialize page selection for the opened topic
        if (currentPage != null && currentTopic != null) {
            boolean pageBelongsToTopic = false;
            for (Page p : currentTopic.getPages()) {
                if (p.getId() == currentPage.getId()) {
                    pageBelongsToTopic = true;
                    break;
                }
            }
            if (pageBelongsToTopic) {
                renderPageCanvas(currentPage);
            } else if (!currentTopic.getPages().isEmpty()) {
                selectPage(currentTopic, currentTopic.getPages().get(0));
            } else {
                currentPage = null;
                showEmptyPlaygroundState();
            }
        } else if (firstPageToSelect != null) {
            selectPage(firstPageTopic, firstPageToSelect);
        } else {
            showEmptyPlaygroundState();
        }
    }

    /**
     * Sets the specified topic as the currently opened topic.
     * Selects its first page if available, or displays the empty playground prompt.
     */
    private void openTopic(Topic topic) {
        if (topic == null) return;
        this.currentTopic = topic;
        if (topic.getPages() != null && !topic.getPages().isEmpty()) {
            selectPage(topic, topic.getPages().get(0));
        } else {
            this.currentPage = null;
            if (notebookBreadcrumbLabel != null && currentNotebook != null) {
                notebookBreadcrumbLabel.setText("📘 " + currentNotebook.getTitle()
                        + "  ›  📂 " + topic.getTitle());
            }
            showEmptyPlaygroundState();
        }
        loadTopicsExplorer();
    }

    /**
     * Prompts the user to enter a topic title and creates it in the current notebook.
     */
    @FXML
    public void promptAddTopic() {
        if (currentNotebook == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Topic");
        dialog.setHeaderText("Enter topic name (e.g. Chapter 1: Introduction, Trees, Networking):");
        dialog.setContentText("Topic Title:");
        Optional<String> res = dialog.showAndWait();
        res.ifPresent(title -> {
            String clean = title.trim();
            if (!clean.isEmpty()) {
                int newTopicId = DatabaseHelper.createTopic(currentNotebook.getId(), clean);
                this.currentTopic = new Topic(newTopicId, currentNotebook.getId(), clean, 0);
                this.currentPage = null;
                if (notebookBreadcrumbLabel != null) {
                    notebookBreadcrumbLabel.setText("📘 " + currentNotebook.getTitle()
                            + "  ›  📂 " + clean);
                }
                loadTopicsExplorer();
                showEmptyPlaygroundState();
            }
        });
    }

    /**
     * Prompts the user to enter a page title and creates it under the specified topic.
     */
    public void promptAddPage(Topic topic) {
        if (topic == null) return;
        TextInputDialog dialog = new TextInputDialog("Untitled Page");
        dialog.setTitle("Add Page");
        dialog.setHeaderText("Enter page title for \"" + topic.getTitle() + "\":");
        dialog.setContentText("Page Title:");
        Optional<String> res = dialog.showAndWait();
        res.ifPresent(title -> {
            String clean = title.trim();
            if (!clean.isEmpty()) {
                this.currentTopic = topic;
                int pageId = DatabaseHelper.createPage(topic.getId(), clean);
                Page newPage = DatabaseHelper.getPageById(pageId);
                loadTopicsExplorer();
                if (newPage != null) {
                    selectPage(topic, newPage);
                }
            }
        });
    }

    private void promptRenameTopic(Topic topic) {
        TextInputDialog dialog = new TextInputDialog(topic.getTitle());
        dialog.setTitle("Rename Topic");
        dialog.setHeaderText("Enter new name for topic:");
        dialog.setContentText("Topic Name:");
        Optional<String> res = dialog.showAndWait();
        res.ifPresent(newTitle -> {
            String clean = newTitle.trim();
            if (!clean.isEmpty()) {
                DatabaseHelper.renameTopic(topic.getId(), clean);
                loadTopicsExplorer();
            }
        });
    }

    private void deleteTopic(Topic topic) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Topic");
        confirm.setHeaderText("Delete topic \"" + topic.getTitle() + "\"?");
        confirm.setContentText("All pages and file attachments inside this topic will be permanently deleted.");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            DatabaseHelper.deleteTopic(topic.getId());
            if (currentTopic != null && currentTopic.getId() == topic.getId()) {
                currentPage = null;
                currentTopic = null;
            }
            loadTopicsExplorer();
        }
    }

    private void promptRenamePage(Topic topic, Page page) {
        TextInputDialog dialog = new TextInputDialog(page.getTitle());
        dialog.setTitle("Rename Page");
        dialog.setHeaderText("Enter new name for page:");
        dialog.setContentText("Page Title:");
        Optional<String> res = dialog.showAndWait();
        res.ifPresent(newTitle -> {
            String clean = newTitle.trim();
            if (!clean.isEmpty()) {
                DatabaseHelper.updatePage(page.getId(), clean, page.getContentJson());
                page.setTitle(clean);
                loadTopicsExplorer();
            }
        });
    }

    private void deletePage(Topic topic, Page page) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Page");
        confirm.setHeaderText("Delete page \"" + page.getTitle() + "\"?");
        confirm.setContentText("Are you sure you want to delete this page?");
        Optional<ButtonType> res = confirm.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            DatabaseHelper.deletePage(page.getId());
            if (currentPage != null && currentPage.getId() == page.getId()) {
                currentPage = null;
            }
            loadTopicsExplorer();
        }
    }

    /**
     * Selects an active page and renders its canvas.
     */
    public void selectPage(Topic topic, Page page) {
        this.currentTopic = topic;
        this.currentPage = page;

        if (notebookBreadcrumbLabel != null && currentNotebook != null) {
            notebookBreadcrumbLabel.setText("📘 " + currentNotebook.getTitle()
                    + "  ›  📂 " + topic.getTitle()
                    + "  ›  📄 " + page.getTitle());
        }

        renderPageCanvas(page);
    }

    /**
     * Renders the page playground canvas.
     */
    private void renderPageCanvas(Page page) {
        if (pagePlaygroundContainer == null) return;
        pagePlaygroundContainer.getChildren().clear();

        // 1. Page Header: Title + Timestamp + Status
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        TextField pageTitleField = new TextField(page.getTitle());
        pageTitleField.getStyleClass().add("page-title-field");
        pageTitleField.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; -fx-padding: 0;");
        HBox.setHgrow(pageTitleField, Priority.ALWAYS);
        pageTitleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                String updatedTitle = pageTitleField.getText().trim();
                if (!updatedTitle.isEmpty() && !updatedTitle.equals(page.getTitle())) {
                    page.setTitle(updatedTitle);
                    if (currentNotebook != null) {
                        NotebookJobQueue.getInstance().submitJob(new PageSaveJob(
                                currentNotebook.getId(),
                                page.getId(),
                                updatedTitle,
                                page.getContentJson(),
                                workspaceStatusLabel,
                                true
                        ));
                    }
                    if (currentNotebook != null && currentTopic != null) {
                        notebookBreadcrumbLabel.setText("📘 " + currentNotebook.getTitle()
                                + "  ›  📂 " + currentTopic.getTitle()
                                + "  ›  📄 " + updatedTitle);
                    }
                    loadTopicsExplorer();
                }
            }
        });

        Label saveBadge = new Label("Saved ✓");
        saveBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-background-color: #ecfdf5; -fx-padding: 2px 8px; -fx-background-radius: 10px;");

        topRow.getChildren().addAll(pageTitleField, saveBadge);

        Label updatedLabel = new Label("Last edited: " + (page.getUpdatedAt() != null ? page.getUpdatedAt() : "Recently"));
        updatedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        VBox pageHeader = new VBox(4);
        pageHeader.setPadding(new Insets(0, 0, 10, 0));
        pageHeader.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 0 0 1px 0;");
        pageHeader.getChildren().addAll(topRow, updatedLabel);

        // 2. Deserialize blocks
        currentPageBlocks = PageBlock.deserializeList(page.getContentJson());
        if (currentPageBlocks.isEmpty()) {
            currentPageBlocks.add(new PageBlock(PageBlock.TYPE_TEXT, "", ""));
        }

        // 3. Scrollable Blocks Container
        blocksContainer = new VBox(14);
        blocksContainer.setPadding(new Insets(10, 4, 10, 0));

        ScrollPane scrollPane = new ScrollPane(blocksContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        refreshBlocksView();

        // 4. Bottom Block Insertion Toolbar
        HBox bottomToolbar = new HBox(10);
        bottomToolbar.setAlignment(Pos.CENTER_LEFT);
        bottomToolbar.setPadding(new Insets(10, 0, 0, 0));
        bottomToolbar.setStyle("-fx-border-color: #e2e8f0; -fx-border-width: 1px 0 0 0;");

        Button addNoteBtn = new Button("📝 + Add Note");
        addNoteBtn.getStyleClass().add("btn-playground-add");
        addNoteBtn.setOnAction(e -> {
            currentPageBlocks.add(new PageBlock(PageBlock.TYPE_TEXT, "", ""));
            saveCurrentPageBlocks();
            refreshBlocksView();
        });

        Button addCodeBtn = new Button("💻 + Add Code Snippet");
        addCodeBtn.getStyleClass().add("btn-playground-add");
        addCodeBtn.setOnAction(e -> {
            currentPageBlocks.add(new PageBlock(PageBlock.TYPE_CODE, "", "Java"));
            saveCurrentPageBlocks();
            refreshBlocksView();
        });

        Button addImageBtn = new Button("🖼 + Insert Image");
        addImageBtn.getStyleClass().add("btn-playground-add");
        addImageBtn.setOnAction(e -> promptUploadImage());

        Button pasteScreenshotBtn = new Button("📷 Paste Screenshot (Ctrl + V)");
        pasteScreenshotBtn.getStyleClass().add("btn-playground-add");
        pasteScreenshotBtn.setStyle("-fx-border-color: #6366f1; -fx-text-fill: #4338ca; -fx-background-color: #eef2ff;");
        pasteScreenshotBtn.setOnAction(e -> handlePasteClipboardImage());

        bottomToolbar.getChildren().addAll(addNoteBtn, addCodeBtn, addImageBtn, pasteScreenshotBtn);

        pagePlaygroundContainer.getChildren().addAll(pageHeader, scrollPane, bottomToolbar);
    }

    private void refreshBlocksView() {
        if (blocksContainer == null) return;
        blocksContainer.getChildren().clear();

        for (int i = 0; i < currentPageBlocks.size(); i++) {
            final int index = i;
            PageBlock block = currentPageBlocks.get(i);
            Node blockNode = createBlockNode(block, index);
            blocksContainer.getChildren().add(blockNode);
        }
    }

    private Node createBlockNode(PageBlock block, int index) {
        String type = block.getType();
        if (PageBlock.TYPE_CODE.equalsIgnoreCase(type)) {
            return createCodeBlockNode(block, index);
        } else if (PageBlock.TYPE_IMAGE.equalsIgnoreCase(type)) {
            return createImageBlockNode(block, index);
        } else {
            return createTextBlockNode(block, index);
        }
    }

    private Node createTextBlockNode(PageBlock block, int index) {
        VBox card = new VBox(6);
        card.getStyleClass().add("block-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("block-header");

        Label typeLbl = new Label("📝 Note");
        typeLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #64748b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            currentPageBlocks.remove(index);
            saveCurrentPageBlocks();
            refreshBlocksView();
        });

        header.getChildren().addAll(typeLbl, spacer, delBtn);

        TextArea textArea = new TextArea(block.getContent());
        textArea.setWrapText(true);
        int lineCount = block.getContent().isEmpty() ? 4 : Math.max(4, block.getContent().split("\n", -1).length + 1);
        textArea.setPrefRowCount(Math.min(lineCount, 16));
        textArea.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b;");

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            block.setContent(newText);
            int lines = Math.max(4, newText.split("\n", -1).length + 1);
            textArea.setPrefRowCount(Math.min(lines, 16));
            saveCurrentPageBlocksQuietly();
        });

        textArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                saveCurrentPageBlocks();
            }
        });

        card.getChildren().addAll(header, textArea);
        return card;
    }

    private Node createCodeBlockNode(PageBlock block, int index) {
        VBox card = new VBox(6);
        card.getStyleClass().add("block-code-container");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 0 0 6px 0; -fx-border-color: #334155; -fx-border-width: 0 0 1px 0;");

        Label typeLbl = new Label("💻 Code Snippet");
        typeLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        ComboBox<String> langCombo = new ComboBox<>();
        langCombo.getItems().addAll("Java", "Python", "C", "C++", "JavaScript", "HTML/CSS", "SQL", "Bash", "Text");
        langCombo.setValue((block.getExtra() != null && !block.getExtra().isEmpty()) ? block.getExtra() : "Java");
        langCombo.setStyle("-fx-background-color: #1e293b; -fx-mark-color: #94a3b8; -fx-font-size: 11px;");
        langCombo.setOnAction(e -> {
            block.setExtra(langCombo.getValue());
            saveCurrentPageBlocks();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button copyBtn = new Button("📋 Copy Code");
        copyBtn.getStyleClass().add("btn-copy-code");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent cc = new ClipboardContent();
            cc.putString(block.getContent());
            clipboard.setContent(cc);

            copyBtn.setText("✓ Copied!");
            copyBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: #ffffff; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-padding: 4px 10px;");
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(ev -> {
                copyBtn.setText("📋 Copy Code");
                copyBtn.setStyle("");
                copyBtn.getStyleClass().add("btn-copy-code");
            });
            pause.play();
        });

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 12px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            currentPageBlocks.remove(index);
            saveCurrentPageBlocks();
            refreshBlocksView();
        });

        header.getChildren().addAll(typeLbl, langCombo, spacer, copyBtn, delBtn);

        TextArea codeArea = new TextArea(block.getContent());
        codeArea.getStyleClass().add("code-text-area");
        codeArea.setWrapText(false);
        int lineCount = block.getContent().isEmpty() ? 5 : Math.max(5, block.getContent().split("\n", -1).length + 2);
        codeArea.setPrefRowCount(Math.min(lineCount, 20));

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            block.setContent(newText);
            int lines = Math.max(5, newText.split("\n", -1).length + 2);
            codeArea.setPrefRowCount(Math.min(lines, 20));
            saveCurrentPageBlocksQuietly();
        });

        codeArea.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                saveCurrentPageBlocks();
            }
        });

        card.getChildren().addAll(header, codeArea);
        return card;
    }

    private Node createImageBlockNode(PageBlock block, int index) {
        VBox card = new VBox(6);
        card.getStyleClass().add("block-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("block-header");

        Label typeLbl = new Label("🖼 Image / Screenshot");
        typeLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #64748b;");

        TextField captionField = new TextField(block.getExtra());
        captionField.setPromptText("Add caption or note...");
        captionField.setStyle("-fx-font-size: 11px; -fx-background-color: #f1f5f9; -fx-background-radius: 4px; -fx-padding: 3px 8px;");
        HBox.setHgrow(captionField, Priority.ALWAYS);
        captionField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                block.setExtra(captionField.getText().trim());
                saveCurrentPageBlocksQuietly();
            }
        });

        File imgFile = new File(block.getContent());

        Button openFullBtn = new Button("🔍 Open");
        openFullBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-size: 11px; -fx-padding: 3px 8px; -fx-cursor: hand; -fx-background-radius: 4px;");
        openFullBtn.setOnAction(e -> {
            if (imgFile.exists()) {
                try {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(imgFile);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button delBtn = new Button("🗑");
        delBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            currentPageBlocks.remove(index);
            saveCurrentPageBlocks();
            refreshBlocksView();
        });

        header.getChildren().addAll(typeLbl, captionField, openFullBtn, delBtn);

        if (imgFile.exists()) {
            try {
                Image img = new Image(imgFile.toURI().toString());
                ImageView imgView = new ImageView(img);
                imgView.setPreserveRatio(true);
                imgView.setSmooth(true);
                imgView.setFitWidth(650);
                imgView.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 2);");
                imgView.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        try {
                            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                                Desktop.getDesktop().open(imgFile);
                            }
                        } catch (Exception ignored) {}
                    }
                });
                card.getChildren().addAll(header, imgView);
            } catch (Exception ex) {
                Label errLabel = new Label("Error loading image: " + imgFile.getAbsolutePath());
                card.getChildren().addAll(header, errLabel);
            }
        } else {
            Label missingLabel = new Label("Image file not found: " + block.getContent());
            missingLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444;");
            card.getChildren().addAll(header, missingLabel);
        }

        return card;
    }

    private void saveCurrentPageBlocks() {
        if (currentPage == null || currentNotebook == null) return;
        String json = PageBlock.serializeList(currentPageBlocks);
        currentPage.setContentJson(json);
        // Producer: dispatch asynchronous save job to consumer worker thread pool
        PageSaveJob job = new PageSaveJob(
                currentNotebook.getId(),
                currentPage.getId(),
                currentPage.getTitle(),
                json,
                workspaceStatusLabel,
                true
        );
        NotebookJobQueue.getInstance().submitJob(job);
    }

    private void saveCurrentPageBlocksQuietly() {
        if (currentPage == null || currentNotebook == null) return;
        String json = PageBlock.serializeList(currentPageBlocks);
        currentPage.setContentJson(json);
        // Producer: dispatch asynchronous save job without distracting badge change
        PageSaveJob job = new PageSaveJob(
                currentNotebook.getId(),
                currentPage.getId(),
                currentPage.getTitle(),
                json,
                workspaceStatusLabel,
                false
        );
        NotebookJobQueue.getInstance().submitJob(job);
    }

    private void promptUploadImage() {
        if (currentPage == null || currentNotebook == null) return;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Insert Image into Page");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg, *.gif, *.bmp)", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );
        Stage stage = (Stage) notebookWorkspaceView.getScene().getWindow();
        File selected = fileChooser.showOpenDialog(stage);
        if (selected == null) return;

        try {
            File dir = new File("study_buddy_data/images/" + currentNotebook.getId());
            if (!dir.exists()) dir.mkdirs();

            String storedName = "img_" + System.currentTimeMillis() + "_" + selected.getName();
            File targetFile = new File(dir, storedName);
            Files.copy(selected.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            PageBlock newBlock = new PageBlock(PageBlock.TYPE_IMAGE, targetFile.getAbsolutePath(), selected.getName());
            currentPageBlocks.add(newBlock);
            saveCurrentPageBlocks();
            refreshBlocksView();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Image Error", "Failed to insert image: " + ex.getMessage());
        }
    }

    private void handlePasteClipboardImage() {
        if (currentPage == null || currentNotebook == null) return;
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (!clipboard.hasImage()) {
            showAlert("No Screenshot Found", "No image found in clipboard.\n\nTip: Press Win + Shift + S to take a screenshot, then click Paste or press Ctrl + V.");
            return;
        }

        try {
            Image fxImage = clipboard.getImage();
            File targetFile = saveClipboardImageToFile(fxImage);
            if (targetFile != null) {
                PageBlock newBlock = new PageBlock(PageBlock.TYPE_IMAGE, targetFile.getAbsolutePath(), "Screenshot " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                currentPageBlocks.add(newBlock);
                saveCurrentPageBlocks();
                refreshBlocksView();
                if (workspaceStatusLabel != null) {
                    workspaceStatusLabel.setText("Screenshot pasted ✓");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Paste Error", "Failed to paste image from clipboard: " + ex.getMessage());
        }
    }

    private File saveClipboardImageToFile(Image fxImage) throws IOException {
        if (fxImage == null || currentNotebook == null) return null;
        File dir = new File("study_buddy_data/images/" + currentNotebook.getId());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = "screenshot_" + System.currentTimeMillis() + ".png";
        File targetFile = new File(dir, fileName);

        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        PixelReader reader = fxImage.getPixelReader();
        if (reader == null) {
            throw new IOException("Cannot read image pixels from clipboard image");
        }

        BufferedImage bImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bImage.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        ImageIO.write(bImage, "png", targetFile);
        return targetFile;
    }

    private void showEmptyPlaygroundState() {
        if (pagePlaygroundContainer == null) return;
        pagePlaygroundContainer.getChildren().clear();

        VBox emptyPrompt = new VBox(12);
        emptyPrompt.setAlignment(Pos.CENTER);
        emptyPrompt.setPadding(new Insets(60, 20, 60, 20));
        emptyPrompt.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-style: dashed;");
        VBox.setVgrow(emptyPrompt, Priority.ALWAYS);

        Label icon = new Label("📄");
        icon.setStyle("-fx-font-size: 40px;");

        Label title = new Label(currentTopic != null ? "No Pages in \"" + currentTopic.getTitle() + "\"" : "No Page Selected");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label sub = new Label(currentTopic != null
                ? "This topic doesn't have any pages yet. Click below to add the first page."
                : "Select a page from the Topics Explorer on the left, or add a new page to begin writing.");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        emptyPrompt.getChildren().addAll(icon, title, sub);

        if (currentTopic != null) {
            Button addFirstPageBtn = new Button("📝 + Add First Page to \"" + currentTopic.getTitle() + "\"");
            addFirstPageBtn.setStyle("-fx-background-color: #6366f1; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 8px 16px; -fx-background-radius: 6px; -fx-cursor: hand;");
            addFirstPageBtn.setOnAction(e -> promptAddPage(currentTopic));
            emptyPrompt.getChildren().add(addFirstPageBtn);
        }

        pagePlaygroundContainer.getChildren().add(emptyPrompt);
    }

    /**
     * Attaches one or more files to a topic.
     */
    private void attachFileToTopic(Topic topic) {
        if (topic == null || currentNotebook == null) return;
        this.currentTopic = topic;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Attach Files to " + topic.getTitle());
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Supported Documents", "*.pdf", "*.pptx", "*.ppt", "*.docx", "*.doc", "*.xlsx", "*.xls", "*.txt", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("PowerPoint Presentations (*.pptx, *.ppt)", "*.pptx", "*.ppt"),
                new FileChooser.ExtensionFilter("Word Documents (*.docx, *.doc)", "*.docx", "*.doc"),
                new FileChooser.ExtensionFilter("Images (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        Stage stage = (Stage) notebookWorkspaceView.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        if (selectedFiles == null || selectedFiles.isEmpty()) return;

        File destDir = new File("study_buddy_data/attachments/" + currentNotebook.getId());
        if (!destDir.exists()) destDir.mkdirs();

        int attachedCount = 0;
        for (File srcFile : selectedFiles) {
            try {
                String originalName = srcFile.getName();
                String ext = "";
                int dotIdx = originalName.lastIndexOf('.');
                if (dotIdx > 0) ext = originalName.substring(dotIdx + 1).toLowerCase();

                String storedName = System.currentTimeMillis() + "_" + originalName;
                File targetFile = new File(destDir, storedName);
                Files.copy(srcFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                DatabaseHelper.addTopicFile(topic.getId(), originalName, targetFile.getAbsolutePath(), ext, srcFile.length());
                attachedCount++;
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("File Error", "Failed to attach file " + srcFile.getName() + ": " + ex.getMessage());
            }
        }

        if (workspaceStatusLabel != null) {
            workspaceStatusLabel.setText("Attached " + attachedCount + " file(s) ✓");
        }
        loadTopicsExplorer();
    }

    /**
     * Opens an attached file using the OS default application.
     */
    private void handleOpenFile(TopicFile topicFile) {
        if (topicFile == null || topicFile.getStoredFilePath() == null) return;
        try {
            File file = new File(topicFile.getStoredFilePath());
            if (file.exists()) {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file);
                } else {
                    new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath()).start();
                }
            } else {
                showAlert("File Not Found", "The attachment file could not be found on disk:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showError("Unable to Open File", "Error launching native application: " + e.getMessage());
        }
    }

    /**
     * Opens an attached file with the Windows "Open With" dialog.
     */
    private void handleOpenWith(TopicFile topicFile) {
        if (topicFile == null || topicFile.getStoredFilePath() == null) return;
        try {
            File file = new File(topicFile.getStoredFilePath());
            if (file.exists()) {
                new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath()).start();
            } else {
                showAlert("File Not Found", "The attachment file could not be found on disk:\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            handleOpenFile(topicFile);
        }
    }

    /**
     * Reveals the file in Windows File Explorer.
     */
    private void handleShowInExplorer(TopicFile topicFile) {
        if (topicFile == null || topicFile.getStoredFilePath() == null) return;
        try {
            File file = new File(topicFile.getStoredFilePath());
            if (file.exists()) {
                new ProcessBuilder("explorer.exe", "/select,", file.getAbsolutePath()).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            loadNotebooks();
            loadTasksSidebar();
            renderCalendar();
            handleOpenMainMenu();
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
            loadTasksSidebar();
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
            timeHeader.setTooltip(new Tooltip("Click to add slot to left/right, edit, or delete"));
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

            // Weekday Header Cell (Click to add above/below, rename, or delete)
            Label dayHeader = new Label(day + "\n⚙");
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            dayHeader.setPrefSize(130, 85);
            dayHeader.setTooltip(new Tooltip("Click to add weekday above/below, rename, or delete"));
            dayHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b; "
                    + "-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-cursor: hand;");

            // Context menu for weekday operations (Add Weekday Above, Add Weekday Below, Rename, Delete)
            ContextMenu weekdayMenu = createWeekdayContextMenu(dayIndex, day);

            dayHeader.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY || e.getButton() == MouseButton.PRIMARY) {
                    weekdayMenu.show(dayHeader, e.getScreenX(), e.getScreenY());
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

        loadTasksSidebar();
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

        MenuItem addAboveItem = new MenuItem("⬆ Add Weekday Above");
        addAboveItem.setOnAction(e -> promptAddWeekdayAt(index, "Above"));

        MenuItem addBelowItem = new MenuItem("⬇ Add Weekday Below");
        addBelowItem.setOnAction(e -> promptAddWeekdayAt(index + 1, "Below"));

        MenuItem renameItem = new MenuItem("✏ Rename Weekday");
        renameItem.setOnAction(e -> handleRenameWeekday(index, currentDay));

        MenuItem deleteItem = new MenuItem("🗑 Delete Weekday");
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
                    () -> {
                        buildRoutineGrid();
                        loadTasksSidebar();
                    }
            );
        });

        return card;
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
            javafx.application.Platform.runLater(() -> stage.setMaximized(true));
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
            leftToggleBtn.setText(isLeftSidebarOpen ? "✕" : "☰");
        }
    }

    /**
     * Toggles the Right Sidebar (Tasks & Deadlines) with ultra-smooth animation.
     */
    @FXML
    public void handleToggleRight() {
        isRightSidebarOpen = !isRightSidebarOpen;
        if (isRightSidebarOpen) {
            loadTasksSidebar();
        }
        animateSidebar(rightSidebar, isRightSidebarOpen, SIDEBAR_WIDTH);
        updateRightSidebarButtonLabels();
    }

    private void updateRightSidebarButtonLabels() {
        if (rightToggleBtn != null) {
            rightToggleBtn.setText(isRightSidebarOpen ? "✕" : "📋");
        }
    }

    /**
     * Loads all pending activities and calendar tasks with deadlines,
     * sorts them chronologically by nearest deadline, and populates the Tasks sidebar.
     */
    public void loadTasksSidebar() {
        if (currentUser == null) return;

        activeTasks.clear();

        // 1. Load tasks from calendar_tasks
        List<RoutineTaskItem> calendarTasks = DatabaseHelper.getUserCalendarTasks(currentUser.getId());
        activeTasks.addAll(calendarTasks);

        // 2. Load any legacy activities from routine slots
        Map<String, RoutineSlot> slots = DatabaseHelper.getAllRoutineSlots(currentUser.getId());
        for (RoutineSlot slot : slots.values()) {
            if (slot.getActivities() != null) {
                for (SpecialActivity activity : slot.getActivities()) {
                    if (activity.getDeadlineInfo() != null && !activity.getDeadlineInfo().trim().isEmpty()) {
                        boolean exists = false;
                        for (RoutineTaskItem item : activeTasks) {
                            if (item.getActivityId() == activity.getId()) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            activeTasks.add(new RoutineTaskItem(
                                    activity.getId(),
                                    slot.getId(),
                                    slot.getSubjectName(),
                                    slot.getTeacherCode(),
                                    slot.getDayOfWeek(),
                                    slot.getTimeSlot(),
                                    activity.getActivityType(),
                                    activity.getDeadlineInfo()
                            ));
                        }
                    }
                }
            }
        }

        Collections.sort(activeTasks);

        if (taskCountBadge != null) {
            taskCountBadge.setText(String.valueOf(activeTasks.size()));
        }
        updateRightSidebarButtonLabels();

        if (tasksContainer == null) return;
        tasksContainer.getChildren().clear();
        taskCardMap.clear();

        if (activeTasks.isEmpty()) {
            VBox emptyPrompt = new VBox(10);
            emptyPrompt.setAlignment(Pos.CENTER);
            emptyPrompt.setPadding(new Insets(36, 16, 36, 16));
            emptyPrompt.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-style: dashed;");

            Label icon = new Label("🎉");
            icon.setStyle("-fx-font-size: 30px;");

            Label title = new Label("No Pending Tasks");
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label sub = new Label("Tasks and activities with deadlines added in your calendar will appear here with live countdowns.");
            sub.setWrapText(true);
            sub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            emptyPrompt.getChildren().addAll(icon, title, sub);
            tasksContainer.getChildren().add(emptyPrompt);
        } else {
            for (RoutineTaskItem task : activeTasks) {
                HBox card = createTaskCard(task);
                tasksContainer.getChildren().add(card);
            }
        }

        startTaskCountdownTimeline();
    }

    /**
     * Builds an interactive minimalist card for a task:
     * - Row 1: [Subject] <task name> and subtle options button
     * - Row 2: Live ticking countdown ("Due in DD : HH : MM : SS" or "Ended")
     * - Left vertical accent stripe and dynamic color progression (green -> yellow -> red -> ash)
     * - Left-click opens dedicated Task Details dialog (date, weekday, time, subject, status)
     * - Right-click context menu: <Details> and <Remove Task>
     */
    private HBox createTaskCard(RoutineTaskItem task) {
        HBox card = new HBox(0);
        card.setAlignment(Pos.CENTER_LEFT);

        // Left accent stripe showing progressive urgency color
        Region stripe = new Region();
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);
        stripe.setMaxWidth(5);

        // Main card body
        VBox body = new VBox(5);
        body.setPadding(new Insets(8, 10, 8, 10));
        HBox.setHgrow(body, Priority.ALWAYS);

        // Row 1: Subject badge + Task Name + Options button
        HBox topRow = new HBox(6);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label subjectLabel = new Label(task.getSubjectName());
        subjectLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 2px 6px;");

        Label taskNameLabel = new Label(task.getActivityType());
        taskNameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(taskNameLabel, Priority.ALWAYS);

        Button optionsBtn = new Button("⋮");
        optionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 4px; -fx-cursor: hand;");

        topRow.getChildren().addAll(subjectLabel, taskNameLabel, optionsBtn);

        // Row 2: Live ticking clock countdown
        Label countdownLabel = new Label(task.getFormattedClockCountdown());
        countdownLabel.setMaxWidth(Double.MAX_VALUE);
        countdownLabel.setAlignment(Pos.CENTER_LEFT);

        body.getChildren().addAll(topRow, countdownLabel);
        card.getChildren().addAll(stripe, body);

        // Apply initial dynamic color theme (Green -> Yellow -> Cherry Red -> Ash)
        RoutineTaskItem.TaskColorTheme theme = task.getColorTheme();
        applyCardTheme(card, stripe, subjectLabel, countdownLabel, theme);

        taskCardMap.put(task, new TaskCardNodes(card, stripe, subjectLabel, countdownLabel));

        // Right-Click Context Menu with <Details>, <Edit Task>, and <Remove Task>
        ContextMenu menu = new ContextMenu();
        MenuItem detailsItem = new MenuItem("🔍 Details");
        detailsItem.setOnAction(e -> TaskDetailDialog.show(card.getScene().getWindow(), currentUser.getId(), task, () -> {
            renderCalendar();
            loadTasksSidebar();
        }));

        MenuItem editItem = new MenuItem("✏ Edit Task");
        editItem.setOnAction(e -> CalendarTaskDialog.show(card.getScene().getWindow(), currentUser.getId(), task.getDeadlineDate(), task, () -> {
            renderCalendar();
            loadTasksSidebar();
        }));

        MenuItem removeItem = new MenuItem("🗑 Remove Task");
        removeItem.setOnAction(e -> handleRemoveTask(task));

        menu.getItems().addAll(detailsItem, editItem, new SeparatorMenuItem(), removeItem);

        // Right click on card
        card.setOnContextMenuRequested(e -> menu.show(card, e.getScreenX(), e.getScreenY()));
        optionsBtn.setOnAction(e -> menu.show(optionsBtn, javafx.geometry.Side.BOTTOM, 0, 0));

        // Left click on card opens Dedicated Task Details Dialog (no routine editor)
        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && !optionsBtn.isHover()) {
                TaskDetailDialog.show(card.getScene().getWindow(), currentUser.getId(), task, () -> {
                    renderCalendar();
                    loadTasksSidebar();
                });
            }
        });

        Tooltip.install(card, new Tooltip("Left-click for details • Right-click for options"));

        return card;
    }

    private void applyCardTheme(HBox card, Region stripe, Label subjectLabel, Label countdownLabel,
                                RoutineTaskItem.TaskColorTheme theme) {
        if (theme.isAsh()) {
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #cbd5e1; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-cursor: hand;");
            stripe.setStyle("-fx-background-color: #94a3b8; -fx-background-radius: 8px 0 0 8px;");
            subjectLabel.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 2px 6px;");
            countdownLabel.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 3px 6px;");
        } else {
            card.setStyle("-fx-background-color: " + theme.getCardBg() + "; -fx-background-radius: 8px; -fx-border-color: " + theme.getBorderColor() + "; -fx-border-radius: 8px; -fx-border-width: 1px; -fx-cursor: hand;");
            stripe.setStyle("-fx-background-color: " + theme.getAccentColor() + "; -fx-background-radius: 8px 0 0 8px;");
            subjectLabel.setStyle("-fx-background-color: " + theme.getBadgeBg() + "; -fx-text-fill: " + theme.getBadgeText() + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 2px 6px;");
            countdownLabel.setStyle("-fx-background-color: " + theme.getBadgeBg() + "; -fx-text-fill: " + theme.getBadgeText() + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 3px 6px;");
        }
    }

    private void handleRemoveTask(RoutineTaskItem task) {
        if (task == null || currentUser == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Task");
        confirm.setHeaderText("Remove task \"" + task.getActivityType() + "\"?");
        String details = "Subject: " + task.getSubjectName()
                + "\nDate: " + task.getFormattedDate() + " (" + task.getWeekdayDisplay() + ")"
                + "\nTime: " + task.getFormattedTime();
        confirm.setContentText(details);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            DatabaseHelper.deleteCalendarTask(task.getActivityId());
            DatabaseHelper.deleteRoutineActivity(task.getActivityId());
            renderCalendar();
            loadTasksSidebar();
        }
    }

    /**
     * Starts or refreshes the 1-second countdown timeline updating clock digits and dynamic colors.
     */
    private void startTaskCountdownTimeline() {
        if (taskCountdownTimeline != null) {
            taskCountdownTimeline.stop();
        }
        if (taskCardMap.isEmpty()) {
            return;
        }

        taskCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            for (Map.Entry<RoutineTaskItem, TaskCardNodes> entry : taskCardMap.entrySet()) {
                RoutineTaskItem task = entry.getKey();
                TaskCardNodes nodes = entry.getValue();
                if (nodes != null && nodes.countdownLabel != null && task != null) {
                    nodes.countdownLabel.setText(task.getFormattedClockCountdown());
                    RoutineTaskItem.TaskColorTheme theme = task.getColorTheme();
                    applyCardTheme(nodes.card, nodes.stripe, nodes.subjectLabel, nodes.countdownLabel, theme);
                }
            }
        }));
        taskCountdownTimeline.setCycleCount(Animation.INDEFINITE);
        taskCountdownTimeline.play();
    }

    /**
     * Initializes the static weekday headers (SUN - SAT) for the monthly calendar grid.
     */
    private void setupCalendarDayHeaders() {
        if (calendarDayHeaders == null) return;
        calendarDayHeaders.getChildren().clear();
        calendarDayHeaders.getColumnConstraints().clear();

        String[] dayNames = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            calendarDayHeaders.getColumnConstraints().add(col);

            Label header = new Label(dayNames[i]);
            header.getStyleClass().add("calendar-day-header");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            calendarDayHeaders.add(header, i, 0);
        }
    }

    /**
     * Sets up the interactive Month and Year selector dropdowns.
     */
    private void setupCalendarSelectors() {
        if (calendarMonthSelect == null || calendarYearSelect == null) return;

        calendarMonthSelect.getItems().setAll(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        );

        int currentYear = YearMonth.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int y = currentYear - 5; y <= currentYear + 10; y++) {
            years.add(y);
        }
        calendarYearSelect.getItems().setAll(years);

        calendarMonthSelect.setOnAction(e -> {
            if (!isUpdatingCalendarSelectors) {
                int selectedMonthIdx = calendarMonthSelect.getSelectionModel().getSelectedIndex();
                Integer selectedYear = calendarYearSelect.getValue();
                if (selectedMonthIdx >= 0 && selectedYear != null) {
                    currentCalendarMonth = YearMonth.of(selectedYear, selectedMonthIdx + 1);
                    renderCalendar();
                }
            }
        });

        calendarYearSelect.setOnAction(e -> {
            if (!isUpdatingCalendarSelectors) {
                int selectedMonthIdx = calendarMonthSelect.getSelectionModel().getSelectedIndex();
                Integer selectedYear = calendarYearSelect.getValue();
                if (selectedMonthIdx >= 0 && selectedYear != null) {
                    currentCalendarMonth = YearMonth.of(selectedYear, selectedMonthIdx + 1);
                    renderCalendar();
                }
            }
        });
    }

    @FXML
    public void handlePrevMonth() {
        currentCalendarMonth = currentCalendarMonth.minusMonths(1);
        renderCalendar();
    }

    @FXML
    public void handleNextMonth() {
        currentCalendarMonth = currentCalendarMonth.plusMonths(1);
        renderCalendar();
    }

    @FXML
    public void handleToday() {
        currentCalendarMonth = YearMonth.now();
        renderCalendar();
    }

    @FXML
    public void handleAddNewCalendarTask() {
        if (currentUser == null || calendarView == null) return;
        CalendarTaskDialog.show(
                calendarView.getScene().getWindow(),
                currentUser.getId(),
                LocalDate.now(),
                () -> {
                    renderCalendar();
                    loadTasksSidebar();
                }
        );
    }

    /**
     * Renders the interactive monthly Google Calendar-style view in calendarGrid.
     */
    public void renderCalendar() {
        if (calendarGrid == null) return;

        isUpdatingCalendarSelectors = true;
        try {
            if (calendarYearSelect != null) {
                int y = currentCalendarMonth.getYear();
                if (!calendarYearSelect.getItems().contains(y)) {
                    calendarYearSelect.getItems().add(y);
                    Collections.sort(calendarYearSelect.getItems());
                }
                calendarYearSelect.setValue(y);
            }
            if (calendarMonthSelect != null) {
                calendarMonthSelect.getSelectionModel().select(currentCalendarMonth.getMonthValue() - 1);
            }
        } finally {
            isUpdatingCalendarSelectors = false;
        }

        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 7.0);
            col.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(col);
        }

        // Fetch all calendar tasks & group by date
        Map<LocalDate, List<RoutineTaskItem>> tasksByDate = new HashMap<>();
        if (currentUser != null) {
            List<RoutineTaskItem> tasks = DatabaseHelper.getUserCalendarTasks(currentUser.getId());
            for (RoutineTaskItem t : tasks) {
                LocalDate d = t.getDeadlineDate();
                if (d != null) {
                    tasksByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(t);
                }
            }
            // Also legacy routine activities
            Map<String, RoutineSlot> slots = DatabaseHelper.getAllRoutineSlots(currentUser.getId());
            for (RoutineSlot slot : slots.values()) {
                if (slot.getActivities() != null) {
                    for (SpecialActivity activity : slot.getActivities()) {
                        if (activity.getDeadlineInfo() != null && !activity.getDeadlineInfo().trim().isEmpty()) {
                            RoutineTaskItem legacyItem = new RoutineTaskItem(
                                    activity.getId(),
                                    slot.getId(),
                                    slot.getSubjectName(),
                                    slot.getTeacherCode(),
                                    slot.getDayOfWeek(),
                                    slot.getTimeSlot(),
                                    activity.getActivityType(),
                                    activity.getDeadlineInfo()
                            );
                            LocalDate d = legacyItem.getDeadlineDate();
                            if (d != null) {
                                boolean exists = false;
                                for (RoutineTaskItem existing : tasksByDate.getOrDefault(d, Collections.emptyList())) {
                                    if (existing.getActivityId() == legacyItem.getActivityId()) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    tasksByDate.computeIfAbsent(d, k -> new ArrayList<>()).add(legacyItem);
                                }
                            }
                        }
                    }
                }
            }
        }

        LocalDate firstOfMonth = currentCalendarMonth.atDay(1);
        int daysInMonth = currentCalendarMonth.lengthOfMonth();
        // Sunday = 0, Monday = 1, ..., Saturday = 6
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;

        LocalDate today = LocalDate.now();

        int totalCells = (startDayOfWeek + daysInMonth <= 35) ? 35 : 42;
        LocalDate startDate = firstOfMonth.minusDays(startDayOfWeek);

        for (int i = 0; i < totalCells; i++) {
            LocalDate date = startDate.plusDays(i);
            int row = i / 7;
            int col = i % 7;

            boolean isCurrentMonth = date.getMonth().equals(currentCalendarMonth.getMonth());
            boolean isToday = date.equals(today);

            VBox cell = new VBox(4);
            cell.getStyleClass().add("calendar-day-cell");
            if (!isCurrentMonth) {
                cell.getStyleClass().add("calendar-day-cell-other-month");
            }
            VBox.setVgrow(cell, Priority.ALWAYS);

            // Day Header Row
            HBox dayHeader = new HBox();
            dayHeader.setAlignment(Pos.CENTER_LEFT);

            Label dayNumLabel = new Label(String.valueOf(date.getDayOfMonth()));
            if (isToday) {
                dayNumLabel.getStyleClass().add("calendar-day-today-badge");
            } else {
                dayNumLabel.getStyleClass().add("calendar-day-number");
                if (!isCurrentMonth) {
                    dayNumLabel.setStyle("-fx-text-fill: #94a3b8;");
                }
            }
            dayHeader.getChildren().add(dayNumLabel);

            // Task pills container
            VBox tasksBox = new VBox(3);
            tasksBox.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(tasksBox, Priority.ALWAYS);

            List<RoutineTaskItem> dayTasks = tasksByDate.get(date);
            if (dayTasks != null && !dayTasks.isEmpty()) {
                int displayLimit = 3;
                int shown = 0;
                for (RoutineTaskItem task : dayTasks) {
                    if (shown >= displayLimit) {
                        int remaining = dayTasks.size() - displayLimit;
                        Label moreLabel = new Label("+" + remaining + " more");
                        moreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6366f1; -fx-font-weight: bold; -fx-padding: 1px 4px;");
                        tasksBox.getChildren().add(moreLabel);
                        break;
                    }

                    RoutineTaskItem.TaskColorTheme theme = task.getColorTheme();
                    HBox pill = new HBox(4);
                    pill.setAlignment(Pos.CENTER_LEFT);
                    pill.getStyleClass().add("calendar-task-pill");
                    pill.setStyle("-fx-background-color: " + theme.getBadgeBg() + "; -fx-border-color: " + theme.getBorderColor() + "; -fx-border-radius: 4px; -fx-border-width: 0.8px;");

                    String taskText = (task.getSubjectName() != null && !task.getSubjectName().isEmpty() && !task.getSubjectName().equals("Activity"))
                            ? "[" + task.getSubjectName() + "] " + task.getActivityType()
                            : task.getActivityType();
                    Label pillLabel = new Label(taskText);
                    pillLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + theme.getBadgeText() + ";");
                    pillLabel.setMaxWidth(110);
                    pillLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

                    pill.getChildren().add(pillLabel);

                    // Clicking the pill opens TaskDetailDialog
                    pill.setOnMouseClicked(pe -> {
                        pe.consume();
                        TaskDetailDialog.show(calendarView.getScene().getWindow(), currentUser.getId(), task, () -> {
                            renderCalendar();
                            loadTasksSidebar();
                        });
                    });

                    tasksBox.getChildren().add(pill);
                    shown++;
                }
            }

            cell.getChildren().addAll(dayHeader, tasksBox);

            // Clicking the day cell opens CalendarTaskDialog for that date
            cell.setOnMouseClicked(ce -> {
                if (currentUser == null || calendarView == null) return;
                CalendarTaskDialog.show(
                        calendarView.getScene().getWindow(),
                        currentUser.getId(),
                        date,
                        () -> {
                            renderCalendar();
                            loadTasksSidebar();
                        }
                );
            });

            calendarGrid.add(cell, col, row);
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
