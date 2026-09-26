package com.example.study_buddy;

/**
 * Abstract base class for notebook-related background jobs in the Producer-Consumer architecture.
 * Implements common job metadata and template execution pattern.
 */
public abstract class AbstractNotebookJob implements JobItem {
    protected final int notebookId;
    protected final String jobName;
    protected final String producerThreadName;
    protected final long queuedTimestamp;

    public AbstractNotebookJob(int notebookId, String jobName) {
        this.notebookId = notebookId;
        this.jobName = jobName;
        this.producerThreadName = Thread.currentThread().getName();
        this.queuedTimestamp = System.currentTimeMillis();
    }

    public int getNotebookId() {
        return notebookId;
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public String getProducerThreadName() {
        return producerThreadName;
    }

    @Override
    public long getQueuedTimestamp() {
        return queuedTimestamp;
    }

    @Override
    public void execute() {
        long waitTimeMs = System.currentTimeMillis() - queuedTimestamp;
        String consumerThreadName = Thread.currentThread().getName();
        System.out.printf("[CONSUMER: %s] Processing job '%s' for Notebook %d (waited %dms in queue)%n",
                consumerThreadName, jobName, notebookId, waitTimeMs);
        try {
            processJob();
        } catch (Exception e) {
            System.err.printf("[CONSUMER: %s] Error executing job '%s': %s%n",
                    consumerThreadName, jobName, e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Abstract method implemented by concrete subclasses to perform specific background work.
     */
    protected abstract void processJob() throws Exception;
}
