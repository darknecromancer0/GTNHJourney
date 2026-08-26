package dev.gtnhjourney.acquisition;

import net.minecraft.entity.player.EntityPlayerMP;

import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.persistence.PlayerResearchService;
import dev.gtnhjourney.recovery.JourneyMutationService;

/** Shared implementation for the S button and /journey rescan. */
public final class ManualInventoryResearchService {

    private ManualInventoryResearchService() {}

    public static Result scan(
        EntityPlayerMP player,
        PlayerResearchService research,
        JourneyMutationService mutations) {
        if (player == null || research == null || mutations == null) return new Result(0, 0, 0, 0);

        DeepInventoryResearchCollector.Result collected = DeepInventoryResearchCollector.collect(player);
        int added = mutations.applyBulkAdd(player, collected.candidates(), "Manual inventory deep scan");

        // Manual recovery is also a client-repair path. Always refresh the complete mirror even when added == 0.
        JourneyNetwork.sendFullSync(player, research.snapshotStacksInUnlockOrder(player), research.snapshotActivityOrder(player));
        return new Result(
            collected.topLevelStacks(),
            collected.embeddedStacks(),
            collected.skippedStacks(),
            added);
    }

    public static final class Result {

        private final int topLevelStacks;
        private final int embeddedStacks;
        private final int skippedStacks;
        private final int addedStates;

        Result(int topLevelStacks, int embeddedStacks, int skippedStacks, int addedStates) {
            this.topLevelStacks = Math.max(0, topLevelStacks);
            this.embeddedStacks = Math.max(0, embeddedStacks);
            this.skippedStacks = Math.max(0, skippedStacks);
            this.addedStates = Math.max(0, addedStates);
        }

        public int topLevelStacks() {
            return topLevelStacks;
        }

        public int embeddedStacks() {
            return embeddedStacks;
        }

        public int skippedStacks() {
            return skippedStacks;
        }

        public int addedStates() {
            return addedStates;
        }

        public String summary() {
            StringBuilder out = new StringBuilder();
            out.append("Inventory scan: ")
                .append(topLevelStacks)
                .append(" top-level, ")
                .append(embeddedStacks)
                .append(" embedded, +")
                .append(addedStates)
                .append(" new states");
            if (skippedStacks > 0) out.append(", ").append(skippedStacks).append(" skipped");
            return out.toString();
        }
    }
}
