package dev.gtnhjourney.nei;

import java.util.List;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.research.ResearchKey;

/** Native NEI order/family metadata. A family is one registry item across its native subtype states. */
final class JourneyNativeFamilyIndex {

    private final JourneyPanelPrecache.NativeCatalog catalog;

    JourneyNativeFamilyIndex(List<ItemStack> nativeItems) {
        catalog = JourneyPanelPrecache.nativeCatalog(nativeItems);
    }

    int nativeIndex(ItemStack display, ResearchKey key) {
        return catalog.nativeIndex(display, key);
    }

    String familyKey(ItemStack display, ResearchKey key) {
        return catalog.familyKey(display, key);
    }
}
