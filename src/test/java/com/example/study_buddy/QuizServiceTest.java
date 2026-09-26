package com.example.study_buddy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Quiz models, 2-call JSON serialization/deserialization with Jackson,
 * scoring algorithms, and source text extraction.
 */
public class QuizServiceTest {

    private GeminiApiService apiService;

    @BeforeEach
    void setUp() {
        apiService = new GeminiApiService();
    }

    @Test
    @DisplayName("MCQ Question validates user selections and correctness accurately")
    void testMcqEvaluation() {
        QuizQuestion mcq = QuizQuestion.createMcq(
                "What is the time complexity of binary search on a sorted array?",
                Arrays.asList("O(n)", "O(log n)", "O(n^2)", "O(1)"),
                1, // Index 1 = O(log n)
                "Binary search halves the search space each step."
        );

        assertEquals(QuizQuestion.QuestionType.MCQ, mcq.getType());
        assertEquals("B", QuizQuestion.getOptionLetter(1));
        assertFalse(mcq.isAnswered());

        // Select wrong answer (index 0 = A)
        mcq.setUserSelectedOption(0);
        assertTrue(mcq.isAnswered());
        assertFalse(mcq.isMcqCorrect());

        // Select correct answer (index 1 = B)
        mcq.setUserSelectedOption(1);
        assertTrue(mcq.isMcqCorrect());
    }

    @Test
    @DisplayName("QuizSession computes metrics, scores, and letter grades accurately")
    void testQuizSessionScoring() {
        QuizQuestion q1 = QuizQuestion.createMcq("Q1", Arrays.asList("A", "B", "C", "D"), 0, "Exp 1");
        q1.setUserSelectedOption(0); // Correct (+1 pt)

        QuizQuestion q2 = QuizQuestion.createMcq("Q2", Arrays.asList("A", "B", "C", "D"), 2, "Exp 2");
        q2.setUserSelectedOption(1); // Incorrect (0 pt)

        QuizQuestion q3 = QuizQuestion.createShortAnswer("Explain Encapsulation", "Data hiding and getters/setters", 5);
        q3.setStudentAnswer("Encapsulation binds data and code together.");
        q3.setAwardedScore(4); // Graded by AI (+4 pts)

        QuizSession session = new QuizSession("OOP Quiz", "Java OOP", "Medium", Arrays.asList(q1, q2, q3));

        assertEquals(3, session.getTotalQuestions());
        assertEquals(3, session.getAnsweredCount());
        assertEquals(2, session.getMcqCount());
        assertEquals(1, session.getShortAnswerCount());
        assertTrue(session.hasShortAnswerQuestions());

        assertEquals(1, session.getCorrectMcqCount());
        // Max score = 1 + 1 + 5 = 7
        assertEquals(7, session.getTotalMaxScore());
        // Earned score = 1 + 0 + 4 = 5
        assertEquals(5, session.getTotalScoreEarned());
        // Percentage = 5/7 = 71%
        assertEquals(71, session.getPercentageScore());
        assertEquals("B", session.getGradeLetter());
    }

