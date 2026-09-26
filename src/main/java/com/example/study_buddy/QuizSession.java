package com.example.study_buddy;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages an active quiz attempt, including questions, submission state,
 * and comprehensive scoring metrics.
 */
public class QuizSession {

    private String title = "AI Quiz";
    private String topic = "General Study";
    private String difficulty = "Medium";
    private List<QuizQuestion> questions = new ArrayList<>();
    private boolean submitted = false;

    public QuizSession() {
    }

    public QuizSession(String title, String topic, String difficulty, List<QuizQuestion> questions) {
        this.title = title;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questions = (questions != null) ? questions : new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = (questions != null) ? questions : new ArrayList<>();
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public void setSubmitted(boolean submitted) {
        this.submitted = submitted;
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public int getAnsweredCount() {
        int count = 0;
        for (QuizQuestion q : questions) {
            if (q.isAnswered()) count++;
        }
        return count;
    }

    public int getMcqCount() {
        int count = 0;
        for (QuizQuestion q : questions) {
            if (q.getType() == QuizQuestion.QuestionType.MCQ) count++;
        }
        return count;
    }

    public int getShortAnswerCount() {
        int count = 0;
        for (QuizQuestion q : questions) {
            if (q.getType() == QuizQuestion.QuestionType.SHORT_ANSWER) count++;
        }
        return count;
    }

    public boolean hasShortAnswerQuestions() {
        return getShortAnswerCount() > 0;
    }

    public int getCorrectMcqCount() {
        int correct = 0;
        for (QuizQuestion q : questions) {
            if (q.isMcqCorrect()) correct++;
        }
        return correct;
    }

    public int getTotalMaxScore() {
        int total = 0;
        for (QuizQuestion q : questions) {
            total += q.getMaxScore();
        }
        return total > 0 ? total : 1;
    }

    public int getTotalScoreEarned() {
        int score = 0;
        for (QuizQuestion q : questions) {
            if (q.getType() == QuizQuestion.QuestionType.MCQ) {
                if (q.isMcqCorrect()) score += q.getMaxScore();
            } else {
                score += q.getAwardedScore();
            }
        }
        return score;
    }

    public int getPercentageScore() {
        int max = getTotalMaxScore();
        int earned = getTotalScoreEarned();
        return (int) Math.round(((double) earned / max) * 100.0);
    }

    public String getGradeLetter() {
        int pct = getPercentageScore();
        if (pct >= 90) return "A+";
        if (pct >= 80) return "A";
        if (pct >= 70) return "B";
        if (pct >= 60) return "C";
        if (pct >= 50) return "D";
        return "F";
    }
}
