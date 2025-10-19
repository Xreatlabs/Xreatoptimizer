package com.xreatlabs.xreatoptimizer;

import com.xreatlabs.xreatoptimizer.commands.OptimizeCommand;
import com.xreatlabs.xreatoptimizer.commands.ReportCommand;
import com.xreatlabs.xreatoptimizer.commands.OptimizeGUICommand;
import com.xreatlabs.xreatoptimizer.listeners.ServerEventListener;
import com.xreatlabs.xreatoptimizer.listeners.EntityEventListener;
import com.xreatlabs.xreatoptimizer.listeners.GUIClickListener;
import com.xreatlabs.xreatoptimizer.managers.*;
import com.xreatlabs.xreatoptimizer.storage.StatisticsStorage;
import com.xreatlabs.xreatoptimizer.config.ConfigReloader;
import com.xreatlabs.xreatoptimizer.version.VersionAdapter;
import com.xreatlabs.xreatoptimizer.ai.AutoTuningEngine;
// import org.incendo.libby.BukkitLibraryManager; // Commented out due to dependency issues
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class for XreatOptimizer - Advanced Minecraft Server Performance Engine
 * Compatible with Minecraft 1.8 through 1.21.10
 */
public class XreatOptimizer extends JavaPlugin {
    
    private static XreatOptimizer instance;
    private VersionAdapter versionAdapter;
    private OptimizationManager optimizationManager;
    private ThreadPoolManager threadPoolManager;
    private AnnouncementSystem announcementSystem;
    private PerformanceMonitor performanceMonitor;
    private AutoTuningEngine autoTuningEngine;
    private AdvancedEntityOptimizer advancedEntityOptimizer;
    private SmartTickDistributor smartTickDistributor;
    private NetworkOptimizer networkOptimizer;
    private AdvancedCPURAMOptimizer advancedCPURAMOptimizer;
    private EntityCullingManager entityCullingManager;
    private SelfProtectionManager selfProtectionManager;
    private EmptyServerOptimizer emptyServerOptimizer;
    private PredictiveChunkLoader predictiveChunkLoader;
    private RedstoneHopperOptimizer redstoneHopperOptimizer;
    private LagSpikeDetector lagSpikeDetector;
    private TickBudgetManager tickBudgetManager;
    private PathfindingCache pathfindingCache;
    // Store managers that need to be accessed by other components
    private HibernateManager hibernateManager;
    private ChunkPreGenerator chunkPreGenerator;
    private MemorySaver memorySaver;
    private AutoClearTask autoClearTask;
    private DynamicViewDistance dynamicViewDistance;
    private ItemDropTracker itemDropTracker;
    private StatisticsStorage statisticsStorage;
    private ConfigReloader configReloader;

    @Override
    public void onEnable() {
        instance = this;
        
        // Display startup banner
        getLogger().info(Constants.STARTUP_BANNER);
        
        // Initialize self protection manager first
        selfProtectionManager = new SelfProtectionManager(this);
        selfProtectionManager.runInitialSecurityChecks();
        
        // Initialize version adapter
        versionAdapter = new VersionAdapter(this);
        getLogger().info("Detected server version: " + versionAdapter.getServerVersion());
        
        // Initialize thread pool manager first
        threadPoolManager = new ThreadPoolManager();
        
        // Initialize performance monitor
        performanceMonitor = new PerformanceMonitor(this);
        
        // Initialize other managers
        optimizationManager = new OptimizationManager(this);
        announcementSystem = new AnnouncementSystem(this);
        
        // Initialize managers that depend on others
        hibernateManager = new HibernateManager(this);
        chunkPreGenerator = new ChunkPreGenerator(this);
        memorySaver = new MemorySaver(this);
        autoClearTask = new AutoClearTask(this);
        dynamicViewDistance = new DynamicViewDistance(this);
        itemDropTracker = new ItemDropTracker(this);
        
        // Initialize advanced optimization systems
        advancedEntityOptimizer = new AdvancedEntityOptimizer(this);
        smartTickDistributor = new SmartTickDistributor(this);
        networkOptimizer = new NetworkOptimizer(this);
        advancedCPURAMOptimizer = new AdvancedCPURAMOptimizer(this);
        entityCullingManager = new EntityCullingManager(this);
        emptyServerOptimizer = new EmptyServerOptimizer(this);
        
        // Initialize next-gen optimization systems
        predictiveChunkLoader = new PredictiveChunkLoader(this);
        redstoneHopperOptimizer = new RedstoneHopperOptimizer(this);
        lagSpikeDetector = new LagSpikeDetector(this);
        tickBudgetManager = new TickBudgetManager(this);
        pathfindingCache = new PathfindingCache(this);
        
        // Initialize AI auto-tuning engine
        autoTuningEngine = new AutoTuningEngine(this);
        
        // Register commands
        getCommand("xreatopt").setExecutor(new OptimizeCommand(this));
        getCommand("xreatreport").setExecutor(new ReportCommand(this));
        getCommand("xreatgui").setExecutor(new OptimizeGUICommand(this));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ServerEventListener(this), this);
        getServer().getPluginManager().registerEvents(new EntityEventListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIClickListener(this), this);

        // Initialize configuration
        saveDefaultConfig();

        // Initialize storage and config systems
        statisticsStorage = new StatisticsStorage(this);
        configReloader = new ConfigReloader(this);

