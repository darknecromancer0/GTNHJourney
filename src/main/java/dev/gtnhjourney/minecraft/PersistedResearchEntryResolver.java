package dev.gtnhjourney.minecraft;

import cpw.mods.fml.common.registry.GameRegistry;
import dev.gtnhjourney.acquisition.ResearchObservationPolicy;
import dev.gtnhjourney.research.ResearchKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/** Rebuilds and migrates persisted research identity from the exact stored retrieval template. */
public final class PersistedResearchEntryResolver {

    private PersistedResearchEntryResolver() {}

    public static final class ResolvedEntry {
        private final ResearchKey key;
        private final NBTTagCompound template;

        private ResolvedEntry(ResearchKey key, NBTTagCompound template) {
            this.key = key;
            this.template = template == null ? null : (NBTTagCompound) template.copy();
        }

        public ResearchKey key() {
            return key;
        }

        public NBTTagCompound template() {
            return template == null ? null : (NBTTagCompound) template.copy();
        }
    }

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

        String persistedCanonical = persistedCanonicalNbt == null ? "" : persistedCanonicalNbt;
        if (persistedTemplate == null && !persistedCanonical.isEmpty()) return null;

        NBTTagCompound fallbackTemplate = normalizePersistedTemplate(itemId, meta, persistedTemplate);
        final String fallbackCanonical;
        try {
            fallbackCanonical = fallbackTemplate == null ? "" : ResearchNbtIdentity.canonicalize(fallbackTemplate);
        } catch (IllegalArgumentException unsafeNbt) {
            return null;
        } catch (RuntimeException unsafeNbt) {
            return null;
        } catch (LinkageError unsafeNbt) {
            return null;
        }
        ResearchKey fallback = new ResearchKey(itemId, meta, fallbackCanonical);

        ItemStack reconstructed = reconstruct(fallback, fallbackTemplate);
        return resolveReconstructed(fallback, fallbackTemplate, reconstructed);
    }

    /**
     * Applies optional-mod semantic migration to one reconstructed legacy stack. Any runtime or linkage failure falls
     * back to the already validated persisted identity rather than escaping WorldSavedData loading.
     */
    static ResolvedEntry resolveReconstructed(
        ResearchKey fallback,
        NBTTagCompound fallbackTemplate,
        ItemStack reconstructed) {
        if (fallback == null) return null;
        if (reconstructed == null) return new ResolvedEntry(fallback, fallbackTemplate);
        try {
            if (!ResearchObservationPolicy.shouldPersist(reconstructed)) return null;

            ItemStack semantic = GtChargeStatePolicy.identityStack(reconstructed);
            if (GtChargeStatePolicy.classify(reconstructed) == GtChargeStatePolicy.State.EXACT) {
                if (OpenComputersChargeStatePolicy.classify(semantic) != OpenComputersChargeStatePolicy.State.EXACT) {
                    semantic = OpenComputersChargeStatePolicy.identityStack(semantic);
                } else if (Ic2ChargeStatePolicy.classify(semantic) != Ic2ChargeStatePolicy.State.EXACT) {
                    semantic = Ic2ChargeStatePolicy.identityStack(semantic);
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
        } catch (LinkageError ignored) {
            return new ResolvedEntry(fallback, fallbackTemplate);
        }
    }

    /**
     * Applies policies that are proven safe from persisted registry id + NBT alone before touching Forge registries.
     * This lets old snapshots and recovery journals migrate even in headless tests or if an optional item cannot be
     * reconstructed, while unknown item state remains exact/fail-closed.
     */
    private static NBTTagCompound normalizePersistedTemplate(String itemId, int meta, NBTTagCompound original) {
        NBTTagCompound out = ResearchTemplateNormalizer.normalize(original);
        if (out == null) return null;
        try {
            KnownTransientItemStatePolicy.normalize(itemId, meta, out);
            EmbeddedInventoryPolicy.normalize(itemId, out);
            if (isForestry(itemId)) ForestryGeneticsNbtPolicy.normalizeGeneticsTag(out);
        } catch (IllegalArgumentException ignored) {
            return out;
        } catch (RuntimeException ignored) {
            return out;
        } catch (LinkageError ignored) {
            return out;
        }
        return out.func_150296_c().isEmpty() ? null : out;
    }

    private static boolean isForestry(String itemId) {
        int colon = itemId == null ? -1 : itemId.indexOf(':');
        return colon > 0 && "Forestry".equalsIgnoreCase(itemId.substring(0, colon));
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
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
