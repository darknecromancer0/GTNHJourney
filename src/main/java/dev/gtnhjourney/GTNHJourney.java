package dev.gtnhjourney;

import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import dev.gtnhjourney.acquisition.FurnaceOwnershipTracker;
import dev.gtnhjourney.acquisition.InventoryResearchTracker;
import dev.gtnhjourney.acquisition.ResearchObservationService;
import dev.gtnhjourney.command.CommandJourney;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.debug.ItemDebugResearcherTool;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.network.ServerRequestQueue;
import dev.gtnhjourney.network.ServerResearchSyncQueue;
import dev.gtnhjourney.persistence.PlayerResearchService;
import dev.gtnhjourney.recovery.JourneyMutationService;
import dev.gtnhjourney.recovery.JourneySnapshotTicker;

@Mod(
    modid = GTNHJourney.MODID,
    name = GTNHJourney.NAME,
    version = GTNHJourney.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "[" + GTNHJourney.VERSION + "]")
public final class GTNHJourney {

    public static final String MODID = "gtnhjourney";
    public static final String NAME = "GTNH Journey";
    public static final String VERSION = "0.1.0-pre9";
    public static final String TARGET_GTNH = "2.9.0-beta-2";
    public static final String TARGET_NEI = "2.8.111-GTNH";
    public static final PlayerResearchService RESEARCH = new PlayerResearchService();
    public static final JourneySnapshotTicker SNAPSHOT_TICKER = new JourneySnapshotTicker();
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
    public void serverStopped(FMLServerStoppedEvent event) {
        ServerRequestQueue.clearPending();
        ServerResearchSyncQueue.clear();
        if (inventoryTracker != null) inventoryTracker.clearCaches();
        if (furnaceTracker != null) furnaceTracker.clear();
        SNAPSHOT_TICKER.clear();
        dev.gtnhjourney.diagnostics.ResearchTrace.clear();
        dev.gtnhjourney.diagnostics.ResearchFailureLog.clear();
    }
}
