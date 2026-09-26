# Implementation Plan: Progress Tracker Refinements & Enhancements

> **Target Requirements**:
> 1. **Syllabus Action Menu (Add & Delete)**: Clicking `Upload Syllabus` reveals contextual actions for `➕ Add Syllabus` and `🗑️ Delete Syllabus`. Supports **Pictures (`.png`, `.jpg`, `.jpeg`)**, **PDF (`.pdf`)**, **Word Documents (`.doc`, `.docx`)**, **Presentations (`.pptx`, `.ppt`)**, and **Text/Markdown (`.txt`, `.md`)**.
> 2. **Text & Label Streamlining**:
>    - Add marks dialog: `"Add Assessment Marks"` &rarr; `"Add Marks"`, `"Enter assessment marks for..."` &rarr; `"Enter marks for..."`, `"Assessment Type:"` &rarr; `"Type:"`, `"Assessment Name:"` &rarr; `"Name:"`.
>    - **Type Suggestions**: In the Type dropdown/suggestion box, include: **`CT`**, **`Assignment`**, **`Spot test`**, **`Lab test`**, **`Lab task`**, **`Lab quiz`**.
>    - UI Card 1: `"📝 Assessment Marks"` &rarr; `"📝 Marks"`.
> 3. **Term Exam Forecast & Expandable Toggle**:
>    - Header title: `"🤖 AI Term Exam Forecast"` &rarr; `"🤖 Term exam Forecast"`.
>    - Expandable toggle: Implement `See More ▾` / `See Less ▴` button (no eye icons, clean styling) so long forecasts never get cut off.

---

## 1. Requirement Breakdown & Technical Design

### A. Syllabus Action Menu (Add & Delete) & Multi-Format Parsing
- **Button Behavior (`uploadSyllabusBtn`)**:
  - Button text: `"📄 Syllabus ▾"`
  - Clicking displays a JavaFX `ContextMenu` below the button with two options:
    1. `➕ Add Syllabus (PDF, Doc, PPTX, Image)`
    2. `🗑️ Delete Syllabus`
- **Delete Syllabus Workflow**:
  - Displays a confirmation alert: *"Are you sure you want to delete the syllabus for [Course Code]? This will remove all chapters and topics."*
  - On confirmation:
    - Calls `DatabaseHelper.saveSyllabusChapters(currentSelectedCourse.getId(), Collections.emptyList());`
    - Re-renders checklist (shows empty placeholder state).
    - Resets progress bar to 0% and coverage pie chart.
    - Updates forecast to reflect missing syllabus.
- **Add Syllabus Multi-Format Ingestion**:
  - `FileChooser` configured with extension filters:
    - `All Supported Files (*.pdf, *.png, *.jpg, *.jpeg, *.docx, *.pptx, *.txt, *.md)`
    - `Images (*.png, *.jpg, *.jpeg)`
    - `PDF Documents (*.pdf)`
    - `Word Documents (*.docx, *.doc)`
    - `PowerPoint Presentations (*.pptx, *.ppt)`
    - `Text / Markdown (*.txt, *.md)`
  - **Extraction Pipeline**:
    - **Images (`.png`, `.jpg`, `.jpeg`)**: Base64-encoded and sent to Gemini's native multimodal vision API (`inlineData`) with structured JSON instructions. Extracts chapters and topics directly from textbook snapshots, handwritten syllabus sheets, or lecture slide photos without external OCR dependencies.
    - **PDF (`.pdf`)**: Extracted locally in milliseconds using Apache PDFBox (`Loader.loadPDF(file)` + `PDFTextStripper`).
    - **Word Documents (`.docx`)**: Decompressed via `java.util.zip.ZipInputStream` to read `word/document.xml` and strip XML tags into clean text.
    - **PowerPoint Slides (`.pptx`)**: Decompressed via `java.util.zip.ZipInputStream` to read `ppt/slides/slide*.xml` and extract slide text.
    - **Plain Text / Markdown (`.txt`, `.md`)**: Extracted via UTF-8 standard file reader.
  - Extracted text/image is processed by Gemini (`parseSyllabusHierarchyAsync`) and persisted into SQLite (`syllabus_chapters` & `syllabus_topics`).

---

### B. Text & Label Simplification + Type Suggestions
Clean up redundant "Assessment" wording across the Add Marks modal and the course detail view:
1. **In `HelloController.java` (`handleAddMarksDialog`)**:
   - Modal title: `"Add Assessment Marks"` &rarr; `"Add Marks"`
   - Header text: `"Enter assessment marks for [Course Code]"` &rarr; `"Enter marks for [Course Code]"`
   - Field 1 Label: `"Assessment Type:"` &rarr; `"Type:"`
   - Field 2 Label: `"Assessment Name:"` &rarr; `"Name:"`
   - **Type Suggestions ComboBox**: Populated with the exact requested options:
     - `CT`
     - `Assignment`
     - `Spot test`
     - `Lab test`
     - `Lab task`
     - `Lab quiz`
     *(Configured with `typeCombo.setEditable(true)` so user can either pick from suggestions or type any custom assessment type)*
