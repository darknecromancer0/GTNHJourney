package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Rebuilds and migrates persisted research identity without trusting stale or incomplete canonical-NBT text.
 *
 * <p>The original NBT template is authoritative because it is also what retrieval recreates. If a persisted entry
 * claims to have NBT but its template is missing, the entry is rejected instead of silently becoming a different bare
 * item. If the referenced item still exists, the reconstructed stack is passed through the current semantic identity
 * and retrieval-template policies together. This is important for migrations such as partial electric charge: the key
 * must become the base endpoint <em>and</em> the stored template must lose the partial charge in the same operation.</p>
 */
public final class PersistedResearchEntryResolver {

    private PersistedResearchEntryResolver() {}

    /** Immutable migration result containing the identity and the exact template that must be stored under it. */
    public static final class ResolvedEntry {
        private final ResearchKey key;
        private final NBTTagCompound template;

        private ResolvedEntry(ResearchKey key, NBTTagCompound template) {
            this.key = key;
            this.template = template == null ? null : (NBTTagCompound) template.copy();
        }

        public ResearchKey key() { return key; }
        public NBTTagCompound template() {
            return template == null ? null : (NBTTagCompound) template.copy();
        }
    }

    /** Backwards-compatible key-only view used by older callers/tests. */
    public static ResearchKey resolve(String itemId, int meta, String persistedCanonicalNbt, NBTTagCompound template) {
        ResolvedEntry resolved = resolveEntry(itemId, meta, persistedCanonicalNbt, template);
        return resolved == null ? null : resolved.key();
    }

    public static ResolvedEntry resolveEntry(
        String itemId,
        int meta,
        String persistedCanonicalNbt,
        NBTTagCompound persistedTemplate) {

        if (itemId == null || itemId.trim().isEmpty()) return null;

        // A non-empty canonical identity without its retrieval payload cannot be recreated safely. Fail closed.
        String persistedCanonical = persistedCanonicalNbt == null ? "" : persistedCanonicalNbt;
        if (persistedTemplate == null && !persistedCanonical.isEmpty()) return null;

        NBTTagCompound fallbackTemplate = ResearchTemplateNormalizer.normalize(persistedTemplate);
        final String fallbackCanonical;
        try {
            fallbackCanonical = fallbackTemplate == null ? "" : ResearchNbtIdentity.canonicalize(fallbackTemplate);
        } catch (IllegalArgumentException unsafeNbt) {
            // A corrupt/hostile old entry must never prevent the entire WorldSavedData from loading.
            return null;
        } catch (RuntimeException unsafeNbt) {
            return null;
        }
        ResearchKey fallback = new ResearchKey(itemId, meta, fallbackCanonical);

        ItemStack reconstructed = reconstruct(fallback, persistedTemplate);
        if (reconstructed == null) {
            // Keep unavailable/orphaned entries structurally intact so diagnostics, prune and undo still work.
            return new ResolvedEntry(fallback, fallbackTemplate);
        }

        try {
            // Apply endpoint semantics to the ItemStack first, then normalize the retrieval template from that same
            // semantic stack. This keeps ResearchKey and retrieval payload impossible to drift apart during migration.
            ItemStack semantic = GtChargeStatePolicy.identityStack(reconstructed);
            if (GtChargeStatePolicy.classify(reconstructed) == GtChargeStatePolicy.State.EXACT) {
                ItemStack ic2Semantic = Ic2ChargeStatePolicy.identityStack(semantic);
                if (Ic2ChargeStatePolicy.classify(semantic) != Ic2ChargeStatePolicy.State.EXACT) {
                    semantic = ic2Semantic;
                } else {
                    semantic = CofhChargeStatePolicy.identityStack(semantic);
                }
            }
            if (semantic == null || semantic.getItem() == null) return new ResolvedEntry(fallback, fallbackTemplate);
            semantic.stackSize = 1;

            NBTTagCompound normalizedTemplate = ResearchTemplateNormalizer.normalize(semantic);
            semantic.setTagCompound(normalizedTemplate == null ? null : (NBTTagCompound) normalizedTemplate.copy());

            ResearchKey key = ItemStackKeyFactory.from(semantic);
            return new ResolvedEntry(key, normalizedTemplate);
        } catch (IllegalArgumentException ignored) {
            return new ResolvedEntry(fallback, fallbackTemplate);
        } catch (RuntimeException ignored) {
            return new ResolvedEntry(fallback, fallbackTemplate);
        }
    }

    private static ItemStack reconstruct(ResearchKey key, NBTTagCompound template) {
        int colon = key.getItemId().indexOf(':');
        if (colon <= 0 || colon >= key.getItemId().length() - 1) return null;
        try {
            Item item = GameRegistry.findItem(key.getItemId().substring(0, colon), key.getItemId().substring(colon + 1));
            if (item == null) return null;
            ItemStack stack = new ItemStack(item, 1, key.getMeta());
            if (template != null) stack.setTagCompound((NBTTagCompound) template.copy());
            return stack;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
