package dev.gtnhjourney;

import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.gtnhjourney.acquisition.FurnaceOwnershipTracker;
import dev.gtnhjourney.acquisition.InventoryResearchTracker;
import dev.gtnhjourney.acquisition.ResearchObservationService;
import dev.gtnhjourney.backup.WorldBackupCoordinator;
import dev.gtnhjourney.backup.WorldBackupTicker;
import dev.gtnhjourney.command.CommandJourney1124;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.debug.ItemBotaniaManaDebugTool;
import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import dev.gtnhjourney.network.CommandSuggestionRequestQueue;
import dev.gtnhjourney.network.FavouriteRequestQueue;
import dev.gtnhjourney.network.FavouriteSyncTracker;
import dev.gtnhjourney.network.Journey1124Network;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.network.ServerRequestQueue;
import dev.gtnhjourney.network.ServerResearchSyncQueue;
import dev.gtnhjourney.persistence.JourneyResearchData;
import dev.gtnhjourney.persistence.PlayerResearchService;
import dev.gtnhjourney.recovery.DeathInventoryGuard;
import dev.gtnhjourney.recovery.DeathInventoryReturnService;
import dev.gtnhjourney.recovery.JourneyMutationService;
import dev.gtnhjourney.recovery.JourneyReversibleActionService;
import dev.gtnhjourney.recovery.JourneySnapshotTicker;
import dev.gtnhjourney.recovery.JourneyUndoCoordinator;
import dev.gtnhjourney.recovery.RuntimeJourneyActionApplier;
import dev.gtnhjourney.safety.ExplosionGuard;
import dev.gtnhjourney.safety.GregTechMachineExplosionSwitch;
import dev.gtnhjourney.safety.PlayerCleanseService;
import dev.gtnhjourney.time.JourneySpeedController;
import dev.gtnhjourney.time.JourneySpeedState;
import dev.gtnhjourney.time.MachineTickAccelerator;
import dev.gtnhjourney.time.ReflectiveServerTickRateAdapter;

@Mod(
    modid = GTNHJourney.MODID,
    name = GTNHJourney.NAME,
    version = GTNHJourney.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "[" + GTNHJourney.VERSION + "]")
public final class GTNHJourney {

    public static final String MODID = "gtnhjourney";
    public static final String NAME = "GTNH Journey";
    public static final String VERSION = "1.1.24";
    public static final String TARGET_GTNH = "2.9.0-beta-2";
    public static final String TARGET_NEI = "2.8.111-GTNH";
    public static final PlayerResearchService RESEARCH = new PlayerResearchService();
    public static final JourneySnapshotTicker SNAPSHOT_TICKER = new JourneySnapshotTicker();
    public static final WorldBackupCoordinator WORLD_BACKUPS = new WorldBackupCoordinator();
    public static final ExplosionGuard EXPLOSION_GUARD = new ExplosionGuard();
    public static final GregTechMachineExplosionSwitch MACHINE_EXPLOSIONS = new GregTechMachineExplosionSwitch();
    public static final PlayerCleanseService CLEANSE = new PlayerCleanseService();
    public static final JourneySpeedController SPEED = new JourneySpeedController(
        new JourneySpeedState(),
        new ReflectiveServerTickRateAdapter());
    private static final MachineTickAccelerator MACHINE_TICK_ACCELERATOR = new MachineTickAccelerator(SPEED);
    private static final WorldBackupTicker WORLD_BACKUP_TICKER = new WorldBackupTicker(WORLD_BACKUPS);
    public static JourneyMutationService MUTATIONS;
    public static JourneyReversibleActionService ACTIONS;
    public static JourneyUndoCoordinator UNDO;
    public static DeathInventoryReturnService DEATH_INVENTORY;
    public static DeathInventoryGuard DEATH_GUARD;
    public static Item DEBUG_RESEARCHER_TOOL;
    public static Item BOTANIA_MANA_DEBUG_TOOL;

