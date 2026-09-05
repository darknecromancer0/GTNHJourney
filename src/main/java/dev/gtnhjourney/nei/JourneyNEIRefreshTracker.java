package dev.gtnhjourney.nei;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientFavouriteMirror;
import dev.gtnhjourney.client.ClientIssuedMirror;
import dev.gtnhjourney.client.ClientPresentationActivityMirror;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;

/** Keeps Journey/NEI presentation synchronized without moving NEI search work onto the client UI thread. */
public final class JourneyNEIRefreshTracker {

    private long seenResearchRevision = Long.MIN_VALUE;
    private long seenStackRevision = Long.MIN_VALUE;
    private long seenActivityRevision = Long.MIN_VALUE;
    private long seenIssuedRevision = Long.MIN_VALUE;
    private long seenFavouriteRevision = Long.MIN_VALUE;
    private long seenPresentationRevision = Long.MIN_VALUE;
    private long seenViewRevision = Long.MIN_VALUE;
    private long seenFilterRevision = Long.MIN_VALUE;
    private long seenSortRevision = Long.MIN_VALUE;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ClientStackMirror.isSyncing()) return;

        long researchRevision = ClientResearchMirror.revision();
        long stackRevision = ClientStackMirror.revision();
        long activityRevision = ClientActivityMirror.revision();
        long issuedRevision = ClientIssuedMirror.revision();
        long favouriteRevision = ClientFavouriteMirror.revision();
        long presentationRevision = ClientPresentationActivityMirror.revision();
        long viewRevision = JourneyViewState.revision();
        long filterRevision = JourneyNeiFilterRevision.revision();
        long sortRevision = JourneySortState.revision();

        boolean contentChanged = researchRevision != seenResearchRevision || stackRevision != seenStackRevision
            || activityRevision != seenActivityRevision || issuedRevision != seenIssuedRevision
            || favouriteRevision != seenFavouriteRevision || presentationRevision != seenPresentationRevision;
        boolean viewChanged = viewRevision != seenViewRevision;
        boolean filterChanged = filterRevision != seenFilterRevision;
        boolean sortChanged = sortRevision != seenSortRevision;
        boolean panelWanted = JourneyPanelController.shouldOwnCurrentView();

        if (panelWanted) {
            if (contentChanged || viewChanged || sortChanged) {
                JourneyPanelController.refresh(viewChanged || filterChanged || sortChanged);
            } else if (filterChanged) {
                // Heavy query/filter/sort work was already completed on NEI Item Filtering's worker thread.
                if (!JourneyPanelController.publishCompletedFilter()) JourneyPanelController.ensureOwned();
            } else {
                JourneyPanelController.ensureOwned();
            }
        } else if (JourneyPanelController.isOwned() || viewChanged || sortChanged) {
            JourneyPanelController.releaseToNei();
        }

        seenResearchRevision = researchRevision;
        seenStackRevision = stackRevision;
        seenActivityRevision = activityRevision;
        seenIssuedRevision = issuedRevision;
        seenFavouriteRevision = favouriteRevision;
        seenPresentationRevision = presentationRevision;
        seenViewRevision = viewRevision;
        seenFilterRevision = filterRevision;
        seenSortRevision = sortRevision;
    }

    public static void resetJourneyPanel() {
        JourneyPanelController.clear();
        JourneyPanelPrecache.clear();
        JourneyNeiFilterRevision.reset();
    }
}
