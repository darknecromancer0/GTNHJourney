package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import codechicken.nei.ItemList;
import codechicken.nei.ItemPanel;
import codechicken.nei.ItemPanels;
import codechicken.nei.LayoutManager;
import codechicken.nei.api.ItemFilter;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Owns NEI's item-panel contents while J or N is active without rebuilding NEI's global item universe. */
public final class JourneyPanelController {

    interface Presenter {
        ItemStack present(ItemStack stack);
    }

    private static final Presenter DEFAULT_PRESENTER = new Presenter() {

        @Override
        public ItemStack present(ItemStack stack) {
            return JourneyPresentationSafety.forNei(stack);
        }
    };

    private static boolean owned;
    private static ArrayList<ItemStack> lastPublishedList;

    private JourneyPanelController() {}

    public static void refresh(boolean resetPage) {
        JourneyViewState.Mode mode = JourneyViewState.mode();
        if (mode == JourneyViewState.Mode.ALL) {
            releaseToNei();
            return;
        }

        List<ItemStack> authoritative = ClientStackMirror.snapshot();
        Map<ResearchKey, ItemStack> byKey = new LinkedHashMap<ResearchKey, ItemStack>();
        for (ItemStack stack : authoritative) {
            if (stack == null || stack.getItem() == null) continue;
            try {
                ResearchKey key = ItemStackKeyFactory.from(stack);
                if (!byKey.containsKey(key)) byKey.put(key, stack);
            } catch (IllegalArgumentException ignored) {
                // A malformed client presentation must not remove the authoritative server research state.
            } catch (RuntimeException ignored) {
                // Third-party ItemStack implementations are allowed to fail closed at the display boundary.
            } catch (LinkageError ignored) {
                // Optional integration failures are presentation-only here.
            }
        }

        List<ResearchKey> ordered = JourneyPanelSnapshot.keys(
            new ArrayList<ResearchKey>(byKey.keySet()),
            ClientActivityMirror.snapshotOldestFirst(),
            mode);
        ArrayList<ItemStack> visible = new ArrayList<ItemStack>(ordered.size());
        // Journey already owns the authoritative subset. Re-applying every global NEI ItemFilterProvider here can
        // silently reject exact Journey templates that were never part of NEI's global item universe. Preserve only
        // the user's live search expression while J/N/D owns the panel.
        ItemFilter activeFilter = LayoutManager.searchField == null ? null : LayoutManager.searchField.getFilter();
        JourneyPresentationKeyResolver.clear();

        for (ResearchKey key : ordered) {
            ItemStack original = byKey.get(key);
            if (original == null) continue;
            try {
                ItemStack display = safePresentation(original);
                if (display == null || display.getItem() == null) continue;
                JourneyPresentationKeyResolver.register(display, key);
                if (activeFilter == null || activeFilter.matches(display)) visible.add(display);
            } catch (Throwable ignored) {
                // Third-party presentation/filter failures omit only this client display entry.
                JourneyRuntimeCounters.presentationFailure();
            }
        }

        ItemPanel.updateItemList(visible);
        JourneyRuntimeCounters.panelIncrementalUpdate();
        if (resetPage) ItemPanels.itemPanel.getGrid().setPage(0);
        owned = true;
        lastPublishedList = visible;
    }

    static ItemStack safePresentation(ItemStack original) {
        return safePresentation(original, DEFAULT_PRESENTER);
    }

    static ItemStack safePresentation(ItemStack original, Presenter presenter) {
        if (original == null || presenter == null) return null;
        try {
            return presenter.present(original);
        } catch (Throwable ignored) {
            JourneyRuntimeCounters.presentationFailure();
            return null;
        }
    }

    public static void ensureOwned() {
        if (JourneyViewState.mode() == JourneyViewState.Mode.ALL) {
            releaseToNei();
            return;
        }
        if (!owned || ItemPanels.itemPanel.realItems != lastPublishedList) refresh(false);
    }

    public static void releaseToNei() {
        boolean wasOwned = owned;
        owned = false;
        lastPublishedList = null;
        JourneyPresentationKeyResolver.clear();
        if (wasOwned) ItemList.updateFilter.restart();
    }

    public static void clear() {
        owned = false;
        lastPublishedList = null;
        JourneyPresentationKeyResolver.clear();
    }

    static boolean isOwned() {
        return owned;
    }
}
