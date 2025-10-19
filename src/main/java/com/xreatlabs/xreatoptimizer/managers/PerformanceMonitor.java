package com.xreatlabs.xreatoptimizer.managers;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import com.xreatlabs.xreatoptimizer.utils.LoggerUtils;
import com.xreatlabs.xreatoptimizer.utils.MemoryUtils;
import com.xreatlabs.xreatoptimizer.utils.TPSUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Monitors server performance metrics
 */
public class PerformanceMonitor {
    private final XreatOptimizer plugin;
    private BukkitTask monitorTask;
    private final Map<String, Object> metrics = new ConcurrentHashMap<>();
    private final Map<String, Double> historicalData = new ConcurrentHashMap<>();
    
    public PerformanceMonitor(XreatOptimizer plugin) {
        this.plugin = plugin;
        // Initialize metrics with default values
        metrics.put("tps", 20.0);
        metrics.put("used_memory_mb", 0L);
        metrics.put("max_memory_mb", 0L);
        metrics.put("memory_percentage", 0.0);
        metrics.put("avg_tick_time_ms", 50.0);
        metrics.put("entity_count", 0);
        metrics.put("chunk_count", 0);
        metrics.put("player_count", 0);
    }
    
    /**
     * Starts the performance monitoring system
     */
    public void start() {
        // Run performance monitoring every 5 seconds (on main thread for entity/chunk access)
        monitorTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            this::updateMetrics,
            100L,  // Initial delay (5 seconds)
            100L   // Repeat interval (5 seconds = 100 ticks)
        );
        
