# Implementation Plan: Academic Progress Tracker

> **Target Teacher Requirements**: 
> - **Requirement 3 (JavaFX UI Design)**: 2-level hierarchical navigation (`FlowPane` Course Cards &rarr; 2-Column Detail View), `PieChart`, `BarChart`, custom checkbox tree items, `ProgressBar`, modal dialogs.
> - **Requirement 4 (Layout Responsiveness)**: Responsive `FlowPane` wrap, dynamic `HGrow`/`VGrow` bindings across window resizes.
> - **Requirement 5 (Concurrency & Multi-threading)**: Multi-threaded background execution for PDF text parsing and Gemini AI calls using worker thread pool and `Platform.runLater()`.
> - **Requirement 6 (Database Integration)**: SQLite relational schema with 5 tables linked via `FOREIGN KEY` and `ON DELETE CASCADE`.
> - **Requirement 7 (CRUD Operations)**: Full CRUD across Courses, Syllabus Chapters/Topics, Marks, and Term Exam Dates.
> - **Requirement 8 (Networking & Remote JSON Parsing)**: Gemini REST API calls with multi-model failover for intelligent syllabus hierarchy extraction and term exam pacing forecasts.

---

## 1. User Interface & Screen Flow

### Entry Point (Left Navigation Sidebar)
Positioned in the Left Navigation Sidebar, directly below `🧠 Quiz`:
```xml
<Button fx:id="navProgressBtn" alignment="BASELINE_LEFT" maxWidth="Infinity" 
        onAction="#handleOpenProgress" styleClass="nav-item" text="📊  Progress" />
```

---

### Screen 1: Course Cards Gallery (Landing Screen)
When the user clicks `📊 Progress`, they first see the clean **Course Cards Gallery**:
- **Header**:
  - Title: `📚 Academic Courses`
  - Subtitle: `Select a course to view syllabus checklist, marks, and AI exam forecast`
  - Action Button: `+ Add Course` (opens dialog to enter Course Code & Course Title)
- **Course Cards Grid (`FlowPane`)**:
  - Each course card displays **strictly Course Code and Course Title**:
  
  ```
  +--------------------------------+   +--------------------------------+   +--------------------------------+
  |  CSE 2100                      |   |  CSE 2201                      |   |  MATH 2105                     |
  |  Object-Oriented Programming   |   |  Data Structures & Algorithms  |   |  Discrete Mathematics          |
  +--------------------------------+   +--------------------------------+   +--------------------------------+
  ```

---

### Screen 2: Course Progress Detail View (After Clicking a Course Card)
Clicking on any course card transitions smoothly to that course's detailed progress workspace:

```
+-----------------------------------------------------------------------------------------------------------------+
| TOP BAR: ◀ Back to Courses   |   CSE 2100 - Object-Oriented Programming                                          |
|                              [ 📄 Upload Syllabus PDF ]  [ 📅 Set Term Exam Date ]  [ ➕ Add Marks ]             |
+-----------------------------------------------------------------------------------------------------------------+
|                                                        |                                                        |
| LEFT COLUMN (55% Width): SYLLABUS HIERARCHY            | RIGHT COLUMN (45% Width): MARKS & FORECAST             |
|                                                        |                                                        |
| Overall Completion: [====================] 68%         | 📝 ASSESSMENT MARKS SUMMARY                            |
| 12 of 18 Topics Completed                              | • CT 1: 18.0 / 20.0          (90%)                     |
|                                                        | • CT 2: 19.5 / 20.0          (97%)                     |
| 📖 Chapter 1: Introduction to OOP & Java (3/3)         | • CT Assignment: 24.0 / 25.0 (96%)                     |
|   ☑ Classes, Objects & Methods                         | • Lab Test 1: 28.0 / 30.0    (93%)                     |
|   ☑ Encapsulation & Access Modifiers                   | • Lab Quiz: 9.0 / 10.0       (90%)                     |
|   ☑ Constructors & this keyword                        |                                                        |
|                                                        | 📊 VISUAL JAVAFX PROGRESS GRAPH                        |
| 📖 Chapter 2: Inheritance & Interfaces (2/3)           | +----------------------------------------------------+ |
|   ☑ Subclasses & super keyword                         | |     [PieChart: 68% Completed / 32% Remaining]      | |
|   ☑ Abstract Classes vs Interfaces                     | +----------------------------------------------------+ |
|   ☐ Multiple Interface Polymorphism                    |                                                        |
|                                                        | 🤖 AI TERM EXAM STRATEGY FORECAST                      |
| 📖 Chapter 3: Multi-threading & Concurrency (1/4)      | +----------------------------------------------------+ |
|   ☑ Thread Creation & Lifecycle                        | | ⏳ 22 Days until Term Final Exam                    | |
|   ☐ Thread Pools (ExecutorService)                     | | 📚 6 Topics Remaining (32% of syllabus)            | |
|   ☐ Producer-Consumer with BlockingQueue               | | 💡 AI Advice: Study ~1 topic every 3.5 days.       | |
|   ☐ JavaFX Platform.runLater() Synchronization         | |    Prioritize Chapter 3 (Concurrency) next.        | |
|                                                        | +----------------------------------------------------+ |
+-----------------------------------------------------------------------------------------------------------------+
```

---

## 2. Database Schema Additions ([DatabaseHelper.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/DatabaseHelper.java))

Five relational SQLite tables linked with cascading foreign keys:

