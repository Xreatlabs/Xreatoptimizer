package com.xreatlabs.xreatoptimizer.managers;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import com.xreatlabs.xreatoptimizer.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Tick Budget Manager - Distributes processing across multiple ticks
 * 
 * Features:
 * - Priority-based task scheduling
 * - Automatic load balancing
 * - Prevents single-tick lag spikes
 * - Dynamic budget adjustment based on TPS
 * - Task queuing and batching
 */
public class TickBudgetManager {
    
    private final XreatOptimizer plugin;
    private final PriorityBlockingQueue<ScheduledTask> taskQueue = new PriorityBlockingQueue<>();
    private final Map<String, TaskCategory> categories = new ConcurrentHashMap<>();
    private BukkitTask processorTask;
    private volatile boolean isRunning = false;
    
    // Budget configuration (in milliseconds per tick)
    private double maxTickBudget = 40.0; // Leave 10ms buffer for other operations
    private double currentBudget = maxTickBudget;
    
    /**
     * Task priority levels
     */
    public enum Priority {
        CRITICAL(0),    // Must execute immediately
        HIGH(1),        // Execute within 1 tick
        NORMAL(2),      // Execute within 5 ticks
        LOW(3),         // Execute within 20 ticks
        BACKGROUND(4);  // Execute when server is idle
        
        private final int value;
        
        Priority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    /**
     * Task category for budget tracking
     */
    private static class TaskCategory {
        final String name;
        double budgetUsed = 0;
        double budgetLimit = 10.0; // ms per tick
        int tasksExecuted = 0;
        int tasksQueued = 0;
        
        public TaskCategory(String name) {
            this.name = name;
        }
        
        public boolean hasbudgetRemaining() {
            return budgetUsed < budgetLimit;
        }
        
        public void reset() {
            budgetUsed = 0;
            tasksExecuted = 0;
        }
    }
    
    /**
     * Scheduled task with priority and timing
     */
    private static class ScheduledTask implements Comparable<ScheduledTask> {
        final String id;
        final Runnable task;
        final Priority priority;
        final String category;
        final long scheduledTime;
        final double estimatedTime; // estimated execution time in ms
        
        public ScheduledTask(String id, Runnable task, Priority priority, String category, double estimatedTime) {
            this.id = id;
            this.task = task;
            this.priority = priority;
            this.category = category;
            this.scheduledTime = System.currentTimeMillis();
            this.estimatedTime = estimatedTime;
        }
        
        @Override
        public int compareTo(ScheduledTask other) {
            // Higher priority first
            int priorityComp = Integer.compare(this.priority.getValue(), other.priority.getValue());
            if (priorityComp != 0) return priorityComp;
            
            // Then by scheduled time (older first)
            return Long.compare(this.scheduledTime, other.scheduledTime);
        }
    }
    
    public TickBudgetManager(XreatOptimizer plugin) {
        this.plugin = plugin;
        initializeCategories();
    }
    
    /**
     * Initialize task categories with default budgets
     */
    private void initializeCategories() {
        registerCategory("entity_processing", 15.0);
        registerCategory("chunk_processing", 10.0);
        registerCategory("redstone_processing", 8.0);
        registerCategory("ai_processing", 10.0);
        registerCategory("physics_processing", 7.0);
        registerCategory("general", 10.0);
    }
    
    /**
     * Register a task category with budget limit
     */
    public void registerCategory(String name, double budgetLimit) {
        TaskCategory category = new TaskCategory(name);
        category.budgetLimit = budgetLimit;
        categories.put(name, category);
    }
    
    /**
     * Start the tick budget manager
     */
    public void start() {
        if (!plugin.getConfig().getBoolean("tick_budget.enabled", true)) {
            LoggerUtils.info("Tick budget manager is disabled in config.");
            return;
        }
        
        isRunning = true;
        
        // Process tasks every tick
        processorTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            this::processTick,
            1L,
            1L
        );
        
        LoggerUtils.info("Tick budget manager started - distributing load across ticks");
    }
    
    /**
     * Stop the tick budget manager
     */
    public void stop() {
        isRunning = false;
        
        if (processorTask != null) {
            processorTask.cancel();
        }
        
        taskQueue.clear();
        categories.values().forEach(TaskCategory::reset);
        
        LoggerUtils.info("Tick budget manager stopped");
    }
    
