package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import codechicken.nei.ItemList;
import codechicken.nei.ItemPanel;
import codechicken.nei.ItemPanels;
import codechicken.nei.LayoutManager;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientFavouriteMirror;
import dev.gtnhjourney.client.ClientIssuedMirror;
import dev.gtnhjourney.client.ClientPresentationActivityMirror;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Owns NEI's item-panel presentation while a Journey view/sort transform is active. */
public final class JourneyPanelController {

    interface Presenter { ItemStack present(ItemStack stack); }

    private static final Presenter DEFAULT_PRESENTER = new Presenter() {
        @Override public ItemStack present(ItemStack stack) { return JourneyPresentationSafety.forNei(stack); }
    };

    private static volatile boolean owned;
    private static volatile ArrayList<ItemStack> lastPublishedList;
    private static volatile StagedFilterResult stagedFilterResult;

    private JourneyPanelController() {}

    public static boolean shouldOwnCurrentView() {
        JourneyViewState.Mode mode = JourneyViewState.mode();
        if (mode != JourneyViewState.Mode.ALL) return true;
        if (JourneySortState.hasTransform(JourneyViewState.Mode.ALL)) return true;
        return JourneyEmptySearchPolicy.isLiteralEmptyQuery(
            JourneyFilterDiagnostics.safeSearchText(LayoutManager.searchField));
    }

    /** Structural refresh used for view/content/sort changes. Search-only changes use the async completion path below. */
    public static void refresh(boolean resetPage) {
        stagedFilterResult = null;
        if (!shouldOwnCurrentView()) {
            releaseToNei();
            return;
        }
        BuiltPanel built = buildPanel(true);
        if (built != null) publishBuiltPanel(built, resetPage);
    }

    /**
     * Called from NEI Item Filtering's worker thread at its final ItemPanel publication point. Heavy Journey filtering,
     * semantic metadata and sorting stay on that worker. Returning true cancels NEI's temporary native-panel overwrite.
     */
    public static boolean captureCompletedNativeFilter(ArrayList<ItemStack> nativeFiltered) {
        if (nativeFiltered == null || !ItemList.updateFilter.name.equals(Thread.currentThread().getName())) return false;
        if (!owned || !shouldOwnCurrentView()) return false;

        StateStamp stamp = StateStamp.capture();
        BuiltPanel built;
        try {
            built = buildPanel(false);
        } catch (Throwable failure) {
            JourneyRuntimeCounters.presentationFailure();
            return false;
        }

        // A new keypress sets RestartableTask.restart while this worker build is running. Never publish that stale query.
        if (ItemList.updateFilter.interrupted() || !stamp.isCurrent()) return true;
        if (built == null) return false;

        stagedFilterResult = new StagedFilterResult(stamp, built);
        JourneyNeiFilterRevision.invalidate();
        return true;
    }

    /** Client-thread hot path: atomically publishes an already-built worker result and performs no filtering/sorting. */
    public static boolean publishCompletedFilter() {
        StagedFilterResult staged = stagedFilterResult;
        stagedFilterResult = null;
        if (staged == null || !staged.stamp.isCurrent() || !shouldOwnCurrentView()) return false;
        publishBuiltPanel(staged.panel, true);
        return true;
    }

    private static BuiltPanel buildPanel(boolean synchronizeLayout) {
        JourneyViewState.Mode mode = JourneyViewState.mode();
        if (!shouldOwnCurrentView()) return null;

        List<ItemStack> authoritative = ClientStackMirror.snapshot();
        Map<ResearchKey, ItemStack> byKey = index(authoritative);
        if (synchronizeLayout) synchronizeSearchWidgetVisibility();
        List<JourneyNeiFilterPipeline.FilterBinding> activeFilters = JourneyNeiFilterPipeline.snapshotActiveFilters();
        JourneyNativeRepresentativeIndex representatives = new JourneyNativeRepresentativeIndex(ItemList.items);
        JourneyNativeFamilyIndex nativeFamilies = new JourneyNativeFamilyIndex(ItemList.items);
        IdentityHashMap<ItemStack, ResearchKey> presentationKeys = new IdentityHashMap<ItemStack, ResearchKey>();

        final ArrayList<ItemStack> visible;
        if (mode == JourneyViewState.Mode.ALL) {
            visible = nativeVisible(activeFilters, representatives, nativeFamilies);
        } else if (mode == JourneyViewState.Mode.CREATIVE) {
            visible = creativeVisible(authoritative, byKey, activeFilters, representatives, nativeFamilies, presentationKeys);
        } else {
            visible = researchVisible(mode, byKey, activeFilters, representatives, nativeFamilies, presentationKeys);
        }
        return new BuiltPanel(visible, presentationKeys, authoritative.size(), byKey.size());
    }

