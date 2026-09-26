package com.example.study_buddy;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single question in an AI-generated quiz.
 * Supports both Multiple Choice Questions (MCQ) and Short Answer questions.
 */
public class QuizQuestion {

    public enum QuestionType {
        MCQ,
        SHORT_ANSWER
    }

    private QuestionType type;
    private String questionText;

    // MCQ specific fields
    private List<String> options = new ArrayList<>();
    private int correctIndex = -1;
    private String explanation = "";
    private Integer userSelectedOption = null; // 0, 1, 2, 3 or null if unanswered

    // Short Answer specific fields
    private String rubric = "";             // Expected key points or sample answer
    private String studentAnswer = "";       // Text input typed by the user
    private int maxScore = 5;               // Standard maximum points
    private int awardedScore = 0;           // Evaluated by Gemini AI
    private String aiFeedback = "";         // Constructive grading remarks

    public QuizQuestion() {
    }

    public static QuizQuestion createMcq(String questionText, List<String> options, int correctIndex, String explanation) {
        QuizQuestion q = new QuizQuestion();
        q.type = QuestionType.MCQ;
        q.questionText = questionText;
        q.options = options != null ? options : new ArrayList<>();
        q.correctIndex = correctIndex;
        q.explanation = explanation != null ? explanation : "";
        q.maxScore = 1; // 1 point for MCQ
        return q;
    }

    public static QuizQuestion createShortAnswer(String questionText, String rubric, int maxScore) {
        QuizQuestion q = new QuizQuestion();
        q.type = QuestionType.SHORT_ANSWER;
        q.questionText = questionText;
        q.rubric = rubric != null ? rubric : "";
        q.maxScore = maxScore > 0 ? maxScore : 5;
        return q;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getQuestionText() {
        return questionText != null ? questionText : "";
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options != null ? options : new ArrayList<>();
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public void setCorrectIndex(int correctIndex) {
        this.correctIndex = correctIndex;
    }

    public String getExplanation() {
        return explanation != null ? explanation : "";
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Integer getUserSelectedOption() {
        return userSelectedOption;
    }

    public void setUserSelectedOption(Integer userSelectedOption) {
        this.userSelectedOption = userSelectedOption;
    }

    public String getRubric() {
        return rubric != null ? rubric : "";
    }

    public void setRubric(String rubric) {
        this.rubric = rubric;
    }

    public String getStudentAnswer() {
        return studentAnswer != null ? studentAnswer : "";
    }

    public void setStudentAnswer(String studentAnswer) {
        this.studentAnswer = studentAnswer;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public int getAwardedScore() {
        return awardedScore;
    }

    public void setAwardedScore(int awardedScore) {
        this.awardedScore = awardedScore;
    }

    public String getAiFeedback() {
        return aiFeedback != null ? aiFeedback : "";
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public boolean isAnswered() {
        if (type == QuestionType.MCQ) {
            return userSelectedOption != null && userSelectedOption >= 0;
        } else {
            return studentAnswer != null && !studentAnswer.trim().isEmpty();
        }
    }

    public boolean isMcqCorrect() {
        if (type != QuestionType.MCQ) return false;
        return userSelectedOption != null && userSelectedOption == correctIndex;
    }

    public static String getOptionLetter(int index) {
        switch (index) {
            case 0: return "A";
            case 1: return "B";
            case 2: return "C";
            case 3: return "D";
            default: return String.valueOf((char) ('A' + index));
        }
    }
}
