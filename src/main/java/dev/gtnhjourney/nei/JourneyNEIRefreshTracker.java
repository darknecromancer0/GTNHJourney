package dev.gtnhjourney.nei;

import codechicken.nei.ItemList;
import codechicken.nei.SubsetWidget;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;
import dev.gtnhjourney.config.JourneyConfig;

/** Keeps dynamic Journey filters, order and temporary exact variants synchronized with server/client view state. */
public final class JourneyNEIRefreshTracker {

    private long seenResearchRevision = Long.MIN_VALUE;
    private long seenViewRevision = Long.MIN_VALUE;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ClientStackMirror.isSyncing()) return;
        long researchRevision = ClientResearchMirror.revision();
        long viewRevision = JourneyViewState.revision();
        if (researchRevision == seenResearchRevision && viewRevision == seenViewRevision) return;

        // ItemListLoader reads ItemInfo.itemVariants from its worker thread without taking our lock. Never mutate that
        // multimap while a previous NEI item-list load is in progress; simply retry on the next client tick.
        if (!ItemList.loadFinished) return;

        boolean researchChanged = researchRevision != seenResearchRevision;
        JourneyViewState.Mode mode = JourneyViewState.mode();
        JourneyVariantScope scope = JourneyVariantScope.forMode(mode);
        boolean variantsChanged;
        switch (scope) {
            case ALL_RESEARCHED:
                variantsChanged = JourneyNEIVariantBridge.replaceWith(ClientStackMirror.snapshot());
                break;
            case NEWEST_ONLY:
                variantsChanged = JourneyNEIVariantBridge
                    .replaceWith(ClientStackMirror.snapshotNewest(JourneyConfig.newestLimit()));
                break;
            case NONE:
            default:
                variantsChanged = JourneyNEIVariantBridge.clear();
                break;
        }
        boolean orderChanged = JourneyNEIOrderBridge.update(mode);

        seenResearchRevision = researchRevision;
        seenViewRevision = viewRevision;

        if (researchChanged) {
            // Tags were registered once in NEIGTNHJourneyConfig. Re-index their dynamic filters without replacing the
            // tag objects, which preserves NEI's subset UI state and avoids needless tag churn.
            SubsetWidget.updateHiddenItems();
        }

        // Variant changes require a full gather. Pure chronology changes only need NEI's cheaper public reorder task.
        if (variantsChanged) ItemList.loadItems.restart();
        else if (orderChanged) ItemList.refreshItems.restart();
        else ItemList.updateFilter.restart();
    }

    public static void clearInjectedVariants() {
        boolean variantsChanged = JourneyNEIVariantBridge.clear();
        boolean orderChanged = JourneyNEIOrderBridge.reset();
        if (variantsChanged) ItemList.loadItems.restart();
        else if (orderChanged) ItemList.refreshItems.restart();
        else ItemList.updateFilter.restart();
    }
}
