# Implementation Plan: AI Quiz System (MCQ & Short Answer with Gemini API)

> **Document Version**: 1.0  
> **Target Requirement**: Requirement 8 (Networking & Remote JSON Parsing), Requirement 3 (BorderPane UI Design), Requirement 4 (Responsiveness), and Requirement 5 (Concurrency).

---

## 1. Executive Summary & Evaluation Mechanism

### How Evaluation Works: The 2-Call Model
You asked:
> *"How the evaluation will be done? by submit button and later different api call? total 2 calls per quize?"*

**Answer:** **Yes, exactly! It is a clean, cost-effective 2-call model:**

```
                    ┌────────────────────────────────────────────────────────┐
                    │                      STUDENT UI                        │
                    └────────────────────────────────────────────────────────┘
                                    │                           ▲
                 1. User clicks     │                           │ 4. Render Quiz
                 "Generate Quiz"    ▼                           │    in BorderPane
                    ┌────────────────────────┐                  │
                    │   CALL #1: GENERATION  │──────────────────┘
                    │  (Gemini REST API)     │
                    └────────────────────────┘
                                    │
                                    ▼ [Student solves MCQs & types Short Answers]
                                    │
                 2. User clicks     │
                 "Submit Quiz"      ▼
                    ┌────────────────────────────────────────────────────────┐
                    │               SUBMISSION & EVALUATION                  │
                    │                                                        │
                    │  ┌──────────────────────┐  ┌────────────────────────┐  │
                    │  │   MCQ Questions      │  │ Short Answer Questions │  │
                    │  │  (Graded LOCALLY in  │  │   (Requires AI grader) │  │
                    │  │   Java, 0ms latency) │  │                        │  │
                    │  └──────────────────────┘  └───────────┬────────────┘  │
                    │                                        │               │
                    │                             3. Send    ▼               │
                    │                             Student Responses          │
                    │                            ┌────────────────────────┐  │
                    │                            │   CALL #2: AI GRADING  │  │
                    │                            │   (Gemini REST API)    │  │
                    │                            └───────────┬────────────┘  │
                    │                                        │               │
                    │                                        ▼               │
                    │                             Returns Scores & Feedback  │
                    └────────────────────────────────────────┼───────────────┘
                                                             │
                                                             ▼
                                     ┌────────────────────────────────────────────────┐
                                     │  5. Display Comprehensive Results & Feedback   │
                                     └────────────────────────────────────────────────┘
```

### Call Breakdown:
1. **Call #1 (Quiz Generation)**:
   - Triggered when the user clicks **"Generate Quiz"**.
   - Sends: Question count, difficulty level (`Easy`, `Medium`, `Hard`), question types (`MCQ`, `Short Answer`, or `Both`), user prompt/topic, and optional source context (uploaded file text or extracted notes from a Study Buddy notebook page).
   - Returns: Structured JSON containing question prompts, 4 options per MCQ with the correct index, and rubric/model answers for short answer questions.
2. **Call #2 (Short Answer Grading)**:
   - Triggered when the user clicks **"Submit Quiz"**.
   - **MCQs**: Graded **instantly and locally** in Java code ($O(1)$) by checking the student's selected card index against `correctIndex`. Zero latency, zero cost!
   - **Short Answers**: Study Buddy bundles the questions, reference rubrics, and the student's typed responses into a single grading request to Gemini.
   - Returns: Structured JSON with numeric scores (e.g. out of 5) and constructive qualitative feedback for each question.
   - *Note*: If the user creates an "MCQ Only" quiz, Call #2 is skipped entirely (1 call total). If short answers are included, it is **2 calls total per quiz**.

---

## 2. Architecture & Design Alignment

### Fulfilling Teacher's Academic Requirements

