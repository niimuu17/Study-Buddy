package com.example.study_buddy;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a study topic within a Notebook.
 * Contains pages and file attachments.
 */
public class Topic {
    private int id;
    private int notebookId;
    private String title;
    private int orderIndex;
    private String createdAt;
    private List<Page> pages = new ArrayList<>();
    private List<TopicFile> files = new ArrayList<>();

    public Topic(int id, int notebookId, String title, int orderIndex, String createdAt) {
        this.id = id;
        this.notebookId = notebookId;
        this.title = title;
        this.orderIndex = orderIndex;
        this.createdAt = createdAt;
    }

    public Topic(int id, int notebookId, String title, int orderIndex) {
        this(id, notebookId, title, orderIndex, null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(int notebookId) {
        this.notebookId = notebookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages != null ? pages : new ArrayList<>();
    }

    public List<TopicFile> getFiles() {
        return files;
    }

    public void setFiles(List<TopicFile> files) {
        this.files = files != null ? files : new ArrayList<>();
    }
}
