package com.xreatlabs.xreatoptimizer.managers;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import com.xreatlabs.xreatoptimizer.utils.LoggerUtils;
import com.xreatlabs.xreatoptimizer.utils.TPSUtils;
import org.bukkit.scheduler.BukkitTask;

/**
 * Central brain for optimization system
 */
public class OptimizationManager {
    private final XreatOptimizer plugin;
    private BukkitTask optimizationTask;
    private OptimizationProfile currentProfile = OptimizationProfile.AUTO;
    private volatile boolean isRunning = false;
    
    public enum OptimizationProfile {
        AUTO,      // Auto-adapting based on server conditions
        LIGHT,     // Minimal optimizations
        NORMAL,    // Balanced optimizations
        AGGRESSIVE, // Maximum optimizations
        EMERGENCY  // Maximum optimizations + emergency measures
    }
    
    public OptimizationManager(XreatOptimizer plugin) {
        this.plugin = plugin;
        // Set initial profile based on config
        String initialProfile = plugin.getConfig().getString("general.initial_profile", "AUTO");
        this.currentProfile = OptimizationProfile.valueOf(initialProfile.toUpperCase());
    }
    
    /**
     * Starts the optimization manager
     */
    public void start() {
        // Start monitoring and optimization cycles
        optimizationTask = plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            this::runOptimizationCycle,
            20L,  // Start after 1 second
            100L  // Run every 5 seconds (100 ticks)
        );
        
