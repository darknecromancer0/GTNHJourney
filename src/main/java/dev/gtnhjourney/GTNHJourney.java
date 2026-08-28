package dev.gtnhjourney;

import net.minecraft.item.Item;
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
import dev.gtnhjourney.command.CommandJourney;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.network.ServerRequestQueue;
import dev.gtnhjourney.network.ServerResearchSyncQueue;
import dev.gtnhjourney.persistence.PlayerResearchService;
import dev.gtnhjourney.recovery.JourneyMutationService;
import dev.gtnhjourney.recovery.JourneySnapshotTicker;
import dev.gtnhjourney.safety.ExplosionGuard;
import dev.gtnhjourney.safety.PlayerCleanseService;

@Mod(
    modid = GTNHJourney.MODID,
    name = GTNHJourney.NAME,
    version = GTNHJourney.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "[" + GTNHJourney.VERSION + "]")
public final class GTNHJourney {

    public static final String MODID = "gtnhjourney";
    public static final String NAME = "GTNH Journey";
    public static final String VERSION = "1.1.3";
    public static final String TARGET_GTNH = "2.9.0-beta-2";
    public static final String TARGET_NEI = "2.8.111-GTNH";
    public static final PlayerResearchService RESEARCH = new PlayerResearchService();
    public static final JourneySnapshotTicker SNAPSHOT_TICKER = new JourneySnapshotTicker();
    public static final WorldBackupCoordinator WORLD_BACKUPS = new WorldBackupCoordinator();
    public static final ExplosionGuard EXPLOSION_GUARD = new ExplosionGuard();
    public static final PlayerCleanseService CLEANSE = new PlayerCleanseService();
    private static final WorldBackupTicker WORLD_BACKUP_TICKER = new WorldBackupTicker(WORLD_BACKUPS);
    public static JourneyMutationService MUTATIONS;
    public static Item DEBUG_RESEARCHER_TOOL;

    @SidedProxy(clientSide = "dev.gtnhjourney.ClientProxy", serverSide = "dev.gtnhjourney.CommonProxy")
    public static CommonProxy proxy;
    private InventoryResearchTracker inventoryTracker;
    private FurnaceOwnershipTracker furnaceTracker;
    private ResearchObservationService observations;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        JourneyConfig.load(event.getSuggestedConfigurationFile());
        JourneyNetwork.init();
        proxy.preInit(event);
        MUTATIONS = new JourneyMutationService();
        DEBUG_RESEARCHER_TOOL = new ItemDebugResearcherTool();
        GameRegistry.registerItem(DEBUG_RESEARCHER_TOOL, "debug_researcher_tool");
        observations = new ResearchObservationService(RESEARCH, MUTATIONS);
        inventoryTracker = new InventoryResearchTracker(RESEARCH, observations);
        furnaceTracker = new FurnaceOwnershipTracker(observations);
        FMLCommonHandler.instance()
            .bus()
            .register(inventoryTracker);
        FMLCommonHandler.instance()
            .bus()
            .register(furnaceTracker);
        MinecraftForge.EVENT_BUS.register(inventoryTracker);
        MinecraftForge.EVENT_BUS.register(furnaceTracker);
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerRequestQueue(RESEARCH, MUTATIONS));
        FMLCommonHandler.instance()
            .bus()
            .register(new ServerResearchSyncQueue());
        FMLCommonHandler.instance()
            .bus()
            .register(SNAPSHOT_TICKER);
        WorldSafetyRegistration.register(
            new WorldSafetyRegistration.Registrar() {

                @Override
                public void register(Object listener) {
                    FMLCommonHandler.instance()
                        .bus()
                        .register(listener);
                }
            },
            new WorldSafetyRegistration.Registrar() {

                @Override
                public void register(Object listener) {
                    MinecraftForge.EVENT_BUS.register(listener);
                }
            },
            WORLD_BACKUP_TICKER,
            EXPLOSION_GUARD);
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        // A remote server may have overwritten the client-side static identity policy in the same JVM earlier.
        // Every actual server start reasserts this instance's local authoritative config before touching a world.
        dev.gtnhjourney.minecraft.ResearchCompatibilityOptions.configure(
            JourneyConfig.normalizeGtTransientIdentity(),
            JourneyConfig.resetGtToolTemplateState(),
            JourneyConfig.normalizeGtChargeEndpoints(),
            JourneyConfig.normalizeIc2ChargeEndpoints(),
            JourneyConfig.normalizeTconToolWear(),
            JourneyConfig.normalizeCofhChargeEndpoints());
        dev.gtnhjourney.diagnostics.RuntimeCompatibilityReport.logStartup();
        event.registerServerCommand(new CommandJourney());
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        WORLD_BACKUPS.markWorldLoaded();
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        WORLD_BACKUPS.finishForShutdown();
    }

    @EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        ServerRequestQueue.clearPending();
        ServerResearchSyncQueue.clear();
        if (inventoryTracker != null) inventoryTracker.clearCaches();
        if (furnaceTracker != null) furnaceTracker.clear();
        SNAPSHOT_TICKER.clear();
        WORLD_BACKUPS.resetSession();
        EXPLOSION_GUARD.reset();
        dev.gtnhjourney.diagnostics.ResearchTrace.clear();
        dev.gtnhjourney.diagnostics.ResearchFailureLog.clear();
    }
}
