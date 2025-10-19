package com.xreatlabs.xreatoptimizer.utils;

import org.bukkit.Bukkit;

/**
 * Utility class for TPS (Ticks Per Second) related calculations
 */
public class TPSUtils {
    
    /**
     * Gets the current TPS (Ticks Per Second) of the server
     * @return Current TPS value (typically 0-20)
     */
    public static double getTPS() {
        try {
            // Use reflection to access TPS if available
            Object server = Bukkit.getServer();
            java.lang.reflect.Method getTPSMethod = server.getClass().getMethod("getTPS");
            double[] tpsArray = (double[]) getTPSMethod.invoke(server);
            if (tpsArray.length > 0) {
                return tpsArray[0]; // 1-minute average
            }
        } catch (Exception e) {
            // Fallback if reflection fails
        }
        
        // Fallback method - this is a simplified calculation
        // In a real implementation, you'd use more accurate methods
        return 20.0; // Default fallback
    }
    
    /**
     * Gets the average TPS over different time periods
     * @return Array of TPS values [1min, 5min, 15min]
     */
    public static double[] getTPSArray() {
        try {
            // Use reflection to access TPS if available
            Object server = Bukkit.getServer();
            java.lang.reflect.Method getTPSMethod = server.getClass().getMethod("getTPS");
            return (double[]) getTPSMethod.invoke(server);
        } catch (Exception e) {
            // Fallback
            return new double[]{20.0, 20.0, 20.0};
        }
    }
    
    /**
     * Checks if TPS is below a certain threshold
     * @param threshold The threshold to check against
     * @return True if TPS is below threshold
     */
    public static boolean isTPSBelow(double threshold) {
        return getTPS() < threshold;
    }
    
    /**
     * Checks if TPS is in a dangerous range (emergency mode)
     * @return True if TPS < 10
     */
    public static boolean isTPSDangerous() {
        return isTPSBelow(10.0);
    }
    
    /**
     * Calculates the tick time in milliseconds
     * @return Average tick time in milliseconds
     */
    public static double getAverageTickTime() {
        try {
            // Use reflection to access WorldTickTimes
            Object server = Bukkit.getServer();
            java.lang.reflect.Method getWorldTickTimesMethod = server.getClass().getMethod("getWorldTickTimes");
            long[] recentTickTimes = (long[]) getWorldTickTimesMethod.invoke(server);
            if (recentTickTimes.length > 0) {
                // Calculate average and convert from nanoseconds to milliseconds
                long sum = 0;
                for (long time : recentTickTimes) {
                    sum += time;
                }
                return (sum / (double) recentTickTimes.length) / 1_000_000.0;
            }
        } catch (Exception e) {
            // Fallback
        }
        
        return 50.0; // Default to 50ms if unable to get real data
    }
}