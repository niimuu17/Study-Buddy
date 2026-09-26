package com.example.study_buddy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service to communicate with Google Gemini API (gemini-1.5-flash)
 * using Java 21's native HttpClient and Jackson for JSON processing.
 *
 * Implements the 2-Call Model:
 * 1. Call #1: generateQuiz - creates structured MCQ & Short Answer questions.
 * 2. Call #2: gradeShortAnswers - grades student's typed responses with feedback.
 */
public class GeminiApiService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Call #1: Generates an AI Quiz based on user topic, source material, question count, and difficulty.
     * Runs asynchronously and returns a CompletableFuture<QuizSession>.
     */
    public CompletableFuture<QuizSession> generateQuizAsync(
            String topicPrompt,
            String sourceText,
            int numQuestions,
            String difficulty,
            String questionTypeMode) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateQuiz(topicPrompt, sourceText, numQuestions, difficulty, questionTypeMode);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /**
     * Synchronous implementation of Call #1 (Quiz Generation).
     */
    public QuizSession generateQuiz(
            String topicPrompt,
            String sourceText,
            int numQuestions,
            String difficulty,
            String questionTypeMode) throws IOException, InterruptedException {

        String apiKey = ApiKeyManager.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Google Gemini API Key is missing. Please configure it in API Settings.");
        }

        // Determine breakdown of questions
        int mcqCount;
        int shortAnswerCount;
        if ("MCQ Only".equalsIgnoreCase(questionTypeMode)) {
            mcqCount = numQuestions;
            shortAnswerCount = 0;
        } else if ("Short Answer Only".equalsIgnoreCase(questionTypeMode)) {
            mcqCount = 0;
            shortAnswerCount = numQuestions;
        } else {
            // Both / Mixed mode
            shortAnswerCount = Math.max(1, numQuestions / 3);
            mcqCount = numQuestions - shortAnswerCount;
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an expert academic tutor. Generate a high-quality study quiz.\n");
        promptBuilder.append("Difficulty Level: ").append(difficulty).append("\n");
        promptBuilder.append("Total Questions: ").append(numQuestions).append(" (");
        promptBuilder.append(mcqCount).append(" Multiple Choice, ").append(shortAnswerCount).append(" Short Answer).\n\n");

        if (topicPrompt != null && !topicPrompt.trim().isEmpty()) {
            promptBuilder.append("Topic / Student Prompt: ").append(topicPrompt.trim()).append("\n\n");
        }

        if (sourceText != null && !sourceText.trim().isEmpty()) {
            // Limit source text length to avoid token overflow
            String trimmedSource = sourceText.length() > 15000 ? sourceText.substring(0, 15000) + "..." : sourceText;
            promptBuilder.append("Reference Source Material:\n\"\"\"\n").append(trimmedSource).append("\n\"\"\"\n\n");
        }

        promptBuilder.append("REQUIREMENTS:\n");
        promptBuilder.append("1. For MCQ: Exactly 4 distinct options. Specify 'correctIndex' as an integer 0, 1, 2, or 3. Provide a clear educational 'explanation'.\n");
        promptBuilder.append("2. For SHORT_ANSWER: Provide a clear rubric detailing key points expected in a complete answer, and set maxScore to 5.\n");
        promptBuilder.append("3. Return strictly valid JSON with this exact structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"title\": \"Quiz Title\",\n");
        promptBuilder.append("  \"topic\": \"Summary Topic\",\n");
        promptBuilder.append("  \"difficulty\": \"").append(difficulty).append("\",\n");
        promptBuilder.append("  \"questions\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"type\": \"MCQ\",\n");
        promptBuilder.append("      \"question\": \"Question text?\",\n");
        promptBuilder.append("      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n");
        promptBuilder.append("      \"correctIndex\": 0,\n");
        promptBuilder.append("      \"explanation\": \"Why option A is correct...\"\n");
        promptBuilder.append("    },\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"type\": \"SHORT_ANSWER\",\n");
        promptBuilder.append("      \"question\": \"Question text?\",\n");
        promptBuilder.append("      \"rubric\": \"Key concepts and points expected in the answer...\",\n");
        promptBuilder.append("      \"maxScore\": 5\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n");

        String requestJson = buildGeminiRequestBody(promptBuilder.toString());
        String responseBody = sendGeminiRequest(apiKey, requestJson);

        String jsonText = extractContentText(responseBody);
        return parseQuizSession(jsonText, topicPrompt, difficulty);
    }

    /**
     * Call #2: Grades short answer responses using Gemini AI.
     * Evaluates conceptual correctness, accuracy, and completeness.
     */
    public CompletableFuture<Void> gradeShortAnswersAsync(QuizSession session) {
        return CompletableFuture.runAsync(() -> {
            try {
                gradeShortAnswers(session);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        });
    }

    /**
     * Synchronous implementation of Call #2 (Short Answer Grading).
     */
    public void gradeShortAnswers(QuizSession session) throws IOException, InterruptedException {
        List<QuizQuestion> shortAnswerQuestions = new ArrayList<>();
        for (int i = 0; i < session.getQuestions().size(); i++) {
            QuizQuestion q = session.getQuestions().get(i);
            if (q.getType() == QuizQuestion.QuestionType.SHORT_ANSWER) {
                shortAnswerQuestions.add(q);
            }
        }

        if (shortAnswerQuestions.isEmpty()) {
            return; // No short answer questions to grade
        }

        String apiKey = ApiKeyManager.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Google Gemini API Key is missing. Please configure it in API Settings.");
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an encouraging and fair academic evaluator.\n");
        promptBuilder.append("Grade each of the following student responses against the question and the expected rubric.\n\n");

        ArrayNode itemsArray = objectMapper.createArrayNode();
        for (int i = 0; i < shortAnswerQuestions.size(); i++) {
            QuizQuestion q = shortAnswerQuestions.get(i);
            ObjectNode item = objectMapper.createObjectNode();
            item.put("id", i);
            item.put("question", q.getQuestionText());
            item.put("expectedRubric", q.getRubric());
            item.put("maxScore", q.getMaxScore());
            item.put("studentAnswer", q.getStudentAnswer().trim().isEmpty() ? "(No answer submitted)" : q.getStudentAnswer().trim());
            itemsArray.add(item);
        }

        promptBuilder.append("Student Submissions JSON:\n");
        promptBuilder.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(itemsArray)).append("\n\n");
        promptBuilder.append("EVALUATION RULES:\n");
        promptBuilder.append("1. Score each response from 0 up to maxScore.\n");
        promptBuilder.append("2. If the answer is blank or irrelevant, award 0 points.\n");
        promptBuilder.append("3. Provide concise, constructive feedback explaining why points were awarded or deducted.\n");
        promptBuilder.append("4. Return strictly valid JSON with this structure:\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"evaluations\": [\n");
        promptBuilder.append("    {\n");
        promptBuilder.append("      \"id\": 0,\n");
        promptBuilder.append("      \"awardedScore\": 4,\n");
        promptBuilder.append("      \"feedback\": \"Great explanation of key concepts. To achieve full marks, include...\"\n");
        promptBuilder.append("    }\n");
        promptBuilder.append("  ]\n");
        promptBuilder.append("}\n");

        String requestJson = buildGeminiRequestBody(promptBuilder.toString());
        String responseBody = sendGeminiRequest(apiKey, requestJson);

        String jsonText = extractContentText(responseBody);
        applyGradingResults(jsonText, shortAnswerQuestions);
    }

    /**
     * Builds standard Gemini REST payload requesting application/json output.
     */
    private String buildGeminiRequestBody(String promptText) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        ObjectNode part = parts.addObject();
        part.put("text", promptText);

        ObjectNode genConfig = root.putObject("generationConfig");
        genConfig.put("responseMimeType", "application/json");
        genConfig.put("temperature", 0.3);

        return objectMapper.writeValueAsString(root);
    }

    /**
     * Executes the HTTP POST request to the Google Gemini API.
     */
    private String sendGeminiRequest(String apiKey, String jsonBody) throws IOException, InterruptedException {
        String fullUrl = GEMINI_API_URL + "?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            String errorMsg = "Gemini API error (HTTP " + response.statusCode() + "): ";
            try {
                JsonNode errNode = objectMapper.readTree(response.body());
                if (errNode.has("error") && errNode.get("error").has("message")) {
                    errorMsg += errNode.get("error").get("message").asText();
                } else {
                    errorMsg += response.body();
                }
            } catch (Exception ignored) {
                errorMsg += response.body();
            }
            throw new IOException(errorMsg);
        }

        return response.body();
    }

    /**
     * Extracts text from Gemini's candidate parts response, stripping any markdown wrappers.
     */
    private String extractContentText(String rawApiResponse) throws IOException {
        JsonNode root = objectMapper.readTree(rawApiResponse);
        JsonNode candidates = root.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            throw new IOException("Gemini API returned no candidates. Possible content filter trigger.");
        }

        JsonNode content = candidates.get(0).get("content");
        if (content == null || !content.has("parts")) {
            throw new IOException("Gemini API response missing content parts.");
        }

        JsonNode parts = content.get("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IOException("Gemini API response has empty parts.");
        }

        String text = parts.get(0).path("text").asText("");
        return sanitizeJsonText(text);
    }

    /**
     * Removes ```json ... ``` code fence markers if present in the model's text.
     */
    public static String sanitizeJsonText(String text) {
        if (text == null) return "{}";
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }

        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    /**
     * Parses the sanitized JSON text into a QuizSession instance.
     */
    public QuizSession parseQuizSession(String jsonText, String fallbackTopic, String fallbackDifficulty) throws IOException {
        JsonNode root = objectMapper.readTree(jsonText);

        String title = root.path("title").asText("AI Study Quiz");
        String topic = root.path("topic").asText(fallbackTopic != null && !fallbackTopic.isEmpty() ? fallbackTopic : "General");
        String difficulty = root.path("difficulty").asText(fallbackDifficulty != null ? fallbackDifficulty : "Medium");

        List<QuizQuestion> questions = new ArrayList<>();
        JsonNode qArray = root.path("questions");
        if (qArray.isArray()) {
            for (JsonNode qNode : qArray) {
                String typeStr = qNode.path("type").asText("MCQ").toUpperCase();
                String questionText = qNode.path("question").asText("");

                if ("MCQ".equals(typeStr)) {
                    List<String> options = new ArrayList<>();
                    JsonNode optArray = qNode.path("options");
                    if (optArray.isArray()) {
                        for (JsonNode opt : optArray) {
                            options.add(opt.asText());
                        }
                    }
                    int correctIndex = qNode.path("correctIndex").asInt(0);
                    String explanation = qNode.path("explanation").asText("");
                    questions.add(QuizQuestion.createMcq(questionText, options, correctIndex, explanation));
                } else {
                    String rubric = qNode.path("rubric").asText("");
                    int maxScore = qNode.path("maxScore").asInt(5);
                    questions.add(QuizQuestion.createShortAnswer(questionText, rubric, maxScore));
                }
            }
        }

        return new QuizSession(title, topic, difficulty, questions);
    }

    /**
     * Applies grading evaluations back onto the short answer questions.
     */
    public void applyGradingResults(String jsonText, List<QuizQuestion> shortAnswerQuestions) throws IOException {
        JsonNode root = objectMapper.readTree(jsonText);
        JsonNode evals = root.path("evaluations");
        if (evals.isArray()) {
            for (JsonNode ev : evals) {
                int id = ev.path("id").asInt(-1);
                int score = ev.path("awardedScore").asInt(0);
                String feedback = ev.path("feedback").asText("");

                if (id >= 0 && id < shortAnswerQuestions.size()) {
                    QuizQuestion q = shortAnswerQuestions.get(id);
                    q.setAwardedScore(Math.min(score, q.getMaxScore()));
                    q.setAiFeedback(feedback);
                }
            }
        }
    }
}