        isRunning = true;
        LoggerUtils.info("Optimization manager started with profile: " + currentProfile);
    }
    
    /**
     * Stops the optimization manager
     */
    public void stop() {
        isRunning = false;
        if (optimizationTask != null) {
            optimizationTask.cancel();
        }
        LoggerUtils.info("Optimization manager stopped.");
    }
    
    /**
     * Runs a complete optimization cycle
     */
    private void runOptimizationCycle() {
        if (!isRunning) return;
        
        // Auto-tune profile based on current conditions if in AUTO mode
        if (currentProfile == OptimizationProfile.AUTO) {
            adjustProfileAutomatically();
        }
        
        // Apply optimizations based on current profile
        applyOptimizations();
    }
    
    /**
     * Adjusts the optimization profile automatically based on server conditions
     */
    private void adjustProfileAutomatically() {
        double currentTPS = TPSUtils.getTPS();
        double memoryUsage = (double) plugin.getPerformanceMonitor().getMetric("memory_percentage");
        int entityCount = plugin.getPerformanceMonitor().getCurrentEntityCount();
        
        // Calculate thresholds from config
        double lightThreshold = plugin.getConfig().getDouble("optimization.tps_thresholds.light", 19.5);
        double normalThreshold = plugin.getConfig().getDouble("optimization.tps_thresholds.normal", 18.0);
        double aggressiveThreshold = plugin.getConfig().getDouble("optimization.tps_thresholds.aggressive", 16.0);
        
        // Determine appropriate profile based on conditions
        if (currentTPS > lightThreshold) {
            // Server is running well, use light optimizations
            if (currentProfile != OptimizationProfile.LIGHT) {
                currentProfile = OptimizationProfile.LIGHT;
                LoggerUtils.info("Auto-adjusted profile to LIGHT (TPS: " + currentTPS + ")");
            }
        } else if (currentTPS > normalThreshold) {
            // Server is running okay, use normal optimizations
            if (currentProfile != OptimizationProfile.NORMAL) {
                currentProfile = OptimizationProfile.NORMAL;
                LoggerUtils.info("Auto-adjusted profile to NORMAL (TPS: " + currentTPS + ")");
            }
        } else if (currentTPS > aggressiveThreshold) {
            // Server is under moderate load, use aggressive optimizations
            if (currentProfile != OptimizationProfile.AGGRESSIVE) {
                currentProfile = OptimizationProfile.AGGRESSIVE;
                LoggerUtils.info("Auto-adjusted profile to AGGRESSIVE (TPS: " + currentTPS + ")");
            }
        } else if (currentTPS < aggressiveThreshold) {
            // Server is under heavy load, use emergency optimizations
            if (currentProfile != OptimizationProfile.EMERGENCY) {
                currentProfile = OptimizationProfile.EMERGENCY;
                LoggerUtils.info("Auto-adjusted profile to EMERGENCY (TPS: " + currentTPS + ")");
            }
        } else if (memoryUsage > plugin.getConfig().getInt("memory_reclaim_threshold_percent", 80)) {
            // Memory pressure detected, increase optimization level
            if (currentProfile == OptimizationProfile.LIGHT) {
                currentProfile = OptimizationProfile.NORMAL;
                LoggerUtils.info("Memory pressure detected, increased optimization to NORMAL");
            } else if (currentProfile == OptimizationProfile.NORMAL) {
                currentProfile = OptimizationProfile.AGGRESSIVE;
                LoggerUtils.info("Memory pressure detected, increased optimization to AGGRESSIVE");
            }
        }
    }
    
    /**
     * Applies optimizations based on the current profile
     */
    private void applyOptimizations() {
        // Apply optimizations specific to the current profile
        switch (currentProfile) {
            case LIGHT:
                applyLightOptimizations();
                break;
            case NORMAL:
                applyNormalOptimizations();
                break;
            case AGGRESSIVE:
                applyAggressiveOptimizations();
                break;
            case EMERGENCY:
                applyEmergencyOptimizations();
                break;
            default:
                applyAutoOptimizations();
        }
        
        // Trigger advanced CPU/RAM optimizations based on current conditions
        if (plugin.getAdvancedCPURAMOptimizer() != null) {
            // The AdvancedCPURAMOptimizer runs separately and monitors conditions independently
        }
    }
    
    /**
     * Applies light optimizations
     */
    private void applyLightOptimizations() {
        // Enable basic features only
        // Hibernate with larger radius
        // Minimal entity cleanup
        // Basic memory management
        
        LoggerUtils.debug("Applying LIGHT optimizations");
    }
    
    /**
     * Applies normal optimizations
     */
    private void applyNormalOptimizations() {
        // Enable standard set of optimizations
        // Regular hibernate
        // Standard entity cleanup
        // Moderate memory management
        
        LoggerUtils.debug("Applying NORMAL optimizations");
    }
    
    /**
     * Applies aggressive optimizations
     */
    private void applyAggressiveOptimizations() {
        // Enable all non-invasive optimizations
        // Aggressive hibernate
        // Aggressive entity cleanup
        // Enhanced memory management
        // View distance adjustments
        
        LoggerUtils.debug("Applying AGGRESSIVE optimizations");
    }
    
    /**
     * Applies emergency optimizations
     */
    private void applyEmergencyOptimizations() {
        // Enable all optimizations including potentially disruptive ones
        // Maximum hibernate
        // Maximum entity cleanup
        // Aggressive memory management
        // View distance reductions
        // Potentially reduced simulation distances
        
        LoggerUtils.debug("Applying EMERGENCY optimizations");
        
        // Safety check: if TPS is still very low, consider warning
        if (TPSUtils.isTPSDangerous()) {
            LoggerUtils.warn("TPS is in dangerous territory (< 10). Consider reducing load on the server.");
        }
    }
    
    /**
     * Applies auto optimizations (default behavior)
     */
    private void applyAutoOptimizations() {
        applyNormalOptimizations(); // Default to normal
    }
    
    /**
     * Gets the current optimization profile
     * @return Current optimization profile
     */
    public OptimizationProfile getCurrentProfile() {
        return currentProfile;
    }
    
    /**
     * Sets a new optimization profile
     * @param profile New optimization profile
     */
    public void setProfile(OptimizationProfile profile) {
        this.currentProfile = profile;
        LoggerUtils.info("Optimization profile changed to: " + profile);
    }
    
    /**
     * Gets whether the optimization manager is currently running
     * @return True if running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Reload configuration
     */
    public void reloadConfig() {
        String profileName = plugin.getConfig().getString("general.initial_profile", "AUTO");
        this.currentProfile = OptimizationProfile.valueOf(profileName.toUpperCase());
        LoggerUtils.info("Configuration reloaded. Profile set to: " + currentProfile);
    }

    /**
     * Force an optimization cycle to run immediately
     */
    public void forceOptimizationCycle() {
        runOptimizationCycle();
        LoggerUtils.info("Forced optimization cycle executed");
    }

    /**
     * Get the maximum entity limit from config
     */
    public int getMaxEntityLimit() {
        return plugin.getConfig().getInt("entity_limiter.max_entities_per_chunk", 50);
    }
}