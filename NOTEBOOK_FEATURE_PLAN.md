# Implementation Plan: Notebook Feature (Header & Actions)

## 1. Overview
The goal of this task is to begin the implementation of the core **Notebook** feature in the **Main Menu**. Specifically, we will:
1. Ensure the `notebook` branch incorporates the latest work from `main` (where the routine was moved to the left sidebar and the main menu was prepared as a clean workspace).
2. Layout the Notebooks section in the top-left of the Main Menu with:
   - Header: **"Recent notebooks"**
   - Action Button: **"My Notebooks"**
   - Action Button: **"+ New Notebook"**
3. Create a placeholder content container underneath for the notebook cards/grid that will be populated in subsequent steps.

---

## 2. Architecture & Visual Layout

```
+-----------------------------------------------------------------------------------------+
| [Study Buddy 🎓]  [Dashboard]                                             [Sidebar ▤] [Log Out] |
+-----------------------------------------------------------------------------------------+
| [Sidebar] | Main Menu Workspace (mainMenuView)                                          |
|           |                                                                             |
|           |   [Recent notebooks]    [ 📚 My Notebooks ]    [ + New Notebook ]           |
|           |   ─────────────────────────────────────────────────────────────             |
|           |   (Container ready for recent notebook cards / empty state)                 |
|           |                                                                             |
+-----------------------------------------------------------------------------------------+
```

---

## 3. Step-by-Step Implementation Steps

### Step 0: Branch Synchronization
- **Action**: Merge `main` into `notebook` (`git merge main`).
- **Rationale**: The `notebook` branch was created before the sidebar routine migration (`9c6b44b`). Merging `main` ensures the dual-view workspace (`mainMenuView` and `routineView`) is available.

---

### Step 1: Layout Updates in `src/main/resources/com/example/study_buddy/hello-view.fxml`
In `mainMenuView`:
1. Change alignment to `alignment="TOP_LEFT"` with comfortable padding (`28px`).
2. Add a top header bar (`HBox alignment="CENTER_LEFT" spacing="16.0"`):
   - **Header Label**:
     - Text: `"Recent notebooks"`
     - Style: Bold, 22px font size, `#1e293b` text color.
   - **"My Notebooks" Button**:
     - `fx:id="myNotebooksBtn"`
     - Text: `"📚 My Notebooks"`
     - Action: `#handleMyNotebooks`
     - Style Class: `btn-secondary-action`
   - **"+ New Notebook" Button**:
     - `fx:id="newNotebookBtn"`
     - Text: `"+ New Notebook"`
     - Action: `#handleNewNotebook`
     - Style Class: `btn-primary` (accent gradient with drop shadow and hover effect)
3. Add a placeholder content container underneath:
   - `VBox fx:id="notebooksContentArea"` with subtle dashed border or clean container for upcoming cards.

---

### Step 2: Styling in `src/main/resources/com/example/study_buddy/styles.css`
Add styling for the secondary action button to complement `.btn-primary`:
```css
/* Secondary Header Action Button */
.btn-secondary-action {
    -fx-background-color: #ffffff;
    -fx-text-fill: #334155;
    -fx-font-weight: bold;
    -fx-font-size: 13px;
    -fx-background-radius: 8px;
    -fx-border-color: #cbd5e1;
    -fx-border-radius: 8px;
    -fx-border-width: 1.2px;
    -fx-padding: 8px 16px;
    -fx-cursor: hand;
}

.btn-secondary-action:hover {
    -fx-background-color: #f8fafc;
    -fx-border-color: #94a3b8;
    -fx-text-fill: #0f172a;
}
```

---

### Step 3: Controller Wiring in `src/main/java/com/example/study_buddy/HelloController.java`
1. Add `@FXML` injections:
   ```java
   @FXML private Button myNotebooksBtn;
   @FXML private Button newNotebookBtn;
   @FXML private VBox notebooksContentArea;
   ```
2. Add action handler stubs:
   ```java
   @FXML
   public void handleMyNotebooks() {
       // Placeholder for viewing all user notebooks
   }

   @FXML
   public void handleNewNotebook() {
       // Placeholder for opening create notebook modal/dialog
   }
   ```

---

## 4. Affected Files

| Component | File Path | Scope of Modification |
| :--- | :--- | :--- |
| **Git Branch** | Local repository | Merge `main` into `notebook` |
| **FXML View** | [hello-view.fxml](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/resources/com/example/study_buddy/hello-view.fxml) | Header bar with "Recent notebooks", "My Notebooks", and "+ New Notebook" |
| **Styles** | [styles.css](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/resources/com/example/study_buddy/styles.css) | `.btn-secondary-action` styles |
| **Controller** | [HelloController.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/HelloController.java) | Button injections and handler methods |

---

## 5. Verification Plan
1. **Compilation**: Run `.\mvnw.cmd clean compile` to ensure zero compilation or FXML loading errors.
2. **Visual Inspection**:
   - Run the application.
   - On the Main Menu, verify that "Recent notebooks" appears in the top-left.
   - Verify that "My Notebooks" and "+ New Notebook" buttons sit horizontally aligned next to it.
   - Check hover animations on both buttons.
   - Open Left Sidebar and switch to Routine, then switch back to Main Menu to ensure view switching remains smooth.
