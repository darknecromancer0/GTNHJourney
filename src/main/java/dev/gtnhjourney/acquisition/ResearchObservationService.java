package dev.gtnhjourney.acquisition;

import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import dev.gtnhjourney.diagnostics.ResearchTrace;
import dev.gtnhjourney.network.JourneyNetwork;
import dev.gtnhjourney.persistence.PlayerResearchService;

/** Shared server-authoritative path for newly observed player-owned item states. */
public final class ResearchObservationService {

    private final PlayerResearchService research;

    public ResearchObservationService(PlayerResearchService research) {
        if (research == null) throw new IllegalArgumentException("research must not be null");
        this.research = research;
    }

    public List<ItemStack> observe(EntityPlayerMP player, ItemStack observed) {
        if (player == null || observed == null || observed.getItem() == null || observed.stackSize <= 0) {
            return Collections.emptyList();
        }

        List<ItemStack> unlocked = research.unlockStates(player, observed);
        for (ItemStack endpoint : unlocked) {
            ResearchTrace.unlocked(player, endpoint);
            try {
                JourneyNetwork.sendUnlock(player, endpoint);
            } catch (IllegalArgumentException ignored) {
                // A malformed optional presentation endpoint must not break the authoritative research observation.
            } catch (RuntimeException ignored) {
                // Network serialization/integration failures are isolated from persistence.
            } catch (LinkageError ignored) {
                // Optional-mod linkage failures stay at the incremental sync boundary.
            }
        }
        return unlocked;
    }
}