    /**
     * Schedule a task with priority and category
     */
    public void scheduleTask(String id, Runnable task, Priority priority, String category, double estimatedTimeMs) {
        if (!isRunning) {
            // If not running, execute immediately
            task.run();
            return;
        }
        
        TaskCategory cat = categories.getOrDefault(category, categories.get("general"));
        cat.tasksQueued++;
        
        ScheduledTask scheduledTask = new ScheduledTask(id, task, priority, category, estimatedTimeMs);
        taskQueue.offer(scheduledTask);
    }
    
    /**
     * Schedule a task with default category
     */
    public void scheduleTask(String id, Runnable task, Priority priority) {
        scheduleTask(id, task, priority, "general", 1.0);
    }
    
    /**
     * Process tasks for this tick
     */
    private void processTick() {
        if (!isRunning) return;
        
        long tickStart = System.nanoTime();
        double tickBudgetUsed = 0;
        
        // Adjust budget based on current TPS
        adjustBudgetBasedOnTPS();
        
        // Reset category budgets
        categories.values().forEach(TaskCategory::reset);
        
        // Process tasks until budget exhausted
        while (!taskQueue.isEmpty() && tickBudgetUsed < currentBudget) {
            ScheduledTask task = taskQueue.peek();
            if (task == null) break;
            
            // Check category budget
            TaskCategory category = categories.get(task.category);
            if (category != null && !category.hasbudgetRemaining()) {
                // This category exhausted, skip for now
                // In a real implementation, you'd want to handle this better
                break;
            }
            
            // Check if we have enough budget for estimated task time
            if (tickBudgetUsed + task.estimatedTime > currentBudget && task.priority != Priority.CRITICAL) {
                break; // Save for next tick
            }
            
            // Execute task
            task = taskQueue.poll();
            if (task != null) {
                long taskStart = System.nanoTime();
                
                try {
                    task.task.run();
                } catch (Exception e) {
                    LoggerUtils.error("Error executing budgeted task " + task.id, e);
                }
                
                long taskTime = System.nanoTime() - taskStart;
                double taskTimeMs = taskTime / 1_000_000.0;
                
                // Update budgets
                tickBudgetUsed += taskTimeMs;
                if (category != null) {
                    category.budgetUsed += taskTimeMs;
                    category.tasksExecuted++;
                }
            }
        }
        
        long tickTime = System.nanoTime() - tickStart;
        double tickTimeMs = tickTime / 1_000_000.0;
        
        // Warn if over budget
        if (tickTimeMs > currentBudget) {
            LoggerUtils.warn(String.format(
                "Tick budget exceeded: %.2fms used / %.2fms budget | Queue: %d tasks",
                tickTimeMs, currentBudget, taskQueue.size()
            ));
        }
    }
    
    /**
     * Adjust budget based on current TPS
     */
    private void adjustBudgetBasedOnTPS() {
        double tps = getTPS();
        
        if (tps >= 19.5) {
            // Server running well, can use more budget
            currentBudget = Math.min(maxTickBudget, currentBudget + 0.5);
        } else if (tps < 18.0) {
            // Server struggling, reduce budget
            currentBudget = Math.max(20.0, currentBudget - 1.0);
        } else if (tps < 19.0) {
            // Server slightly struggling
            currentBudget = Math.max(30.0, currentBudget - 0.5);
        }
    }
    
    /**
     * Get current TPS (simplified)
     */
    private double getTPS() {
        try {
            return com.xreatlabs.xreatoptimizer.utils.TPSUtils.getTPS();
        } catch (Exception e) {
            return 20.0;
        }
    }
    
    /**
     * Get statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queue_size", taskQueue.size());
        stats.put("current_budget_ms", String.format("%.2f", currentBudget));
        stats.put("max_budget_ms", String.format("%.2f", maxTickBudget));
        
        Map<String, Map<String, Object>> categoryStats = new HashMap<>();
        for (Map.Entry<String, TaskCategory> entry : categories.entrySet()) {
            TaskCategory cat = entry.getValue();
            Map<String, Object> catStats = new HashMap<>();
            catStats.put("budget_used_ms", String.format("%.2f", cat.budgetUsed));
            catStats.put("budget_limit_ms", String.format("%.2f", cat.budgetLimit));
            catStats.put("tasks_executed", cat.tasksExecuted);
            catStats.put("tasks_queued", cat.tasksQueued);
            categoryStats.put(entry.getKey(), catStats);
        }
        stats.put("categories", categoryStats);
        
        return stats;
    }
    
    /**
     * Get queue size
     */
    public int getQueueSize() {
        return taskQueue.size();
    }
    
    /**
     * Clear all queued tasks
     */
    public void clearQueue() {
        taskQueue.clear();
        LoggerUtils.info("Cleared tick budget task queue");
    }
}