        // Validate configuration
        if (!configReloader.validateConfig()) {
            getLogger().warning("Configuration validation failed - using defaults where applicable");
        }

        // Start all systems
        optimizationManager.start();
        announcementSystem.start();
        performanceMonitor.start();
        advancedEntityOptimizer.start();
        smartTickDistributor.start();
        networkOptimizer.start();
        advancedCPURAMOptimizer.start();
        entityCullingManager.start();
        emptyServerOptimizer.start();
        itemDropTracker.start();
        
        // Start next-gen systems
        predictiveChunkLoader.start();
        redstoneHopperOptimizer.start();
        lagSpikeDetector.start();
        tickBudgetManager.start();
        pathfindingCache.start();
        
        autoTuningEngine.start();
        
        // Register PlaceholderAPI expansion if available
        // registerPlaceholderExpansion();  // Commented out due to dependency issues
        
        getLogger().info("XreatOptimizer has been enabled!");
    }



    @Override
    public void onDisable() {
        // Save statistics before shutdown
        if (statisticsStorage != null) {
            statisticsStorage.saveStatistics();
            getLogger().info("Statistics saved to disk");
        }

        // Stop all systems safely
        if (performanceMonitor != null) {
            performanceMonitor.stop();
        }
        
        if (announcementSystem != null) {
            announcementSystem.stop();
        }
        
        if (autoTuningEngine != null) {
            autoTuningEngine.stop();
        }
        
        if (advancedEntityOptimizer != null) {
            advancedEntityOptimizer.stop();
        }
        
        if (smartTickDistributor != null) {
            smartTickDistributor.stop();
        }
        
        if (networkOptimizer != null) {
            networkOptimizer.stop();
        }
        
        if (advancedCPURAMOptimizer != null) {
            advancedCPURAMOptimizer.stop();
        }
        
        if (entityCullingManager != null) {
            entityCullingManager.stop();
        }
        
        if (emptyServerOptimizer != null) {
            emptyServerOptimizer.stop();
        }

        if (itemDropTracker != null) {
            itemDropTracker.stop();
        }
        
        // Stop next-gen systems
        if (predictiveChunkLoader != null) {
            predictiveChunkLoader.stop();
        }
        
        if (redstoneHopperOptimizer != null) {
            redstoneHopperOptimizer.stop();
        }
        
        if (lagSpikeDetector != null) {
            lagSpikeDetector.stop();
        }
        
        if (tickBudgetManager != null) {
            tickBudgetManager.stop();
        }
        
        if (pathfindingCache != null) {
            pathfindingCache.stop();
        }
        
        if (optimizationManager != null) {
            optimizationManager.stop();
        }
        
        // Shutdown thread pools
        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }
        
        // Shutdown Libby manager
        // if (libbyManager != null) {  // Commented out due to dependency issues
        //     libbyManager.shutdown();
        // }
        
        getLogger().info("XreatOptimizer has been disabled!");
    }

    // Getters
    public static XreatOptimizer getInstance() {
        return instance;
    }

    public VersionAdapter getVersionAdapter() {
        return versionAdapter;
    }

    public OptimizationManager getOptimizationManager() {
        return optimizationManager;
    }

    public ThreadPoolManager getThreadPoolManager() {
        return threadPoolManager;
    }

    public AnnouncementSystem getAnnouncementSystem() {
        return announcementSystem;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return performanceMonitor;
    }

    public HibernateManager getHibernateManager() {
        return hibernateManager;
    }

    public MemorySaver getMemorySaver() {
        return memorySaver;
    }

    public AutoClearTask getAutoClearTask() {
        return autoClearTask;
    }

    public ChunkPreGenerator getChunkPreGenerator() {
        return chunkPreGenerator;
    }

    public DynamicViewDistance getDynamicViewDistance() {
        return dynamicViewDistance;
    }

    public AdvancedCPURAMOptimizer getAdvancedCPURAMOptimizer() {
        return advancedCPURAMOptimizer;
    }

    public AdvancedEntityOptimizer getAdvancedEntityOptimizer() {
        return advancedEntityOptimizer;
    }

    public SmartTickDistributor getSmartTickDistributor() {
        return smartTickDistributor;
    }

    public NetworkOptimizer getNetworkOptimizer() {
        return networkOptimizer;
    }

    public EntityCullingManager getEntityCullingManager() {
        return entityCullingManager;
    }

    public SelfProtectionManager getSelfProtectionManager() {
        return selfProtectionManager;
    }

    public EmptyServerOptimizer getEmptyServerOptimizer() {
        return emptyServerOptimizer;
    }

    public PredictiveChunkLoader getPredictiveChunkLoader() {
        return predictiveChunkLoader;
    }

    public RedstoneHopperOptimizer getRedstoneHopperOptimizer() {
        return redstoneHopperOptimizer;
    }

    public LagSpikeDetector getLagSpikeDetector() {
        return lagSpikeDetector;
    }

    public TickBudgetManager getTickBudgetManager() {
        return tickBudgetManager;
    }

    public PathfindingCache getPathfindingCache() {
        return pathfindingCache;
    }

    public StatisticsStorage getStatisticsStorage() {
        return statisticsStorage;
    }

    public ConfigReloader getConfigReloader() {
        return configReloader;
    }

    public AutoTuningEngine getAutoTuningEngine() {
        return autoTuningEngine;
    }

    public ItemDropTracker getItemDropTracker() {
        return itemDropTracker;
    }
}