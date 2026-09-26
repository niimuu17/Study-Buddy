# Implementation Plan: Course Cards 3-Dot (⋮) Action Menu (Edit & Delete)

> **Feature Request**: Add a 3-dot button on the right side of each course card providing **Edit** and **Delete** options.  
> **Evaluation**: This is an **excellent, intuitive, and standard design pattern** (used in Google Classroom, Notion, Trello, Canvas). It provides immediate visual affordance for course management without requiring users to discover secondary right-click menus, and cleanly completes the full CRUD lifecycle (Create, Read, Update, Delete) for Courses.

---

## 1. Visual & Interactive Design

### A. Course Card Layout
The course card layout in the Courses Gallery (`FlowPane`) will feature the 3-dot button on the top-right of each card:

```
+---------------------------------------------+
|  [ CSE 2100 ]                           ⋮   |  <-- Top Row: Code Badge (left) & 3-Dot Menu (right)
|                                             |
|  Object-Oriented Programming                |  <-- Card Title
+---------------------------------------------+
```

When the user clicks `⋮`, a contextual menu appears directly below the button:

```
+---------------------------------------------+
|  [ CSE 2100 ]                           ⋮   |
|                                        +-------------------+
|  Object-Oriented Programming           | ✏️ Edit Course     |
|                                        | 🗑️ Delete Course   |
+----------------------------------------+-------------------+
```

### B. Event Propagation Isolation (Critical UX)
- Clicking anywhere on the card opens the detailed progress workspace (`openCourseProgressDetail(c)`).
- When the user clicks the 3-dot button `⋮`, event propagation is stopped via `event.consume()`. This guarantees opening the menu will **never** accidentally trigger card navigation.

---

## 2. Technical Implementation Details

### A. Database Layer: `updateCourse` ([DatabaseHelper.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/DatabaseHelper.java))
Add method to update course code and title in SQLite:
```java
public static boolean updateCourse(int courseId, String courseCode, String courseTitle) {
    String sql = "UPDATE courses SET course_code = ?, course_title = ? WHERE id = ?";
    try (Connection conn = getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setString(1, courseCode != null ? courseCode.trim() : "");
        pstmt.setString(2, courseTitle != null ? courseTitle.trim() : "");
        pstmt.setInt(3, courseId);
        return pstmt.executeUpdate() > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
}
```

---

### B. UI & Controller Layer ([HelloController.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/HelloController.java))

1. **Card Building (`loadAndRenderCourses`)**:
   - In `topRow`: Add `Button dotsBtn = new Button("⋮");` with style class `course-card-dots-btn`.
   - Wire `dotsBtn.setOnAction` to open `ContextMenu` with `✏️ Edit Course` and `🗑️ Delete Course`.
   - Wire `dotsBtn.setOnMouseClicked(Event::consume)` to prevent card navigation on click.

2. **Edit Course Dialog (`handleEditCourseDialog(Course c)`)**:
   - Displays a clean JavaFX `Dialog` pre-filled with the current `courseCode` and `courseTitle`.
   - On confirmation:
     - Calls `DatabaseHelper.updateCourse(c.getId(), newCode, newTitle)`.
     - Re-renders the gallery with updated details.
     - If the user is currently viewing this course, updates the detail view header.

3. **Delete Course Confirmation (`handleDeleteCourseConfirm(Course c)`)**:
   - Displays a confirmation alert: *"Delete course '[Course Code]'? This will permanently remove this course and all associated syllabus data, marks, and exam settings."*
   - On confirmation:
     - Calls `DatabaseHelper.deleteCourse(c.getId())` (cascading foreign keys cleanly delete related records in SQLite).
     - Re-renders the course cards gallery.

---

### C. Styling ([styles.css](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/resources/com/example/study_buddy/styles.css))
Add dedicated CSS for the 3-dot button:
```css
.course-card-dots-btn {
    -fx-background-color: transparent;
    -fx-text-fill: #94a3b8;
    -fx-font-size: 16px;
    -fx-font-weight: bold;
    -fx-cursor: hand;
    -fx-padding: 0 6px;
    -fx-background-radius: 4px;
}

.course-card-dots-btn:hover {
    -fx-background-color: #f1f5f9;
    -fx-text-fill: #334155;
}
```

---

## 3. Files to Modify

| File | Changes |
|---|---|
| [DatabaseHelper.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/DatabaseHelper.java) | Add `updateCourse(int courseId, String courseCode, String courseTitle)`. |
| [HelloController.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/HelloController.java) | Add 3-dot button to `loadAndRenderCourses()`, implement `handleEditCourseDialog()` and `handleDeleteCourseConfirm()`. |
| [styles.css](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/resources/com/example/study_buddy/styles.css) | Add styling rules for `.course-card-dots-btn` and hover state. |
| [ProgressServiceTest.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/test/java/com/example/study_buddy/ProgressServiceTest.java) | Add unit test verifying `DatabaseHelper.updateCourse()`. |

---

## 4. Verification Plan

1. **Automated Unit Tests**:
   - Run `$env:JAVA_HOME = "C:\Users\User\.jdks\ms-21.0.12"; .\mvnw.cmd test` to ensure all 28+ existing and new tests pass.
2. **Card Rendering Test**:
   - Verify each course card displays the course code badge on the left and the 3-dot button (`⋮`) on the right.
3. **Edit Course Action Test**:
   - Click `⋮` &rarr; click `✏️ Edit Course`.
   - Change code to `CSE 2101` and title to `Advanced OOP & Software Engineering`.
   - Save & verify the card updates immediately in the gallery and database.
4. **Delete Course Action Test**:
   - Click `⋮` &rarr; click `🗑️ Delete Course`.
   - Verify confirmation alert appears.
   - Confirm deletion & verify the card and its syllabus data are removed.
5. **Click Event Isolation Test**:
   - Click `⋮` button &rarr; verify the menu opens **without** navigating into the course detail view.