    private static void publishBuiltPanel(BuiltPanel built, boolean resetPage) {
        if (built == null) return;
        int previousPage = Math.max(0, ItemPanels.itemPanel.getGrid().getPage() - 1);
        JourneyPresentationKeyResolver.replace(built.presentationKeys);
        JourneyRuntimeCounters.panelPublication(built.authoritativeCount, built.indexedCount, built.visible.size());
        ItemPanel.updateItemList(built.visible);
        JourneyRuntimeCounters.panelIncrementalUpdate();
        ItemPanels.itemPanel.getGrid().setPage(JourneyPageRetentionPolicy.pageAfterRefresh(
            previousPage,
            ItemPanels.itemPanel.getGrid().getNumPages(),
            resetPage));
        owned = true;
        lastPublishedList = built.visible;
    }

    private static ArrayList<ItemStack> researchVisible(
        JourneyViewState.Mode mode,
        Map<ResearchKey, ItemStack> byKey,
        List<JourneyNeiFilterPipeline.FilterBinding> activeFilters,
        JourneyNativeRepresentativeIndex representatives,
        JourneyNativeFamilyIndex nativeFamilies,
        Map<ItemStack, ResearchKey> presentationKeys) {
        List<ResearchKey> researchOldestFirst = ClientStackMirror.snapshotKeysInResearchOrder();
        List<ResearchKey> canonical = JourneyPanelSnapshot.keys(
            researchOldestFirst,
            ClientActivityMirror.snapshotOldestFirst(),
            mode);
        Map<ResearchKey, Long> unlockSequence = sequenceMap(researchOldestFirst);
        Map<ResearchKey, Long> activitySequence = sequenceMap(ClientActivityMirror.snapshotOldestFirst());
        List<JourneySortEntry> survivors = new ArrayList<JourneySortEntry>(canonical.size());

        int canonicalIndex = 0;
        for (ResearchKey key : canonical) {
            if (mode == JourneyViewState.Mode.FAVOURITE && !ClientFavouriteMirror.contains(ResearchFingerprint.of(key))) {
                canonicalIndex++;
                continue;
            }
            ItemStack original = byKey.get(key);
            if (original == null) {
                canonicalIndex++;
                continue;
            }
            try {
                ItemStack display = safePresentation(original);
                if (display == null || display.getItem() == null) {
                    canonicalIndex++;
                    continue;
                }
                presentationKeys.put(display, key);
                ItemStack nativeRepresentative = representatives.representative(display);
                if (!JourneyNeiFilterPipeline.matchesAll(display, nativeRepresentative, activeFilters)) {
                    canonicalIndex++;
                    continue;
                }
                survivors.add(sortEntry(
                    display,
                    key,
                    nativeFamilies,
                    sequence(unlockSequence, key),
                    Math.max(sequence(activitySequence, key), ClientPresentationActivityMirror.sequence(key)),
                    ClientFavouriteMirror.addSequence(ResearchFingerprint.of(key)),
                    canonicalIndex));
            } catch (Throwable ignored) {
                JourneyRuntimeCounters.presentationFailure();
            }
            canonicalIndex++;
        }
        return stacks(sort(survivors, mode));
    }

    private static ArrayList<ItemStack> creativeVisible(
        List<ItemStack> authoritative,
        Map<ResearchKey, ItemStack> byKey,
        List<JourneyNeiFilterPipeline.FilterBinding> activeFilters,
        JourneyNativeRepresentativeIndex representatives,
        JourneyNativeFamilyIndex nativeFamilies,
        Map<ItemStack, ResearchKey> presentationKeys) {
        ArrayList<ItemStack> union = JourneyCreativeUnion.merge(ItemList.items, authoritative);
        Map<ResearchKey, Long> unlockSequence = sequenceMap(ClientStackMirror.snapshotKeysInResearchOrder());
        Map<ResearchKey, Long> activitySequence = sequenceMap(ClientActivityMirror.snapshotOldestFirst());
        List<JourneySortEntry> survivors = new ArrayList<JourneySortEntry>(union.size());
        int canonicalIndex = 0;
        for (ItemStack display : union) {
            if (display == null || display.getItem() == null) {
                canonicalIndex++;
                continue;
            }
            try {
                ResearchKey key = safeKey(display);
                if (key == null) {
                    canonicalIndex++;
                    continue;
                }
                if (byKey.containsKey(key)) presentationKeys.put(display, key);
                ItemStack nativeRepresentative = representatives.representative(display);
                if (!JourneyNeiFilterPipeline.matchesAll(display, nativeRepresentative, activeFilters)) {
                    canonicalIndex++;
                    continue;
                }
                survivors.add(sortEntry(
                    display,
                    key,
                    nativeFamilies,
                    sequence(unlockSequence, key),
                    Math.max(sequence(activitySequence, key), ClientPresentationActivityMirror.sequence(key)),
                    ClientFavouriteMirror.addSequence(ResearchFingerprint.of(key)),
                    canonicalIndex));
            } catch (Throwable ignored) {
                JourneyRuntimeCounters.presentationFailure();
            }
            canonicalIndex++;
        }
        return stacks(sort(survivors, JourneyViewState.Mode.CREATIVE));
    }

