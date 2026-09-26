package com.example.study_buddy;

/**
 * Interface representing an asynchronous job item in the producer-consumer system.
 * Part of the advanced OOP & Concurrency architecture.
 */
public interface JobItem {
    /**
     * Executes the task payload on the consuming worker thread.
     */
    void execute();

    /**
     * @return Human-readable name of the job for logging and UI telemetry.
     */
    String getJobName();

    /**
     * @return The name of the producer thread that created and queued this job.
     */
    String getProducerThreadName();

    /**
     * @return Epoch millisecond timestamp when the job was produced and enqueued.
     */
    long getQueuedTimestamp();
}
