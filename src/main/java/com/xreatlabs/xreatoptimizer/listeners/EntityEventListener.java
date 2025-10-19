package com.xreatlabs.xreatoptimizer.listeners;

import com.xreatlabs.xreatoptimizer.XreatOptimizer;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * Listener for entity-related events
 * Manages entity limits and stack fusion opportunities
 */
public class EntityEventListener implements Listener {

    private final XreatOptimizer plugin;

    public EntityEventListener(XreatOptimizer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntitySpawn(EntitySpawnEvent event) {
        // Track entity spawns for performance monitoring
        if (plugin.getPerformanceMonitor() != null) {
            plugin.getPerformanceMonitor().incrementEntityCount();
        }

        // NEVER block DROPPED_ITEM spawns - they are managed by ItemDropTracker
        if (event.getEntityType() == EntityType.DROPPED_ITEM) {
            return;
        }

        // Check if we're at entity limits for other entities
        if (plugin.getOptimizationManager() != null) {
            int currentEntities = event.getLocation().getWorld().getEntities().size();
            int maxEntities = plugin.getOptimizationManager().getMaxEntityLimit();

            if (currentEntities >= maxEntities) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemSpawn(ItemSpawnEvent event) {
        // Track item spawn time for timed removal system
        if (plugin.getItemDropTracker() != null) {
            plugin.getItemDropTracker().trackItem(event.getEntity());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Manage hostile/passive mob limits
        if (plugin.getOptimizationManager() != null) {
            boolean isHostile = event.getEntity().getType().name().contains("ZOMBIE") ||
                              event.getEntity().getType().name().contains("SKELETON") ||
                              event.getEntity().getType().name().contains("CREEPER") ||
                              event.getEntity().getType().name().contains("SPIDER");

            int limit = isHostile ?
                plugin.getConfig().getInt("optimization.entity_limits.hostile", 150) :
                plugin.getConfig().getInt("optimization.entity_limits.passive", 200);

            long mobCount = event.getLocation().getWorld().getEntities().stream()
                .filter(e -> {
                    String type = e.getType().name();
                    boolean entityHostile = type.contains("ZOMBIE") || type.contains("SKELETON") ||
                                          type.contains("CREEPER") || type.contains("SPIDER");
                    return isHostile == entityHostile;
                })
                .count();

            if (mobCount >= limit) {
                event.setCancelled(true);
            }
        }
    }
}
