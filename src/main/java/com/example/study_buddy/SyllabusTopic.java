package com.example.study_buddy;

/**
 * Domain model representing a single topic inside a syllabus chapter.
 */
public class SyllabusTopic {

    private int id;
    private int chapterId;
    private String title;
    private boolean completed;
    private String completedAt;

    public SyllabusTopic() {}

    public SyllabusTopic(int id, int chapterId, String title, boolean completed, String completedAt) {
        this.id = id;
        this.chapterId = chapterId;
        this.title = title;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    public SyllabusTopic(String title) {
        this(0, 0, title, false, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getChapterId() {
        return chapterId;
    }

    public void setChapterId(int chapterId) {
        this.chapterId = chapterId;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}
