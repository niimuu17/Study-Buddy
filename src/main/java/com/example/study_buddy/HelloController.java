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
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.Transition;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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
            VBox card = createNotebookCard(nb);
            notebooksGrid.getChildren().add(card);
        }
    }

    /**
     * Constructs a modern interactive card for a single notebook.
     */
    private VBox createNotebookCard(Notebook notebook) {
        VBox card = new VBox();
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

        // Header Row: Color dot + Title + Context Menu (Edit, Delete)
        HBox headerRow = new HBox(6);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label("●");
        dot.setStyle("-fx-font-size: 12px; -fx-text-fill: " + themeColor + ";");

        Label titleLabel = new Label(notebook.getTitle());
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        // Menu button (3 dots)
        Button optionsBtn = new Button("⋮");
        optionsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0 3px; -fx-cursor: hand;");

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

        headerRow.getChildren().addAll(dot, titleLabel, optionsBtn);

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
        card.getChildren().addAll(accentStripe, body);

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

        Page firstPageToSelect = null;
        Topic firstPageTopic = null;

        for (Topic topic : topics) {
            VBox topicSection = new VBox(4);

            // Topic Header Bar
            HBox topicHeader = new HBox(6);
            topicHeader.setAlignment(Pos.CENTER_LEFT);
            topicHeader.getStyleClass().add("topic-header");

            Label folderIcon = new Label("📂");
            folderIcon.setStyle("-fx-font-size: 13px;");

            Label topicTitle = new Label(topic.getTitle());
            topicTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            HBox.setHgrow(topicTitle, Priority.ALWAYS);

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

            topicHeader.getChildren().addAll(folderIcon, topicTitle, addPageBtn, addFileBtn, optionsBtn);

            // Pages & Files Container (Indented)
            VBox childrenBox = new VBox(2);
            childrenBox.setPadding(new Insets(2, 0, 4, 16));

            // List Pages
            for (Page page : topic.getPages()) {
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
                renamePageItem.setOnAction(e -> promptRenamePage(topic, page));

                MenuItem deletePageItem = new MenuItem("🗑 Delete Page");
                deletePageItem.setOnAction(e -> deletePage(topic, page));

                pageMenu.getItems().addAll(renamePageItem, new SeparatorMenuItem(), deletePageItem);
                pageMenuBtn.setOnAction(e -> pageMenu.show(pageMenuBtn, javafx.geometry.Side.BOTTOM, 0, 0));

                pageRow.getChildren().addAll(pageIcon, pageTitleLabel, pageMenuBtn);

                pageRow.setOnMouseClicked(e -> {
                    if (e.getTarget() != pageMenuBtn && !pageMenuBtn.isHover()) {
                        selectPage(topic, page);
                    }
                });

                childrenBox.getChildren().add(pageRow);
            }

            // List Attached Files (PDF, PPTX, etc.)
            for (TopicFile file : topic.getFiles()) {
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
                openItem.setOnAction(e -> handleOpenFile(file));

                MenuItem openWithItem = new MenuItem("⚙ Choose App (Open With...)");
                openWithItem.setOnAction(e -> handleOpenWith(file));

                MenuItem showInExplorerItem = new MenuItem("📁 Show in Explorer");
                showInExplorerItem.setOnAction(e -> handleShowInExplorer(file));

                MenuItem deleteFileItem = new MenuItem("🗑 Remove File");
                deleteFileItem.setOnAction(e -> {
                    DatabaseHelper.deleteTopicFile(file.getId());
                    loadTopicsExplorer();
                });

                fileMenu.getItems().addAll(openItem, openWithItem, showInExplorerItem, new SeparatorMenuItem(), deleteFileItem);
                fileMenuBtn.setOnAction(e -> fileMenu.show(fileMenuBtn, javafx.geometry.Side.BOTTOM, 0, 0));

                fileRow.getChildren().addAll(fileIcon, fileNameLabel, sizeLabel, fileMenuBtn);

                fileRow.setOnContextMenuRequested(e -> fileMenu.show(fileRow, e.getScreenX(), e.getScreenY()));

                fileRow.setOnMouseClicked(e -> {
                    if (e.getTarget() != fileMenuBtn && !fileMenuBtn.isHover()) {
                        handleOpenFile(file);
                    }
                });

                childrenBox.getChildren().add(fileRow);
            }

            topicSection.getChildren().addAll(topicHeader, childrenBox);
            topicsListContainer.getChildren().add(topicSection);
        }

        // Maintain or initialize page selection
        if (currentPage != null) {
            renderPageCanvas(currentPage);
        } else if (firstPageToSelect != null) {
            selectPage(firstPageTopic, firstPageToSelect);
        } else {
            showEmptyPlaygroundState();
        }
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
                DatabaseHelper.createTopic(currentNotebook.getId(), clean);
                loadTopicsExplorer();
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
                    DatabaseHelper.updatePage(page.getId(), updatedTitle, page.getContentJson());
                    if (workspaceStatusLabel != null) workspaceStatusLabel.setText("Title saved ✓");
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
        if (currentPage == null) return;
        String json = PageBlock.serializeList(currentPageBlocks);
        currentPage.setContentJson(json);
        DatabaseHelper.updatePage(currentPage.getId(), currentPage.getTitle(), json);
        if (workspaceStatusLabel != null) {
            workspaceStatusLabel.setText("Saved ✓ " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        }
    }

    private void saveCurrentPageBlocksQuietly() {
        if (currentPage == null) return;
        String json = PageBlock.serializeList(currentPageBlocks);
        currentPage.setContentJson(json);
        DatabaseHelper.updatePage(currentPage.getId(), currentPage.getTitle(), json);
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

        Label title = new Label("No Page Selected");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label sub = new Label("Select a page from the Topics Explorer on the left, or add a new page to begin writing.");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        emptyPrompt.getChildren().addAll(icon, title, sub);
        pagePlaygroundContainer.getChildren().add(emptyPrompt);
    }

    /**
     * Attaches one or more files to a topic.
     */
    private void attachFileToTopic(Topic topic) {
        if (topic == null || currentNotebook == null) return;
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
