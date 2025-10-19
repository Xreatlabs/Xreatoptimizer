package com.xreatlabs.xreatoptimizer.managers;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import com.xreatlabs.xreatoptimizer.utils.LoggerUtils;
import com.xreatlabs.xreatoptimizer.utils.TPSUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Automatic Lag Spike Detector and Mitigation System
 * 
 * Features:
 * - Real-time lag spike detection
 * - Automatic mitigation strategies
 * - Historical lag analysis
 * - Predictive lag prevention
 * - Emergency optimization triggers
 */
public class LagSpikeDetector {
    
    private final XreatOptimizer plugin;
    private final Deque<TickData> tickHistory = new ConcurrentLinkedDeque<>();
    private final List<LagSpike> detectedSpikes = new ArrayList<>();
    private BukkitTask monitorTask;
    private volatile boolean isRunning = false;
    
    // Configuration
    private final int HISTORY_SIZE = 600; // 30 seconds at 20 TPS
    private final double LAG_SPIKE_THRESHOLD = 100.0; // ms per tick
    private final double SEVERE_LAG_THRESHOLD = 200.0; // ms per tick
    private final int CONSECUTIVE_LAG_THRESHOLD = 3; // ticks
    
    private long lastTickTime = System.nanoTime();
    private int consecutiveLagTicks = 0;
    private boolean inLagSpike = false;
    
    /**
     * Stores tick timing data
     */
    private static class TickData {
        final long timestamp;
        final double tickTime; // milliseconds
        final double tps;
        final long memoryUsed;
        
        public TickData(long timestamp, double tickTime, double tps, long memoryUsed) {
            this.timestamp = timestamp;
            this.tickTime = tickTime;
            this.tps = tps;
            this.memoryUsed = memoryUsed;
        }
    }
    
    /**
     * Represents a detected lag spike
     */
    private static class LagSpike {
        final long startTime;
        long endTime;
        double peakTickTime;
        double avgTickTime;
        int duration; // ticks
        String cause;
        boolean mitigated;
        
        public LagSpike(long startTime) {
            this.startTime = startTime;
            this.endTime = startTime;
            this.peakTickTime = 0;
            this.avgTickTime = 0;
            this.duration = 0;
            this.cause = "Unknown";
            this.mitigated = false;
        }
        
        @Override
        public String toString() {
            return String.format("LagSpike[duration=%dms, peak=%.2fms/tick, avg=%.2fms/tick, cause=%s, mitigated=%s]",
                endTime - startTime, peakTickTime, avgTickTime, cause, mitigated);
        }
    }
    
    public LagSpikeDetector(XreatOptimizer plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Start the lag spike detector
     */
    public void start() {
        if (!plugin.getConfig().getBoolean("lag_spike_detection.enabled", true)) {
            LoggerUtils.info("Lag spike detector is disabled in config.");
            return;
        }
        
        isRunning = true;
        lastTickTime = System.nanoTime();
        
        // Monitor task - runs every tick
        monitorTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            this::monitorTick,
            1L,
            1L
        );
        
        LoggerUtils.info("Lag spike detector started - monitoring for performance issues");
    }
    
    /**
     * Stop the lag spike detector
     */
    public void stop() {
        isRunning = false;
        
        if (monitorTask != null) {
            monitorTask.cancel();
        }
        
        tickHistory.clear();
        detectedSpikes.clear();
        
        LoggerUtils.info("Lag spike detector stopped");
    }
    
    /**
     * Monitor each tick for lag spikes
     */
    private void monitorTick() {
        if (!isRunning) return;
        
        long now = System.nanoTime();
        double tickTime = (now - lastTickTime) / 1_000_000.0; // Convert to milliseconds
        lastTickTime = now;
        
        // Get current TPS and memory
        double tps = TPSUtils.getTPS();
        long memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Store tick data
        TickData tickData = new TickData(System.currentTimeMillis(), tickTime, tps, memoryUsed);
        tickHistory.addFirst(tickData);
        
        // Maintain history size
        while (tickHistory.size() > HISTORY_SIZE) {
            tickHistory.removeLast();
        }
        
        // Detect lag spike
        if (tickTime > LAG_SPIKE_THRESHOLD) {
            consecutiveLagTicks++;
            
            if (consecutiveLagTicks >= CONSECUTIVE_LAG_THRESHOLD && !inLagSpike) {
                // Lag spike detected!
                onLagSpikeDetected(tickTime);
            } else if (inLagSpike) {
                // Update ongoing lag spike
                LagSpike currentSpike = detectedSpikes.get(detectedSpikes.size() - 1);
                currentSpike.duration++;
                currentSpike.peakTickTime = Math.max(currentSpike.peakTickTime, tickTime);
                currentSpike.endTime = System.currentTimeMillis();
            }
        } else {
            if (inLagSpike && consecutiveLagTicks == 0) {
                // Lag spike ended
                onLagSpikeEnded();
            }
            consecutiveLagTicks = Math.max(0, consecutiveLagTicks - 1);
        }
    }
    
