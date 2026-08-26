package dev.gtnhjourney.diagnostics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.network.ItemStackPayloadSizer;
import dev.gtnhjourney.recovery.JourneySnapshot;
import dev.gtnhjourney.research.ResearchKey;

/** Writes a compact, attachable diagnostic snapshot for live GTNH compatibility testing. */
public final class JourneyDiagnosticDump {

    private JourneyDiagnosticDump() {}

    public static File write(EntityPlayerMP player) throws IOException {
        if (player == null) throw new IllegalArgumentException("player must not be null");
        File dir = new File("logs");
        if (!dir.exists() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("cannot create log directory: " + dir.getAbsolutePath());
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        String shortId = player.getUniqueID()
            .toString()
            .substring(0, 8);
        File file = new File(dir, "gtnhjourney-dump-" + stamp + "-" + shortId + ".txt");

        List<ResearchKey> keys = GTNHJourney.RESEARCH.snapshot(player);
        int nbtStates = 0;
        int serverOnly = 0;
        int unavailable = 0;
        HashSet<String> bases = new HashSet<String>();
        Map<String, Integer> semanticMatches = new LinkedHashMap<String, Integer>();
        String[] semanticPolicyOrder = {
            "GT-charge", "IC2-charge", "CoFH-charge", "OC-charge", "GT-tool", "TCon-tool", "Botania-magnet",
            "Draconic-tool", "wearable-transient" };
        for (String policy : semanticPolicyOrder) semanticMatches.put(policy, Integer.valueOf(0));
        List<ResearchKey> unknownExactNbt = new ArrayList<ResearchKey>();
        for (ResearchKey key : keys) {
            bases.add(key.getItemId() + "@" + key.getMeta());
            boolean hasSemanticNbt = !key.getCanonicalNbt()
                .isEmpty();
            if (hasSemanticNbt) nbtStates++;
            ItemStack diagnosticStack = GTNHJourney.RESEARCH.retrieve(player, key, 1);
            if (diagnosticStack == null) {
                unavailable++;
                continue;
            }
            if (!ItemStackPayloadSizer.canSync(diagnosticStack)) serverOnly++;
            SemanticDiagnosticSnapshot semantic = new SemanticDiagnosticSnapshot(
                dev.gtnhjourney.minecraft.GtChargeStatePolicy.describe(diagnosticStack),
                dev.gtnhjourney.minecraft.Ic2ChargeStatePolicy.describe(diagnosticStack),
                dev.gtnhjourney.minecraft.CofhChargeStatePolicy.describe(diagnosticStack),
                dev.gtnhjourney.minecraft.OpenComputersChargeStatePolicy.describe(diagnosticStack),
                dev.gtnhjourney.minecraft.GtToolStatePolicy.isVerifiedTool(diagnosticStack),
                dev.gtnhjourney.minecraft.TconToolStatePolicy.isVerifiedTool(diagnosticStack),
                dev.gtnhjourney.minecraft.BotaniaTransientStatePolicy.isVerifiedMagnetRing(diagnosticStack),
                dev.gtnhjourney.minecraft.DraconicTransientStatePolicy.isVerifiedTool(diagnosticStack),
                dev.gtnhjourney.minecraft.WearableTransientStatePolicy.matches(diagnosticStack),
                dev.gtnhjourney.minecraft.ResearchStateExpander.expand(diagnosticStack)
                    .size());
            String matches = semantic.matchedPoliciesCsv();
            if (!matches.isEmpty()) {
                for (String policy : matches.split(", ")) {
                    Integer count = semanticMatches.get(policy);
                    if (count != null) semanticMatches.put(policy, Integer.valueOf(count.intValue() + 1));
                }
            }
            if (semantic.isUnknownExactNbt(hasSemanticNbt)) unknownExactNbt.add(key);
        }

        JourneyRuntimeCounters.Snapshot counters = JourneyRuntimeCounters.snapshot();
        List<JourneySnapshot> recoverySnapshots = GTNHJourney.MUTATIONS == null
            ? java.util.Collections.<JourneySnapshot>emptyList()
            : GTNHJourney.MUTATIONS.snapshots(player);
        int undoDepth = GTNHJourney.MUTATIONS == null ? 0 : GTNHJourney.MUTATIONS.undoDepth(player);
        int redoDepth = GTNHJourney.MUTATIONS == null ? 0 : GTNHJourney.MUTATIONS.redoDepth(player);
        int activeDeleted = GTNHJourney.MUTATIONS == null ? 0 : GTNHJourney.MUTATIONS.activeDeletionCount(player);
        int totalDeleted = GTNHJourney.MUTATIONS == null ? 0 : GTNHJourney.MUTATIONS.deletionCount(player);
        long currentWorldTick = player.worldObj == null ? 0L : player.worldObj.getTotalWorldTime();
        List<String> recoveryLines = RecoveryDiagnosticSummary.lines(
            undoDepth,
            redoDepth,
            activeDeleted,
            totalDeleted,
            recoverySnapshots,
            currentWorldTick,
            GTNHJourney.SNAPSHOT_TICKER.skippedSuspiciousSnapshots());

        BufferedWriter out = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        try {
            out.write("GTNH Journey diagnostic dump\n");
            out.write("Generated: " + stamp + "\n");
            out.write("Player UUID: " + player.getUniqueID() + "\n\n");
            out.write("== Runtime ==\n");
            for (String line : RuntimeCompatibilityReport.lines()) out.write(line + "\n");

            out.write("\n== Runtime counters ==\n");
            out.write("panelIncrementalUpdates=" + counters.getPanelIncrementalUpdates() + "\n");
            out.write("panelAuthoritativeStacks=" + counters.getPanelAuthoritativeStacks() + "\n");
            out.write("panelSemanticStacks=" + counters.getPanelSemanticStacks() + "\n");
            out.write("panelVisibleStacks=" + counters.getPanelVisibleStacks() + "\n");
            out.write("fullNeiReloadRequests=" + counters.getFullNeiReloadRequests() + "\n");
            out.write("presentationFailures=" + counters.getPresentationFailures() + "\n");
            out.write(
                "creativeUnsafeFlaskVariantsRemoved=" + counters.getCreativeUnsafeFlaskVariantsRemoved() + "\n");
            out.write("unlockNotifications=" + counters.getUnlockNotifications() + "\n");
            out.write("furnaceOutputObservations=" + counters.getFurnaceOutputObservations() + "\n");
            out.write("furnaceOutputUnlocks=" + counters.getFurnaceOutputUnlocks() + "\n");
            out.write("debugResearchScans=" + counters.getDebugResearchScans() + "\n");
            out.write("debugResearchPositionsVisited=" + counters.getDebugResearchPositionsVisited() + "\n");
            out.write("debugResearchInventoriesVisited=" + counters.getDebugResearchInventoriesVisited() + "\n");
            out.write("debugResearchUniqueCandidates=" + counters.getDebugResearchUniqueCandidates() + "\n");
            out.write("debugResearchNewStates=" + counters.getDebugResearchNewStates() + "\n");

            out.write("\n== Recovery ==\n");
            for (String line : recoveryLines) out.write(line + "\n");

            out.write("\n== Research summary ==\n");
            out.write("states=" + keys.size() + "\n");
            out.write("baseItemMeta=" + bases.size() + "\n");
            out.write("nbtStates=" + nbtStates + "\n");
            out.write("serverOnlyOversized=" + serverOnly + "\n");
            out.write("unavailable=" + unavailable + "\n");
            out.write("unknownExactNbt=" + unknownExactNbt.size() + "\n");
            out.write("observationFailureUnique=" + ResearchFailureLog.uniqueCount() + "\n");
            out.write("observationFailureDroppedUnique=" + ResearchFailureLog.droppedUnique() + "\n");

            out.write("\n== Observation failures ==\n");
            for (ResearchFailureLog.Entry failure : ResearchFailureLog.snapshot()) {
                out.write(Integer.toString(failure.getOccurrences()));
                out.write('\t');
                out.write(failure.getItem());
                out.write('\t');
                out.write(
                    failure.getFailure()
                        .replace('\n', ' '));
                out.write('\n');
            }

            out.write("\n== Semantic policy matches ==\n");
            boolean wroteSemanticPolicy = false;
            for (Map.Entry<String, Integer> entry : semanticMatches.entrySet()) {
                if (entry.getValue().intValue() <= 0) continue;
                out.write(entry.getKey() + "=" + entry.getValue() + "\n");
                wroteSemanticPolicy = true;
            }
            if (!wroteSemanticPolicy) out.write("none\n");

            out.write("\n== Unknown exact-NBT states (preserved exact) ==\n");
            if (unknownExactNbt.isEmpty()) {
                out.write("none\n");
            } else {
                for (ResearchKey key : unknownExactNbt) {
                    out.write(key.getItemId());
                    out.write('\t');
                    out.write(Integer.toString(key.getMeta()));
                    out.write('\t');
                    out.write(key.getCanonicalNbt());
                    out.write('\n');
                }
            }

            out.write("\n== Hotspots ==\n");
            for (ResearchHotspotAnalyzer.Hotspot hotspot : ResearchHotspotAnalyzer.top(keys, 50)) {
                out.write(hotspot.getStates() + "\t" + hotspot.getBaseId() + "\n");
            }

            out.write("\n== Research keys ==\n");
            for (int i = 0; i < keys.size(); i++) {
                ResearchKey key = keys.get(i);
                out.write(Integer.toString(i + 1));
                out.write('\t');
                out.write(key.getItemId());
                out.write('\t');
                out.write(Integer.toString(key.getMeta()));
                out.write('\t');
                out.write(
                    key.getCanonicalNbt()
                        .isEmpty() ? "BASE" : key.getCanonicalNbt());
                out.write('\n');
            }
        } finally {
            out.close();
        }
        return file;
    }
}