    private static ArrayList<ItemStack> nativeVisible(
        List<JourneyNeiFilterPipeline.FilterBinding> activeFilters,
        JourneyNativeRepresentativeIndex representatives,
        JourneyNativeFamilyIndex nativeFamilies) {
        Map<ResearchKey, Long> unlockSequence = sequenceMap(ClientStackMirror.snapshotKeysInResearchOrder());
        Map<ResearchKey, Long> activitySequence = sequenceMap(ClientActivityMirror.snapshotOldestFirst());
        List<JourneySortEntry> survivors = new ArrayList<JourneySortEntry>(ItemList.items.size());
        int canonicalIndex = 0;
        for (ItemStack nativeStack : ItemList.items) {
            if (nativeStack == null || nativeStack.getItem() == null) {
                canonicalIndex++;
                continue;
            }
            try {
                ItemStack display = nativeStack.copy();
                ResearchKey key = safeKey(display);
                if (key == null) {
                    canonicalIndex++;
                    continue;
                }
                ItemStack representative = representatives.representative(display);
                if (!JourneyNeiFilterPipeline.matchesAll(display, representative, activeFilters)) {
                    canonicalIndex++;
                    continue;
                }
                survivors.add(sortEntry(
                    display,
                    key,
                    nativeFamilies,
                    sequence(unlockSequence, key),
                    Math.max(sequence(activitySequence, key), ClientPresentationActivityMirror.sequence(key)),
                    ClientFavouriteMirror.addSequence(ResearchFingerprint.of(key)),
                    canonicalIndex));
            } catch (Throwable ignored) {
                JourneyRuntimeCounters.presentationFailure();
            }
            canonicalIndex++;
        }
        return stacks(sort(survivors, JourneyViewState.Mode.ALL));
    }

    private static List<JourneySortEntry> sort(List<JourneySortEntry> source, JourneyViewState.Mode mode) {
        return JourneySortPlanner.sort(
            source,
            JourneySortState.group(mode),
            JourneySortState.order(mode),
            JourneySortState.latest(mode));
    }

    private static JourneySortEntry sortEntry(
        ItemStack display,
        ResearchKey key,
        JourneyNativeFamilyIndex nativeFamilies,
        long unlockSequence,
        long activitySequence,
        long favouriteSequence,
        int canonicalIndex) {
        return new JourneySortEntry(
            key,
            display,
            nativeFamilies.nativeIndex(display, key),
            nativeFamilies.familyKey(display, key),
            JourneySemanticClassifier.modGroup(key),
            JourneySemanticClassifier.typeGroup(display, key),
            JourneySemanticClassifier.kindGroup(display, key),
            safeDisplayName(display, key),
            unlockSequence,
            activitySequence,
            favouriteSequence,
            canonicalIndex);
    }

    private static ArrayList<ItemStack> stacks(List<JourneySortEntry> entries) {
        ArrayList<ItemStack> out = new ArrayList<ItemStack>(entries == null ? 0 : entries.size());
        if (entries != null) {
            for (JourneySortEntry entry : entries) {
                if (entry != null && entry.stack() != null) out.add(entry.stack());
            }
        }
        return out;
    }

    private static Map<ResearchKey, Long> sequenceMap(List<ResearchKey> oldestFirst) {
        Map<ResearchKey, Long> out = new LinkedHashMap<ResearchKey, Long>();
        if (oldestFirst == null) return out;
        long sequence = 0L;
        for (ResearchKey key : oldestFirst) if (key != null) out.put(key, Long.valueOf(++sequence));
        return out;
    }

    private static long sequence(Map<ResearchKey, Long> values, ResearchKey key) {
        Long value = values == null || key == null ? null : values.get(key);
        return value == null ? -1L : value.longValue();
    }

