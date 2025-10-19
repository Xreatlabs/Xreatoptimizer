package com.xreatlabs.xreatoptimizer.managers;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import com.xreatlabs.xreatoptimizer.utils.EntityUtils;
import com.xreatlabs.xreatoptimizer.utils.LoggerUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Manages automatic clearing of excess entities
 */
public class AutoClearTask {
    private final XreatOptimizer plugin;
    private BukkitTask clearTask;
    private volatile boolean isRunning = false;
    
    public AutoClearTask(XreatOptimizer plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Starts the auto clear task system
     */
    public void start() {
        int intervalSeconds = plugin.getConfig().getInt("clear_interval_seconds", 300); // 5 minutes default
        
        clearTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::runClearCycle,
            intervalSeconds * 20L,  // Initial delay
            intervalSeconds * 20L   // Repeat interval
        );
        
        isRunning = true;
        LoggerUtils.info("Auto clear task started. Will run every " + intervalSeconds + " seconds.");
    }
    
    /**
     * Stops the auto clear task system
     */
    public void stop() {
        isRunning = false;
        if (clearTask != null) {
            clearTask.cancel();
        }
        LoggerUtils.info("Auto clear task stopped.");
    }
    
    /**
     * Runs a clear cycle to remove excess entities
     */
    private void runClearCycle() {
        if (!isRunning) return;
        
        LoggerUtils.debug("Running auto clear cycle...");
        
        int totalRemoved = 0;
        
        // Process each world
        for (World world : Bukkit.getWorlds()) {
            int removed = clearExcessEntitiesInWorld(world);
            totalRemoved += removed;
            
            if (removed > 0) {
                LoggerUtils.debug("Cleared " + removed + " excess entities in world: " + world.getName());
            }
        }
        
        if (totalRemoved > 0) {
            LoggerUtils.info("Auto clear task completed. Removed " + totalRemoved + " excess entities across all worlds.");
        }
    }
    
    /**
     * Clears excess entities in a specific world based on thresholds
     */
    private int clearExcessEntitiesInWorld(World world) {
        int totalRemoved = 0;
        
        // Get thresholds from config
        int passiveLimit = plugin.getConfig().getInt("optimization.entity_limits.passive", 200);
        int hostileLimit = plugin.getConfig().getInt("optimization.entity_limits.hostile", 150);
        int itemLimit = plugin.getConfig().getInt("optimization.entity_limits.item", 1000);
        
        // Define entity types to clear (excluding DROPPED_ITEM - now handled by ItemDropTracker)
        Map<EntityType, Integer> limits = Map.of(
            EntityType.EXPERIENCE_ORB, itemLimit,
            EntityType.ARROW, 50, // Arrows typically don't persist long
            EntityType.SPECTRAL_ARROW, 50,
            EntityType.ENDER_PEARL, 20,
            EntityType.SNOWBALL, 20,
            EntityType.EGG, 20
        );
        
        // Process each entity type with limits
        for (Map.Entry<EntityType, Integer> entry : limits.entrySet()) {
            EntityType type = entry.getKey();
            int limit = entry.getValue();
            
            int removed = EntityUtils.removeExcessEntities(world, type, limit);
            totalRemoved += removed;
        }
        
        // Clear excess passive mobs (animals, ambient mobs, etc.)
        for (EntityType passive : Arrays.asList(
                EntityType.PIG, EntityType.COW, EntityType.SHEEP, EntityType.CHICKEN,
                EntityType.MUSHROOM_COW, EntityType.RABBIT, EntityType.HORSE, EntityType.DONKEY,
                EntityType.MULE, EntityType.LLAMA, EntityType.CAT, EntityType.OCELOT,
                EntityType.PARROT, EntityType.VILLAGER, EntityType.SNOWMAN, EntityType.IRON_GOLEM
        )) {
            int limit = plugin.getConfig().getInt("optimization.entity_limits.passive", 200);
            int removed = EntityUtils.removeExcessEntities(world, passive, limit);
            totalRemoved += removed;
        }
        
        // Clear excess hostile mobs
        for (EntityType hostile : Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.ENDERMAN, EntityType.SLIME, EntityType.ENDER_DRAGON, EntityType.WITHER,
                EntityType.BLAZE, EntityType.CAVE_SPIDER, EntityType.GHAST, EntityType.MAGMA_CUBE,
                EntityType.ZOMBIFIED_PIGLIN, EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.STRAY,
                EntityType.HUSK, EntityType.ZOMBIE_VILLAGER, EntityType.PHANTOM, EntityType.DROWNED,
                EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.ILLUSIONER, EntityType.RAVAGER
        )) {
            int limit = plugin.getConfig().getInt("optimization.entity_limits.hostile", 150);
            int removed = EntityUtils.removeExcessEntities(world, hostile, limit);
            totalRemoved += removed;
        }
        
        return totalRemoved;
    }
    
    /**
     * Performs a one-time immediate clear of all excess entities
     * @return Number of entities removed
     */
    public int immediateClear() {
        int totalRemoved = 0;
        
        for (World world : Bukkit.getWorlds()) {
            totalRemoved += clearExcessEntitiesInWorld(world);
        }
        
        LoggerUtils.info("Immediate clear completed. Removed " + totalRemoved + " entities.");
        return totalRemoved;
    }
    
    /**
     * Clears specific entity types in a world
     * @param world World to clear in
     * @param type Entity type to clear
     * @param limit Maximum number to keep
     * @return Number of entities removed
     */
    public int clearSpecificType(World world, EntityType type, int limit) {
        int removed = EntityUtils.removeExcessEntities(world, type, limit);
        if (removed > 0) {
            LoggerUtils.info("Cleared " + removed + " " + type.name() + " entities in world " + world.getName());
        }
        return removed;
    }
    
    /**
     * Asynchronously clears entities to avoid main thread lag
     * @return CompletableFuture for tracking completion
     */
    public CompletableFuture<Integer> asyncClear() {
        return CompletableFuture.supplyAsync(this::immediateClear, plugin.getThreadPoolManager().getEntityCleanupPool());
    }
    
    /**
     * Checks if the auto clear task is running
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Reload configuration
     */
    public void reloadConfig() {
        if (isRunning) {
            stop();
        }
        start();
    }

    /**
     * Manually clear entities across all worlds
     */
    public void clearEntities() {
        immediateClear();
    }
}