package com.example.study_buddy;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests verifying the Producer-Consumer synchronization,
 * thread pool execution, and queue lifecycle in NotebookJobQueue.
 */
public class NotebookJobQueueTest {

    private static NotebookJobQueue jobQueue;

    @BeforeAll
    public static void setUp() {
        jobQueue = NotebookJobQueue.getInstance();
        jobQueue.startConsumers(2);
    }

    @Test
    public void testProducerConsumerExecution() throws InterruptedException {
        if (jobQueue == null) {
            jobQueue = NotebookJobQueue.getInstance();
            jobQueue.startConsumers(2);
        }

        int numberOfJobs = 10;
        CountDownLatch latch = new CountDownLatch(numberOfJobs);
        AtomicInteger executedCount = new AtomicInteger(0);

        // Multiple simulated producer submissions
        for (int i = 0; i < numberOfJobs; i++) {
            final int jobId = i;
            JobItem testJob = new AbstractNotebookJob(1, "TestJob-" + jobId) {
                @Override
                protected void processJob() {
                    executedCount.incrementAndGet();
                    latch.countDown();
                }
            };
            boolean submitted = jobQueue.submitJob(testJob);
            assertTrue(submitted, "Producer should successfully enqueue job");
        }

        // Wait for consumers in the background thread pool to finish processing all jobs
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "All jobs should be consumed within timeout");
        assertEquals(numberOfJobs, executedCount.get(), "Consumer pool should execute exactly all queued jobs");
    }

    @Test
    public void testJobMetadataAndProducerThreadTracking() {
        String callingThread = Thread.currentThread().getName();
        JobItem job = new AbstractNotebookJob(99, "MetadataTestJob") {
            @Override
            protected void processJob() {
                // no-op
            }
        };

        assertEquals("MetadataTestJob", job.getJobName());
        assertEquals(callingThread, job.getProducerThreadName());
        assertTrue(job.getQueuedTimestamp() > 0);
    }
}