    private static String safeDisplayName(ItemStack stack, ResearchKey key) {
        try {
            String name = stack == null ? null : stack.getDisplayName();
            if (name != null && !name.isEmpty()) return name;
        } catch (RuntimeException ignored) {}
        return key == null ? "" : key.getItemId();
    }

    private static Map<ResearchKey, ItemStack> index(List<ItemStack> authoritative) {
        Map<ResearchKey, ItemStack> byKey = new LinkedHashMap<ResearchKey, ItemStack>();
        if (authoritative == null) return byKey;
        for (ItemStack stack : authoritative) {
            if (stack == null || stack.getItem() == null) continue;
            ResearchKey key = safeKey(stack);
            if (key != null && !byKey.containsKey(key)) byKey.put(key, stack);
        }
        return byKey;
    }

    private static ResearchKey safeKey(ItemStack stack) {
        try { return stack == null || stack.getItem() == null ? null : ItemStackKeyFactory.from(stack); }
        catch (IllegalArgumentException ignored) { return null; }
        catch (RuntimeException ignored) { return null; }
        catch (LinkageError ignored) { return null; }
    }

    private static void synchronizeSearchWidgetVisibility() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null) return;
        GuiScreen currentScreen = minecraft.currentScreen;
        if (currentScreen instanceof GuiContainer) LayoutManager.layout((GuiContainer) currentScreen);
    }

    static ItemStack safePresentation(ItemStack original) { return safePresentation(original, DEFAULT_PRESENTER); }

    static ItemStack safePresentation(ItemStack original, Presenter presenter) {
        if (original == null || presenter == null) return null;
        try { return presenter.present(original); }
        catch (Throwable ignored) {
            JourneyRuntimeCounters.presentationFailure();
            return null;
        }
    }

    public static void ensureOwned() {
        if (!shouldOwnCurrentView()) {
            releaseToNei();
            return;
        }
        if (!owned || ItemPanels.itemPanel.realItems != lastPublishedList) refresh(false);
    }

    public static void releaseToNei() {
        boolean wasOwned = owned;
        owned = false;
        lastPublishedList = null;
        stagedFilterResult = null;
        JourneyPresentationKeyResolver.clear();
        if (wasOwned) ItemList.updateFilter.restart();
    }

    public static void clear() {
        owned = false;
        lastPublishedList = null;
        stagedFilterResult = null;
        JourneyPresentationKeyResolver.clear();
    }

    static boolean isOwned() { return owned; }

    private static final class BuiltPanel {
        final ArrayList<ItemStack> visible;
        final Map<ItemStack, ResearchKey> presentationKeys;
        final int authoritativeCount;
        final int indexedCount;

        BuiltPanel(
            ArrayList<ItemStack> visible,
            Map<ItemStack, ResearchKey> presentationKeys,
            int authoritativeCount,
            int indexedCount) {
            this.visible = visible == null ? new ArrayList<ItemStack>() : visible;
            this.presentationKeys = presentationKeys;
            this.authoritativeCount = authoritativeCount;
            this.indexedCount = indexedCount;
        }
    }

    private static final class StagedFilterResult {
        final StateStamp stamp;
        final BuiltPanel panel;

        StagedFilterResult(StateStamp stamp, BuiltPanel panel) {
            this.stamp = stamp;
            this.panel = panel;
        }
    }

    /** Rejects worker results if research/view/sort/chronology changed while the expensive filter build was running. */
    private static final class StateStamp {
        final long research;
        final long activity;
        final long issued;
        final long favourite;
        final long presentation;
        final long view;
        final long sort;

        private StateStamp(
            long research,
            long activity,
            long issued,
            long favourite,
            long presentation,
            long view,
            long sort) {
            this.research = research;
            this.activity = activity;
            this.issued = issued;
            this.favourite = favourite;
            this.presentation = presentation;
            this.view = view;
            this.sort = sort;
        }

        static StateStamp capture() {
            return new StateStamp(
                ClientResearchMirror.revision(),
                ClientActivityMirror.revision(),
                ClientIssuedMirror.revision(),
                ClientFavouriteMirror.revision(),
                ClientPresentationActivityMirror.revision(),
                JourneyViewState.revision(),
                JourneySortState.revision());
        }

        boolean isCurrent() {
            return research == ClientResearchMirror.revision()
                && activity == ClientActivityMirror.revision()
                && issued == ClientIssuedMirror.revision()
                && favourite == ClientFavouriteMirror.revision()
                && presentation == ClientPresentationActivityMirror.revision()
                && view == JourneyViewState.revision()
                && sort == JourneySortState.revision();
        }
    }
}
