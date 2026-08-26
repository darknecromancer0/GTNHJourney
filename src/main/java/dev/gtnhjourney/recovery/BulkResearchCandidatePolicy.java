package dev.gtnhjourney.recovery;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.acquisition.ResearchObservationPolicy;

/** Shared candidate gate for explicit migration/rescan research batches. */
final class BulkResearchCandidatePolicy {

    private BulkResearchCandidatePolicy() {}

    static boolean shouldObserve(ItemStack stack) {
        return ResearchObservationPolicy.shouldObserve(stack);
    }
}
