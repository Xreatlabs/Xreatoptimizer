package com.xreatlabs.xreatoptimizer.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Utility class for memory-related calculations
 */
public class MemoryUtils {
    
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    /**
     * Gets the current heap memory usage in MB
     * @return Current heap memory usage in MB
     */
    public static long getUsedMemoryMB() {
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        return heapMemoryUsage.getUsed() / (1024 * 1024);
    }
    
    /**
     * Gets the maximum heap memory in MB
     * @return Maximum heap memory in MB
     */
    public static long getMaxMemoryMB() {
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        return heapMemoryUsage.getMax() / (1024 * 1024);
    }
    
    /**
     * Gets the committed heap memory in MB
     * @return Committed heap memory in MB
     */
    public static long getCommittedMemoryMB() {
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        return heapMemoryUsage.getCommitted() / (1024 * 1024);
    }
    
    /**
     * Gets the percentage of heap memory currently used
     * @return Percentage of heap memory used (0-100)
     */
    public static double getMemoryUsagePercentage() {
        long max = getMaxMemoryMB();
        if (max == 0) return 0;
        return (double) getUsedMemoryMB() / max * 100;
    }
    
    /**
     * Triggers a garbage collection (non-blocking suggestion)
     */
    public static void suggestGarbageCollection() {
        // Suggest garbage collection, but don't force it
        System.gc();
    }
    
    /**
     * Checks if memory usage is above a threshold
     * @param percentageThreshold Percentage threshold (0-100)
     * @return True if memory usage is above threshold
     */
    public static boolean isMemoryUsageAbove(double percentageThreshold) {
        return getMemoryUsagePercentage() > percentageThreshold;
    }
    
    /**
     * Checks if memory pressure is high
     * @return True if memory usage is above 80%
     */
    public static boolean isMemoryPressureHigh() {
        return isMemoryUsageAbove(80.0);
    }

    /**
     * Gets used memory in MB (alias for getUsedMemoryMB)
     */
    public static long getUsedMemory() {
        return getUsedMemoryMB();
    }

    /**
     * Gets max memory in MB (alias for getMaxMemoryMB)
     */
    public static long getMaxMemory() {
        return getMaxMemoryMB();
    }

    /**
     * Gets memory usage percentage (alias for getMemoryUsagePercentage)
     */
    public static double getMemoryUsagePercent() {
        return getMemoryUsagePercentage();
    }
}