package com.xreatlabs.xreatoptimizer.config;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import org.bukkit.ChatColor;

/**
 * Handles dynamic configuration reloading
 * Updates all managers with new configuration values
 */
public class ConfigReloader {

    private final XreatOptimizer plugin;

    public ConfigReloader(XreatOptimizer plugin) {
        this.plugin = plugin;
    }

    /**
     * Reload configuration and apply changes to all managers
     */
    public void reloadConfiguration() {
        plugin.getLogger().info("Reloading XreatOptimizer configuration...");

        // Reload config from disk
        plugin.reloadConfig();

        // Update optimization manager settings
        if (plugin.getOptimizationManager() != null) {
            plugin.getOptimizationManager().reloadConfig();
        }

        // Update hibernate manager
        if (plugin.getHibernateManager() != null) {
            boolean hibernateEnabled = plugin.getConfig().getBoolean("hibernate.enabled", true);
            int radius = plugin.getConfig().getInt("hibernate.radius", 64);
            plugin.getHibernateManager().setEnabled(hibernateEnabled);
            plugin.getHibernateManager().setRadius(radius);
        }

        // Update empty server optimizer
        if (plugin.getEmptyServerOptimizer() != null) {
            plugin.getEmptyServerOptimizer().reloadConfig();
        }

        // Update auto-clear task interval
        if (plugin.getAutoClearTask() != null) {
            plugin.getAutoClearTask().reloadConfig();
        }

        // Update chunk pre-generator settings
        if (plugin.getChunkPreGenerator() != null) {
            int maxThreads = plugin.getConfig().getInt("pregen.max_threads", 2);
            int defaultSpeed = plugin.getConfig().getInt("pregen.default_speed", 100);
            plugin.getChunkPreGenerator().setMaxThreads(maxThreads);
            plugin.getChunkPreGenerator().setDefaultSpeed(defaultSpeed);
        }

        // Update dynamic view distance
        if (plugin.getDynamicViewDistance() != null) {
            plugin.getDynamicViewDistance().reloadConfig();
        }

        // Update entity optimizer
        if (plugin.getAdvancedEntityOptimizer() != null) {
            boolean stackFusion = plugin.getConfig().getBoolean("enable_stack_fusion", true);
            plugin.getAdvancedEntityOptimizer().setStackFusionEnabled(stackFusion);
        }

        // Update memory saver
        if (plugin.getMemorySaver() != null) {
            boolean compressCache = plugin.getConfig().getBoolean("compress_ram_cache", true);
            int threshold = plugin.getConfig().getInt("memory_reclaim_threshold_percent", 80);
            plugin.getMemorySaver().setCompressionEnabled(compressCache);
            plugin.getMemorySaver().setThreshold(threshold);
        }

        // Update announcement system (but branding cannot change)
        if (plugin.getAnnouncementSystem() != null) {
            int broadcastInterval = plugin.getConfig().getInt("general.broadcast_interval_minutes", 30);
            plugin.getAnnouncementSystem().setBroadcastInterval(broadcastInterval);
        }

        plugin.getLogger().info(ChatColor.GREEN + "Configuration reloaded successfully!");
    }

    /**
     * Validate configuration values
     */
    public boolean validateConfig() {
        boolean valid = true;

        // Validate TPS thresholds
        double light = plugin.getConfig().getDouble("optimization.tps_thresholds.light", 19.5);
        double normal = plugin.getConfig().getDouble("optimization.tps_thresholds.normal", 18.0);
        double aggressive = plugin.getConfig().getDouble("optimization.tps_thresholds.aggressive", 16.0);

        if (light <= normal || normal <= aggressive) {
            plugin.getLogger().warning("Invalid TPS thresholds: light > normal > aggressive required!");
            valid = false;
        }

        // Validate entity limits
        int passive = plugin.getConfig().getInt("optimization.entity_limits.passive", 200);
        int hostile = plugin.getConfig().getInt("optimization.entity_limits.hostile", 150);
        int item = plugin.getConfig().getInt("optimization.entity_limits.item", 1000);

        if (passive < 0 || hostile < 0 || item < 0) {
            plugin.getLogger().warning("Entity limits must be positive numbers!");
            valid = false;
        }

        // Validate hibernate radius
        int radius = plugin.getConfig().getInt("hibernate.radius", 64);
        if (radius < 16 || radius > 512) {
            plugin.getLogger().warning("Hibernate radius should be between 16 and 512 blocks!");
            valid = false;
        }

        // Validate empty server delay
        int delay = plugin.getConfig().getInt("empty_server.delay_seconds", 30);
        if (delay < 5 || delay > 600) {
            plugin.getLogger().warning("Empty server delay should be between 5 and 600 seconds!");
            valid = false;
        }

        return valid;
    }

    /**
     * Reset configuration to defaults
     */
    public void resetToDefaults() {
        plugin.getLogger().info("Resetting configuration to defaults...");
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        reloadConfiguration();
    }
}
