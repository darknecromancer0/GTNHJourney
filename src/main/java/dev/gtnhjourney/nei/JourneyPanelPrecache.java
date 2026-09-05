package dev.gtnhjourney.nei;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;

import dev.gtnhjourney.research.ResearchKey;

/**
 * Session-only precache for stable NEI catalog and semantic sorting metadata.
 *
 * <p>
 * NEI replaces {@code ItemList.items} atomically and never mutates its contents in place, so source-list identity is an
 * O(1) generation stamp. Dynamic Journey state such as unlock/activity/issued/favourite sequences is deliberately not
 * cached here.
 * </p>
 */
final class JourneyPanelPrecache {

    interface SemanticComputer {
        SemanticMetadata compute(ResearchKey key, String displayName);
    }

    static final class SemanticMetadata {
        final String displayName;
        final String modGroup;
        final String typeGroup;
        final String kindGroup;

        SemanticMetadata(String displayName, String modGroup, String typeGroup, String kindGroup) {
            this.displayName = safe(displayName, "");
            this.modGroup = safe(modGroup, "misc");
            this.typeGroup = safe(typeGroup, "misc");
            this.kindGroup = safe(kindGroup, this.typeGroup);
        }

        boolean matchesDisplayName(String value) {
            return displayName.equals(safe(value, ""));
        }
    }

    static final class NativeCatalog {
        private final List<ItemStack> source;
        private final JourneyNativeRepresentativeIndex representatives;
        private final JourneyNativeFamilyIndex families;
        private final Map<ResearchKey, SemanticMetadata> semantics =
            new ConcurrentHashMap<ResearchKey, SemanticMetadata>();

        NativeCatalog(List<ItemStack> source) {
            this.source = source;
            this.representatives = new JourneyNativeRepresentativeIndex(source);
            this.families = new JourneyNativeFamilyIndex(source);
        }

        boolean owns(List<ItemStack> items) { return source == items; }
        JourneyNativeRepresentativeIndex representatives() { return representatives; }
        JourneyNativeFamilyIndex families() { return families; }
    }

    private static final Object CATALOG_LOCK = new Object();
    private static volatile NativeCatalog catalog;

    private JourneyPanelPrecache() {}

    static NativeCatalog nativeCatalog(List<ItemStack> nativeItems) {
        NativeCatalog current = catalog;
        if (current != null && current.owns(nativeItems)) return current;
        synchronized (CATALOG_LOCK) {
            current = catalog;
            if (current != null && current.owns(nativeItems)) return current;
            NativeCatalog replacement = new NativeCatalog(nativeItems);
            catalog = replacement;
            return replacement;
        }
    }

    static SemanticMetadata semantic(NativeCatalog nativeCatalog, ItemStack display, ResearchKey key, String displayName) {
        if (nativeCatalog == null || key == null) {
            return computeSemantic(display, key, displayName);
        }
        SemanticMetadata cached = nativeCatalog.semantics.get(key);
        if (cached != null && cached.matchesDisplayName(displayName)) return cached;
        synchronized (nativeCatalog.semantics) {
            cached = nativeCatalog.semantics.get(key);
            if (cached != null && cached.matchesDisplayName(displayName)) return cached;
            SemanticMetadata computed = computeSemantic(display, key, displayName);
            nativeCatalog.semantics.put(key, computed);
            return computed;
        }
    }

    static SemanticMetadata semanticForTest(ResearchKey key, String displayName, SemanticComputer computer) {
        NativeCatalog current = catalog;
        if (current == null) current = nativeCatalog(null);
        SemanticMetadata cached = current.semantics.get(key);
        if (cached != null && cached.matchesDisplayName(displayName)) return cached;
        synchronized (current.semantics) {
            cached = current.semantics.get(key);
            if (cached != null && cached.matchesDisplayName(displayName)) return cached;
            SemanticMetadata computed = computer.compute(key, displayName);
            current.semantics.put(key, computed);
            return computed;
        }
    }

    static void clear() {
        synchronized (CATALOG_LOCK) {
            catalog = null;
        }
    }

    private static SemanticMetadata computeSemantic(ItemStack display, ResearchKey key, String displayName) {
        return new SemanticMetadata(
            displayName,
            JourneySemanticClassifier.modGroup(key),
            JourneySemanticClassifier.typeGroup(display, key),
            JourneySemanticClassifier.kindGroup(display, key));
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
