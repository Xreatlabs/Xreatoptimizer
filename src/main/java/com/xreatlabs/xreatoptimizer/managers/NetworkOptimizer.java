package com.xreatlabs.xreatoptimizer.managers;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import com.xreatlabs.xreatoptimizer.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages network optimizations like packet batching and frequency throttling
 */
public class NetworkOptimizer {
    private final XreatOptimizer plugin;
    private BukkitTask optimizationTask;
    private final Map<Player, PlayerNetworkStats> playerStats = new WeakHashMap<>();
    private volatile boolean isRunning = false;
    
    private static class PlayerNetworkStats {
        long lastTabUpdate = 0;
        long lastScoreboardUpdate = 0;
        long packetCount = 0;
        long compressedPacketCount = 0;
        double compressionRatio = 1.0;
        
        public PlayerNetworkStats() {}
    }

    public NetworkOptimizer(XreatOptimizer plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts the network optimization system
     */
    public void start() {
        // Network optimizations run every 5 seconds
        optimizationTask = Bukkit.getScheduler().runTaskTimer(
            plugin,
            this::runNetworkOptimizations,
            100L,  // Initial delay (5 seconds)
            100L   // Repeat interval (5 seconds)
        );
        
        isRunning = true;
        LoggerUtils.info("Network optimizer started.");
    }
    
    /**
     * Stops the network optimization system
     */
    public void stop() {
        isRunning = false;
        if (optimizationTask != null) {
            optimizationTask.cancel();
        }
        
        playerStats.clear();
        LoggerUtils.info("Network optimizer stopped.");
    }
    
    /**
     * Enable or disable network optimization
     */
    public void setEnabled(boolean enabled) {
        isRunning = enabled;
        LoggerUtils.info("Network optimizer " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if network optimization is enabled
     */
    public boolean isEnabled() {
        return isRunning;
    }
    
    /**
     * Runs network optimization cycle
     */
    private void runNetworkOptimizations() {
        if (!isRunning) return;
        
        // Update stats for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerStats.computeIfAbsent(player, k -> new PlayerNetworkStats());
        }
        
        // Log network optimization stats periodically
        if (System.currentTimeMillis() % 60000 < 5000) { // Every minute
            LoggerUtils.debug("Network optimization stats: Processed players=" + 
                             Bukkit.getOnlinePlayers().size());
        }
    }
    
    /**
     * Throttles tab list updates for a player based on current server performance
     */
    public boolean shouldThrottleTabUpdate(Player player) {
        if (!plugin.getVersionAdapter().isVersionAtLeast(1, 7)) {
            return true; // Don't apply network optimizations to very old versions
        }
        
        PlayerNetworkStats stats = playerStats.computeIfAbsent(player, k -> new PlayerNetworkStats());
        
        // Throttle tab updates if server is under load
        long minInterval = getOptimalUpdateInterval();
        boolean shouldUpdate = System.currentTimeMillis() - stats.lastTabUpdate > minInterval;
        
        if (shouldUpdate) {
            stats.lastTabUpdate = System.currentTimeMillis();
        }
        
        return !shouldUpdate;
    }
    
    /**
     * Throttles scoreboard updates for a player based on current server performance
     */
    public boolean shouldThrottleScoreboardUpdate(Player player) {
        if (!plugin.getVersionAdapter().isVersionAtLeast(1, 7)) {
            return true; // Don't apply network optimizations to very old versions
        }
        
        PlayerNetworkStats stats = playerStats.computeIfAbsent(player, k -> new PlayerNetworkStats());
        
        // Throttle scoreboard updates if server is under load
        long minInterval = getOptimalUpdateInterval();
        boolean shouldUpdate = System.currentTimeMillis() - stats.lastScoreboardUpdate > minInterval;
        
        if (shouldUpdate) {
            stats.lastScoreboardUpdate = System.currentTimeMillis();
        }
        
        return !shouldUpdate;
    }
    
    /**
     * Gets optimal update interval based on server performance
     */
    private long getOptimalUpdateInterval() {
        double tps = com.xreatlabs.xreatoptimizer.utils.TPSUtils.getTPS();
        
        if (tps > 19.5) {
            return 1000; // 1 second interval when server is doing well
        } else if (tps > 18.0) {
            return 2000; // 2 seconds when server is okay
        } else if (tps > 15.0) {
            return 3000; // 3 seconds when under moderate load
        } else {
            return 5000; // 5 seconds when under heavy load
        }
    }
    
    /**
     * Records a packet for statistics
     */
    public void recordPacket(Player player, boolean wasCompressed) {
        PlayerNetworkStats stats = playerStats.computeIfAbsent(player, k -> new PlayerNetworkStats());
        stats.packetCount++;
        
        if (wasCompressed) {
            stats.compressedPacketCount++;
        }
        
        if (stats.packetCount > 0) {
            stats.compressionRatio = (double) stats.compressedPacketCount / stats.packetCount;
        }
    }
    
    /**
     * Checks if the network optimizer is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Gets statistics for a player
     */
    public PlayerNetworkStats getPlayerStats(Player player) {
        return playerStats.get(player);
    }
}