| Teacher Requirement | How the AI Quiz System Delivers It |
|---|---|
| **Requirement 8: Networking & JSON Parsing** | Uses Java 21's native `java.net.http.HttpClient` to make asynchronous HTTPS POST requests to Google Gemini REST endpoint, sending JSON payloads and deserializing JSON responses using `Jackson` (`ObjectMapper`). |
| **Requirement 3: JavaFX UI Design (BorderPane)** | The entire Quiz view is built using a **`BorderPane`**: <br>• `Top`: Quiz title, countdown/progress indicator, settings & back buttons. <br>• `Center`: Scrollable question viewport (MCQ 4-card grid + Short Answer text areas). <br>• `Bottom`: Action bar with "Submit Quiz" button, answered counter, and score display. |
| **Requirement 5: Concurrency** | All network I/O executes asynchronously on background worker threads (`ExecutorService` / `CompletableFuture`). The JavaFX Application Thread is never blocked; loading spinners animate smoothly, and UI updates are safely dispatched via `Platform.runLater()`. |
| **Requirement 4: Responsiveness** | Question cards and MCQ choices adapt dynamically to window resizing using percentage widths and wrapping. |
| **Requirement 6 & 7: Database CRUD** | Saved quiz results can be logged to SQLite (`quizzes` and `quiz_history` tables), letting students review past performance. |

---

## 3. Data Models & JSON Schemas

### A. Quiz Question Model (`QuizQuestion.java`)
```java
public class QuizQuestion {
    public enum Type { MCQ, SHORT_ANSWER }

    private Type type;
    private String questionText;
    
    // MCQ fields
    private List<String> options;      // Exactly 4 options: [A, B, C, D]
    private int correctIndex;          // 0 to 3
    private String explanation;
    private Integer userSelectedOption; // student choice (-1 or null if unanswered)
    
    // Short Answer fields
    private String rubric;             // Key points expected in answer
    private String studentAnswer;      // User typed response
    private int maxScore;              // e.g., 5
    private int awardedScore;          // graded by AI (0 to maxScore)
    private String aiFeedback;         // explanation of points given/deducted
}
```

### B. Gemini API Schemas
**Call 1 Request Prompt:**
```json
{
  "contents": [{
    "parts": [{
      "text": "Generate a 5-question quiz (3 MCQ, 2 Short Answer) on 'Java Concurrency' with Medium difficulty. Format strictly as JSON."
    }]
  }],
  "generationConfig": {
    "responseMimeType": "application/json",
    "temperature": 0.3
  }
}
```

**Call 1 Response JSON Schema:**
```json
{
  "title": "Java Concurrency & Threading Quiz",
  "questions": [
    {
      "type": "MCQ",
      "question": "Which interface represents a task that can return a value and throw an exception?",
      "options": ["Runnable", "Callable", "Thread", "Future"],
      "correctIndex": 1,
      "explanation": "Callable<V> has a call() method returning V, unlike Runnable's run() which is void."
    },
    {
      "type": "SHORT_ANSWER",
      "question": "Explain the difference between synchronized blocks and ReentrantLock in Java.",
      "rubric": "Mentions intrinsic locking vs explicit locking, tryLock() capability, fairness policies, and interruptibility.",
      "maxScore": 5
    }
  ]
}
```

**Call 2 Grading Request JSON Schema:**
```json
{
  "contents": [{
    "parts": [{
      "text": "Grade the following student answers against the questions and rubrics. Return a JSON array with awarded score (0 to maxScore) and helpful feedback."
    }]
  }],
  "generationConfig": {
    "responseMimeType": "application/json"
  }
}
```

---

## 4. UI/UX Design (BorderPane & Modern Glassmorphism)

### Layout Wireframe (`BorderPane fx:id="quizView"`)
```
+-----------------------------------------------------------------------------------+
| TOP: [◀ Back]  [🧠 AI Quiz Generator]       [Progress: 4/5 Answered]  [⚙️ API Key] |
+-----------------------------------------------------------------------------------+
| CENTER: Scrollable Quiz Viewport                                                  |
|                                                                                   |
|  [ Card 1: Question Setup / Configuration Screen (Before Generation) ]            |
|    - Number of Questions: [ 5  ▼ ]     Difficulty: [ Medium ▼ ]                   |
|    - Question Types: [ (•) Both   ( ) MCQ Only   ( ) Short Answer Only ]          |
|    - Source Material: [ 📎 Upload File (.txt, .pdf) ]  or  [ 📚 Select Notebook Page ] |
|    - Custom Topic / Prompt: [ Enter topics or paste study notes here...        ]  |
|    - [ 🚀 Generate Quiz with AI ] (Shows sleek animated loader during Call #1)   |
|                                                                                   |
|  -- OR (During Quiz Playback) --                                                  |
|                                                                                   |
|  [ Question 1 of 5 (MCQ) ]                                                        |
|  "Which interface represents a task that can return a value?"                     |
|  +--------------------------------+  +--------------------------------+          |
|  | [A] Runnable                   |  | [B] Callable       ✓ (Selected)|          |
|  +--------------------------------+  +--------------------------------+          |
|  | [C] Thread                     |  | [D] Future                     |          |
|  +--------------------------------+  +--------------------------------+          |
|                                                                                   |
|  [ Question 2 of 5 (Short Answer) ]                                               |
|  "Explain the difference between synchronized blocks and ReentrantLock."         |
|  +----------------------------------------------------------------------------+  |
|  | TextArea: "ReentrantLock allows tryLock and lockInterruptibly..."          |  |
|  +----------------------------------------------------------------------------+  |
|                                                                                   |
|  -- OR (After Submit: Results & Review Mode) --                                   |
|  [ 🎉 Score: 18 / 20 (90%) - Grade A | MCQ: 3/3 | Short Answer: 15/17 ]           |
|  (MCQ cards show Green/Red highlighting with explanations)                         |
|  (Short Answer cards show Score Pill: "4/5" + "🤖 AI Feedback: Great analysis...") |
+-----------------------------------------------------------------------------------+
| BOTTOM: [ Reset Quiz ]          [ Status: 5 of 5 answered ]       [ 📤 Submit Quiz ]|
+-----------------------------------------------------------------------------------+
```

