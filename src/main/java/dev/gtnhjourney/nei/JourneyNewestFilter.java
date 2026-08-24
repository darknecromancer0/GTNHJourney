package dev.gtnhjourney.nei;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.item.ItemStack;

import codechicken.nei.api.ItemFilter;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Cached fallback subset for the most recently researched states. */
public final class JourneyNewestFilter implements ItemFilter {

    private volatile long cachedRevision = Long.MIN_VALUE;
    private volatile Set<ResearchKey> cached = Collections.emptySet();

    @Override
    public boolean matches(ItemStack item) {
        if (item == null || item.getItem() == null) return false;
        refreshIfNeeded();
        try {
            return cached.contains(JourneyPresentationKeyResolver.keyOf(item));
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void refreshIfNeeded() {
        long revision = ClientResearchMirror.revision();
        if (revision == cachedRevision) return;
        synchronized (this) {
            revision = ClientResearchMirror.revision();
            if (revision == cachedRevision) return;
            Set<ResearchKey> newest = new HashSet<ResearchKey>();
            for (ItemStack stack : ClientStackMirror.snapshotNewest(JourneyConfig.newestLimit())) {
                try {
                    newest.add(ItemStackKeyFactory.from(stack));
                } catch (IllegalArgumentException ignored) {}
            }
            cached = Collections.unmodifiableSet(newest);
            cachedRevision = revision;
        }
    }
}
