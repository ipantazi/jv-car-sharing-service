package com.github.ipantazi.carsharing.util.concurrency;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ConcurrencyTestHelper {
    private final ExecutorService executor;
    private final int threadCount;

    public ConcurrencyTestHelper(int threadCount) {
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    public <T> List<Future<T>> runConcurrentTasks(List<Callable<T>> tasks)
            throws InterruptedException {

        if (tasks.size() != threadCount) {
            throw new IllegalArgumentException(
                    "Number of tasks (" + tasks.size() + ") must match threadCount ("
                            + threadCount + ")");
        }

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<T>> wrapped = tasks.stream()
                .map(task -> (Callable<T>) () -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                })
                .toList();

        final List<Future<T>> futures = wrapped.stream()
                .map(executor::submit)
                .toList();

        ready.await();
        start.countDown();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        return futures;
    }

    public static <T> T safeGet(Future<T> f) {
        try {
            return f.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