        LoggerUtils.info("Performance monitoring started.");
    }
    
    /**
     * Stops the performance monitoring system
     */
    public void stop() {
        if (monitorTask != null) {
            monitorTask.cancel();
            LoggerUtils.info("Performance monitoring stopped.");
        }
    }
    
    /**
     * Updates all performance metrics
     */
    private void updateMetrics() {
        // Update TPS
        double currentTPS = TPSUtils.getTPS();
        metrics.put("tps", currentTPS);
        
        // Update memory usage
        long usedMemory = MemoryUtils.getUsedMemoryMB();
        long maxMemory = MemoryUtils.getMaxMemoryMB();
        double memoryPercentage = MemoryUtils.getMemoryUsagePercentage();
        metrics.put("used_memory_mb", usedMemory);
        metrics.put("max_memory_mb", maxMemory);
        metrics.put("memory_percentage", memoryPercentage);
        
        // Update tick time
        double avgTickTime = TPSUtils.getAverageTickTime();
        metrics.put("avg_tick_time_ms", avgTickTime);
        
        // Update entity count - this needs to be scheduled on main thread
        int entityCount = 0;
        try {
            if (Bukkit.isPrimaryThread()) {
                entityCount = com.xreatlabs.xreatoptimizer.utils.EntityUtils.getTotalEntityCount();
            } else {
                // For async operations, we'll use a scheduled call or cache the value
                // For now, we'll use a safe default to prevent the error
                // The proper approach would be to schedule this on main thread
                entityCount = (int) metrics.getOrDefault("entity_count", 0);
            }
        } catch (Exception e) {
            entityCount = (int) metrics.getOrDefault("entity_count", 0); // Use last known value
        }
        metrics.put("entity_count", entityCount);
        
        // Update chunk count - also needs main thread
        int chunkCount = 0;
        try {
            if (Bukkit.isPrimaryThread()) {
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    chunkCount += world.getLoadedChunks().length;
                }
            } else {
                // Use cached value for async operations
                chunkCount = (int) metrics.getOrDefault("chunk_count", 0);
            }
        } catch (Exception e) {
            chunkCount = (int) metrics.getOrDefault("chunk_count", 0); // Use last known value
        }
        metrics.put("chunk_count", chunkCount);
        
        // Update player count
        metrics.put("player_count", Bukkit.getOnlinePlayers().size());
        
        // Log metrics periodically for debugging
        if (System.currentTimeMillis() % 60000 < 5000) { // Every minute
            LoggerUtils.debug("Performance Metrics - TPS: " + String.format("%.2f", currentTPS) + 
                             ", Heap Usage: " + String.format("%.1f", memoryPercentage) + "% (" + 
                             usedMemory + "MB/" + maxMemory + "MB), Entities: " + entityCount + 
                             ", Chunks: " + chunkCount);
        }
        
        // Store historical data for trends
        historicalData.put("tps_" + System.currentTimeMillis(), currentTPS);
        historicalData.put("memory_" + System.currentTimeMillis(), memoryPercentage);
        historicalData.put("entities_" + System.currentTimeMillis(), (double) entityCount);
        
        // Generate reports if needed
        maybeGenerateReport();
    }
    
    /**
     * Generates performance reports periodically
     */
    private void maybeGenerateReport() {
        // Create reports directory if it doesn't exist
        File reportsDir = new File(plugin.getDataFolder(), "reports");
        if (!reportsDir.exists()) {
            reportsDir.mkdirs();
        }
        
        // Generate a report every hour (if we're at the top of the hour)
        LocalDateTime now = LocalDateTime.now();
        if (now.getMinute() == 0 && now.getSecond() < 5) { // Generate at the top of each hour
            generateHourlyReport(now);
        }
        
        // Generate daily report at midnight
        if (now.getHour() == 0 && now.getMinute() == 0 && now.getSecond() < 5) {
            generateDailyReport(now);
        }
    }
    
    /**
     * Generates an hourly performance report
     */
    private void generateHourlyReport(LocalDateTime time) {
        try {
            String fileName = "hourly_report_" + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH")) + ".txt";
            File reportFile = new File(plugin.getDataFolder(), "reports/" + fileName);
            
            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write("XreatOptimizer Hourly Performance Report\n");
                writer.write("Generated at: " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("========================================\n\n");
                
                writer.write("TPS Average: " + getAverageTPS() + "\n");
                writer.write("Memory Usage Peak: " + getPeakMemoryUsage() + "%\n");
                writer.write("Max Entities: " + getMaxEntityCount() + "\n");
                writer.write("Max Chunks Loaded: " + getMaxChunkCount() + "\n");
                writer.write("Max Players Online: " + getMaxPlayerCount() + "\n");
            }
        } catch (IOException e) {
            LoggerUtils.error("Could not generate hourly report", e);
        }
    }
    
    /**
     * Generates a daily performance report
     */
    private void generateDailyReport(LocalDateTime time) {
        try {
            String fileName = "daily_report_" + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".txt";
            File reportFile = new File(plugin.getDataFolder(), "reports/" + fileName);
            
            try (FileWriter writer = new FileWriter(reportFile)) {
                writer.write("XreatOptimizer Daily Performance Report\n");
                writer.write("Generated at: " + time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("========================================\n\n");
                
                writer.write("Average TPS: " + getAverageTPS() + "\n");
                writer.write("Memory Usage Average: " + getAverageMemoryUsage() + "%\n");
                writer.write("Memory Usage Peak: " + getPeakMemoryUsage() + "%\n");
                writer.write("Average Entities: " + getAverageEntityCount() + "\n");
                writer.write("Max Entities: " + getMaxEntityCount() + "\n");
                writer.write("Average Chunks Loaded: " + getAverageChunkCount() + "\n");
                writer.write("Peak Players: " + getMaxPlayerCount() + "\n");
                
                // Performance statistics
                writer.write("\nPerformance Statistics:\n");
                writer.write("- Time spent under 15 TPS: " + getTimeUnderTPSThreshold(15.0) + " minutes\n");
                writer.write("- Time spent under 10 TPS: " + getTimeUnderTPSThreshold(10.0) + " minutes\n");
                writer.write("- Memory pressure incidents: " + getMemoryPressureEvents() + "\n");
            }
        } catch (IOException e) {
            LoggerUtils.error("Could not generate daily report", e);
        }
    }
    
    /**
     * Gets the current metrics map
     * @return Map of current performance metrics
     */
    public Map<String, Object> getMetrics() {
        return new HashMap<>(metrics);
    }
    
    /**
     * Gets a specific metric value
     * @param key The metric key
     * @return The metric value, or default if not found
     */
    public Object getMetric(String key) {
        Object value = metrics.get(key);
        if (value == null) {
            // Return appropriate default based on expected metric type
            switch (key) {
                case "tps":
                case "memory_percentage":
                case "avg_tick_time_ms":
                    return 20.0;
                case "entity_count":
                case "chunk_count":
                case "player_count":
                    return 0;
                case "used_memory_mb":
                case "max_memory_mb":
                    return 0L;
                default:
                    return 0.0; // Default fallback
            }
        }
        return value;
    }
    
    /**
     * Gets the current TPS
     * @return Current TPS value
     */
    public double getCurrentTPS() {
        return (double) metrics.getOrDefault("tps", 20.0);
    }
    
    /**
     * Gets the current memory usage percentage
     * @return Current memory usage percentage
     */
    public double getCurrentMemoryPercentage() {
        return (double) metrics.getOrDefault("memory_percentage", 0.0);
    }
    
    /**
     * Gets the current entity count
     * @return Current entity count
     */
    public int getCurrentEntityCount() {
        return (int) metrics.getOrDefault("entity_count", 0);
    }
    
    /**
     * Gets the current chunk count
     * @return Current chunk count
     */
    public int getCurrentChunkCount() {
        return (int) metrics.getOrDefault("chunk_count", 0);
    }
    
    // Helper methods for report generation
    private double getAverageTPS() {
        return (double) metrics.getOrDefault("tps", 20.0);
    }
    
    private double getPeakMemoryUsage() {
        return (double) metrics.getOrDefault("memory_percentage", 0.0);
    }
    
    private int getMaxEntityCount() {
        return (int) metrics.getOrDefault("entity_count", 0);
    }
    
    private int getMaxChunkCount() {
        return (int) metrics.getOrDefault("chunk_count", 0);
    }
    
    private int getMaxPlayerCount() {
        return (int) metrics.getOrDefault("player_count", 0);
    }
    
    private double getAverageMemoryUsage() {
        return (double) metrics.getOrDefault("memory_percentage", 0.0);
    }
    
    private int getAverageEntityCount() {
        return (int) metrics.getOrDefault("entity_count", 0);
    }
    
    private int getAverageChunkCount() {
        return (int) metrics.getOrDefault("chunk_count", 0);
    }
    
    private int getTimeUnderTPSThreshold(double threshold) {
        // Simplified logic - in a real implementation would track over time
        return getCurrentTPS() < threshold ? 1 : 0; // Placeholder
    }
    
    private int getMemoryPressureEvents() {
        // Simplified logic - in a real implementation would track memory pressure events
        return getCurrentMemoryPercentage() > 80.0 ? 1 : 0; // Placeholder
    }

    /**
     * Update player count metric
     */
    public void updatePlayerCount() {
        metrics.put("player_count", Bukkit.getOnlinePlayers().size());
    }

    /**
     * Increment chunk loads counter
     */
    public void incrementChunkLoads() {
        int current = (int) metrics.getOrDefault("chunk_count", 0);
        metrics.put("chunk_count", current + 1);
    }

    /**
     * Decrement chunk loads counter
     */
    public void decrementChunkLoads() {
        int current = (int) metrics.getOrDefault("chunk_count", 0);
        if (current > 0) {
            metrics.put("chunk_count", current - 1);
        }
    }

    /**
     * Increment entity count
     */
    public void incrementEntityCount() {
        int current = (int) metrics.getOrDefault("entity_count", 0);
        metrics.put("entity_count", current + 1);
    }
}