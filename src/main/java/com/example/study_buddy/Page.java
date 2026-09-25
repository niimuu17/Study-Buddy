package com.example.study_buddy;

/**
 * Represents a learning page / canvas within a Topic.
 * Contains the rich notes, code snippets, and image blocks.
 */
public class Page {
    private int id;
    private int topicId;
    private String title;
    private String contentJson;
    private String createdAt;
    private String updatedAt;

    public Page(int id, int topicId, String title, String contentJson, String createdAt, String updatedAt) {
        this.id = id;
        this.topicId = topicId;
        this.title = title != null ? title : "Untitled Page";
        this.contentJson = contentJson != null ? contentJson : "";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Page(int id, int topicId, String title, String contentJson) {
        this(id, topicId, title, contentJson, null, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
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