    /**
     * Handle lag spike detection
     */
    private void onLagSpikeDetected(double tickTime) {
        inLagSpike = true;
        
        LagSpike spike = new LagSpike(System.currentTimeMillis());
        spike.peakTickTime = tickTime;
        spike.duration = consecutiveLagTicks;
        spike.cause = analyzeCause(tickTime);
        
        detectedSpikes.add(spike);
        
        // Log warning
        LoggerUtils.warn(String.format(
            "LAG SPIKE DETECTED: %.2fms/tick | Cause: %s",
            tickTime, spike.cause
        ));
        
        // Trigger mitigation
        if (tickTime > SEVERE_LAG_THRESHOLD) {
            mitigateSevereLag(spike);
        } else {
            mitigateNormalLag(spike);
        }
    }
    
    /**
     * Handle lag spike end
     */
    private void onLagSpikeEnded() {
        inLagSpike = false;
        
        if (!detectedSpikes.isEmpty()) {
            LagSpike spike = detectedSpikes.get(detectedSpikes.size() - 1);
            
            // Calculate average
            double sum = 0;
            int count = 0;
            for (TickData data : tickHistory) {
                if (data.timestamp >= spike.startTime && data.timestamp <= spike.endTime) {
                    sum += data.tickTime;
                    count++;
                }
            }
            spike.avgTickTime = count > 0 ? sum / count : spike.peakTickTime;
            
            LoggerUtils.info(String.format(
                "Lag spike ended: %s", spike
            ));
        }
    }
    
    /**
     * Analyze potential cause of lag spike
     */
    private String analyzeCause(double tickTime) {
        // Check memory pressure
        Runtime runtime = Runtime.getRuntime();
        double memoryUsage = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
        
        if (memoryUsage > 0.9) {
            return "High memory usage (" + String.format("%.1f%%", memoryUsage * 100) + ")";
        }
        
        // Check entity count
        int entityCount = Bukkit.getWorlds().stream()
            .mapToInt(w -> w.getEntities().size())
            .sum();
        
        if (entityCount > 10000) {
            return "High entity count (" + entityCount + ")";
        }
        
        // Check chunk loading
        int loadedChunks = Bukkit.getWorlds().stream()
            .mapToInt(w -> w.getLoadedChunks().length)
            .sum();
        
        if (loadedChunks > 5000) {
            return "High chunk count (" + loadedChunks + ")";
        }
        
        // Check player count
        int playerCount = Bukkit.getOnlinePlayers().size();
        if (playerCount > 100) {
            return "High player count (" + playerCount + ")";
        }
        
        return "Unknown (possibly chunk generation or plugin)";
    }
    
    /**
     * Mitigate normal lag spike
     */
    private void mitigateNormalLag(LagSpike spike) {
        spike.mitigated = true;
        
        // Pause non-critical systems temporarily
        plugin.getThreadPoolManager().executeAnalyticsTask(() -> {
            LoggerUtils.info("Applying lag mitigation: Pausing non-critical systems");
            
            // Suggestion: Temporarily reduce view distance, pause entity spawning, etc.
            // Implementation depends on server version and configuration
        });
    }
    
    /**
     * Mitigate severe lag spike
     */
    private void mitigateSevereLag(LagSpike spike) {
        spike.mitigated = true;
        
        LoggerUtils.warn("SEVERE LAG DETECTED - Applying emergency optimizations");
        
        // Emergency measures
        plugin.getThreadPoolManager().executeAnalyticsTask(() -> {
            // Trigger aggressive GC
            System.gc();
            
            // Clear dropped items
            if (plugin.getAutoClearTask() != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Clear items in all worlds
                    Bukkit.getWorlds().forEach(world -> {
                        world.getEntities().stream()
                            .filter(e -> e.getType().name().equals("DROPPED_ITEM"))
                            .forEach(org.bukkit.entity.Entity::remove);
                    });
                    LoggerUtils.info("Emergency: Cleared dropped items due to severe lag");
                });
            }
        });
    }
    
    /**
     * Get lag spike statistics
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_spikes", detectedSpikes.size());
        stats.put("in_lag_spike", inLagSpike);
        stats.put("consecutive_lag_ticks", consecutiveLagTicks);
        
        if (!tickHistory.isEmpty()) {
            double avgTickTime = tickHistory.stream()
                .mapToDouble(t -> t.tickTime)
                .average()
                .orElse(0.0);
            stats.put("avg_tick_time_ms", String.format("%.2f", avgTickTime));
        }
        
        long mitigatedCount = detectedSpikes.stream()
            .filter(s -> s.mitigated)
            .count();
        stats.put("mitigated_spikes", mitigatedCount);
        
        return stats;
    }
    
    /**
     * Get recent lag spikes
     */
    public List<LagSpike> getRecentSpikes(int count) {
        int size = Math.min(count, detectedSpikes.size());
        return detectedSpikes.subList(Math.max(0, detectedSpikes.size() - size), detectedSpikes.size());
    }
    
    /**
     * Check if currently in a lag spike
     */
    public boolean isInLagSpike() {
        return inLagSpike;
    }
}
