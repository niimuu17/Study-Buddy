package com.example.study_buddy;

import java.util.List;

/**
 * Concrete job representing background analysis and statistics indexing for a Notebook.
 * Demonstrates polymorphic execution of AbstractNotebookJob in the Consumer thread pool.
 */
public class NotebookStatsJob extends AbstractNotebookJob {
    private final String notebookTitle;

    public NotebookStatsJob(int notebookId, String notebookTitle) {
        super(notebookId, "NotebookStatsJob: " + notebookTitle);
        this.notebookTitle = notebookTitle;
    }

    public String getNotebookTitle() {
        return notebookTitle;
    }

    @Override
    protected void processJob() {
        List<Topic> topics = DatabaseHelper.getTopicsByNotebook(notebookId);
        int totalTopics = topics.size();
        int totalPages = 0;
        int totalFiles = 0;

        for (Topic t : topics) {
            if (t.getPages() != null) totalPages += t.getPages().size();
            if (t.getFiles() != null) totalFiles += t.getFiles().size();
        }

        String consumerThreadName = Thread.currentThread().getName();
        System.out.printf("[CONSUMER: %s] Analyzed Notebook '%s' (ID %d): %d topics, %d pages, %d files.%n",
                consumerThreadName, notebookTitle, notebookId, totalTopics, totalPages, totalFiles);
    }
}
