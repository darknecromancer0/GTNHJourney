package dev.gtnhjourney.debug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.research.ResearchKey;

/** Deduplicates migration candidates by their canonical research identity. */
public final class ResearchCandidateDeduplicator {

    public interface IdentityResolver {

        ResearchKey identity(ItemStack stack);
    }

    private ResearchCandidateDeduplicator() {}

    public static List<ItemStack> deduplicate(List<ItemStack> candidates, IdentityResolver resolver) {
        Map<ResearchKey, ItemStack> unique = new LinkedHashMap<ResearchKey, ItemStack>();
        if (candidates == null || resolver == null) return new ArrayList<ItemStack>();

        for (ItemStack stack : candidates) {
            if (stack == null) continue;

            try {
                ResearchKey identity = resolver.identity(stack);
                if (identity != null && !unique.containsKey(identity)) {
                    unique.put(identity, stack.copy());
                }
            } catch (RuntimeException | LinkageError ignored) {
                // Optional integrations may fail to resolve individual stacks. A migration scan must continue.
            }
        }

        return new ArrayList<ItemStack>(unique.values());
    }
}
