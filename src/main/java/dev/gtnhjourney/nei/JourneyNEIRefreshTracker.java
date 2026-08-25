package dev.gtnhjourney.nei;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import dev.gtnhjourney.client.ClientResearchMirror;
import dev.gtnhjourney.client.ClientStackMirror;

/** Keeps direct Journey panel ownership synchronized with research and view revisions. */
public final class JourneyNEIRefreshTracker {

    private long seenResearchRevision = Long.MIN_VALUE;
    private long seenViewRevision = Long.MIN_VALUE;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ClientStackMirror.isSyncing()) return;

        long researchRevision = ClientResearchMirror.revision();
        long viewRevision = JourneyViewState.revision();
        boolean researchChanged = researchRevision != seenResearchRevision;
        boolean viewChanged = viewRevision != seenViewRevision;

        JourneyRefreshDecision.Action action = JourneyRefreshDecision
            .decide(JourneyViewState.mode(), researchChanged, viewChanged);
        switch (action) {
            case PANEL_REFRESH:
                JourneyPanelController.refresh(true);
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
        seenViewRevision = viewRevision;
    }

    /** Clears Journey-owned client panel state during connection/session teardown without forcing a global NEI reload. */
    public static void resetJourneyPanel() {
        JourneyPanelController.clear();
    }
}
