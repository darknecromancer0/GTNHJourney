package dev.gtnhjourney.nei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import codechicken.nei.ItemList;
import codechicken.nei.ItemPanel;
import codechicken.nei.ItemPanels;
import codechicken.nei.api.ItemFilter;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.config.JourneyConfig;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchKey;

/** Owns NEI's item-panel contents while J or N is active without rebuilding NEI's global item universe. */
public final class JourneyPanelController {

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

        List<ResearchKey> ordered = JourneyPanelSnapshot
            .keys(new ArrayList<ResearchKey>(byKey.keySet()), mode, JourneyConfig.newestLimit());
        ArrayList<ItemStack> visible = new ArrayList<ItemStack>(ordered.size());
        ItemFilter activeFilter = ItemList.getItemListFilter();
        JourneyPresentationKeyResolver.clear();

        for (ResearchKey key : ordered) {
            ItemStack original = byKey.get(key);
            if (original == null) continue;
            try {
                ItemStack display = JourneyPresentationSafety.forNei(original);
                if (display == null || display.getItem() == null) continue;
                JourneyPresentationKeyResolver.register(display, key);
                if (activeFilter == null || activeFilter.matches(display)) visible.add(display);
            } catch (RuntimeException ignored) {
                // Omit only the broken display state; retrieval remains server-authoritative and persisted.
            } catch (LinkageError ignored) {
                // A renderer/filter integration failure must never crash Journey panel construction.
            }
        }

        ItemPanel.updateItemList(visible);
        JourneyRuntimeCounters.panelIncrementalUpdate();
        if (resetPage) ItemPanels.itemPanel.getGrid().setPage(0);
        owned = true;
        lastPublishedList = visible;
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