    @SidedProxy(clientSide = "dev.gtnhjourney.ClientProxy", serverSide = "dev.gtnhjourney.CommonProxy")
    public static CommonProxy proxy;
    private InventoryResearchTracker inventoryTracker;
    private FurnaceOwnershipTracker furnaceTracker;
    private ResearchObservationService observations;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        JourneyConfig.load(event.getSuggestedConfigurationFile());
        JourneyNetwork.init();
        Journey1124Network.init();
        proxy.preInit(event);
        MUTATIONS = new JourneyMutationService();
        ACTIONS = new JourneyReversibleActionService(new RuntimeJourneyActionApplier());
        UNDO = new JourneyUndoCoordinator(MUTATIONS, ACTIONS);
        DEATH_INVENTORY = new DeathInventoryReturnService(ACTIONS);
        DEATH_GUARD = new DeathInventoryGuard();
        DEBUG_RESEARCHER_TOOL = new ItemDebugResearcherTool();
        GameRegistry.registerItem(DEBUG_RESEARCHER_TOOL, "debug_researcher_tool");
        BOTANIA_MANA_DEBUG_TOOL = new ItemBotaniaManaDebugTool();
        GameRegistry.registerItem(BOTANIA_MANA_DEBUG_TOOL, "botania_mana_debug_tool");
        observations = new ResearchObservationService(RESEARCH, MUTATIONS);
        inventoryTracker = new InventoryResearchTracker(RESEARCH, observations);
        furnaceTracker = new FurnaceOwnershipTracker(observations);
        FMLCommonHandler.instance().bus().register(inventoryTracker);
        FMLCommonHandler.instance().bus().register(furnaceTracker);
        FMLCommonHandler.instance().bus().register(MACHINE_TICK_ACCELERATOR);
        FMLCommonHandler.instance().bus().register(new FavouriteRequestQueue());
        FMLCommonHandler.instance().bus().register(new FavouriteSyncTracker());
        FMLCommonHandler.instance().bus().register(new CommandSuggestionRequestQueue());
        FMLCommonHandler.instance().bus().register(DEATH_GUARD);
        MinecraftForge.EVENT_BUS.register(inventoryTracker);
        MinecraftForge.EVENT_BUS.register(furnaceTracker);
        MinecraftForge.EVENT_BUS.register(DEATH_GUARD);
        FMLCommonHandler.instance().bus().register(new ServerRequestQueue(RESEARCH, MUTATIONS));
        FMLCommonHandler.instance().bus().register(new ServerResearchSyncQueue());
        FMLCommonHandler.instance().bus().register(SNAPSHOT_TICKER);
        WorldSafetyRegistration.register(
            new WorldSafetyRegistration.Registrar() {
                @Override public void register(Object listener) {
                    FMLCommonHandler.instance().bus().register(listener);
                }
            },
            new WorldSafetyRegistration.Registrar() {
                @Override public void register(Object listener) {
                    MinecraftForge.EVENT_BUS.register(listener);
                }
            },
            WORLD_BACKUP_TICKER,
            EXPLOSION_GUARD);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        SPEED.reset();
        dev.gtnhjourney.minecraft.ResearchCompatibilityOptions.configure(
            JourneyConfig.normalizeGtTransientIdentity(),
            JourneyConfig.resetGtToolTemplateState(),
            JourneyConfig.normalizeGtChargeEndpoints(),
            JourneyConfig.normalizeIc2ChargeEndpoints(),
            JourneyConfig.normalizeTconToolWear(),
            JourneyConfig.normalizeCofhChargeEndpoints());
        dev.gtnhjourney.diagnostics.RuntimeCompatibilityReport.logStartup();
        event.registerServerCommand(new CommandJourney1124());
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        World rootWorld = DimensionManager.getWorld(0);
        if (rootWorld != null) JourneyResearchData.get(rootWorld).markDirty();
        WORLD_BACKUPS.markWorldLoaded();
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        WORLD_BACKUPS.finishForShutdown();
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        SPEED.reset();
        ServerRequestQueue.clearPending();
        ServerResearchSyncQueue.clear();
        FavouriteRequestQueue.clear();
        CommandSuggestionRequestQueue.clear();
        if (DEATH_GUARD != null) DEATH_GUARD.clear();
        if (inventoryTracker != null) inventoryTracker.clearCaches();
        if (furnaceTracker != null) furnaceTracker.clear();
        SNAPSHOT_TICKER.clear();
        WORLD_BACKUPS.resetSession();
        EXPLOSION_GUARD.reset();
        dev.gtnhjourney.diagnostics.ResearchTrace.clear();
        dev.gtnhjourney.diagnostics.ResearchFailureLog.clear();
    }
}