2. **In `hello-view.fxml`**:
   - Right Column Card 1 Title: `"📝 Assessment Marks"` &rarr; `"📝 Marks"`

---

### C. Term Exam Forecast & Expandable Toggle (See More / See Less)
1. **Label Renaming**:
   - Right Column Card 3 Title: `"🤖 AI Term Exam Forecast"` &rarr; `"🤖 Term exam Forecast"`
2. **Expandable Text System**:
   - Add `<Button fx:id="examForecastToggleBtn" styleClass="quiz-see-more-btn" onAction="#handleToggleExamForecastSeeMore" text="See More ▾" visible="false" managed="false" />` below `examForecastText`.
   - In `HelloController.java`:
     - Maintain `fullExamForecastText` and boolean `isExamForecastExpanded`.
     - When forecast text length &le; 110 characters: Display complete text, hide toggle button.
     - When forecast text length > 110 characters:
       - Show toggle button (`visible=true`, `managed=true`).
       - If collapsed (`isExamForecastExpanded == false`): Display first 100 characters + `"..."` with button text `"See More ▾"`.
       - If expanded (`isExamForecastExpanded == true`): Display full text with button text `"See Less ▴"`.
     - Clicking the toggle smoothly toggles between expanded and collapsed states without any eye icons.

---

## 2. Files to Modify

| File | Changes |
|---|---|
| [QuizSourceHelper.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/QuizSourceHelper.java) | Add extraction methods for `.docx` and `.pptx` XML content, plus `isImageFile(File)` helper. |
| [GeminiApiService.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/GeminiApiService.java) | Add `parseSyllabusHierarchyFromImageAsync(File imageFile)` sending base64 `inlineData` to Gemini multimodal vision API. |
| [hello-view.fxml](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/resources/com/example/study_buddy/hello-view.fxml) | - Change `uploadSyllabusBtn` text to `"📄 Syllabus ▾"`.<br>- Rename `📝 Assessment Marks` &rarr; `📝 Marks`.<br>- Rename `🤖 AI Term Exam Forecast` &rarr; `🤖 Term exam Forecast`.<br>- Add `examForecastToggleBtn` under `examForecastText`. |
| [HelloController.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/main/java/com/example/study_buddy/HelloController.java) | - Show `ContextMenu` on `uploadSyllabusBtn` click with Add & Delete options.<br>- Support image and multi-format syllabus loading.<br>- Implement Delete Syllabus confirmation & SQLite clearing.<br>- Update `handleAddMarksDialog` labels to `"Add Marks"`, `"Enter marks for..."`, `"Type:"`, `"Name:`.<br>- Update Type ComboBox suggestions: `CT`, `Assignment`, `Spot test`, `Lab test`, `Lab task`, `Lab quiz`.<br>- Implement `setExamForecastContent()`, `renderExamForecast()`, and `handleToggleExamForecastSeeMore()`. |
| [ProgressServiceTest.java](file:///c:/Users/User/IdeaProjects/Study_Buddy/src/test/java/com/example/study_buddy/ProgressServiceTest.java) | Add unit tests for deleting syllabus, handling multi-format syllabus files, and forecast truncation logic. |

---

## 3. Verification Plan

1. **Automated Unit Tests**:
   - Execute `$env:JAVA_HOME = "C:\Users\User\.jdks\ms-21.0.12"; .\mvnw.cmd test` to ensure all 25+ existing and new tests pass.
2. **Syllabus Action Menu Verification**:
   - Click `📄 Syllabus ▾`: verify popup menu shows `➕ Add Syllabus` and `🗑️ Delete Syllabus`.
   - Test `Delete Syllabus`: confirm alert appears; on accept, verify all chapters/topics are cleared, progress bar resets to 0%, and pie chart resets.
   - Test `Add Syllabus`: verify FileChooser filters for Images (`.png`, `.jpg`), PDFs, Word (`.docx`), PowerPoint (`.pptx`), and Text files.
3. **Add Marks Label & Type Suggestion Verification**:
   - Open course detail &rarr; verify Card 1 title is `📝 Marks`.
   - Click `➕ Add Marks` &rarr; verify modal title is `Add Marks`, header is `Enter marks for [CourseCode]`, and labels are `Type:` and `Name:`.
   - Check Type dropdown: verify suggestions are `CT`, `Assignment`, `Spot test`, `Lab test`, `Lab task`, `Lab quiz`.
4. **Term Exam Forecast & See More/Less Verification**:
   - Verify Card 3 title is `🤖 Term exam Forecast`.
   - When a long AI forecast advice is rendered, verify it is truncated to 100 characters + `"..."` with a `See More ▾` button.
   - Click `See More ▾` &rarr; text expands fully, button switches to `See Less ▴`.
   - Click `See Less ▴` &rarr; text collapses cleanly back to preview.
