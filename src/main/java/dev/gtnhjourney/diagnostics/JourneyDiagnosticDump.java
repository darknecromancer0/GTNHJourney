package dev.gtnhjourney.diagnostics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayerMP;

import dev.gtnhjourney.GTNHJourney;
import dev.gtnhjourney.network.ItemStackPayloadSizer;
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
        for (ResearchKey key : keys) {
            bases.add(key.getItemId() + "@" + key.getMeta());
            if (!key.getCanonicalNbt()
                .isEmpty()) nbtStates++;
            net.minecraft.item.ItemStack diagnosticStack = GTNHJourney.RESEARCH.retrieve(player, key, 1);
            if (diagnosticStack == null) unavailable++;
            else if (!ItemStackPayloadSizer.canSync(diagnosticStack)) serverOnly++;
        }

        BufferedWriter out = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
        try {
            out.write("GTNH Journey diagnostic dump\n");
            out.write("Generated: " + stamp + "\n");
            out.write("Player UUID: " + player.getUniqueID() + "\n\n");
            out.write("== Runtime ==\n");
            for (String line : RuntimeCompatibilityReport.lines()) out.write(line + "\n");
            out.write("\n== Research summary ==\n");
            out.write("states=" + keys.size() + "\n");
            out.write("baseItemMeta=" + bases.size() + "\n");
            out.write("nbtStates=" + nbtStates + "\n");
            out.write("serverOnlyOversized=" + serverOnly + "\n");
            out.write("unavailable=" + unavailable + "\n");
            out.write("undoSnapshot=" + GTNHJourney.RESEARCH.undoSize(player) + "\n");
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
