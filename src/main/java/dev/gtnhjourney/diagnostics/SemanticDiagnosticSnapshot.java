package dev.gtnhjourney.diagnostics;

import java.util.ArrayList;
import java.util.List;

/** Pure presentation snapshot for one stack's semantic Journey classification. */
public final class SemanticDiagnosticSnapshot {

    private final String gtCharge;
    private final String ic2Charge;
    private final String cofhCharge;
    private final String ocCharge;
    private final boolean gtTool;
    private final boolean tconTool;
    private final boolean botaniaMagnet;
    private final boolean draconicTool;
    private final boolean wearableTransient;
    private final int observedEndpoints;

    public SemanticDiagnosticSnapshot(String gtCharge, String ic2Charge, String cofhCharge, String ocCharge,
        boolean gtTool, boolean tconTool, boolean botaniaMagnet, boolean draconicTool, int observedEndpoints) {
        this(
            gtCharge,
            ic2Charge,
            cofhCharge,
            ocCharge,
            gtTool,
            tconTool,
            botaniaMagnet,
            draconicTool,
            false,
            observedEndpoints);
    }

    public SemanticDiagnosticSnapshot(String gtCharge, String ic2Charge, String cofhCharge, String ocCharge,
        boolean gtTool, boolean tconTool, boolean botaniaMagnet, boolean draconicTool, boolean wearableTransient,
        int observedEndpoints) {
        this.gtCharge = normalizeState(gtCharge);
        this.ic2Charge = normalizeState(ic2Charge);
        this.cofhCharge = normalizeState(cofhCharge);
        this.ocCharge = normalizeState(ocCharge);
        this.gtTool = gtTool;
        this.tconTool = tconTool;
        this.botaniaMagnet = botaniaMagnet;
        this.draconicTool = draconicTool;
        this.wearableTransient = wearableTransient;
        this.observedEndpoints = Math.max(0, observedEndpoints);
    }

    public String inspectLine() {
        return "Semantic: GT-charge=" + gtCharge
            + ", IC2-charge="
            + ic2Charge
            + ", CoFH-charge="
            + cofhCharge
            + ", OC-charge="
            + ocCharge
            + ", GT-tool="
            + gtTool
            + ", TCon-tool="
            + tconTool
            + ", Botania-magnet="
            + botaniaMagnet
            + ", Draconic-tool="
            + draconicTool
            + ", Wearable-transient="
            + wearableTransient
            + ", observed endpoints="
            + observedEndpoints;
    }

    public String matchedPoliciesCsv() {
        List<String> matched = new ArrayList<String>();
        addChargePolicy(matched, "GT-charge", gtCharge);
        addChargePolicy(matched, "IC2-charge", ic2Charge);
        addChargePolicy(matched, "CoFH-charge", cofhCharge);
        addChargePolicy(matched, "OC-charge", ocCharge);
        if (gtTool) matched.add("GT-tool");
        if (tconTool) matched.add("TCon-tool");
        if (botaniaMagnet) matched.add("Botania-magnet");
        if (draconicTool) matched.add("Draconic-tool");
        if (wearableTransient) matched.add("wearable-transient");
        StringBuilder out = new StringBuilder();
        for (String policy : matched) {
            if (out.length() > 0) out.append(", ");
            out.append(policy);
        }
        return out.toString();
    }

    public boolean isUnknownExactNbt(boolean hasSemanticNbt) {
        return hasSemanticNbt && matchedPoliciesCsv().isEmpty();
    }

    private static void addChargePolicy(List<String> out, String policy, String state) {
        if (!"EXACT".equals(state)) out.add(policy);
    }

    private static String normalizeState(String state) {
        return state == null || state.trim().isEmpty() ? "EXACT" : state;
    }
}