---

## 5. Detailed Step-by-Step Implementation Steps

### Phase 1: Dependencies & Configuration
1. Update `pom.xml`:
   - Add `com.fasterxml.jackson.core:jackson-databind` (`2.17.2`) for clean JSON parsing, object mapping, and payload formatting.
2. Create `ApiKeyManager.java` or `SettingsService.java`:
   - Manages Gemini API Key securely (checks environment variable `GEMINI_API_KEY` or persists user-entered key in `study_buddy_data/settings.properties`).
   - Includes a sleek API Key input dialog with a direct link to Google AI Studio (free key generation).

### Phase 2: Core Networking & Data Models
1. Create `QuizQuestion.java`:
   - Enums and properties for MCQ options, selections, correct index, rubric, and short answer feedback.
2. Create `QuizSession.java`:
   - Encapsulates the active quiz, metadata, question list, submission state, and score calculation logic.
3. Create `GeminiApiService.java`:
   - Uses `java.net.http.HttpClient` with asynchronous HTTP POST requests.
   - `generateQuiz(QuizConfig config)`: Call #1 (Generates quiz JSON and maps to `QuizSession`).
   - `gradeShortAnswers(QuizSession session)`: Call #2 (Submits short answer responses and parses grading JSON).
   - Handles network timeouts, invalid keys, and rate limits gracefully with user-friendly error banners.

### Phase 3: UI Construction with BorderPane
1. Add `navQuizBtn` to the left navigation sidebar in `hello-view.fxml`.
2. Add `<BorderPane fx:id="quizView" ...>` to `centerWorkspace` in `hello-view.fxml`.
3. Build the interactive UI components:
   - **Config View**: Inputs for question count, difficulty, topic prompt, and source picker (file upload or notebook page extractor).
   - **Quiz View**: Dynamic question builder with 4-card MCQ selection (custom CSS cards with hover/active states) and styled `TextArea` for short answers.
   - **Submission & Results View**: Score breakdown header, color-coded answers, explanations, and AI grading remarks.

### Phase 4: Controller Integration & Concurrency
1. Update `HelloController.java`:
   - Add `handleOpenQuiz()` view toggle.
   - Bind quiz generation and submission to background worker threads.
   - Integrate "Pull from current Notebook Page" allowing the student to directly quiz themselves on their open notebook notes!
2. Add CSS styles in `styles.css` for quiz cards, option selection effects, score badges, and result highlights.

### Phase 5: Verification & Testing
1. Create unit tests in `src/test/java/com/example/study_buddy/QuizServiceTest.java`:
   - Test JSON deserialization of Call 1 quiz schemas.
   - Test local MCQ score evaluation ($O(1)$ verification).
   - Test JSON deserialization of Call 2 grading responses.
2. Verify application builds cleanly with `mvn clean test`.
3. Update `checklist.md` to mark:
   - **Requirement 8 (Networking & Remote JSON Parsing)**: 🟢 **Complete**
   - **Requirement 3 (JavaFX UI Design - BorderPane)**: 🟢 **Complete**

---

## 6. Approval Gate
Per repository rules (`AGENTS.md`), code modifications will begin only after your explicit review and confirmation.
