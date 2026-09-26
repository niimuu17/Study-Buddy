package com.example.study_buddy;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe shared buffer & thread pool manager solving the Producer-Consumer problem.
 * Manages a bounded BlockingQueue and a dedicated pool of consumer worker threads.
 */
public class NotebookJobQueue {
    private static volatile NotebookJobQueue instance;

    // Shared thread-safe Bounded Buffer for Producer-Consumer pattern
    private final BlockingQueue<JobItem> queue;

    // Background Thread Pool for Consumer Workers
    private ExecutorService consumerPool;
    private final AtomicInteger processedJobsCount = new AtomicInteger(0);
    private volatile boolean isRunning = false;

    private NotebookJobQueue() {
        this.queue = new LinkedBlockingQueue<>(150);
    }

    public static NotebookJobQueue getInstance() {
        if (instance == null) {
            synchronized (NotebookJobQueue.class) {
                if (instance == null) {
                    instance = new NotebookJobQueue();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes and starts the background consumer worker threads.
     *
     * @param numWorkers Number of consumer threads to spawn in the pool.
     */
    public synchronized void startConsumers(int numWorkers) {
        if (isRunning) return;
        isRunning = true;

        AtomicInteger workerIndex = new AtomicInteger(1);
        ThreadFactory threadFactory = r -> {
            Thread t = new Thread(r, "NotebookWorker-" + workerIndex.getAndIncrement());
            t.setDaemon(true); // Allows JVM to exit cleanly
            return t;
        };

        consumerPool = Executors.newFixedThreadPool(numWorkers, threadFactory);

        for (int i = 0; i < numWorkers; i++) {
            consumerPool.submit(this::consumerLoop);
        }
        System.out.printf("[CONCURRENCY] NotebookJobQueue started with %d consumer worker threads.%n", numWorkers);
    }

    /**
     * The consumer loop running on background worker threads.
     * Takes jobs from the shared buffer and executes them.
     */
    private void consumerLoop() {
        String threadName = Thread.currentThread().getName();
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // Blocks until a job is produced into the queue (Producer-Consumer synchronization)
                JobItem job = queue.take();
                job.execute();
                processedJobsCount.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                System.err.printf("[CONSUMER: %s] Unexpected error: %s%n", threadName, ex.getMessage());
            }
        }
        System.out.printf("[CONSUMER: %s] Consumer thread terminated.%n", threadName);
    }

    /**
     * Producer entry point: enqueues a job into the shared buffer.
     * Non-blocking or bounded-timeout offer to ensure the UI thread is never frozen.
     */
    public boolean submitJob(JobItem job) {
        if (!isRunning) {
            startConsumers(2);
        }
        String producerName = Thread.currentThread().getName();
        boolean queued = queue.offer(job);
        if (queued) {
            System.out.printf("[PRODUCER: %s] Enqueued '%s' into shared buffer (queue depth: %d)%n",
                    producerName, job.getJobName(), queue.size());
        } else {
            System.err.printf("[PRODUCER: %s] Buffer full! Dropped job '%s'%n", producerName, job.getJobName());
        }
        return queued;
    }

    /**
     * Gracefully shuts down the consumer thread pool.
     */
    public synchronized void shutdown() {
        isRunning = false;
        if (consumerPool != null) {
            consumerPool.shutdownNow();
            try {
                if (!consumerPool.awaitTermination(1, TimeUnit.SECONDS)) {
                    consumerPool.shutdownNow();
                }
            } catch (InterruptedException ignored) {}
        }
        System.out.println("[CONCURRENCY] NotebookJobQueue consumer thread pool stopped.");
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getProcessedJobsCount() {
        return processedJobsCount.get();
    }

    public boolean isRunning() {
        return isRunning;
    }
}
