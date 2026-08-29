package dev.gtnhjourney.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable, formatting-only 1.1.4 diagnostic state. */
public final class JourneyDiagnosticSnapshot {

    private final int clientMirrorStacks;
    private final int serverAuthoritativeResearch;
    private final int serverSyncable;
    private final int serverOnlyOversized;
    private final String journeyMode;
    private final String neiSearchText;
    private final List<String> filterProviders;
    private final long panelAuthoritativeStacks;
    private final long panelSemanticStacks;
    private final long panelVisibleStacks;
    private final boolean commandHintsRegistered;
    private final String commandHintResolverPath;
    private final long commandHintResolverFailures;
    private final int commandHintLastSuggestionCount;
    private final boolean backupRunning;
    private final String backupLastResult;
    private final long backupLastDurationMillis;

    public JourneyDiagnosticSnapshot(
        int clientMirrorStacks,
        int serverAuthoritativeResearch,
        int serverSyncable,
        int serverOnlyOversized,
        String journeyMode,
        String neiSearchText,
        List<String> filterProviders,
        long panelAuthoritativeStacks,
        long panelSemanticStacks,
        long panelVisibleStacks,
        boolean commandHintsRegistered,
        String commandHintResolverPath,
        long commandHintResolverFailures,
        int commandHintLastSuggestionCount,
        boolean backupRunning,
        String backupLastResult,
        long backupLastDurationMillis) {
        this.clientMirrorStacks = Math.max(0, clientMirrorStacks);
        this.serverAuthoritativeResearch = Math.max(0, serverAuthoritativeResearch);
        this.serverSyncable = Math.max(0, serverSyncable);
        this.serverOnlyOversized = Math.max(0, serverOnlyOversized);
        this.journeyMode = safe(journeyMode);
        this.neiSearchText = safe(neiSearchText);
        this.filterProviders = Collections.unmodifiableList(
            new ArrayList<String>(filterProviders == null ? Collections.<String>emptyList() : filterProviders));
        this.panelAuthoritativeStacks = Math.max(0L, panelAuthoritativeStacks);
        this.panelSemanticStacks = Math.max(0L, panelSemanticStacks);
        this.panelVisibleStacks = Math.max(0L, panelVisibleStacks);
        this.commandHintsRegistered = commandHintsRegistered;
        this.commandHintResolverPath = safe(commandHintResolverPath);
        this.commandHintResolverFailures = Math.max(0L, commandHintResolverFailures);
        this.commandHintLastSuggestionCount = Math.max(0, commandHintLastSuggestionCount);
        this.backupRunning = backupRunning;
        this.backupLastResult = safe(backupLastResult);
        this.backupLastDurationMillis = backupLastDurationMillis;
    }

    public List<String> lines() {
        List<String> lines = new ArrayList<String>();
        lines.add("clientMirrorStacks=" + clientMirrorStacks);
        lines.add("serverAuthoritativeResearch=" + serverAuthoritativeResearch);
        lines.add("serverSyncable=" + serverSyncable);
        lines.add("serverOnlyOversized=" + serverOnlyOversized);
        lines.add("journeyMode=" + journeyMode);
        lines.add("neiSearchText=" + neiSearchText);
        lines.add("journeyAppliedFilterProviders=" + filterProviders.size());
        for (String provider : filterProviders) lines.add("journeyAppliedFilterProvider=" + safe(provider));
        lines.add("panelAuthoritativeStacks=" + panelAuthoritativeStacks);
        lines.add("panelSemanticStacks=" + panelSemanticStacks);
        lines.add("panelVisibleStacks=" + panelVisibleStacks);
        lines.add("commandHintsRegistered=" + commandHintsRegistered);
        lines.add("commandHintResolverPath=" + commandHintResolverPath);
        lines.add("commandHintResolverFailures=" + commandHintResolverFailures);
        lines.add("commandHintLastSuggestionCount=" + commandHintLastSuggestionCount);
        lines.add("backupRunning=" + backupRunning);
        lines.add("backupLastResult=" + backupLastResult);
        lines.add("backupLastDurationMillis=" + backupLastDurationMillis);
        return Collections.unmodifiableList(lines);
    }

    private static String safe(String value) {
        if (value == null) return "UNAVAILABLE";
        return value.replace('\n', ' ').replace('\r', ' ');
    }
}