    @Test
    @DisplayName("Jackson parses Call #1 Gemini JSON payload into QuizSession")
    void testCall1JsonParsing() throws IOException {
        String mockGeminiCall1Json = """
        {
          "title": "Data Structures Midterm Quiz",
          "topic": "Binary Search Trees",
          "difficulty": "Medium",
          "questions": [
            {
              "type": "MCQ",
              "question": "What is the worst-case time complexity of BST search?",
              "options": ["O(1)", "O(log n)", "O(n)", "O(n log n)"],
              "correctIndex": 2,
              "explanation": "In degenerate / skewed BSTs, search degrades to O(n)."
            },
            {
              "type": "SHORT_ANSWER",
              "question": "Differentiate between an AVL Tree and a Red-Black Tree.",
              "rubric": "Mentions height-balancing factor (balance factor <= 1) vs color rules and rotation frequencies.",
              "maxScore": 5
            }
          ]
        }
        """;

        QuizSession session = apiService.parseQuizSession(mockGeminiCall1Json, "BST", "Medium");
        assertNotNull(session);
        assertEquals("Data Structures Midterm Quiz", session.getTitle());
        assertEquals("Binary Search Trees", session.getTopic());
        assertEquals(2, session.getTotalQuestions());

        QuizQuestion q1 = session.getQuestions().get(0);
        assertEquals(QuizQuestion.QuestionType.MCQ, q1.getType());
        assertEquals(4, q1.getOptions().size());
        assertEquals(2, q1.getCorrectIndex());
        assertEquals("O(n)", q1.getOptions().get(2));

        QuizQuestion q2 = session.getQuestions().get(1);
        assertEquals(QuizQuestion.QuestionType.SHORT_ANSWER, q2.getType());
        assertEquals(5, q2.getMaxScore());
        assertTrue(q2.getRubric().contains("height-balancing"));
    }

    @Test
    @DisplayName("Jackson parses Call #2 Gemini grading JSON and assigns scores and feedback")
    void testCall2GradingParsing() throws IOException {
        QuizQuestion saQuestion = QuizQuestion.createShortAnswer("Explain Polymorphism", "Method overriding and overloading", 5);
        saQuestion.setStudentAnswer("Polymorphism is when an object can take many forms, like method overriding.");

        List<QuizQuestion> shortAnswers = List.of(saQuestion);

        String mockGeminiGradingJson = """
        {
          "evaluations": [
            {
              "id": 0,
              "awardedScore": 4,
              "feedback": "Clear explanation of dynamic method dispatch and overriding. Consider also mentioning interfaces."
            }
          ]
        }
        """;

        apiService.applyGradingResults(mockGeminiGradingJson, shortAnswers);

        assertEquals(4, saQuestion.getAwardedScore());
        assertTrue(saQuestion.getAiFeedback().contains("Clear explanation"));
    }

    @Test
    @DisplayName("GeminiApiService sanitizeJsonText removes code fences properly")
    void testSanitizeJsonText() {
        String fenced = "```json\n{\"title\": \"Test\"}\n```";
        assertEquals("{\"title\": \"Test\"}", GeminiApiService.sanitizeJsonText(fenced));

        String plain = "{\"title\": \"Test\"}";
        assertEquals("{\"title\": \"Test\"}", GeminiApiService.sanitizeJsonText(plain));
    }

    @Test
    @DisplayName("QuizSourceHelper extracts Page text and code blocks cleanly")
    void testQuizSourceHelperPageExtraction() {
        List<PageBlock> blocks = List.of(
                new PageBlock(PageBlock.TYPE_TEXT, "Chapter 3: Graph Traversal", ""),
                new PageBlock(PageBlock.TYPE_CODE, "void dfs(int v) { visited[v] = true; }", "java")
        );
        String blocksJson = PageBlock.serializeList(blocks);
        Page page = new Page(1, 10, "Graphs Notes", blocksJson);

        String extracted = QuizSourceHelper.extractPageContent(page);
        assertTrue(extracted.contains("Page Title: Graphs Notes"));
        assertTrue(extracted.contains("Chapter 3: Graph Traversal"));
        assertTrue(extracted.contains("void dfs(int v)"));
    }

    @Test
    @DisplayName("ApiKeyManager manages and masks keys correctly")
    void testApiKeyManager() {
        String originalKey = ApiKeyManager.getApiKey();
        try {
            ApiKeyManager.setApiKey("AIzaSyDummyKeyForTesting12345678");
            assertTrue(ApiKeyManager.hasApiKey());
            assertEquals("AIzaSyDummyKeyForTesting12345678", ApiKeyManager.getApiKey());
            assertTrue(ApiKeyManager.getMaskedApiKey().startsWith("AIzaSy..."));
        } finally {
            ApiKeyManager.setApiKey(originalKey);
        }
    }
}
