package com.example.study_buddy;

/**
 * Represents a study notebook belonging to a user.
 * Organizes topics, pages, and attachments.
 */
public class Notebook {
    private int id;
    private int userId;
    private String title;
    private String description;
    private String colorHex;
    private int topicCount;
    private int pageCount;
    private String createdAt;
    private String updatedAt;

    public Notebook(int id, int userId, String title, String description, String colorHex,
                    int topicCount, int pageCount, String createdAt, String updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.description = description != null ? description : "";
        this.colorHex = (colorHex != null && !colorHex.isEmpty()) ? colorHex : "#4f46e5";
        this.topicCount = topicCount;
        this.pageCount = pageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Notebook(int id, int userId, String title, String description, String colorHex) {
        this(id, userId, title, description, colorHex, 0, 0, null, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public int getTopicCount() {
        return topicCount;
    }

    public void setTopicCount(int topicCount) {
        this.topicCount = topicCount;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
