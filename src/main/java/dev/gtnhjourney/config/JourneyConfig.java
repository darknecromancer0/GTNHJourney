package dev.gtnhjourney.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

import dev.gtnhjourney.minecraft.ResearchCompatibilityOptions;

/** Small common config. Security hard limits intentionally remain compile-time constants. */
public final class JourneyConfig {

    public static final int DEFAULT_INVENTORY_SCAN_INTERVAL_TICKS = 5;
    public static final int DEFAULT_WORLD_BACKUP_INTERVAL_SECONDS = 300;
    public static final int DEFAULT_WORLD_BACKUP_RETENTION = 3;

    private static volatile File configFile;
    private static volatile int inventoryScanIntervalTicks = DEFAULT_INVENTORY_SCAN_INTERVAL_TICKS;
    /** @deprecated N now contains the full researched set; retained only so existing config files remain readable. */
    private static volatile int newestLimit = 64;
    private static volatile int inventoryFullRescanIntervalTicks = 200;
    private static volatile boolean normalizeGtTransientIdentity = true;
    private static volatile boolean resetGtToolTemplateState = true;
    private static volatile boolean normalizeGtChargeEndpoints = true;
    private static volatile boolean normalizeIc2ChargeEndpoints = true;
    private static volatile boolean normalizeTconToolWear = true;
    private static volatile boolean normalizeCofhChargeEndpoints = true;
    private static volatile boolean worldBackupsEnabled = true;
    private static volatile int worldBackupIntervalSeconds = DEFAULT_WORLD_BACKUP_INTERVAL_SECONDS;
    private static volatile int worldBackupRetention = DEFAULT_WORLD_BACKUP_RETENTION;
    private static volatile boolean explosionsEnabled = true;

    private JourneyConfig() {}

    public static void load(File file) {
        configFile = file;
        Configuration config = new Configuration(file);
        try {
            config.load();
            inventoryScanIntervalTicks = normalizeInventoryScanIntervalTicks(
                config.getInt(
                    "inventoryScanIntervalTicks",
                    "research",
                    DEFAULT_INVENTORY_SCAN_INTERVAL_TICKS,
                    1,
                    40,
                    "How often the server performs fallback validation of real player inventories. Values below 5 are raised to 5 ticks."));
            newestLimit = config.getInt(
                "newestLimit",
                "client",
                64,
                8,
                512,
                "Legacy compatibility setting. Ignored by pre7: N always contains the full researched set and only changes its ordering.");
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
            worldBackupsEnabled = config.getBoolean(
                "worldBackupsEnabled",
                "backup",
                true,
                "Create synchronous rotating world backups while the server is running.");
            worldBackupIntervalSeconds = normalizeWorldBackupIntervalSeconds(
                config.getInt(
                    "worldBackupIntervalSeconds",
                    "backup",
                    DEFAULT_WORLD_BACKUP_INTERVAL_SECONDS,
                    60,
                    86400,
                    "Seconds of server uptime between automatic world backups."));
            worldBackupRetention = normalizeWorldBackupRetention(
                config.getInt(
                    "worldBackupRetention",
                    "backup",
                    DEFAULT_WORLD_BACKUP_RETENTION,
                    1,
                    32,
                    "Number of successful rotating world backups to keep."));
            explosionsEnabled = config.getBoolean(
                "explosionsEnabled",
                "safety",
                true,
                "Whether explosions are allowed. Set false or use /journey explosions off to cancel Forge explosions globally.");
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

    public static int normalizeInventoryScanIntervalTicks(int configured) {
        return Math.max(DEFAULT_INVENTORY_SCAN_INTERVAL_TICKS, Math.min(40, configured));
    }

    public static int normalizeWorldBackupIntervalSeconds(int configured) {
        return Math.max(60, Math.min(86400, configured));
    }

    public static int normalizeWorldBackupRetention(int configured) {
        return Math.max(1, Math.min(32, configured));
    }

    public static int inventoryScanIntervalTicks() {
        return inventoryScanIntervalTicks;
    }

    /** @deprecated retained only for binary/source compatibility; N no longer truncates by this value. */
    @Deprecated
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

    public static boolean worldBackupsEnabled() {
        return worldBackupsEnabled;
    }

    public static int worldBackupIntervalSeconds() {
        return worldBackupIntervalSeconds;
    }

    public static int worldBackupRetention() {
        return worldBackupRetention;
    }

    public static boolean explosionsEnabled() {
        return explosionsEnabled;
    }

    public static synchronized void setWorldBackupsEnabled(boolean enabled) {
        worldBackupsEnabled = enabled;
        persistBoolean("backup", "worldBackupsEnabled", enabled);
    }

    public static synchronized void setExplosionsEnabled(boolean enabled) {
        explosionsEnabled = enabled;
        persistBoolean("safety", "explosionsEnabled", enabled);
    }

    private static void persistBoolean(String category, String key, boolean value) {
        File file = configFile;
        if (file == null) return;
        Configuration config = new Configuration(file);
        try {
            config.load();
            config.get(category, key, value).set(value);
        } finally {
            if (config.hasChanged()) config.save();
        }
    }
}
