package dev.gtnhjourney.nei;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientFavouriteMirror;
import dev.gtnhjourney.client.ClientPresentationActivityMirror;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;

/** Keeps Journey/NEI presentation synchronized with content, native filters, sort state and presentation activity. */
public final class JourneyNEIRefreshTracker {

    private long seenResearchRevision = Long.MIN_VALUE;
    private long seenActivityRevision = Long.MIN_VALUE;
    private long seenFavouriteRevision = Long.MIN_VALUE;
    private long seenPresentationRevision = Long.MIN_VALUE;
    private long seenViewRevision = Long.MIN_VALUE;
    private long seenFilterRevision = Long.MIN_VALUE;
    private long seenSortRevision = Long.MIN_VALUE;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ClientStackMirror.isSyncing()) return;

        long researchRevision = ClientResearchMirror.revision();
        long activityRevision = ClientActivityMirror.revision();
        long favouriteRevision = ClientFavouriteMirror.revision();
        long presentationRevision = ClientPresentationActivityMirror.revision();
        long viewRevision = JourneyViewState.revision();
        long filterRevision = JourneyNeiFilterRevision.revision();
        long sortRevision = JourneySortState.revision();

        boolean contentChanged = researchRevision != seenResearchRevision || activityRevision != seenActivityRevision
            || favouriteRevision != seenFavouriteRevision || presentationRevision != seenPresentationRevision;
        boolean viewChanged = viewRevision != seenViewRevision;
        boolean filterChanged = filterRevision != seenFilterRevision;
        boolean sortChanged = sortRevision != seenSortRevision;
        boolean panelWanted = JourneyPanelController.shouldOwnCurrentView();

        if (panelWanted) {
            if (contentChanged || viewChanged || filterChanged || sortChanged) {
                // Activity/content refreshes retain the current page. Deliberate view/filter/sort changes may reset.
                JourneyPanelController.refresh(viewChanged || filterChanged || sortChanged);
            } else {
                JourneyPanelController.ensureOwned();
            }
        } else if (JourneyPanelController.isOwned() || viewChanged || sortChanged) {
            JourneyPanelController.releaseToNei();
        }

        seenResearchRevision = researchRevision;
        seenActivityRevision = activityRevision;
        seenFavouriteRevision = favouriteRevision;
        seenPresentationRevision = presentationRevision;
        seenViewRevision = viewRevision;
        seenFilterRevision = filterRevision;
        seenSortRevision = sortRevision;
    }

    public static void resetJourneyPanel() {
        JourneyPanelController.clear();
        JourneyNeiFilterRevision.reset();
    }
}
