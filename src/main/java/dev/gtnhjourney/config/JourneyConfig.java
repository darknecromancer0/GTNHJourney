package dev.gtnhjourney.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import dev.gtnhjourney.minecraft.ResearchCompatibilityOptions;

/** Small common config. Security hard limits intentionally remain compile-time constants. */
public final class JourneyConfig {

    private static volatile int inventoryScanIntervalTicks = 2;
    private static volatile int newestLimit = 64;
    private static volatile int inventoryFullRescanIntervalTicks = 200;
    private static volatile boolean normalizeGtTransientIdentity = true;
    private static volatile boolean resetGtToolTemplateState = true;
    private static volatile boolean normalizeGtChargeEndpoints = true;
    private static volatile boolean normalizeIc2ChargeEndpoints = true;
    private static volatile boolean normalizeTconToolWear = true;
    private static volatile boolean normalizeCofhChargeEndpoints = true;

    private JourneyConfig() {}

    public static void load(File file) {
        Configuration config = new Configuration(file);
        try {
            config.load();
            inventoryScanIntervalTicks = config.getInt(
                "inventoryScanIntervalTicks",
                "research",
                2,
                1,
                40,
                "How often the server validates real player inventories for newly obtained states. 20 ticks = 1 second.");
            newestLimit = config.getInt(
                "newestLimit",
                "client",
                64,
                8,
                512,
                "Maximum number of recently researched states shown by the NEI Newest view.");
            inventoryFullRescanIntervalTicks = config.getInt(
                "inventoryFullRescanIntervalTicks",
                "research",
                200,
                20,
                1200,
                "Forced deep inventory safety scan interval. Stable slots use a cheap signature between these scans.");
            normalizeGtTransientIdentity = config.getBoolean(
                "normalizeGtTransientIdentity",
                "compatibility",
                true,
                "Ignore verified GT5U tool-use counters (GT.ToolStats Damage/Mode) in research identity. Charge is handled separately as base/FULL endpoints.");
            resetGtToolTemplateState = config.getBoolean(
                "resetGtToolTemplateState",
                "compatibility",
                true,
                "Reset verified GT tool wear/mode to fresh defaults in the stored retrieval template. Does not alter GT.ItemCharge.");
            normalizeGtChargeEndpoints = config.getBoolean(
                "normalizeGtChargeEndpoints",
                "compatibility",
                true,
                "Use Journey battery semantics for verified GT electric items: partial charge -> base/empty, full charge -> distinct FULL endpoint. Disable only for compatibility debugging.");
            normalizeIc2ChargeEndpoints = config.getBoolean(
                "normalizeIc2ChargeEndpoints",
                "compatibility",
                true,
                "Use the same base/FULL Journey semantics for verified IC2 IElectricItem stacks, using the IC2 manager rather than guessing arbitrary NBT.");
            normalizeTconToolWear = config.getBoolean(
                "normalizeTconToolWear",
                "compatibility",
                true,
                "Ignore verified Tinkers Construct ToolCore runtime Damage/Broken state and retrieve a fresh usable copy. Materials and modifiers remain exact.");
            normalizeCofhChargeEndpoints = config.getBoolean(
                "normalizeCofhChargeEndpoints",
                "compatibility",
                true,
                "Use base/FULL Journey semantics for verified CoFH IEnergyContainerItem stacks. Disable only for compatibility debugging.");
            ResearchCompatibilityOptions.configure(
                normalizeGtTransientIdentity,
                resetGtToolTemplateState,
                normalizeGtChargeEndpoints,
                normalizeIc2ChargeEndpoints,
                normalizeTconToolWear,
                normalizeCofhChargeEndpoints);
        } finally {
            if (config.hasChanged()) config.save();
        }
    }

    public static int inventoryScanIntervalTicks() {
        return inventoryScanIntervalTicks;
    }

    public static int newestLimit() {
        return newestLimit;
    }

    public static int inventoryFullRescanIntervalTicks() {
        return inventoryFullRescanIntervalTicks;
    }

    public static boolean normalizeGtTransientIdentity() {
        return normalizeGtTransientIdentity;
    }

    public static boolean resetGtToolTemplateState() {
        return resetGtToolTemplateState;
    }

    public static boolean normalizeGtChargeEndpoints() {
        return normalizeGtChargeEndpoints;
    }

    public static boolean normalizeIc2ChargeEndpoints() {
        return normalizeIc2ChargeEndpoints;
    }

    public static boolean normalizeTconToolWear() {
        return normalizeTconToolWear;
    }

    public static boolean normalizeCofhChargeEndpoints() {
        return normalizeCofhChargeEndpoints;
    }
}
