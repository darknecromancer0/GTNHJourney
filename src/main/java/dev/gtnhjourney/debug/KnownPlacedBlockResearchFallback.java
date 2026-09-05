package dev.gtnhjourney.debug;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Narrow registry-only fallbacks for placed blocks whose own pick/getItem hooks intentionally hide their ItemBlock.
 * Keeps optional-mod linkage out of Journey while preserving a canonical, actually registered item representation.
 */
final class KnownPlacedBlockResearchFallback {

    private static final String THAUMCRAFT_AIRY = "Thaumcraft:blockAiry";

    private KnownPlacedBlockResearchFallback() {}

    static boolean supports(String blockRegistryId, int meta) {
        return THAUMCRAFT_AIRY.equals(blockRegistryId) && meta == 0;
    }

    static ItemStack resolve(Block block, int meta) {
        if (block == null) return null;
        try {
            GameRegistry.UniqueIdentifier id = GameRegistry.findUniqueIdentifierFor(block);
            if (id == null || !supports(id.toString(), meta)) return null;
            Item item = GameRegistry.findItem(id.modId, id.name);
            if (item == null) return null;
            return new ItemStack(item, 1, meta);
        } catch (RuntimeException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
