package dev.gtnhjourney.nei;

import java.util.ArrayList;
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
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.diagnostics.JourneyRuntimeCounters;
import dev.gtnhjourney.minecraft.ItemStackKeyFactory;
import dev.gtnhjourney.research.ResearchFingerprint;
import dev.gtnhjourney.research.ResearchKey;

/** Owns NEI's item-panel contents while a Journey view is active without rebuilding NEI's global item universe. */
public final class JourneyPanelController {

    interface Presenter { ItemStack present(ItemStack stack); }

    private static final Presenter DEFAULT_PRESENTER = new Presenter() {
        @Override public ItemStack present(ItemStack stack) { return JourneyPresentationSafety.forNei(stack); }
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
        Map<ResearchKey, ItemStack> byKey = index(authoritative);
        synchronizeSearchWidgetVisibility();
        List<JourneyNeiFilterPipeline.FilterBinding> activeFilters = JourneyNeiFilterPipeline.snapshotActiveFilters();
        JourneyNativeRepresentativeIndex representatives = new JourneyNativeRepresentativeIndex(ItemList.items);
        JourneyPresentationKeyResolver.clear();

        final ArrayList<ItemStack> visible;
        if (mode == JourneyViewState.Mode.CREATIVE) {
            visible = creativeVisible(authoritative, byKey, activeFilters, representatives);
        } else {
            visible = researchVisible(mode, byKey, activeFilters, representatives);
        }

        int previousPage = Math.max(0, ItemPanels.itemPanel.getGrid().getPage() - 1);
        JourneyRuntimeCounters.panelPublication(authoritative.size(), byKey.size(), visible.size());
        ItemPanel.updateItemList(visible);
        JourneyRuntimeCounters.panelIncrementalUpdate();
        if (resetPage) ItemPanels.itemPanel.getGrid().setPage(0);
        else ItemPanels.itemPanel.getGrid().setPage(previousPage);
        owned = true;
        lastPublishedList = visible;
    }

    private static ArrayList<ItemStack> researchVisible(
        JourneyViewState.Mode mode,
        Map<ResearchKey, ItemStack> byKey,
        List<JourneyNeiFilterPipeline.FilterBinding> activeFilters,
        JourneyNativeRepresentativeIndex representatives) {
        List<ResearchKey> ordered = JourneyPanelSnapshot.keys(
            new ArrayList<ResearchKey>(byKey.keySet()),
            ClientActivityMirror.snapshotOldestFirst(),
            mode);
        ArrayList<ItemStack> visible = new ArrayList<ItemStack>(ordered.size());
        for (ResearchKey key : ordered) {
            if (mode == JourneyViewState.Mode.FAVOURITE && !ClientFavouriteMirror.contains(ResearchFingerprint.of(key))) continue;
            ItemStack original = byKey.get(key);
            if (original == null) continue;
            try {
                ItemStack display = safePresentation(original);
                if (display == null || display.getItem() == null) continue;
                JourneyPresentationKeyResolver.register(display, key);
                ItemStack nativeRepresentative = representatives.representative(display);
                if (JourneyNeiFilterPipeline.matchesAll(display, nativeRepresentative, activeFilters)) visible.add(display);
            } catch (Throwable ignored) {
                JourneyRuntimeCounters.presentationFailure();
            }
        }
        return visible;
    }

    private static ArrayList<ItemStack> creativeVisible(
        List<ItemStack> authoritative,
        Map<ResearchKey, ItemStack> byKey,
        List<JourneyNeiFilterPipeline.FilterBinding> activeFilters,
        JourneyNativeRepresentativeIndex representatives) {
        ArrayList<ItemStack> union = JourneyCreativeUnion.merge(ItemList.items, authoritative);
        ArrayList<ItemStack> visible = new ArrayList<ItemStack>(union.size());
        for (ItemStack display : union) {
            if (display == null || display.getItem() == null) continue;
            try {
                ResearchKey key = safeKey(display);
                if (key != null && byKey.containsKey(key)) JourneyPresentationKeyResolver.register(display, key);
                ItemStack nativeRepresentative = representatives.representative(display);
                if (JourneyNeiFilterPipeline.matchesAll(display, nativeRepresentative, activeFilters)) visible.add(display);
            } catch (Throwable ignored) {
                JourneyRuntimeCounters.presentationFailure();
            }
        }
        return visible;
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

    static boolean isOwned() { return owned; }
}
