package dev.gtnhjourney.nei;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;

import codechicken.nei.ItemList;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
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

    private static final class NativeEntry {
        final ItemStack representative;
        final int index;

        NativeEntry(ItemStack representative, int index) {
            this.representative = representative;
            this.index = index;
        }
    }

    static final class NativeCatalog {
        private final List<ItemStack> source;
        private final Map<ResearchKey, NativeEntry> exact = new HashMap<ResearchKey, NativeEntry>();
        private final Map<String, NativeEntry> meta = new HashMap<String, NativeEntry>();
        private final Map<String, Integer> familyIndexes = new HashMap<String, Integer>();
        private final Map<ResearchKey, SemanticMetadata> semantics =
            new ConcurrentHashMap<ResearchKey, SemanticMetadata>();

        NativeCatalog(List<ItemStack> source) {
            this.source = source;
            if (source == null) return;
            for (int i = 0; i < source.size(); i++) {
                ItemStack stack = source.get(i);
                if (stack == null || stack.getItem() == null) continue;
                ResearchKey key = safeKey(stack);

                if (key != null && !exact.containsKey(key)) exact.put(key, new NativeEntry(stack, i));
                String representativeFamily = representativeFamilyKey(key, stack);
                if (!meta.containsKey(representativeFamily)) meta.put(representativeFamily, new NativeEntry(stack, i));

                if (key != null && !familyIndexes.containsKey(key.getItemId())) {
                    familyIndexes.put(key.getItemId(), Integer.valueOf(i));
                }
            }
        }

        boolean owns(List<ItemStack> items) { return source == items; }

        ItemStack representative(ItemStack display) {
            ResearchKey key = safeKey(display);
            if (key != null) {
                NativeEntry exactEntry = exact.get(key);
                if (exactEntry != null) return exactEntry.representative;
            }
            NativeEntry metaEntry = meta.get(representativeFamilyKey(key, display));
            return metaEntry == null ? display : metaEntry.representative;
        }

        int nativeIndex(ItemStack display, ResearchKey key) {
            ResearchKey effective = key == null ? safeKey(display) : key;
            if (effective == null) return Integer.MAX_VALUE;
            NativeEntry exactEntry = exact.get(effective);
            if (exactEntry != null) return exactEntry.index;
            NativeEntry metaEntry = meta.get(metaKey(effective));
            if (metaEntry != null) return metaEntry.index;
            Integer family = familyIndexes.get(effective.getItemId());
            return family == null ? Integer.MAX_VALUE : family.intValue();
        }

        String familyKey(ItemStack display, ResearchKey key) {
            ResearchKey effective = key == null ? safeKey(display) : key;
            return effective == null ? "<invalid>" : effective.getItemId();
        }
    }

    private static final Object CATALOG_LOCK = new Object();
    private static final List<ItemStack> EMPTY_ITEMS = Collections.emptyList();
    private static volatile NativeCatalog catalog;

    private JourneyPanelPrecache() {}

    static NativeCatalog nativeCatalog(List<ItemStack> nativeItems) {
        List<ItemStack> effective = nativeItems == null ? EMPTY_ITEMS : nativeItems;
        NativeCatalog current = catalog;
        if (current != null && current.owns(effective)) return current;
        synchronized (CATALOG_LOCK) {
            current = catalog;
            if (current != null && current.owns(effective)) return current;
            NativeCatalog replacement = new NativeCatalog(effective);
            catalog = replacement;
            return replacement;
        }
    }

    static SemanticMetadata semantic(ItemStack display, ResearchKey key, String displayName) {
        return semantic(nativeCatalog(ItemList.items), display, key, displayName);
    }

    static SemanticMetadata semantic(NativeCatalog nativeCatalog, ItemStack display, ResearchKey key, String displayName) {
        if (nativeCatalog == null || key == null) return computeSemantic(display, key, displayName);
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
            JourneySemanticClassifier.uncachedTypeGroup(display, key),
            JourneySemanticClassifier.uncachedKindGroup(display, key));
    }

    private static String representativeFamilyKey(ResearchKey key, ItemStack stack) {
        if (key != null) return metaKey(key);
        if (stack == null || stack.getItem() == null) return "<invalid>";
        return stack.getItem().getUnlocalizedName() + '\u0000' + stack.getItemDamage();
    }

    private static String metaKey(ResearchKey key) {
        return key.getItemId() + '\u0000' + key.getMeta();
    }

    private static ResearchKey safeKey(ItemStack stack) {
        try { return stack == null || stack.getItem() == null ? null : ItemStackKeyFactory.from(stack); }
        catch (RuntimeException ignored) { return null; }
        catch (LinkageError ignored) { return null; }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
