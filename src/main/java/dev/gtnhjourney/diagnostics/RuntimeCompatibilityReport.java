package dev.gtnhjourney.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.minecraft.BotaniaTransientStatePolicy;
import dev.gtnhjourney.minecraft.CofhChargeStatePolicy;
import dev.gtnhjourney.minecraft.DraconicTransientStatePolicy;
import dev.gtnhjourney.minecraft.GtToolStatePolicy;
import dev.gtnhjourney.minecraft.Ic2ChargeStatePolicy;
import dev.gtnhjourney.minecraft.OpenComputersChargeStatePolicy;
import dev.gtnhjourney.minecraft.TconToolStatePolicy;

/** Lightweight runtime facts for first-launch diagnostics. Never blocks loading on a version mismatch. */
public final class RuntimeCompatibilityReport {

    private RuntimeCompatibilityReport() {}

    public static List<String> lines() {
        List<String> out = new ArrayList<String>();
        out.add(
            "Journey " + GTNHJourney.VERSION
                + " target: GTNH "
                + GTNHJourney.TARGET_GTNH
                + ", NEI "
                + GTNHJourney.TARGET_NEI);
        out.add(describeMod("NotEnoughItems", "NEI"));
        out.add(describeMod("gregtech", "GregTech"));
        out.add(describeMod("IC2", "IndustrialCraft 2"));
        out.add(describeMod("TConstruct", "Tinkers Construct"));
        out.add(describeMod("CoFHCore", "CoFH Core"));
        out.add(describeMod("OpenComputers", "OpenComputers"));
        out.add(describeMod("Botania", "Botania"));
        out.add(describeMod("DraconicEvolution", "Draconic Evolution"));
        out.add(describeMod("Baubles", "Baubles"));
        out.add(
            "Semantic APIs: GT generated-tool=" + yesNo(GtToolStatePolicy.isRuntimeAvailable())
                + ", TCon ToolCore="
                + yesNo(TconToolStatePolicy.isRuntimeAvailable())
                + ", IC2 electric="
                + yesNo(Ic2ChargeStatePolicy.isApiAvailable())
                + ", IC2 manager="
                + (Ic2ChargeStatePolicy.isManagerReady() ? "ready" : "not ready/unavailable")
                + ", CoFH energy="
                + yesNo(CofhChargeStatePolicy.isApiAvailable())
                + ", OC hover charge="
                + yesNo(OpenComputersChargeStatePolicy.isApiAvailable())
                + ", Botania magnet="
                + yesNo(BotaniaTransientStatePolicy.isRuntimeAvailable())
                + ", Draconic ToolBase="
                + yesNo(DraconicTransientStatePolicy.isRuntimeAvailable())
                + ", Baubles API="
                + yesNo(classPresent("baubles.api.BaublesApi")));
        out.add("Runtime Java: " + safeProperty("java.version") + " (VM " + safeProperty("java.vm.name") + ")");
        out.add(
            "Config: scan=" + JourneyConfig.inventoryScanIntervalTicks()
                + "t, fullRescan="
                + JourneyConfig.inventoryFullRescanIntervalTicks()
                + "t, N=full-research/activity-order"
                + ", GT transient identity="
                + JourneyConfig.normalizeGtTransientIdentity()
                + ", fresh GT tool templates="
                + JourneyConfig.resetGtToolTemplateState()
                + ", GT charge endpoints="
                + JourneyConfig.normalizeGtChargeEndpoints()
                + ", IC2 charge endpoints="
                + JourneyConfig.normalizeIc2ChargeEndpoints()
                + ", TCon wear normalization="
                + JourneyConfig.normalizeTconToolWear()
                + ", CoFH charge endpoints="
                + JourneyConfig.normalizeCofhChargeEndpoints());
        out.add(
            "Observation failures this server session: " + ResearchFailureLog.uniqueCount()
                + (ResearchFailureLog.droppedUnique() > 0
                    ? " (+" + ResearchFailureLog.droppedUnique() + " unique dropped)"
                    : ""));
        String neiWarning = versionWarning("NotEnoughItems", GTNHJourney.TARGET_NEI, "NEI");
        if (neiWarning != null) out.add(neiWarning);
        return Collections.unmodifiableList(out);
    }

    public static void logStartup() {
        for (String line : lines()) FMLLog.info("[GTNH Journey] %s", line);
    }

    private static String describeMod(String expectedId, String label) {
        ModContainer found = find(expectedId);
        return found == null ? label + ": not detected" : label + ": " + found.getModId() + " " + found.getVersion();
    }

    private static String versionWarning(String expectedId, String targetVersion, String label) {
        ModContainer found = find(expectedId);
        if (found == null || targetVersion == null || targetVersion.equals(found.getVersion())) return null;
        return "Compatibility note: " + label
            + " runtime="
            + found.getVersion()
            + ", development baseline="
            + targetVersion;
    }

    private static ModContainer find(String expectedId) {
        try {
            Map<String, ModContainer> mods = Loader.instance()
                .getIndexedModList();
            ModContainer exact = mods.get(expectedId);
            if (exact != null) return exact;
            for (Map.Entry<String, ModContainer> entry : mods.entrySet()) {
                if (entry.getKey() != null && entry.getKey()
                    .equalsIgnoreCase(expectedId)) return entry.getValue();
            }
        } catch (RuntimeException ignored) {}
        return null;
    }

    private static boolean classPresent(String name) {
        try {
            Class.forName(name, false, RuntimeCompatibilityReport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        } catch (LinkageError ignored) {
            return false;
        }
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    private static String safeProperty(String key) {
        try {
            String value = System.getProperty(key);
            return value == null ? "unknown" : value;
        } catch (SecurityException ignored) {
            return "unknown";
        }
    }
}
