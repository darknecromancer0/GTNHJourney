package dev.gtnhjourney.acquisition;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

/** Coalesces one observed logical acquisition even when it unlocks multiple semantic endpoints. */
public final class ResearchObservationResult {

    private final int endpointCount;

    private ResearchObservationResult(int endpointCount) {
        this.endpointCount = Math.max(0, endpointCount);
    }

    public static ResearchObservationResult of(List<ItemStack> endpoints) {
        return new ResearchObservationResult(endpoints == null ? 0 : endpoints.size());
    }

    public boolean isNewLogicalUnlock() {
        return endpointCount > 0;
    }

    public int endpointCount() {
        return endpointCount;
    }

    public int notificationCount() {
        return isNewLogicalUnlock() ? 1 : 0;
    }
}
