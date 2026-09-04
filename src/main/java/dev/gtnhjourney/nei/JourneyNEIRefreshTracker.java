package dev.gtnhjourney.nei;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.client.ClientActivityMirror;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;

/** Keeps direct Journey panel ownership synchronized with research, activity, view and native NEI filter revisions. */
public final class JourneyNEIRefreshTracker {

    private long seenResearchRevision = Long.MIN_VALUE;
    private long seenActivityRevision = Long.MIN_VALUE;
    private long seenViewRevision = Long.MIN_VALUE;
    private long seenFilterRevision = Long.MIN_VALUE;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ClientStackMirror.isSyncing()) return;

        long researchRevision = ClientResearchMirror.revision();
        long activityRevision = ClientActivityMirror.revision();
        long viewRevision = JourneyViewState.revision();
        long filterRevision = JourneyNeiFilterRevision.revision();
        boolean researchChanged = researchRevision != seenResearchRevision;
        boolean activityChanged = activityRevision != seenActivityRevision;
        boolean viewChanged = viewRevision != seenViewRevision;
        boolean filterChanged = filterRevision != seenFilterRevision;
        boolean researchOrActivityChanged = researchChanged || activityChanged;

        JourneyRefreshDecision.Action action = JourneyRefreshDecision.decide(
            JourneyViewState.mode(),
            researchOrActivityChanged,
            viewChanged,
            filterChanged);
        switch (action) {
            case PANEL_REFRESH:
                JourneyPanelController.refresh(
                    JourneyRefreshDecision.shouldResetPage(researchOrActivityChanged, viewChanged, filterChanged));
                break;
            case PANEL_ENSURE:
                JourneyPanelController.ensureOwned();
                break;
            case NEI_FILTER_REFRESH:
                JourneyPanelController.releaseToNei();
                break;
            case NONE:
            default:
                break;
        }

        seenResearchRevision = researchRevision;
        seenActivityRevision = activityRevision;
        seenViewRevision = viewRevision;
        seenFilterRevision = filterRevision;
    }

    /** Clears Journey-owned client panel state during connection/session teardown without forcing a global NEI reload. */
    public static void resetJourneyPanel() {
        JourneyPanelController.clear();
    }
}
