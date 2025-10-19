package com.xreatlabs.xreatoptimizer.async;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous execution utility for safe off-main-thread operations
 */
public class AsyncExecutor {
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "XreatOpt-AsyncExecutor");
        t.setDaemon(true);
        return t;
    });
    
    /**
     * Executes a task asynchronously
     * @param task The task to execute
     * @return CompletableFuture for the operation
     */
    public static CompletableFuture<Void> executeAsync(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }
    
    /**
     * Executes a task asynchronously with result
     * @param task The task to execute that returns a result
     * @return CompletableFuture for the operation
     */
    public static <T> CompletableFuture<T> executeAsyncWithResult(java.util.concurrent.Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    /**
     * Executes an async task with error handling
     * @param task The task to execute
     * @param errorHandler Handler for errors
     * @return CompletableFuture for the operation
     */
    public static CompletableFuture<Void> executeAsyncWithErrorHandler(Runnable task, java.util.function.Consumer<Throwable> errorHandler) {
        return CompletableFuture.runAsync(task, executor)
            .exceptionally(throwable -> {
                errorHandler.accept(throwable);
                return null;
            });
    }
    
    /**
     * Shuts down the async executor
     */
    public static void shutdown() {
        executor.shutdown();
    }
}