```sql
-- 1. Courses Table
CREATE TABLE IF NOT EXISTS courses (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    course_code TEXT NOT NULL,
    course_title TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 2. Syllabus Chapters for a Course
CREATE TABLE IF NOT EXISTS syllabus_chapters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id INTEGER NOT NULL,
    chapter_number INTEGER NOT NULL,
    title TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 3. Individual Topics under each Chapter
CREATE TABLE IF NOT EXISTS syllabus_topics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chapter_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    is_completed INTEGER DEFAULT 0,
    completed_at TIMESTAMP,
    FOREIGN KEY(chapter_id) REFERENCES syllabus_chapters(id) ON DELETE CASCADE
);

-- 4. Academic Marks (CT, Assignment, Lab Test, Lab Quiz)
CREATE TABLE IF NOT EXISTS academic_marks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id INTEGER NOT NULL,
    assessment_type TEXT NOT NULL, -- 'CT', 'CT Assignment', 'Lab Test', 'Lab Quiz'
    assessment_name TEXT NOT NULL, -- e.g. 'CT 1', 'Lab Test 2'
    obtained_marks REAL NOT NULL,
    total_marks REAL NOT NULL,
    exam_date TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- 5. Term Exam Target Date Configuration
CREATE TABLE IF NOT EXISTS term_exam_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id INTEGER NOT NULL UNIQUE,
    exam_date TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(course_id) REFERENCES courses(id) ON DELETE CASCADE
);
```

---

## 3. Core Logic & Implementation Details

### A. PDF Syllabus Text Extraction
- Uses **Apache PDFBox (`org.apache.pdfbox:pdfbox:3.0.2`)** added to [pom.xml](file:///c:/Users/User/IdeaProjects/Study_Buddy/pom.xml).
- Extracts text from uploaded `.pdf` files locally in milliseconds. Text/Markdown files are read directly via standard UTF-8 stream.

### B. Gemini AI Hierarchy Extraction ([GeminiApiService.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/GeminiApiService.java))
- The extracted text is sent to Google Gemini with a structured JSON schema instruction:
  ```json
  {
    "chapters": [
      {
        "chapterNumber": 1,
        "title": "Chapter Title",
        "topics": ["Topic A", "Topic B", "Topic C"]
      }
    ]
  }
  ```
- Uses our robust multi-model failover chain (`gemini-3.8-flash` &rarr; `gemini-3.6-flash` &rarr; `gemini-3.5-flash-lite`) to avoid 429 quota spikes.
- Parsed using Jackson `ObjectMapper` and saved directly to `syllabus_chapters` and `syllabus_topics`.

### C. Checkbox State Persistence & Live Chart Updates
- When a topic checkbox is clicked (`[✓] Studied`):
  - Updates `is_completed` in SQLite.
  - Automatically updates the progress bar, chapter counters, and JavaFX `PieChart` in real time.

### D. Assessment Marks Tracker Dialog
- Dialog allowing user to enter:
  - Assessment Type: `CT`, `CT Assignment`, `Lab Test`, `Lab Quiz`
  - Name: e.g. `CT 1`
  - Obtained Score & Total Marks (e.g. `18.5` / `20.0`)
- Renders assessment cards with percentage calculations.

### E. AI Term Exam Forecast
- Calculates days remaining until the Term Final Exam.
- Gemini generates a concise, personalized strategy based on remaining unstudied topics and days left.

---

## 4. Execution Steps

| Step | Component | Description |
|:---:|---|---|
| **1** | [pom.xml](file:///c:/Users/User/IdeaProjects/Study_Buddy/pom.xml) | Add `org.apache.pdfbox:pdfbox:3.0.2` dependency for PDF extraction. |
| **2** | [DatabaseHelper.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/DatabaseHelper.java) | Implement 5 new tables (`courses`, `syllabus_chapters`, `syllabus_topics`, `academic_marks`, `term_exam_config`) and complete CRUD operations. |
| **3** | Domain Models | Create `Course.java`, `SyllabusChapter.java`, `SyllabusTopic.java`, `AcademicMark.java`. |
| **4** | [GeminiApiService.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/GeminiApiService.java) | Add `parseSyllabusHierarchy()` and `generateExamForecast()` with multi-model failover. |
| **5** | [hello-view.fxml](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/resources/com/example/study_buddy/hello-view.fxml) & [styles.css](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/resources/com/example/study_buddy/styles.css) | Add `navProgressBtn` under `navQuizBtn`, `progressView` with Course Cards Gallery (`FlowPane`) and Course Detail View. |
| **6** | [HelloController.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/HelloController.java) | Implement navigation handlers, course creation dialog, card click transitions, checkbox listeners, and JavaFX charts. |
| **7** | Verification | Run `mvn test` and test full flow (Add course &rarr; Upload PDF syllabus &rarr; Check topics &rarr; Add marks &rarr; View AI forecast). |

---

## 5. Verification Plan

1. **Automated Unit Tests**:
   - Run `$env:JAVA_HOME = "C:\Users\User\.jdks\ms-21.0.12"; .\mvnw.cmd test` ensuring all tests pass.
2. **Course Gallery Test**:
   - Click `+ Add Course`, enter `CSE 2100` and `Object-Oriented Programming`.
   - Verify card displays strictly **Course Code** and **Course Title**.
3. **Card Click & Transition Test**:
   - Click course card; verify smooth transition to detailed Course Progress Workspace.
   - Verify `◀ Back to Courses` returns to the gallery.
4. **Syllabus PDF Extraction & Hierarchy Test**:
   - Upload sample PDF; verify AI parses chapters and topics into the checklist.
5. **Interactive Checkbox Test**:
   - Check and uncheck topics; verify immediate SQLite persistence and chart updates.
6. **Marks Tracker Test**:
   - Add CT 1 (18/20) and Lab Test (28/30); verify summary displays correctly.
7. **AI Term Exam Forecast Test**:
   - Set exam date; verify AI advice generates based on remaining unstudied topics.
