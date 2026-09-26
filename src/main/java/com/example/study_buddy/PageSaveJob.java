package com.example.study_buddy;

import javafx.application.Platform;
import javafx.scene.control.Label;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Concrete job representing an asynchronous page autosave in the Producer-Consumer pipeline.
 * Persists page content to SQLite, analyzes word stats, and safely updates the UI status badge.
 */
public class PageSaveJob extends AbstractNotebookJob {
    private final int pageId;
    private final String title;
    private final String contentJson;
    private final Label statusLabel;
    private final boolean updateStatusBadge;

    public PageSaveJob(int notebookId, int pageId, String title, String contentJson,
                       Label statusLabel, boolean updateStatusBadge) {
        super(notebookId, "PageSaveJob: " + title);
        this.pageId = pageId;
        this.title = title;
        this.contentJson = contentJson;
        this.statusLabel = statusLabel;
        this.updateStatusBadge = updateStatusBadge;
    }

    public int getPageId() {
        return pageId;
    }

    public String getTitle() {
        return title;
    }

    public String getContentJson() {
        return contentJson;
    }

    @Override
    protected void processJob() {
        // 1. Commit changes to SQLite
        DatabaseHelper.updatePage(pageId, title, contentJson);

        // 2. Perform analytical computation (word count) in background thread
        int wordCount = computeWordCount(contentJson);

        String consumerThreadName = Thread.currentThread().getName();
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        System.out.printf("[CONSUMER: %s] Successfully saved page ID %d ('%s') - %d words at %s%n",
                consumerThreadName, pageId, title, wordCount, timeStr);

        // 3. Safely update UI via Platform.runLater()
        if (updateStatusBadge && statusLabel != null) {
            Platform.runLater(() -> {
                statusLabel.setText("Saved ✓ [" + consumerThreadName + "] " + timeStr + " (" + wordCount + " words)");
            });
        }
    }

    private int computeWordCount(String json) {
        if (json == null || json.isEmpty()) return 0;
        int count = 0;
        String[] tokens = json.split("\\s+");
        for (String t : tokens) {
            String clean = t.replaceAll("[^a-zA-Z0-9]", "");
            if (!clean.isEmpty()) count++;
        }
        return count;
    }
}
