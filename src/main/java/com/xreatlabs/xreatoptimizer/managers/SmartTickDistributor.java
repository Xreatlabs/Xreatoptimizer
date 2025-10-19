package com.xreatlabs.xreatoptimizer.managers;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import com.xreatlabs.xreatoptimizer.utils.LoggerUtils;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Distributes heavy tick tasks across multiple ticks to avoid spikes
 */
public class SmartTickDistributor {
    private final XreatOptimizer plugin;
    private final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
    private volatile boolean isRunning = false;
    private int tasksPerTick = 5; // Default number of tasks to execute per tick
    
    public SmartTickDistributor(XreatOptimizer plugin) {
        this.plugin = plugin;
        this.tasksPerTick = plugin.getConfig().getInt("smart_tick.tasks_per_tick", 5);
    }
    
    /**
     * Starts the smart tick distributor
     */
    public void start() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    this.cancel();
                    return;
                }
                
                // Execute a limited number of tasks per tick to avoid spikes
                for (int i = 0; i < tasksPerTick && !taskQueue.isEmpty(); i++) {
                    Runnable task = taskQueue.poll();
                    if (task != null) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            LoggerUtils.error("Error executing distributed task", e);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L); // Run every tick
        
        isRunning = true;
        LoggerUtils.info("Smart tick distributor started (" + tasksPerTick + " tasks per tick).");
    }
    
    /**
     * Adds a task to be distributed across ticks
     */
    public void addTask(Runnable task) {
        taskQueue.add(task);
    }
    
    /**
     * Adds multiple tasks to be distributed
     */
    public void addTasks(Iterable<Runnable> tasks) {
        for (Runnable task : tasks) {
            addTask(task);
        }
    }
    
    /**
     * Gets the number of queued tasks
     */
    public int getQueuedTaskCount() {
        return taskQueue.size();
    }
    
    /**
     * Checks if the distributor is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Stops the smart tick distributor
     */
    public void stop() {
        isRunning = false;
        taskQueue.clear();
        LoggerUtils.info("Smart tick distributor stopped.");
    }
}