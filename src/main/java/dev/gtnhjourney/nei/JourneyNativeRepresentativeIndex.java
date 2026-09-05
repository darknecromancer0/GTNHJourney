package dev.gtnhjourney.nei;

import java.util.List;

import net.minecraft.item.ItemStack;

/** Resolves Journey presentation stacks back to a native NEI universe member when possible. */
final class JourneyNativeRepresentativeIndex {

    private final JourneyPanelPrecache.NativeCatalog catalog;

    JourneyNativeRepresentativeIndex(List<ItemStack> nativeItems) {
        catalog = JourneyPanelPrecache.nativeCatalog(nativeItems);
    }

    ItemStack representative(ItemStack display) {
        return catalog.representative(display);
    }
}
