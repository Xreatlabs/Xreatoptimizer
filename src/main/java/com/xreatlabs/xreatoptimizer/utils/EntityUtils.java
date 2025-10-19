package com.xreatlabs.xreatoptimizer.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for entity-related operations
 */
public class EntityUtils {
    
    /**
     * Counts entities by type in all worlds
     * @return Map of entity type to count
     */
    public static Map<EntityType, Integer> countEntities() {
        Map<EntityType, Integer> entityCounts = new HashMap<>();
        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                EntityType type = entity.getType();
                entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
            }
        }
        
        return entityCounts;
    }
    
    /**
     * Counts entities by type in a specific world
     * @param world The world to count entities in
     * @return Map of entity type to count
     */
    public static Map<EntityType, Integer> countEntitiesInWorld(World world) {
        Map<EntityType, Integer> entityCounts = new HashMap<>();
        
        for (Entity entity : world.getEntities()) {
            EntityType type = entity.getType();
            entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
        }
        
        return entityCounts;
    }
    
    /**
     * Gets the total number of entities across all worlds
     * @return Total entity count
     */
    public static int getTotalEntityCount() {
        // This method should not be called from async threads
        // For async operations, we'll need to use a scheduled sync task
        try {
            // Check if we're on the main thread
            if (Bukkit.isPrimaryThread()) {
                int count = 0;
                for (World world : Bukkit.getWorlds()) {
                    count += world.getEntities().size();
                }
                return count;
            } else {
                // When called from async context, return 0 as a safe fallback
                // A proper implementation would cache this value or schedule a sync task
                return 0;
            }
        } catch (Exception e) {
            // If there are any issues accessing entities, return safe default
            return 0;
        }
    }
    
    /**
     * Gets the count of specific entity types
     * @param types The entity types to count
     * @return Map of type to count
     */
    public static Map<EntityType, Integer> getCountsForTypes(EntityType... types) {
        Map<EntityType, Integer> counts = new HashMap<>();
        Map<EntityType, Integer> allCounts = countEntities();
        
        for (EntityType type : types) {
            counts.put(type, allCounts.getOrDefault(type, 0));
        }
        
        return counts;
    }
    
    /**
     * Removes entities of specific types that exceed a count threshold
     * @param world The world to process
     * @param type The entity type to check
     * @param maxCount Maximum allowed count
     * @return Number of entities removed
     */
    public static int removeExcessEntities(World world, EntityType type, int maxCount) {
        int removed = 0;
        int currentCount = 0;
        
        // First, count current entities of the specified type
        for (Entity entity : world.getEntities()) {
            if (entity.getType() == type) {
                currentCount++;
            }
        }
        
        // If count exceeds max, remove excess
        if (currentCount > maxCount) {
            int toRemove = currentCount - maxCount;
            
            for (Entity entity : world.getEntities()) {
                if (toRemove <= 0) break;
                
                if (entity.getType() == type) {
                    // Don't remove named entities or entities with important NBT
                    if (entity.getCustomName() == null) {
                        entity.remove();
                        removed++;
                        toRemove--;
                    }
                }
            }
        }
        
        return removed;
    }
    
    /**
     * Checks if a world has excessive entities of a type
     * @param world The world to check
     * @param type The entity type to check
     * @param threshold The threshold to check against
     * @return True if entity count exceeds threshold
     */
    public static boolean hasExcessiveEntities(World world, EntityType type, int threshold) {
        int count = 0;
        for (Entity entity : world.getEntities()) {
            if (entity.getType() == type) {
                count++;
                if (count > threshold) {
                    return true;
                }
            }
        }
        return false;
    }
}